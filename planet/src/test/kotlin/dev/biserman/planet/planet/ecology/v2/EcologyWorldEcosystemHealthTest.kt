package dev.biserman.planet.planet.ecology.v2

import com.fasterxml.jackson.databind.ObjectMapper
import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.ClimateDatumSample
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class EcologyWorldEcosystemHealthTest {
    @Test
    fun `authored ecosystems meet their stability or collapse expectation`() {
        val results = notebookScenarios().map(::simulate)
        val failures = mutableListOf<String>()

        results.forEach { result ->
            println(
                "WORLD_ECOSYSTEM_HEALTH name=${result.name} extant=${result.extant}/${result.total} " +
                    "tail_cv=${"%.4f".format(result.tailCv)} expected_stable=${result.intendedStable} " +
                    "expected_extinctions=${result.expectedExtinctions.joinToString(",")} " +
                    "missing_roles=${result.missingRoles.joinToString(",")} " +
                    "survivors=${result.survivors.joinToString(",")} " +
                    "losses=${result.extinctionSeasons.entries.joinToString(",") { "${it.key}@${it.value}" }}",
            )
            val actualExtinctions = result.extinctionSeasons.keys
            val missingExpectedExtinctions =
                result.expectedExtinctions - actualExtinctions
            val unexpectedExtinctions =
                actualExtinctions - result.expectedExtinctions
            if (missingExpectedExtinctions.isNotEmpty()) {
                failures +=
                    "${result.name} retained species expected to go extinct: " +
                        missingExpectedExtinctions.joinToString()
            }
            if (unexpectedExtinctions.isNotEmpty()) {
                failures +=
                    "${result.name} unexpectedly lost: " +
                        unexpectedExtinctions.joinToString()
            }
            if (result.intendedStable) {
                if (result.tailCv >= 0.35) {
                    failures += "${result.name} tail CV was ${result.tailCv}"
                }
                if (result.expectedExtinctions.isEmpty() && result.missingRoles.isNotEmpty()) {
                    failures += "${result.name} lost required roles: ${result.missingRoles.joinToString()}"
                }
            }
            when (result.name) {
                "Boreal forest" -> {
                    val spruceBiomass =
                        result.finalBiomassById.getOrDefault("picea-mariana", 0.0) +
                            result.finalBiomassById.getOrDefault("abies-balsamea", 0.0)
                    if (result.finalBiomassById.getOrDefault("spruce-budworm", 0.0) >= spruceBiomass) {
                        failures += "${result.name} had more spruce budworm biomass than its two modeled spruces"
                    }
                    if (
                        result.finalBiomassById.getOrDefault("canada-lynx", 0.0) >=
                        result.finalBiomassById.getOrDefault("snowshoe-hare", 0.0)
                    ) {
                        failures += "${result.name} had at least as much lynx biomass as snowshoe hare biomass"
                    }
                }
                "Himalayan alpine meadow" -> {
                    val namedPreyBiomass =
                        result.finalBiomassById.getOrDefault("plateau-pika", 0.0) +
                            result.finalBiomassById.getOrDefault("bharal", 0.0)
                    if (result.finalBiomassById.getOrDefault("snow-leopard", 0.0) >= namedPreyBiomass) {
                        failures += "${result.name} had at least as much snow leopard biomass as named prey biomass"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(separator = "\n"))
    }

    private fun simulate(scenario: ParsedScenario): HealthResult {
        val ecology = EcologyCompiler.compile(
            scenario.species + invariantGuilds(scenario.tile),
        )
        val runtime = EcologyRuntime(ecology)
        val initialMarineSnow =
            if (!scenario.tile.isLand || scenario.tile.adjacentToOcean) 0.12 else 0.0
        val seedEnvironment = environmentAt(
            scenario.climate.sampleAt(0.5),
            scenario.climate.averageTemperature,
            scenario.tile,
            scenario.climate.tileId,
            0.5,
            marineSnow = initialMarineSnow,
        )
        val nicheBySpecies = ecology.species.associate { species ->
            species.index to NicheSelection.choose(species, ecology, seedEnvironment).also {
                require(it >= 0) { "No viable niche for ${species.displayName} in ${scenario.name}" }
            }
        }
        val introductionIds = scenario.introductions.map { it.speciesId }.toSet()
        val initialSpecies = ecology.species.filterNot { it.id in introductionIds }
        val nicheCounts = initialSpecies.groupingBy { nicheBySpecies.getValue(it.index) }.eachCount()
        val community = TileCommunity()
        initialSpecies.forEach { species ->
            val nicheIndex = nicheBySpecies.getValue(species.index)
            val biomass = initialBiomass(
                species,
                ecology.niches[nicheIndex],
                seedEnvironment,
                nicheCounts.getValue(nicheIndex),
            )
            val startingReserves = biomass * minOf(0.40, species.reserveCapacity * 0.60)
            community.add(species.index, nicheIndex, biomass, startingReserves)
        }

        val fluxes = CellTurnFluxes()
        val totals = DoubleArray(400)
        var carrion = 0.0
        var detritus = 0.0
        var waste = 0.0
        var marineSnow = initialMarineSnow
        var activeClimate = scenario.climate
        var activeTile = scenario.tile
        val extinctionSeasons = linkedMapOf<String, Int>()
        repeat(400) { season ->
            scenario.climateShifts.filter { it.year * 4 == season }.forEach { shift ->
                activeClimate = shift.climate
            }
            scenario.habitatShifts.filter { it.year * 4 == season }.forEach { shift ->
                activeTile = activeTile.copy(
                    canopyCover = shift.canopyCover ?: activeTile.canopyCover,
                    reefCover = shift.reefCover ?: activeTile.reefCover,
                )
            }
            val environment = environmentAt(
                activeClimate.sampleAt(season / 4.0),
                activeClimate.averageTemperature,
                activeTile,
                activeClimate.tileId,
                season / 4.0,
                carrion = carrion,
                detritus = detritus,
                waste = waste,
                marineSnow = marineSnow,
            )
            val presentBefore = (0 until community.size)
                .map { ecology.species[community.speciesIndices[it]].id }
                .toSet()
            scenario.introductions.filter { it.year * 4 == season }.forEach { introduction ->
                val species = ecology.species.single { it.id == introduction.speciesId }
                val nicheIndex = NicheSelection.choose(species, ecology, environment)
                require(nicheIndex >= 0)
                val biomass = introduction.biomassKg ?: initialBiomass(
                    species,
                    ecology.niches[nicheIndex],
                    environment,
                    1,
                )
                val startingReserves = biomass * minOf(0.40, species.reserveCapacity * 0.60)
                community.add(species.index, nicheIndex, biomass, startingReserves)
            }
            scenario.populationRemovals.filter { it.year * 4 == season }.forEach { removal ->
                val species = ecology.species.single { it.id == removal.speciesId }
                val population = community.find(species.index)
                if (population >= 0) {
                    val retained = (1.0 - removal.fraction).coerceIn(0.0, 1.0)
                    community.activeBiomass[population] *= retained
                    community.dormantBiomass[population] *= retained
                    community.reserves[population] *= retained
                }
            }
            runtime.advanceSeason(community, environment, fluxes)
            val presentAfter = (0 until community.size)
                .map { ecology.species[community.speciesIndices[it]].id }
                .toSet()
            (presentBefore - presentAfter).forEach { extinctionSeasons[it] = season }
            carrion = OrganicPoolDynamics.update(
                carrion,
                fluxes.carrionBiomass,
                fluxes.carrionConsumedBiomass,
                40_000.0 * 10.0,
                0.55,
            )
            detritus = OrganicPoolDynamics.update(
                detritus,
                fluxes.detritusBiomass,
                fluxes.detritusConsumedBiomass,
                40_000.0 * 12.0,
                0.68,
                maximumAccessibleFraction = 0.75,
            )
            waste = OrganicPoolDynamics.update(
                waste,
                fluxes.wasteBiomass,
                fluxes.wasteConsumedBiomass,
                40_000.0 * 10.0,
                0.60,
                maximumAccessibleFraction = 0.80,
            )
            marineSnow = OrganicPoolDynamics.update(
                marineSnow,
                fluxes.marineSnowBiomass,
                fluxes.marineSnowConsumedBiomass,
                40_000.0 * 15.0,
                0.72,
            )
            totals[season] = community.totalBiomass()
        }

        val tail = totals.copyOfRange(320, 400)
        val mean = tail.average()
        val cv =
            if (mean > 0.0) sqrt(tail.sumOf { (it - mean) * (it - mean) } / tail.size) / mean
            else Double.POSITIVE_INFINITY
        val survivors = (0 until community.size).map { populationIndex ->
            ecology.species[community.speciesIndices[populationIndex]].displayName
        }
        val finalBiomassById = (0 until community.size).associate { populationIndex ->
            val species = ecology.species[community.speciesIndices[populationIndex]]
            species.id to
                (
                    community.activeBiomass[populationIndex] +
                        community.dormantBiomass[populationIndex]
                    )
        }
        val requiredRoles = TrophicRole.entries.filterTo(linkedSetOf()) { role ->
            ecology.species.any { it.supports(role) }
        }
        val survivingSpecies = (0 until community.size).map { populationIndex ->
            ecology.species[community.speciesIndices[populationIndex]]
        }
        val missingRoles = requiredRoles.filter { role ->
            survivingSpecies.none { it.supports(role) }
        }
        return HealthResult(
            scenario.name,
            community.size,
            ecology.species.size,
            cv,
            scenario.intendedStable,
            survivors,
            extinctionSeasons,
            missingRoles,
            scenario.expectedExtinctions,
            finalBiomassById,
        )
    }

    private fun invariantGuilds(tile: ParsedTile): List<SpeciesDefinition> = buildList {
        if (tile.includeAeroplankton) add(InvariantSpecies.AEROPLANKTON)
        if (tile.isLand) {
            add(InvariantSpecies.CARPET_PLANTS)
            add(InvariantSpecies.BUGS)
        }
        if (!tile.isLand || tile.adjacentToOcean || tile.adjacentToMajorRiver) {
            add(InvariantSpecies.PLANKTON)
            add(InvariantSpecies.SMALL_AQUATIC_LIFE)
        }
    }

    private fun initialBiomass(
        species: CompiledSpecies,
        niche: NicheDefinition,
        environment: SeasonalCellEnvironment,
        speciesSharingNiche: Int,
    ): Double {
        val carryingBiomass =
            environment.areaKm2 *
                220.0 *
                species.sizeClass.densityScale *
                environment.fertility *
                environment.habitatAvailability(niche.habitat).coerceAtLeast(0.02) *
                environment.resourceSupport(niche, species.sizeClass).coerceAtLeast(0.04)
        val viableSeedMultiplier = when (niche.strategy) {
            EcoStrategy.AMBUSH_PREDATION,
            EcoStrategy.PURSUIT_PREDATION,
            EcoStrategy.FILTER_FEEDING,
            EcoStrategy.SCAVENGING -> 20.0
            else -> 100.0
        }
        val trophicSeedFraction = when (niche.strategy) {
            EcoStrategy.PHOTOSYNTHESIS, EcoStrategy.ABSORPTION -> 0.65
            EcoStrategy.FILTER_FEEDING,
            EcoStrategy.GRAZING,
            EcoStrategy.DEPOSIT_FEEDING,
            EcoStrategy.DECOMPOSITION,
            EcoStrategy.COPROPHAGY -> 0.45
            EcoStrategy.AMBUSH_PREDATION,
            EcoStrategy.PURSUIT_PREDATION,
            EcoStrategy.SCAVENGING,
            EcoStrategy.PARASITISM -> 0.015
        }
        return maxOf(
            species.massKg * viableSeedMultiplier,
            carryingBiomass * trophicSeedFraction / speciesSharingNiche.coerceAtLeast(1),
        )
    }

    private fun environmentAt(
        sample: ClimateDatumSample,
        annualAverageTemperature: Double,
        tile: ParsedTile,
        climateTileId: Int,
        year: Double,
        carrion: Double = 0.0,
        detritus: Double = 0.0,
        waste: Double = 0.0,
        marineSnow: Double = 0.0,
    ): SeasonalCellEnvironment {
        val anomaly = EcologyClimateVariability.anomaly(climateTileId, year)
        val base = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = sample.averageTemperature + anomaly.temperatureC,
            annualAverageTemperatureC = annualAverageTemperature,
            insolation = (sample.insolation / 340.0).coerceIn(0.0, 1.0),
            precipitationMm = sample.precipitation * anomaly.precipitationMultiplier,
            surfaceFertilityModifier = tile.fertilityModifier,
            isLand = tile.isLand,
            adjacentToOcean = tile.adjacentToOcean,
            adjacentToMajorRiver = tile.adjacentToMajorRiver,
            waterDepthM = tile.waterDepthM,
            usefulSunlightReachesWater = tile.usefulSunlightReachesWater,
            canopyCover = tile.canopyCover,
            reefCover = tile.reefCover,
        )
        return base.withResources(
            FunctionalResources(
                carrion = carrion,
                detritus = detritus,
                waste = waste,
                marineSnow = marineSnow,
            ),
        )
    }

    private fun notebookScenarios(): List<ParsedScenario> {
        val notebook = ObjectMapper().readTree(
            Path.of(
                "src/main/kotlin/dev/biserman/planet/notebooks/ecology_v2_world_ecosystems.ipynb",
            ).readText(),
        )
        return notebook["cells"]
            .filter { it["cell_type"].asText() == "code" }
            .map { cell -> cell["source"].joinToString("") { it.asText() } }
            .filter { Regex("""runEcosystem\(\s*"""").containsMatchIn(it) }
            .map(::parseScenario)
    }

    private fun parseScenario(source: String): ParsedScenario {
        val name = requireMatch(Regex("""runEcosystem\(\s*"([^"]+)""""), source).groupValues[1]
        val climateMatch = requireMatch(
            Regex(
                """seasonalClimate\((\d+), listOf\(([^)]*)\), listOf\(([^)]*)\), listOf\(([^)]*)\)\)""",
            ),
            source,
        )
        val climate = seasonalClimate(
            climateMatch.groupValues[1].toInt(),
            doubles(climateMatch.groupValues[2]),
            doubles(climateMatch.groupValues[3]),
            doubles(climateMatch.groupValues[4]),
        )
        val tileArguments =
            requireMatch(Regex("""TileTemplate\(([^)]*)\)"""), source).groupValues[1]
        val tileValues = Regex("""(\w+)\s*=\s*([^,\n]+)""")
            .findAll(tileArguments)
            .associate { it.groupValues[1] to it.groupValues[2].trim() }
        val tile = ParsedTile(
            isLand = tileValues.getValue("isLand").toBoolean(),
            adjacentToOcean = tileValues["adjacentToOcean"]?.toBoolean() ?: false,
            adjacentToMajorRiver = tileValues["adjacentToMajorRiver"]?.toBoolean() ?: false,
            waterDepthM = tileValues["waterDepthM"]?.toDouble() ?: 0.0,
            usefulSunlightReachesWater =
                tileValues["usefulSunlightReachesWater"]?.toBoolean() ?: true,
            canopyCover = tileValues["canopyCover"]?.toDouble() ?: 0.0,
            reefCover = tileValues["reefCover"]?.toDouble() ?: 0.0,
            fertilityModifier = tileValues["fertilityModifier"]?.toDouble() ?: 0.0,
            includeAeroplankton = tileValues["includeAeroplankton"]?.toBoolean() ?: false,
        )

        val speciesPattern = Regex(
            """sp\("([^"]+)", "([^"]+)", S\.([A-Z]+), (true|false), listOf\(([^)]*)\)(?:, B\.([A-Z_]+))?(?:, camouflage = B\.([A-Z_]+))?\)""",
        )
        val species = speciesPattern.findAll(source).map { match ->
            val traits =
                Regex("""C\.([A-Z_]+)""")
                    .findAll(match.groupValues[5])
                    .map { CommonTrait.valueOf(it.groupValues[1]) }
                    .toMutableList<SpeciesTrait>()
            if ("invasiveMesopredatorSpecialization" in match.groupValues[5]) {
                traits += TargetedRelationshipTrait(
                    displayName = "mesopredator hunting specialization",
                    description =
                        "The invader recognizes and efficiently hunts the resident predator as unusually vulnerable prey.",
                    maintenanceCost = 0.04,
                    relationships = listOf(
                        RelationshipEffect.SupplementalFood(
                            SpeciesSelector.ExactSpecies("control-mesopredator"),
                            attackRate = 2.00,
                            assimilationEfficiency = 0.85,
                        ),
                        RelationshipEffect.RequiresTarget(
                            SpeciesSelector.ExactSpecies("control-mesopredator"),
                        ),
                    ),
                )
            }
            if ("wolfPackHunting" in match.groupValues[5]) {
                traits += TargetedRelationshipTrait(
                    displayName = "cooperative moose hunting",
                    description =
                        "A coordinated pack can isolate and exhaust prey much larger than any individual hunter.",
                    maintenanceCost = 0.05,
                    relationships = listOf(
                        RelationshipEffect.SupplementalFood(
                            SpeciesSelector.ExactSpecies("moose"),
                            attackRate = 0.002,
                            assimilationEfficiency = 0.78,
                        ),
                    ),
                )
            }
            if ("islandBirdHunting" in match.groupValues[5]) {
                traits += TargetedRelationshipTrait(
                    displayName = "ground-nest hunting",
                    description =
                        "Scent tracking and exploratory hunting expose large, poorly defended ground nests.",
                    maintenanceCost = 0.03,
                    relationships = listOf(
                        RelationshipEffect.SupplementalFood(
                            SpeciesSelector.ExactSpecies("island-ground-bird"),
                            attackRate = 0.62,
                            assimilationEfficiency = 0.82,
                        ),
                    ),
                )
            }
            if ("hostSpecialization" in match.groupValues[5]) {
                traits += TargetedRelationshipTrait(
                    displayName = "obligate host feeding",
                    description =
                        "The mouthparts and life cycle are specialized around feeding from one particular host species.",
                    maintenanceCost = 1.20,
                    relationships = listOf(
                        RelationshipEffect.ParasiteOf(
                            SpeciesSelector.ExactSpecies("host-shrub"),
                            drainRate = 0.12,
                        ),
                        RelationshipEffect.RequiresTarget(
                            SpeciesSelector.ExactSpecies("host-shrub"),
                        ),
                    ),
                )
            }
            SpeciesDefinition(
                id = match.groupValues[1],
                displayName = match.groupValues[2],
                sizeClass = SizeClass.valueOf(match.groupValues[3]),
                motile = match.groupValues[4].toBoolean(),
                traits = traits,
                photosyntheticColor =
                    match.groupValues[6].takeIf { it.isNotEmpty() }?.let(BiologicalColor::valueOf),
                camouflageColor =
                    match.groupValues[7].takeIf { it.isNotEmpty() }?.let(BiologicalColor::valueOf),
            )
        }.toList()
        require(species.isNotEmpty()) { "No species parsed for $name" }

        val introductions = Regex(
            """Introduction\("([^"]+)", year = (\d+), biomassKg = ([\d_.]+)\)""",
        ).findAll(source).map { match ->
            ParsedIntroduction(
                match.groupValues[1],
                match.groupValues[2].toInt(),
                match.groupValues[3].replace("_", "").toDouble(),
            )
        }.toList()
        val climateShifts = Regex(
            """ClimateShift\((\d+), seasonalClimate\((\d+), listOf\(([^)]*)\), listOf\(([^)]*)\), listOf\(([^)]*)\)\)\)""",
        ).findAll(source).map { match ->
            ParsedClimateShift(
                match.groupValues[1].toInt(),
                seasonalClimate(
                    match.groupValues[2].toInt(),
                    doubles(match.groupValues[3]),
                    doubles(match.groupValues[4]),
                    doubles(match.groupValues[5]),
                ),
            )
        }.toList()
        val habitatShifts = Regex(
            """HabitatShift\((\d+), ([^)]*)\)""",
        ).findAll(source).map { match ->
            val values = Regex("""(\w+)\s*=\s*([^,\n]+)""")
                .findAll(match.groupValues[2])
                .associate { it.groupValues[1] to it.groupValues[2].trim() }
            ParsedHabitatShift(
                match.groupValues[1].toInt(),
                values["canopyCover"]?.toDouble(),
                values["reefCover"]?.toDouble(),
            )
        }.toList()
        val populationRemovals = Regex(
            """PopulationRemoval\("([^"]+)", year = (\d+)(?:, fraction = ([\d_.]+))?\)""",
        ).findAll(source).map { match ->
            ParsedPopulationRemoval(
                match.groupValues[1],
                match.groupValues[2].toInt(),
                match.groupValues[3].takeIf { it.isNotEmpty() }
                    ?.replace("_", "")
                    ?.toDouble() ?: 1.0,
            )
        }.toList()
        val expectedExtinctions =
            Regex("""expectedExtinctions\s*=\s*setOf\(([^)]*)\)""")
                .find(source)
                ?.groupValues
                ?.get(1)
                ?.let { ids ->
                    Regex(""""([^"]+)"""").findAll(ids).map { it.groupValues[1] }.toSet()
                }
                ?: emptySet()
        return ParsedScenario(
            name,
            climate,
            tile,
            species,
            introductions,
            climateShifts,
            habitatShifts,
            populationRemovals,
            expectedExtinctions,
            intendedStable = "intendedStable = false" !in source,
        )
    }

    private fun seasonalClimate(
        tileId: Int,
        temperatures: List<Double>,
        insolations: List<Double>,
        precipitation: List<Double>,
    ): ClimateDatum {
        fun interpolate(values: List<Double>, month: Int): Double {
            val quarter = month / 3
            val next = (quarter + 1) % 4
            val fraction = (month % 3) / 3.0
            return values[quarter] + (values[next] - values[quarter]) * fraction
        }
        return ClimateDatum(
            tileId,
            (0 until 12).map { month ->
                ClimateDatumSample(
                    interpolate(temperatures, month),
                    interpolate(insolations, month),
                    interpolate(precipitation, month),
                )
            },
        )
    }

    private fun doubles(source: String): List<Double> =
        source.split(",").map { it.trim().toDouble() }

    private fun requireMatch(pattern: Regex, source: String): MatchResult =
        requireNotNull(pattern.find(source)) { "Could not parse ${pattern.pattern} from scenario cell" }

    private data class ParsedScenario(
        val name: String,
        val climate: ClimateDatum,
        val tile: ParsedTile,
        val species: List<SpeciesDefinition>,
        val introductions: List<ParsedIntroduction>,
        val climateShifts: List<ParsedClimateShift>,
        val habitatShifts: List<ParsedHabitatShift>,
        val populationRemovals: List<ParsedPopulationRemoval>,
        val expectedExtinctions: Set<String>,
        val intendedStable: Boolean,
    )

    private data class ParsedTile(
        val isLand: Boolean,
        val adjacentToOcean: Boolean = false,
        val adjacentToMajorRiver: Boolean = false,
        val waterDepthM: Double = 0.0,
        val usefulSunlightReachesWater: Boolean = true,
        val canopyCover: Double = 0.0,
        val reefCover: Double = 0.0,
        val fertilityModifier: Double = 0.0,
        val includeAeroplankton: Boolean = false,
    )

    private data class ParsedIntroduction(
        val speciesId: String,
        val year: Int,
        val biomassKg: Double?,
    )

    private data class ParsedClimateShift(
        val year: Int,
        val climate: ClimateDatum,
    )

    private data class ParsedHabitatShift(
        val year: Int,
        val canopyCover: Double?,
        val reefCover: Double?,
    )

    private data class ParsedPopulationRemoval(
        val speciesId: String,
        val year: Int,
        val fraction: Double,
    )

    private data class HealthResult(
        val name: String,
        val extant: Int,
        val total: Int,
        val tailCv: Double,
        val intendedStable: Boolean,
        val survivors: List<String>,
        val extinctionSeasons: Map<String, Int>,
        val missingRoles: List<TrophicRole>,
        val expectedExtinctions: Set<String>,
        val finalBiomassById: Map<String, Double>,
    )

    private enum class TrophicRole {
        PRODUCER,
        PRIMARY_CONSUMER,
        PREDATOR,
    }

    private fun CompiledSpecies.supports(role: TrophicRole): Boolean = when (role) {
        TrophicRole.PRODUCER ->
            strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] > 0.0
        TrophicRole.PRIMARY_CONSUMER ->
            strategySupport[EcoStrategy.FILTER_FEEDING.ordinal] > 0.0 ||
                strategySupport[EcoStrategy.GRAZING.ordinal] > 0.0 ||
                strategySupport[EcoStrategy.DEPOSIT_FEEDING.ordinal] > 0.0
        TrophicRole.PREDATOR ->
            strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal] > 0.0 ||
                strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal] > 0.0
    }
}
