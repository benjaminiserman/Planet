package dev.biserman.planet.planet.ecology

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomEcosystemExperimentTest {
    @Test
    fun `random experiment is deterministic and records a complete run`() {
        val first = RandomEcosystemExperiment.run(seed = 7)
        val second = RandomEcosystemExperiment.run(seed = 7)

        assertEquals(first.selectedSpecies, second.selectedSpecies)
        assertEquals(first.tile, second.tile)
        assertEquals(first.climate, second.climate)
        assertEquals(first.extinctionSeasons, second.extinctionSeasons)
        assertEquals(first.finalBiomassKg, second.finalBiomassKg)
        assertEquals(10, first.selectedSpecies.size)
        assertEquals(400, first.temperatureHistoryC.size)
        assertTrue(first.biomassHistoryKg.values.all { it.size == 400 })
        assertTrue(first.resourceHistory.values.all { it.size == 400 })
    }

    @Test
    fun `sampled communities remain numerically safe`() {
        val results = (0 until 64).map(RandomEcosystemExperiment::run)

        assertTrue(results.any { it.tile.isLand })
        assertTrue(results.any { !it.tile.isLand })
        assertEquals(
            HersfeldtClimatePresets.ALL.mapTo(hashSetOf()) { it.id },
            results.mapTo(hashSetOf()) { it.climate.presetId },
        )
        val birdNames = EarthSpeciesCatalog.BIRDS.mapTo(hashSetOf()) { it.displayName }
        val openOceanBirds = setOf("Emperor penguin", "Wandering albatross")
        results.forEach { result ->
            println(
                "RANDOM_ECOSYSTEM seed=${result.seed} climate=${result.climate.presetId} land=${result.tile.isLand} " +
                    "mean_temp=${"%.1f".format(result.climate.annualMeanTemperatureC)} " +
                    "rain=${"%.0f".format(result.climate.annualPrecipitationMm)} " +
                    "survivors=${result.survivingSpecies.size}/${result.finalBiomassKg.size} " +
                    "unsupported=${result.unsupportedConsumers.joinToString("|")} " +
                    "anomalies=${result.anomalies.joinToString("|")} " +
                    "tail_cv=${"%.3f".format(result.tailCoefficientOfVariation)}",
            )
            assertEquals(!result.climate.ocean, result.tile.isLand)
            assertTrue(result.climate.presetId in HersfeldtClimatePresets.ALL.map { it.id })
            if (!result.tile.isLand) {
                assertTrue(
                    result.selectedSpecies.filter { it in birdNames }.all { it in openOceanBirds },
                    "Seed ${result.seed} selected non-pelagic birds over open ocean: " +
                        result.selectedSpecies.filter { it in birdNames && it !in openOceanBirds },
                )
                result.chosenNiches["Wandering albatross"]?.let { niche ->
                    assertTrue(
                        niche.startsWith("aerial "),
                        "Seed ${result.seed} placed an albatross in $niche",
                    )
                }
            }
            if (result.seed in notebookSeeds) {
                println(
                    "RANDOM_ECOSYSTEM_DETAIL seed=${result.seed} " +
                        "selected=${result.selectedSpecies.joinToString("|")} " +
                        "losses=${result.extinctionSeasons.entries.joinToString("|") { "${it.key}@${it.value}" }} " +
                        "survivors=${result.survivingSpecies.joinToString("|")}",
                )
            }
            assertTrue(
                result.biomassHistoryKg.values.flatten().all { it.isFinite() && it >= 0.0 },
                "Seed ${result.seed} produced invalid biomass",
            )
            assertTrue(
                result.resourceHistory.values.flatten().all { it.isFinite() && it in 0.0..1.0 },
                "Seed ${result.seed} produced invalid resources",
            )
            assertTrue(
                result.unsupportedConsumers.none { it in result.survivingSpecies },
                "Seed ${result.seed} retained a consumer with no modeled food",
            )
            assertTrue(result.anomalies.isEmpty(), "Seed ${result.seed}: ${result.anomalies}")
        }
    }

    @Test
    fun `long random communities seed and retain only annually viable species`() {
        val results = longRunSeeds.map { seed ->
            RandomEcosystemExperiment.run(seed = seed, seasons = 4_000)
        }

        results.forEach { result ->
            val climateMismatchLosses = result.selectedSpecies
                .filter { result.maximumAnnualClimateFitness.getValue(it) < 0.35 }
                .filter { it in result.extinctionSeasons }
            val dominantSurvivors = result.finalBiomassKg
                .filterValues { it > 0.0 }
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .joinToString("|") { (name, biomass) ->
                    "$name:${"%.2e".format(biomass)}"
                }
            println(
                "RANDOM_ECOSYSTEM_1000Y seed=${result.seed} climate=${result.climate.presetId} " +
                    "land=${result.tile.isLand} survivors=${result.survivingSpecies.size}/${result.finalBiomassKg.size} " +
                    "dominant=$dominantSurvivors " +
                    "climate_mismatch_losses=${climateMismatchLosses.joinToString("|")} " +
                    "losses=${result.extinctionSeasons.entries.joinToString("|") { "${it.key}@${it.value}" }}",
            )
            assertTrue(result.anomalies.isEmpty(), "Seed ${result.seed}: ${result.anomalies}")
            assertTrue(
                result.survivingSpecies.none {
                    result.maximumAnnualClimateFitness.getValue(it) < 0.35 &&
                        it !in result.invariantSpecies
                },
                "Seed ${result.seed} retained a profoundly climate-mismatched species",
            )
        }
        assertTrue(
            results.all { result ->
                result.selectedSpecies.all {
                    result.maximumAnnualClimateFitness.getValue(it) >= 0.35
                }
            },
            "The random experiment seeded a species with no viable season",
        )
    }

    @Test
    fun `random community notebook contains six reproducible runs`() {
        val notebookPath = Path.of(
            "src/main/kotlin/dev/biserman/planet/notebooks/ecology_random_communities.ipynb",
        )
        val notebookText = notebookPath.readText()
        val notebook = ObjectMapper().readTree(notebookText)
        val runCalls = notebook["cells"]
            .flatMap { cell -> cell["source"]?.map { it.asText() }.orEmpty() }
            .count { line -> line.trim().matches(Regex("""showRandomEcosystem\(\d+\)""")) }

        assertEquals(4, notebook["nbformat"].asInt())
        assertEquals(6, runCalls)
        assertTrue(notebookText.contains("speciesCount = 10"))
        assertTrue(notebookText.contains("seasons = 400"))
        assertTrue(notebookText.contains("visibleResources"))
        assertTrue(notebookText.contains("Math.log10"))
        assertTrue(notebookText.contains("org.jetbrains.kotlinx.kandy.dsl.plot"))
        assertTrue(
            notebookText.contains(
                "fun showRandomEcosystem(seed: Int, seasons: Int = 400) = run {",
            ),
        )
    }

    private companion object {
        val notebookSeeds = setOf(0, 12, 23, 29, 36, 44)
        val longRunSeeds = listOf(3, 8, 13, 18, 23, 28, 33, 38, 43, 48, 53, 58)
    }
}