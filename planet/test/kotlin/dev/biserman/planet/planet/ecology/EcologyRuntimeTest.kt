package dev.biserman.planet.planet.ecology

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EcologyRuntimeTest {
    @Test
    fun `nectar feeding consumes seasonal flowers and pollen carrying returns a benefit`() {
        fun visitor(id: String, pollinator: Boolean) = SpeciesDefinition(
            id = id,
            displayName = id,
            sizeClass = SizeClass.TINY,
            motile = true,
            traits = buildList {
                add(CommonTrait.TEMPERATE_BIOCHEMISTRY)
                add(CommonTrait.ECTOTHERMY)
                add(CommonTrait.POWERED_FLIGHT)
                add(CommonTrait.NECTAR_SIPPING_TONGUE)
                if (pollinator) add(CommonTrait.POLLEN_CARRYING_SURFACES)
            },
        )
        val flower = landProducer(
            extraTraits = listOf(CommonTrait.FLOWERS, CommonTrait.NECTARIES),
        )

        fun fluxesFor(pollinator: Boolean): CellTurnFluxes {
            val ecology = EcologyCompiler.compile(listOf(flower, visitor("visitor", pollinator)))
            val environment = landEnvironment()
            val community = TileCommunity().apply {
                ecology.species.forEach { species ->
                    add(
                        species.index,
                        NicheSelection.choose(species, ecology, environment),
                        activeBiomass = if (species.motile) 100_000.0 else 10_000_000.0,
                    )
                }
            }
            return CellTurnFluxes().also { fluxes ->
                EcologyRuntime(ecology).advanceSeason(
                    community,
                    environment,
                    fluxes,
                    finalizeExtinctions = false,
                )
            }
        }

        val robberFluxes = fluxesFor(pollinator = false)
        val pollinatorFluxes = fluxesFor(pollinator = true)
        assertTrue(robberFluxes.nectarBiomass > 0.0)
        assertTrue(robberFluxes.nectarConsumedBiomass > 0.0)
        assertEquals(0.0, robberFluxes.pollinationBenefitBiomass)
        assertTrue(pollinatorFluxes.nectarConsumedBiomass > 0.0)
        assertTrue(pollinatorFluxes.pollinationBenefitBiomass > 0.0)
    }

    @Test
    fun `suspended plankton biomass does not crowd an attached freshwater producer`() {
        val waterLily = EarthSpeciesCatalog.ALL.single { it.id == "white-water-lily" }
        val environment = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 24.0,
            annualAverageTemperatureC = 22.0,
            insolation = 0.8,
            precipitationMm = 100.0,
            surfaceFertilityModifier = 0.5,
            isLand = true,
            adjacentToMajorRiver = 1.0,
        )

        fun lilyBiomassAfterSeason(withPlankton: Boolean): Double {
            val ecology = EcologyCompiler.compile(
                if (withPlankton) {
                    listOf(waterLily, InvariantSpecies.PLANKTON)
                } else {
                    listOf(waterLily)
                },
            )
            val lily = ecology.species.single { it.id == waterLily.id }
            val lilyNiche = NicheSelection.choose(lily, ecology, environment)
            val community = TileCommunity().apply {
                add(lily.index, lilyNiche, 1.0e9)
                if (withPlankton) {
                    val plankton = ecology.species.single { it.id == InvariantSpecies.PLANKTON.id }
                    add(
                        plankton.index,
                        NicheSelection.choose(plankton, ecology, environment),
                        1.0e11,
                    )
                }
            }
            EcologyRuntime(ecology).advanceSeason(community, environment)
            val population = community.find(lily.index)
            return if (population < 0) 0.0 else community.activeBiomass[population]
        }

        assertEquals(
            lilyBiomassAfterSeason(withPlankton = false),
            lilyBiomassAfterSeason(withPlankton = true),
            1.0e-6,
        )
    }

    @Test
    fun `aposematic prey require a same-color dangerous model in the same habitat`() {
        fun prey(
            id: String,
            color: BiologicalColor,
            dangerous: Boolean = false,
        ) = SpeciesDefinition(
            id = id,
            displayName = id,
            sizeClass = SizeClass.TINY,
            motile = true,
            traits = listOfNotNull(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ECTOTHERMY,
                CommonTrait.TERRESTRIAL_LOCOMOTION,
                CommonTrait.GRAZING_MOUTHPARTS,
                CommonTrait.APOSEMATIC_COLORATION,
                CommonTrait.TOXIC_SKIN.takeIf { dangerous },
            ),
            camouflageColor = color,
        )
        val predator = SpeciesDefinition(
            id = "warning-test-predator",
            displayName = "Warning test predator",
            sizeClass = SizeClass.SMALL,
            motile = true,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ENDOTHERMY,
                CommonTrait.TERRESTRIAL_LOCOMOTION,
                CommonTrait.AMBUSH_MUSCULATURE,
            ),
            camouflageColor = BiologicalColor.BROWN,
        )
        val dangerousRedModel = prey("dangerous-red-model", BiologicalColor.RED, dangerous = true)
        val redMimic = prey("red-mimic", BiologicalColor.RED)
        val blueMimic = prey("blue-mimic", BiologicalColor.BLUE)
        val ecology = EcologyCompiler.compile(
            listOf(predator, dangerousRedModel, redMimic, blueMimic),
        )
        val predatorNiche = nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.AMBUSH_PREDATION)
        val redModelNiche = nicheFor(ecology, 1, Habitat.LAND_SURFACE, EcoStrategy.GRAZING)
        val redMimicNiche = nicheFor(ecology, 2, Habitat.LAND_SURFACE, EcoStrategy.GRAZING)
        val blueMimicNiche = nicheFor(ecology, 3, Habitat.LAND_SURFACE, EcoStrategy.GRAZING)
        val withModel = TileCommunity().also {
            it.add(0, predatorNiche, activeBiomass = 1_000.0)
            it.add(1, redModelNiche, activeBiomass = 1.0)
            it.add(2, redMimicNiche, activeBiomass = 100.0)
            it.add(3, blueMimicNiche, activeBiomass = 100.0)
        }
        val withoutModel = TileCommunity().also {
            it.add(0, predatorNiche, activeBiomass = 1_000.0)
            it.add(2, redMimicNiche, activeBiomass = 100.0)
            it.add(3, blueMimicNiche, activeBiomass = 100.0)
        }
        val runtime = EcologyRuntime(ecology)

        runtime.advanceSeason(withModel, landEnvironment(), finalizeExtinctions = false)
        runtime.advanceSeason(withoutModel, landEnvironment(), finalizeExtinctions = false)

        assertTrue(withModel.activeBiomass[2] > withModel.activeBiomass[3])
        assertEquals(withoutModel.activeBiomass[1], withoutModel.activeBiomass[2], 1.0e-9)
    }

    @Test
    fun `living fruit-bearing producers replenish fruit consumed by frugivores`() {
        val fig = EarthSpeciesCatalog.ALL.single { it.id == "strangler-fig" }
        val flyingFox = EarthSpeciesCatalog.ALL.single { it.id == "large-flying-fox" }
        val ecology = EcologyCompiler.compile(listOf(fig, flyingFox))
        val environment = landEnvironment().withResources(FunctionalResources(fruit = 0.80))
        val figNiche = nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.PHOTOSYNTHESIS)
        val batNiche = nicheFor(ecology, 1, Habitat.AERIAL, EcoStrategy.FRUGIVORY)
        val community = TileCommunity().also {
            it.add(0, figNiche, activeBiomass = 1.0e9)
            it.add(1, batNiche, activeBiomass = 1.0e6)
        }
        val fluxes = CellTurnFluxes()

        EcologyRuntime(ecology).advanceSeason(community, environment, fluxes)

        assertTrue(fluxes.fruitBiomass > 0.0)
        assertTrue(fluxes.fruitConsumedBiomass > 0.0)
    }

    @Test
    fun `terrestrial animal above its lethal elevation suffers lethal mortality`() {
        val ecology = EcologyCompiler.compile(listOf(grazer()))
        val niche = nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.GRAZING)
        val lowland = TileCommunity().also { it.add(0, niche, activeBiomass = 1_000.0) }
        val highland = TileCommunity().also { it.add(0, niche, activeBiomass = 1_000.0) }
        val runtime = EcologyRuntime(ecology)

        runtime.advanceSeason(lowland, landEnvironment(elevationM = 2_000.0))
        runtime.advanceSeason(highland, landEnvironment(elevationM = 3_000.0))

        assertTrue(highland.activeBiomass[0] < lowland.activeBiomass[0] * 0.25)
    }

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
    fun `cold dark leaf dormancy does not shelter an oak from a hot desert`() {
        val oak = EarthSpeciesCatalog.PRODUCERS_AND_FUNGI.single { it.id == "english-oak" }
        val ecology = EcologyCompiler.compile(listOf(oak))
        val initialEnvironment = landEnvironment(temperatureC = 24.0)
        val niche = NicheSelection.choose(ecology.species.single(), ecology, initialEnvironment)
        val community = TileCommunity().also {
            it.add(0, niche, activeBiomass = 1.0e9)
        }
        val desert = HersfeldtClimatePresets.DESERT

        repeat(400) { season ->
            val sample = desert.climateDatum(13).sampleAt(season / 4.0)
            val environment = SeasonalCellEnvironment.create(
                areaKm2 = 40_000.0,
                temperatureC = sample.averageTemperature,
                annualAverageTemperatureC = desert.months.map { it.averageTemperature }.average(),
                insolation = (sample.insolation / 345.0).coerceIn(0.0, 1.0),
                precipitationMm = sample.precipitation,
                isLand = true,
            )
            EcologyRuntime(ecology).advanceSeason(community, environment)
        }

        assertEquals(-1, community.find(0))
    }

    @Test
    fun `cold dark leaf dormancy still protects an oak in winter`() {
        val oak = EarthSpeciesCatalog.PRODUCERS_AND_FUNGI.single { it.id == "english-oak" }
        val ecology = EcologyCompiler.compile(listOf(oak))
        val mild = landEnvironment(temperatureC = 18.0)
        val niche = NicheSelection.choose(ecology.species.single(), ecology, mild)
        val community = TileCommunity().also {
            it.add(0, niche, activeBiomass = 1.0e6)
        }
        val winter = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 8.0,
            annualAverageTemperatureC = 10.0,
            insolation = 0.20,
            precipitationMm = 80.0,
            isLand = true,
        )

        EcologyRuntime(ecology).advanceSeason(community, winter)

        assertTrue(community.dormantBiomass[0] > community.activeBiomass[0])
    }

    @Test
    fun `carpet plants disappear without any thawed season`() {
        val ecology = EcologyCompiler.compile(listOf(InvariantSpecies.CARPET_PLANTS))
        val species = ecology.species.single()
        val niche = NicheSelection.choose(species, ecology, landEnvironment())
        val community = TileCommunity().also {
            it.add(species.index, niche, activeBiomass = 1.0e9)
        }
        val permanentlyFrozen = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = -5.0,
            annualAverageTemperatureC = -5.0,
            insolation = 0.45,
            precipitationMm = 20.0,
            isLand = true,
        )
        val runtime = EcologyRuntime(ecology)

        repeat(1_600) {
            runtime.advanceSeason(community, permanentlyFrozen)
        }

        assertEquals(-1, community.find(species.index))
    }

    @Test
    fun `polar plankton overwinter as a small resting stock and regrow below tropical biomass`() {
        val ecology = EcologyCompiler.compile(listOf(InvariantSpecies.PLANKTON))
        val species = ecology.species.single()
        val tropicalEnvironment = oceanEnvironment(temperatureC = 25.0)
        val niche = NicheSelection.choose(species, ecology, tropicalEnvironment)
        val tropical = TileCommunity().also {
            it.add(species.index, niche, activeBiomass = 1.0e10)
        }
        val polar = TileCommunity().also {
            it.add(species.index, niche, activeBiomass = 1.0e10)
        }
        val polarYear = listOf(
            polarOceanEnvironment(
                temperatureC = -8.0,
                insolation = 0.0,
                usefulSunlightReachesWater = false,
            ),
            polarOceanEnvironment(
                temperatureC = -5.0,
                insolation = 0.01,
                usefulSunlightReachesWater = false,
            ),
            polarOceanEnvironment(
                temperatureC = 2.0,
                insolation = 0.55,
                usefulSunlightReachesWater = true,
            ),
            polarOceanEnvironment(
                temperatureC = 7.0,
                insolation = 0.95,
                usefulSunlightReachesWater = true,
            ),
        )
        val tropicalRuntime = EcologyRuntime(ecology)
        val polarRuntime = EcologyRuntime(ecology)
        var lateWinterBiomass = 0.0
        var lateSummerBiomass = 0.0

        repeat(400) { year ->
            repeat(4) {
                tropicalRuntime.advanceSeason(tropical, tropicalEnvironment)
            }
            polarYear.forEachIndexed { season, environment ->
                val beforeSeason = polar.totalBiomass()
                polarRuntime.advanceSeason(polar, environment)
                assertTrue(
                    polar.find(species.index) >= 0,
                    "Polar plankton went locally extinct in year $year season $season " +
                        "from ${"%.3e".format(beforeSeason)} kg",
                )
                if (season == 1) lateWinterBiomass = polar.totalBiomass()
                if (season == 3) lateSummerBiomass = polar.totalBiomass()
            }
        }

        val tropicalBiomass = tropical.totalBiomass()
        assertTrue(lateWinterBiomass > 0.0)
        assertTrue(
            lateSummerBiomass > lateWinterBiomass * 2.0,
            "winter=${"%.3e".format(lateWinterBiomass)} " +
                "summer=${"%.3e".format(lateSummerBiomass)}",
        )
        assertTrue(
            lateSummerBiomass < tropicalBiomass * 0.25,
            "polar=${"%.3e".format(lateSummerBiomass)} " +
                "tropical=${"%.3e".format(tropicalBiomass)}",
        )
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
            adjacentToMajorRiver = 1.0,
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
    fun `soft habitat diversity pressure removes weak populations before the hard cap`() {
        val definitions = List(24) { index -> landProducer(id = "producer-$index") }
        val ecology = EcologyCompiler.compile(definitions)
        val environment = landEnvironment()
        val niche = nicheFor(ecology, 0, Habitat.LAND_SURFACE, EcoStrategy.PHOTOSYNTHESIS)
        val community = TileCommunity().also { community ->
            definitions.indices.forEach { speciesIndex ->
                community.add(
                    speciesIndex = speciesIndex,
                    nicheIndex = niche,
                    activeBiomass = if (speciesIndex == 0) 1_000_000_000.0 else 10_000.0,
                )
            }
        }

        val runtime = EcologyRuntime(ecology)
        repeat(200) {
            runtime.advanceSeason(community, environment)
        }

        assertTrue(community.size < definitions.size)
        assertTrue(community.size < 48)
        assertTrue(community.find(0) >= 0)
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
        assertTrue(ecology.species[speciesIndex].niche.fitFor(index) > 0.0)
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
            CommonTrait.DIFFUSIVE_AQUATIC_GAS_EXCHANGE,
        ) + extraTraits,
        photosyntheticColor = BiologicalColor.BLUE,
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

    private fun landEnvironment(
        temperatureC: Double = 21.0,
        elevationM: Double = 0.0,
    ) =
        SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            elevationM = elevationM,
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

    private fun polarOceanEnvironment(
        temperatureC: Double,
        insolation: Double,
        usefulSunlightReachesWater: Boolean,
    ) =
        SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = temperatureC,
            annualAverageTemperatureC = -1.0,
            insolation = insolation,
            precipitationMm = 30.0,
            isLand = false,
            waterDepthM = 1_000.0,
            usefulSunlightReachesWater = usefulSunlightReachesWater,
            permanentSeaIce = true,
        )
}