package dev.biserman.planet.planet.ecology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcologyMovementTest {
    @Test
    fun `neighbor dispersal establishes a viable adjacent rescue population`() {
        val ecology = EcologyCompiler.compile(listOf(landDisperser()))
        val environments = arrayOf(land(), land(), land())
        val communities = emptyCommunities(3)
        val niche = NicheSelection.choose(ecology.species.single(), ecology, environments[0])
        communities[0].add(0, niche, activeBiomass = 10_000.0)
        val plan = CompiledMovementPlan.compile(ecology, tileCount = 3, routes = emptyList())

        EcologyMovement.applySeason(
            ecology = ecology,
            runtime = EcologyRuntime(ecology),
            communities = communities,
            environments = environments,
            neighbors = arrayOf(intArrayOf(1), intArrayOf(0, 2), intArrayOf(1)),
            seasonIndex = 0,
            movementPlan = plan,
            scratch = MovementScratch(maximumTransfers = 3, nicheCount = ecology.niches.size),
        )

        assertTrue(communities[1].find(0) >= 0)
        assertEquals(-1, communities[2].find(0))
    }

    @Test
    fun `cached seasonal migration follows its fixed destination`() {
        val ecology = EcologyCompiler.compile(listOf(migrant()))
        val environments = arrayOf(land(), land(), land())
        val communities = emptyCommunities(3)
        val niche = NicheSelection.choose(ecology.species.single(), ecology, environments[0])
        communities[0].add(0, niche, activeBiomass = 10_000.0, reserves = 2_000.0)
        val routes = listOf(
            SeasonalRouteDefinition(
                speciesId = "migrant",
                destinationsBySeason = listOf(
                    intArrayOf(2, -1, -1),
                    intArrayOf(-1, -1, 0),
                    intArrayOf(-1, -1, -1),
                    intArrayOf(-1, -1, -1),
                ),
            ),
        )
        val plan = CompiledMovementPlan.compile(ecology, tileCount = 3, routes = routes)

        EcologyMovement.applySeason(
            ecology = ecology,
            runtime = EcologyRuntime(ecology),
            communities = communities,
            environments = environments,
            neighbors = arrayOf(intArrayOf(1), intArrayOf(0, 2), intArrayOf(1)),
            seasonIndex = 0,
            movementPlan = plan,
            scratch = MovementScratch(maximumTransfers = 3, nicheCount = ecology.niches.size),
        )

        assertTrue(communities[2].find(0) >= 0)
        assertEquals(-1, communities[1].find(0))
        assertTrue(communities[0].reserves[0] < 2_000.0)
    }

    @Test
    fun `freshwater specialist cannot radiate into a dry land cell`() {
        val ecology = EcologyCompiler.compile(listOf(freshwaterDisperser()))
        val environments = arrayOf(land(majorRiver = true), land(majorRiver = false))
        val communities = emptyCommunities(2)
        val niche = NicheSelection.choose(ecology.species.single(), ecology, environments[0])
        assertTrue(niche >= 0)
        communities[0].add(0, niche, activeBiomass = 10_000.0)
        val plan = CompiledMovementPlan.compile(ecology, tileCount = 2, routes = emptyList())

        EcologyMovement.applySeason(
            ecology = ecology,
            runtime = EcologyRuntime(ecology),
            communities = communities,
            environments = environments,
            neighbors = arrayOf(intArrayOf(1), intArrayOf(0)),
            seasonIndex = 0,
            movementPlan = plan,
            scratch = MovementScratch(maximumTransfers = 2, nicheCount = ecology.niches.size),
        )

        assertEquals(-1, communities[1].find(0))
    }

    @Test
    fun `population radiation is conservative and reaches only one neighbor per season`() {
        val ecology = EcologyCompiler.compile(listOf(landDisperser()))
        val environments = arrayOf(land(), land(), land())
        val niche = NicheSelection.choose(ecology.species.single(), ecology, environments[0])
        var radiated: Array<TileCommunity>? = null

        for (season in 0L..500L) {
            val communities = emptyCommunities(3)
            communities[0].add(0, niche, activeBiomass = 10_000.0, reserves = 1_000.0)
            EcologyMovement.applyRadiation(
                ecology = ecology,
                communities = communities,
                environments = environments,
                neighbors = arrayOf(intArrayOf(1), intArrayOf(0, 2), intArrayOf(1)),
                seasonIndex = season,
                planetSeed = 42,
                scratch = MovementScratch(maximumTransfers = 3, nicheCount = ecology.niches.size),
            )
            if (communities[1].find(0) >= 0) {
                radiated = communities
                break
            }
        }

        val communities = requireNotNull(radiated)
        assertEquals(-1, communities[2].find(0))
        assertEquals(
            10_000.0,
            communities.sumOf { community ->
                val population = community.find(0)
                if (population < 0) 0.0 else community.activeBiomass[population]
            },
            absoluteTolerance = 1e-9,
        )
        assertEquals(
            1_000.0,
            communities.sumOf { community ->
                val population = community.find(0)
                if (population < 0) 0.0 else community.reserves[population]
            },
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun `radiation establishment gate can reject an otherwise viable founder`() {
        val ecology = EcologyCompiler.compile(listOf(landDisperser()))
        val environments = arrayOf(land(), land())
        val communities = emptyCommunities(2)
        val niche = NicheSelection.choose(ecology.species.single(), ecology, environments[0])
        communities[0].add(0, niche, activeBiomass = 10_000.0, reserves = 1_000.0)

        EcologyMovement.applyRadiation(
            ecology = ecology,
            communities = communities,
            environments = environments,
            neighbors = arrayOf(intArrayOf(1), intArrayOf(0)),
            seasonIndex = 0,
            planetSeed = 42,
            scratch = MovementScratch(maximumTransfers = 2, nicheCount = ecology.niches.size),
            config = EcologyRuntimeConfig(
                unassistedRadiationChancePerSeason = 1.0,
                migrationRadiationChancePerSeason = 1.0,
                neighborRadiationChancePerSeason = 1.0,
            ),
            canEstablish = { _, _, _ -> false },
        )

        assertEquals(-1, communities[1].find(0))
        assertEquals(10_000.0, communities[0].activeBiomass[0])
        assertEquals(1_000.0, communities[0].reserves[0])
    }

    private fun emptyCommunities(count: Int) = Array(count) { TileCommunity() }

    private fun land(majorRiver: Boolean = false) = SeasonalCellEnvironment.create(
        areaKm2 = 40_000.0,
        temperatureC = 20.0,
        insolation = 0.8,
        precipitationMm = 80.0,
        isLand = true,
        adjacentToMajorRiver = if (majorRiver) 1.0 else 0.0,
        resources = FunctionalResources(),
    )

    private fun landDisperser() = SpeciesDefinition(
        id = "land-disperser",
        displayName = "Land disperser",
        sizeClass = SizeClass.SMALL,
        motile = false,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.NEIGHBOR_DISPERSAL,
        ),
        photosyntheticColor = BiologicalColor.GREEN,
    )

    private fun migrant() = SpeciesDefinition(
        id = "migrant",
        displayName = "Migrant",
        sizeClass = SizeClass.MEDIUM,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.SHORT_MIGRATION,
        ),
    )

    private fun freshwaterDisperser() = SpeciesDefinition(
        id = "freshwater-disperser",
        displayName = "Freshwater disperser",
        sizeClass = SizeClass.SMALL,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.GILL_PADS,
            CommonTrait.NEIGHBOR_DISPERSAL,
        ),
    )
}