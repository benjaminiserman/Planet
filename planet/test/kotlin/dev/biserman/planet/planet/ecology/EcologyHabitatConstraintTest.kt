package dev.biserman.planet.planet.ecology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EcologyHabitatConstraintTest {
    private val catalogEcology = EcologyCompiler.compile(
        EarthSpeciesCatalog.ALL + InvariantSpecies.ALL,
    )

    @Test
    fun `light-dependent coral loses habitat fit with depth`() {
        val coral = catalogEcology.species.single { it.id == "staghorn-coral" }
        val shallow = ocean(depthM = 20.0)
        val marginal = ocean(depthM = 55.0)
        val deep = ocean(depthM = 100.0)

        assertEquals(
            1.0,
            EcologyFitness.habitat(coral, shallow, Habitat.SUNLIT_WATER),
        )
        assertTrue(
            EcologyFitness.habitat(coral, marginal, Habitat.SUNLIT_WATER) in 0.0..1.0,
        )
        assertEquals(
            0.0,
            EcologyFitness.habitat(coral, deep, Habitat.SUNLIT_WATER),
        )
        assertTrue(NicheSelection.choose(coral, catalogEcology, shallow) >= 0)
        assertEquals(-1, NicheSelection.choose(coral, catalogEcology, deep))
    }

    @Test
    fun `permanent sea ice is a distinct coastal surface habitat`() {
        val withoutIce = ocean(depthM = 250.0, permanentSeaIce = false, adjacentToLand = 1.0)
        val offshoreIce = ocean(depthM = 250.0, permanentSeaIce = true, adjacentToLand = 0.0)
        val coastalIce = ocean(depthM = 250.0, permanentSeaIce = true, adjacentToLand = 1.0)
        val penguin = catalogEcology.species.single { it.id == "emperor-penguin" }
        val polarBear = catalogEcology.species.single { it.id == "polar-bear" }

        assertEquals(0.0, withoutIce.habitatAvailability(Habitat.SEA_ICE))
        assertTrue(coastalIce.habitatAvailability(Habitat.SEA_ICE) > 0.0)
        assertEquals(-1, NicheSelection.choose(penguin, catalogEcology, withoutIce))
        assertEquals(-1, NicheSelection.choose(penguin, catalogEcology, offshoreIce))
        assertEquals(
            Habitat.SEA_ICE,
            catalogEcology.niches[NicheSelection.choose(penguin, catalogEcology, coastalIce)].habitat,
        )

        assertTrue(polarBear.niche.supportFor(Habitat.SEA_ICE) > 0.0)
        assertEquals(0.0, polarBear.niche.supportFor(Habitat.SUNLIT_WATER))
        assertEquals(0.0, polarBear.niche.supportFor(Habitat.DARK_WATER))
    }

    @Test
    fun `sea-ice penguins can prey across the ice-water boundary`() {
        val definitions = listOf(
            EarthSpeciesCatalog.BIRDS.single { it.id == "emperor-penguin" },
            InvariantSpecies.SMALL_AQUATIC_LIFE,
        )
        val ecology = EcologyCompiler.compile(definitions)
        val penguin = ecology.speciesIndex("emperor-penguin")
        val aquaticPrey = ecology.speciesIndex(InvariantSpecies.SMALL_AQUATIC_LIFE.id)

        assertEquals(InteractionKind.PREDATION, ecology.interactions.get(penguin, aquaticPrey).kind)
    }

    @Test
    fun `open ocean requires underwater breathing or prolonged breath holding`() {
        val blueWhale = catalogEcology.species.single { it.id == "blue-whale" }
        val greatWhiteShark = catalogEcology.species.single { it.id == "great-white-shark" }
        val manatee = catalogEcology.species.single { it.id == "west-indian-manatee" }
        val seaOtter = catalogEcology.species.single { it.id == "sea-otter" }
        val offshoreOcean = ocean(depthM = 250.0)
        val coastalOcean = ocean(depthM = 45.0, adjacentToLand = 1.0)

        assertTrue(blueWhale.physiology.respiration.prolongedBreathHolding)
        assertFalse(blueWhale.physiology.respiration.underwaterBreathing)
        assertTrue(blueWhale.niche.supportFor(Habitat.SUNLIT_WATER) > 0.0)

        assertTrue(greatWhiteShark.physiology.respiration.underwaterBreathing)
        assertFalse(greatWhiteShark.physiology.respiration.prolongedBreathHolding)
        assertTrue(greatWhiteShark.niche.supportFor(Habitat.SUNLIT_WATER) > 0.0)

        for (coastalDiver in listOf(manatee, seaOtter)) {
            assertFalse(coastalDiver.physiology.respiration.underwaterBreathing)
            assertFalse(coastalDiver.physiology.respiration.prolongedBreathHolding)
            assertTrue(coastalDiver.niche.supportFor(Habitat.COASTAL) > 0.0)
            assertEquals(0.0, coastalDiver.niche.supportFor(Habitat.SUNLIT_WATER))
            assertEquals(0.0, coastalDiver.niche.supportFor(Habitat.DARK_WATER))
            assertEquals(-1, NicheSelection.choose(coastalDiver, catalogEcology, offshoreOcean))
            assertTrue(NicheSelection.choose(coastalDiver, catalogEcology, coastalOcean) >= 0)
        }
    }

    @Test
    fun `every species with open-ocean habitat support can respire there`() {
        for (species in catalogEcology.species) {
            val hasOpenOceanSupport =
                species.niche.supportFor(Habitat.SUNLIT_WATER) > 0.0 ||
                    species.niche.supportFor(Habitat.DARK_WATER) > 0.0
            if (hasOpenOceanSupport) {
                assertTrue(
                    species.physiology.respiration.underwaterBreathing || species.physiology.respiration.prolongedBreathHolding,
                    "${species.displayName} has open-ocean support without a qualifying respiration trait",
                )
            }
        }
    }

    @Test
    fun `pandas and koalas consume only their obligate authored plants`() {
        val panda = catalogEcology.speciesIndex("giant-panda")
        val bamboo = catalogEcology.speciesIndex("giant-bamboo")
        val koala = catalogEcology.speciesIndex("koala")
        val eucalyptus = catalogEcology.speciesIndex("eucalyptus")
        val oak = catalogEcology.speciesIndex("english-oak")

        assertEquals(InteractionKind.GRAZING, catalogEcology.interactions.get(panda, bamboo).kind)
        assertTrue(catalogEcology.interactions.get(panda, bamboo).targetRequired)
        assertEquals(InteractionKind.NONE, catalogEcology.interactions.get(panda, oak).kind)

        assertEquals(InteractionKind.GRAZING, catalogEcology.interactions.get(koala, eucalyptus).kind)
        assertTrue(catalogEcology.interactions.get(koala, eucalyptus).targetRequired)
        assertEquals(InteractionKind.NONE, catalogEcology.interactions.get(koala, oak).kind)

        val completed = EcologyAssembly.completeRequiredTargets(
            catalogEcology,
            selected = listOf(catalogEcology.species[panda]),
            availableTargets = catalogEcology.species,
        )
        assertTrue(completed.any { it.id == "giant-bamboo" })

        val impossible = EcologyAssembly.completeRequiredTargets(
            catalogEcology,
            selected = listOf(catalogEcology.species[panda]),
            availableTargets = listOf(catalogEcology.species[oak]),
        )
        assertFalse(impossible.any { it.id == "giant-panda" })
    }

    private fun ocean(
        depthM: Double,
        permanentSeaIce: Boolean = false,
        adjacentToLand: Double = 0.0,
    ) = SeasonalCellEnvironment.create(
        areaKm2 = 40_000.0,
        temperatureC = if (permanentSeaIce) -4.0 else 24.0,
        annualAverageTemperatureC = if (permanentSeaIce) -5.0 else 24.0,
        insolation = 0.8,
        precipitationMm = 800.0,
        isLand = false,
        adjacentToLand = adjacentToLand,
        waterDepthM = depthM,
        usefulSunlightReachesWater = true,
        permanentSeaIce = permanentSeaIce,
    )
}