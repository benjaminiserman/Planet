package dev.biserman.planet.planet.ecology

import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcologyBenchmarkTest {
    @Test
    fun `benchmark 20000 cell seasonal turn`() {
        val scenario = BenchmarkScenario.create(tileCount = 20_000, populationsPerTile = 24)
        val runtime = EcologyRuntime(
            ecology = scenario.ecology,
            config = EcologyRuntimeConfig(minimumViableIndividuals = 0.0),
            maximumPopulationsPerCell = 32,
        )

        repeat(3) {
            scenario.advance(runtime)
        }
        val samples = DoubleArray(7) {
            measureNanoTime { scenario.advance(runtime) } / 1_000_000.0
        }
        samples.sort()
        val medianMs = samples[samples.size / 2]
        val maximumMs = samples.last()
        val populationUpdates = scenario.communities.sumOf { it.size }
        val pairLookups = scenario.communities.sumOf { it.size * (it.size - 1) }

        println(
            "ECOLOGY_BENCHMARK " +
                "median_ms=${"%.3f".format(medianMs)} " +
                "max_ms=${"%.3f".format(maximumMs)} " +
                "cells=${scenario.communities.size} " +
                "populations=$populationUpdates " +
                "ordered_pair_lookups=$pairLookups",
        )

        assertEquals(20_000, scenario.communities.size)
        assertEquals(480_000, populationUpdates)
        assertTrue(medianMs.isFinite() && medianMs > 0.0)
    }
}

private class BenchmarkScenario(
    val ecology: CompiledEcology,
    val communities: Array<TileCommunity>,
    private val environments: Array<SeasonalCellEnvironment>,
) {
    fun advance(runtime: EcologyRuntime) {
        for (tileIndex in communities.indices) {
            runtime.advanceSeason(communities[tileIndex], environments[tileIndex])
        }
    }

    companion object {
        fun create(tileCount: Int, populationsPerTile: Int): BenchmarkScenario {
            require(populationsPerTile == 24)
            val definitions = benchmarkSpecies() + InvariantSpecies.ALL
            val ecology = EcologyCompiler.compile(definitions)
            val landEnvironment = benchmarkLand()
            val oceanEnvironment = benchmarkOcean()
            val landSpecies =
                ecology.species.filter { it.id.startsWith("land-") }.take(22) +
                    ecology.species.single { it.id == InvariantSpecies.BUGS.id } +
                    ecology.species.single { it.id == InvariantSpecies.CARPET_PLANTS.id }
            val oceanSpecies =
                ecology.species.filter { it.id.startsWith("ocean-") }.take(22) +
                    ecology.species.single { it.id == InvariantSpecies.PLANKTON.id } +
                    ecology.species.single { it.id == InvariantSpecies.SMALL_AQUATIC_LIFE.id }
            require(landSpecies.size == populationsPerTile)
            require(oceanSpecies.size == populationsPerTile)

            val environments = Array(tileCount) { tileIndex ->
                if (tileIndex % 2 == 0) landEnvironment else oceanEnvironment
            }
            val communities = Array(tileCount) { tileIndex ->
                val environment = environments[tileIndex]
                val candidates = if (tileIndex % 2 == 0) landSpecies else oceanSpecies
                TileCommunity(capacity = 32).also { community ->
                    candidates.forEachIndexed { localIndex, species ->
                        val niche = NicheSelection.choose(species, ecology, environment)
                        require(niche >= 0) { "${species.displayName} has no benchmark niche" }
                        community.add(
                            speciesIndex = species.index,
                            nicheIndex = niche,
                            activeBiomass = 90_000.0 + localIndex * 2_000.0,
                            reserves = 8_000.0,
                        )
                    }
                }
            }
            return BenchmarkScenario(ecology, communities, environments)
        }

        private fun benchmarkSpecies(): List<SpeciesDefinition> = buildList {
            repeat(8) { index ->
                add(
                    SpeciesDefinition(
                        id = "land-producer-$index",
                        displayName = "Land producer $index",
                        sizeClass = SizeClass.entries[index % 4],
                        motile = false,
                        traits = listOf(
                            CommonTrait.TEMPERATE_BIOCHEMISTRY,
                            CommonTrait.PHOTOSYNTHETIC_SURFACE,
                            CommonTrait.ROOTED_BODY,
                        ),
                        photosyntheticColor = BiologicalColor.entries[index % BiologicalColor.entries.size],
                    ),
                )
            }
            repeat(6) { index ->
                add(landConsumer("land-grazer-$index", EcoStrategy.GRAZING, index))
            }
            repeat(7) { index ->
                add(landConsumer("land-predator-$index", EcoStrategy.AMBUSH_PREDATION, index))
            }
            add(landConsumer("land-scavenger", EcoStrategy.SCAVENGING, 0))
            add(landConsumer("land-decomposer", EcoStrategy.DECOMPOSITION, 1))
            add(landConsumer("land-coprophage", EcoStrategy.COPROPHAGY, 2))

            repeat(8) { index ->
                add(
                    SpeciesDefinition(
                        id = "ocean-producer-$index",
                        displayName = "Ocean producer $index",
                        sizeClass = SizeClass.entries[index % 4],
                        motile = false,
                        traits = listOf(
                            CommonTrait.TEMPERATE_BIOCHEMISTRY,
                            CommonTrait.PHOTOSYNTHETIC_SURFACE,
                            CommonTrait.BUOYANCY_BLADDER,
                            CommonTrait.DIFFUSIVE_AQUATIC_GAS_EXCHANGE,
                        ),
                        photosyntheticColor = BiologicalColor.entries[index % BiologicalColor.entries.size],
                    ),
                )
            }
            repeat(6) { index ->
                add(oceanConsumer("ocean-filter-$index", EcoStrategy.FILTER_FEEDING, index))
            }
            repeat(4) { index ->
                add(oceanConsumer("ocean-grazer-$index", EcoStrategy.GRAZING, index))
            }
            repeat(4) { index ->
                add(oceanConsumer("ocean-predator-$index", EcoStrategy.AMBUSH_PREDATION, index))
            }
            add(oceanConsumer("ocean-deposit", EcoStrategy.DEPOSIT_FEEDING, 0))
            add(oceanConsumer("ocean-decomposer", EcoStrategy.DECOMPOSITION, 1))
        }

        private fun landConsumer(id: String, strategy: EcoStrategy, index: Int): SpeciesDefinition {
            val strategyTrait = when (strategy) {
                EcoStrategy.GRAZING -> CommonTrait.GRAZING_MOUTHPARTS
                EcoStrategy.AMBUSH_PREDATION -> CommonTrait.AMBUSH_MUSCULATURE
                EcoStrategy.SCAVENGING -> CommonTrait.SCAVENGING_SENSES
                EcoStrategy.DECOMPOSITION -> CommonTrait.DETRITUS_DIGESTIVE_TRACT
                EcoStrategy.COPROPHAGY -> CommonTrait.DUNG_FEEDING_MOUTHPARTS
                else -> error("Unsupported land benchmark strategy")
            }
            return SpeciesDefinition(
                id = id,
                displayName = id,
                sizeClass = SizeClass.entries[2 + index % 4],
                motile = true,
                traits = listOf(
                    CommonTrait.TEMPERATE_BIOCHEMISTRY,
                    if (index % 2 == 0) CommonTrait.ECTOTHERMY else CommonTrait.ENDOTHERMY,
                    CommonTrait.TERRESTRIAL_LOCOMOTION,
                    strategyTrait,
                ),
                camouflageColor = BiologicalColor.entries[index % BiologicalColor.entries.size],
            )
        }

        private fun oceanConsumer(id: String, strategy: EcoStrategy, index: Int): SpeciesDefinition {
            val strategyTrait = when (strategy) {
                EcoStrategy.FILTER_FEEDING -> CommonTrait.GILL_PADS
                EcoStrategy.GRAZING -> CommonTrait.GRAZING_MOUTHPARTS
                EcoStrategy.AMBUSH_PREDATION -> CommonTrait.AMBUSH_MUSCULATURE
                EcoStrategy.DEPOSIT_FEEDING -> CommonTrait.MARINE_SNOW_PALPS
                EcoStrategy.DECOMPOSITION -> CommonTrait.DETRITUS_DIGESTIVE_TRACT
                else -> error("Unsupported ocean benchmark strategy")
            }
            return SpeciesDefinition(
                id = id,
                displayName = id,
                sizeClass = SizeClass.entries[1 + index % 5],
                motile = true,
                traits = listOf(
                    CommonTrait.TEMPERATE_BIOCHEMISTRY,
                    CommonTrait.ECTOTHERMY,
                    CommonTrait.BUOYANCY_BLADDER,
                    CommonTrait.GILLS,
                    strategyTrait,
                ),
                camouflageColor = BiologicalColor.entries[index % BiologicalColor.entries.size],
            )
        }

        private fun benchmarkLand() = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 21.0,
            annualAverageTemperatureC = 17.0,
            insolation = 0.8,
            precipitationMm = 85.0,
            surfaceFertilityModifier = 0.5,
            isLand = true,
            canopyCover = 0.35,
            resources = FunctionalResources(
                carrion = 0.42,
                detritus = 0.55,
                waste = 0.38,
            ),
        )

        private fun benchmarkOcean() = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 19.0,
            annualAverageTemperatureC = 16.0,
            insolation = 0.8,
            precipitationMm = 70.0,
            surfaceFertilityModifier = 0.4,
            isLand = false,
            waterDepthM = 600.0,
            usefulSunlightReachesWater = true,
            reefCover = 0.3,
            resources = FunctionalResources(
                carrion = 0.30,
                detritus = 0.44,
                waste = 0.28,
                marineSnow = 0.48,
            ),
        )
    }
}