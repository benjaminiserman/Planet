package dev.biserman.planet.planet.ecology.v2

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EcologyRuntimeTest {
    @Test
    fun `reserves delay starvation without creating biomass`() {
        val ecology = EcologyCompiler.compile(listOf(landProducer()))
        val niche = nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.PHOTOSYNTHESIS)
        val withoutReserves = TileCommunity().also { it.add(0, niche, activeBiomass = 1_000.0) }
        val withReserves = TileCommunity().also {
            it.add(0, niche, activeBiomass = 1_000.0, reserves = 300.0)
        }
        val runtime = EcologyRuntime(ecology)
        val lethalSeason = landEnvironment(temperatureC = 60.0)

        runtime.advanceSeason(withoutReserves, lethalSeason)
        runtime.advanceSeason(withReserves, lethalSeason)

        assertTrue(withReserves.activeBiomass[0] > withoutReserves.activeBiomass[0])
        assertTrue(withReserves.totalBiomass() <= 1_000.0)
    }

    @Test
    fun `whole body desiccation moves land biomass dormant in a lethal season`() {
        val species = landProducer(
            extraTraits = listOf(CommonTrait.WHOLE_BODY_ANHYDROBIOSIS),
        )
        val ecology = EcologyCompiler.compile(listOf(species))
        val niche = nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.PHOTOSYNTHESIS)
        val community = TileCommunity().also { it.add(0, niche, activeBiomass = 1_000.0) }

        EcologyRuntime(ecology).advanceSeason(community, landEnvironment(temperatureC = 105.0))

        assertTrue(community.dormantBiomass[0] > community.activeBiomass[0])
        assertTrue(community.dormantBiomass[0] > 600.0)
    }

    @Test
    fun `whole body desiccation cannot activate while immersed`() {
        val species = aquaticProducer(
            extraTraits = listOf(CommonTrait.WHOLE_BODY_ANHYDROBIOSIS),
        )
        val ecology = EcologyCompiler.compile(listOf(species))
        val niche = nicheFor(ecology, 0, Habitat.SUNLIT_WATER, EcoStrategy.PHOTOSYNTHESIS)
        val community = TileCommunity().also { it.add(0, niche, activeBiomass = 1_000.0) }

        EcologyRuntime(ecology).advanceSeason(community, oceanEnvironment(temperatureC = 105.0))

        assertEquals(0.0, community.dormantBiomass[0])
    }

    @Test
    fun `reef builders emit cover only from aquatic habitat`() {
        val species = aquaticProducer(extraTraits = listOf(CommonTrait.REEF_BUILDING))
        val ecology = EcologyCompiler.compile(listOf(species))
        val niche = nicheFor(ecology, 0, Habitat.SUNLIT_WATER, EcoStrategy.PHOTOSYNTHESIS)
        val community = TileCommunity().also { it.add(0, niche, activeBiomass = 250_000.0) }
        val fluxes = CellTurnFluxes()

        EcologyRuntime(ecology).advanceSeason(community, oceanEnvironment(), fluxes)

        assertTrue(fluxes.reefCoverDelta > 0.0)
        assertEquals(0.0, fluxes.carrionBiomass)
        assertTrue(fluxes.detritusBiomass > 0.0)
        assertTrue(fluxes.marineSnowBiomass > 0.0)
    }

    @Test
    fun `freshwater sessile deaths create detritus but not carrion or marine snow`() {
        val species = aquaticProducer(extraTraits = listOf(CommonTrait.FRESHWATER_OSMOREGULATION))
        val ecology = EcologyCompiler.compile(listOf(species))
        val niche = nicheFor(ecology, 0, Habitat.FRESHWATER, EcoStrategy.PHOTOSYNTHESIS)
        val community = TileCommunity().also { it.add(0, niche, activeBiomass = 250_000.0) }
        val fluxes = CellTurnFluxes()
        val riverEnvironment = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 21.0,
            annualAverageTemperatureC = 18.0,
            insolation = 0.8,
            precipitationMm = 90.0,
            isLand = true,
            adjacentToMajorRiver = true,
        )

        EcologyRuntime(ecology).advanceSeason(community, riverEnvironment, fluxes)

        assertEquals(0.0, fluxes.carrionBiomass)
        assertTrue(fluxes.detritusBiomass > 0.0)
        assertEquals(0.0, fluxes.marineSnowBiomass)
    }

    @Test
    fun `living motile creatures produce waste and only their deaths produce carrion`() {
        val ecology = EcologyCompiler.compile(listOf(InvariantSpecies.CARPET_PLANTS, grazer()))
        val producer = ecology.speciesIndex(InvariantSpecies.CARPET_PLANTS.id)
        val consumer = ecology.speciesIndex("grazer")
        val producerNiche = nicheFor(ecology, producer, Habitat.LAND_SURFACE, EcoStrategy.PHOTOSYNTHESIS)
        val consumerNiche = nicheFor(ecology, consumer, Habitat.LAND_SURFACE, EcoStrategy.GRAZING)
        val community = TileCommunity().also {
            it.add(producer, producerNiche, activeBiomass = 1_000_000.0)
            it.add(consumer, consumerNiche, activeBiomass = 250_000.0)
        }
        val fluxes = CellTurnFluxes()

        EcologyRuntime(ecology).advanceSeason(community, landEnvironment(), fluxes)

        assertTrue(fluxes.wasteBiomass > 0.0)
        assertTrue(fluxes.carrionBiomass > 0.0)
        assertTrue(fluxes.detritusBiomass > 0.0)
    }

    @Test
    fun `waste absorbing roots increase producer growth when waste is available`() {
        val plain = landProducer(id = "plain")
        val fertilized = landProducer(
            id = "fertilized",
            extraTraits = listOf(CommonTrait.WASTE_ABSORBING_ROOTS),
        )
        fun result(species: SpeciesDefinition): Double {
            val ecology = EcologyCompiler.compile(listOf(species))
            val niche = nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.PHOTOSYNTHESIS)
            val community = TileCommunity().also { it.add(0, niche, activeBiomass = 50_000.0) }
            val environment = landEnvironment().withResources(
                landEnvironment().resources.copy(waste = 0.90),
            )
            EcologyRuntime(ecology).advanceSeason(community, environment)
            return community.totalBiomass()
        }

        assertTrue(result(fertilized) > result(plain))
    }

    @Test
    fun `recyclers emit consumption flux for detritus and waste`() {
        val decomposer = SpeciesDefinition(
            id = "decomposer",
            displayName = "Decomposer",
            sizeClass = SizeClass.SMALL,
            motile = false,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ROOTED_BODY,
                CommonTrait.DECOMPOSING_ENZYMES,
            ),
        )
        val coprophage = SpeciesDefinition(
            id = "coprophage",
            displayName = "Coprophage",
            sizeClass = SizeClass.SMALL,
            motile = true,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ECTOTHERMY,
                CommonTrait.TERRESTRIAL_LOCOMOTION,
                CommonTrait.DUNG_FEEDING_MOUTHPARTS,
            ),
        )
        val ecology = EcologyCompiler.compile(listOf(decomposer, coprophage))
        val community = TileCommunity().also {
            it.add(
                0,
                nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.DECOMPOSITION),
                activeBiomass = 100_000.0,
            )
            it.add(
                1,
                nicheFor(ecology, 1, Habitat.LAND_SURFACE, EcoStrategy.COPROPHAGY),
                activeBiomass = 100_000.0,
            )
        }
        val environment = landEnvironment().withResources(
            landEnvironment().resources.copy(detritus = 0.80, waste = 0.80),
        )
        val fluxes = CellTurnFluxes()

        EcologyRuntime(ecology).advanceSeason(community, environment, fluxes)

        assertTrue(fluxes.detritusConsumedBiomass > 0.0)
        assertTrue(fluxes.wasteConsumedBiomass > 0.0)
    }

    @Test
    fun `reef cover decays without builders`() {
        val ecology = EcologyCompiler.compile(listOf(aquaticProducer()))
        val fluxes = CellTurnFluxes()
        val reef = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 21.0,
            insolation = 0.8,
            precipitationMm = 80.0,
            isLand = false,
            waterDepthM = 80.0,
            reefCover = 0.6,
        )

        EcologyRuntime(ecology).advanceSeason(TileCommunity(), reef, fluxes)

        assertTrue(fluxes.reefCoverDelta < 0.0)
    }

    @Test
    fun `local extinction uses approximate individual count`() {
        val tiny = landProducer(sizeClass = SizeClass.TINY)
        val ecology = EcologyCompiler.compile(listOf(tiny))
        val niche = nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.PHOTOSYNTHESIS)
        val community = TileCommunity().also {
            it.add(0, niche, activeBiomass = tiny.sizeClass.typicalMassKg)
        }

        EcologyRuntime(ecology).advanceSeason(community, landEnvironment(temperatureC = 60.0))

        assertEquals(0, community.size)
        assertTrue(isGloballyExtinct(0, listOf(community)))
    }

    @Test
    fun `seasonal update is independent of population storage order`() {
        val definitions = listOf(
            landProducer(),
            grazer(),
            predator(),
        )
        val ecology = EcologyCompiler.compile(definitions)
        val first = community(ecology, listOf(0, 1, 2))
        val reversed = community(ecology, listOf(2, 1, 0))
        val runtime = EcologyRuntime(ecology)
        val environment = landEnvironment()

        runtime.advanceSeason(first, environment)
        runtime.advanceSeason(reversed, environment)

        definitions.indices.forEach { speciesIndex ->
            val firstPopulation = first.find(speciesIndex)
            val reversedPopulation = reversed.find(speciesIndex)
            assertTrue(firstPopulation >= 0 && reversedPopulation >= 0)
            assertTrue(
                abs(first.activeBiomass[firstPopulation] - reversed.activeBiomass[reversedPopulation]) < 1e-8,
            )
        }
    }

    @Test
    fun `long unchanged run stays finite non-negative and bounded`() {
        val ecology = EcologyCompiler.compile(listOf(landProducer(), grazer(), predator()))
        val community = community(ecology, listOf(0, 1, 2))
        val runtime = EcologyRuntime(ecology)
        val environment = landEnvironment()

        repeat(400) {
            runtime.advanceSeason(community, environment)
            for (index in 0 until community.size) {
                assertTrue(community.activeBiomass[index].isFinite())
                assertTrue(community.activeBiomass[index] >= 0.0)
                assertTrue(community.reserves[index].isFinite())
                assertTrue(community.dormantBiomass[index].isFinite())
            }
        }

        assertTrue(community.totalBiomass() < 1e12)
        assertFalse(community.size > 3)
    }

    @Test
    fun `large predator becomes locally extinct after its last modeled prey disappears`() {
        val ecology = EcologyCompiler.compile(listOf(landProducer(), grazer(), predator()))
        val community = community(ecology, listOf(0, 1, 2))
        val runtime = EcologyRuntime(ecology)
        val environment = landEnvironment()

        val grazerIndex = community.find(1)
        assertTrue(grazerIndex >= 0)
        community.removeAt(grazerIndex)

        repeat(400) {
            runtime.advanceSeason(community, environment)
        }

        assertEquals(-1, community.find(2))
    }

    @Test
    fun `large predator persists when abundant modeled prey is present`() {
        val ecology = EcologyCompiler.compile(listOf(landProducer(), grazer(), predator(), scavenger()))
        val environment = landEnvironment()
        val community = TileCommunity().also {
            listOf(2_000_000.0, 700_000.0, 220_000.0, 90_000.0).forEachIndexed { speciesIndex, biomass ->
                val niche = NicheSelection.choose(ecology.species[speciesIndex], ecology, environment)
                it.add(speciesIndex, niche, biomass, reserves = biomass * 0.12)
            }
        }
        val runtime = EcologyRuntime(ecology)

        repeat(400) {
            runtime.advanceSeason(community, environment)
        }

        val predatorIndex = community.find(2)
        assertTrue(predatorIndex >= 0)
        assertTrue(community.activeBiomass[predatorIndex] >= predator().sizeClass.typicalMassKg * 10.0)
    }

    private fun community(ecology: CompiledEcology, order: List<Int>): TileCommunity =
        TileCommunity().also { community ->
            order.forEach { speciesIndex ->
                val species = ecology.species[speciesIndex]
                val niche = NicheSelection.choose(species, ecology, landEnvironment())
                community.add(speciesIndex, niche, activeBiomass = 250_000.0)
            }
        }

    private fun nicheFor(
        ecology: CompiledEcology,
        speciesIndex: Int,
        habitat: Habitat,
        strategy: EcoStrategy,
    ): Int {
        val index = ecology.niches.indexOf(NicheDefinition(habitat, strategy))
        assertTrue(index >= 0)
        assertTrue(ecology.species[speciesIndex].nicheFit[index] > 0.0)
        return index
    }

    private fun landProducer(
        sizeClass: SizeClass = SizeClass.SMALL,
        extraTraits: List<SpeciesTrait> = emptyList(),
        id: String = "land-producer",
    ) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = sizeClass,
        motile = false,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
        ) + extraTraits,
        photosyntheticColor = BiologicalColor.GREEN,
    )

    private fun aquaticProducer(
        extraTraits: List<SpeciesTrait> = emptyList(),
    ) = SpeciesDefinition(
        id = "aquatic-producer",
        displayName = "Aquatic producer",
        sizeClass = SizeClass.SMALL,
        motile = false,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.BUOYANCY_BLADDER,
        ) + extraTraits,
        photosyntheticColor = BiologicalColor.BLUE_GREEN,
    )

    private fun grazer() = SpeciesDefinition(
        id = "grazer",
        displayName = "Grazer",
        sizeClass = SizeClass.MEDIUM,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.FAT_RESERVES,
        ),
        camouflageColor = BiologicalColor.BROWN,
    )

    private fun predator() = SpeciesDefinition(
        id = "predator",
        displayName = "Predator",
        sizeClass = SizeClass.LARGE,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.CAMOUFLAGE_PATTERN,
        ),
        camouflageColor = BiologicalColor.BROWN,
    )

    private fun scavenger() = SpeciesDefinition(
        id = "scavenger",
        displayName = "Scavenger",
        sizeClass = SizeClass.MEDIUM,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.SCAVENGING_SENSES,
        ),
        camouflageColor = BiologicalColor.BLACK,
    )

    private fun landEnvironment(temperatureC: Double = 21.0) =
        SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = temperatureC,
            annualAverageTemperatureC = 18.0,
            insolation = 0.8,
            precipitationMm = 90.0,
            surfaceFertilityModifier = 0.7,
            isLand = true,
            resources = FunctionalResources(
                carrion = 0.30,
            ),
        )

    private fun oceanEnvironment(temperatureC: Double = 21.0) =
        SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = temperatureC,
            annualAverageTemperatureC = 18.0,
            insolation = 0.8,
            precipitationMm = 90.0,
            surfaceFertilityModifier = 0.7,
            isLand = false,
            waterDepthM = 80.0,
            usefulSunlightReachesWater = true,
            resources = FunctionalResources(
                marineSnow = 0.25,
            ),
        )
}
