package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatumSample
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class EcologyWorldEcosystemHealthTest {
    @Test
    fun `authored ecosystems meet their stability or collapse expectation`() {
        val results = AuthoredEcosystems.ALL.map(::simulate)
        val failures = mutableListOf<String>()

        results.forEach { result ->
            println(
                "WORLD_ECOSYSTEM_HEALTH name=${result.name} extant=${result.extant}/${result.total} " +
                    "tail_cv=${"%.4f".format(result.tailCv)} expected_stable=${result.intendedStable} " +
                    "expected_extinctions=${result.expectedExtinctions.joinToString(",")} " +
                    "missing_roles=${result.missingRoles.joinToString(",")} " +
                    "producer_kg=${"%.3e".format(result.trophicBiomass.producers)} " +
                    "primary_kg=${"%.3e".format(result.trophicBiomass.primaryConsumers)} " +
                    "predator_kg=${"%.3e".format(result.trophicBiomass.predators)} " +
                    "apex_kg=${"%.3e".format(result.trophicBiomass.apexPredators)} " +
                    "plankton_kg=${"%.3e".format(result.finalBiomassById.getOrDefault("invariant-plankton", 0.0))} " +
                    "survivors=${result.survivors.joinToString(",")} " +
                    "losses=${result.extinctionSeasons.entries.joinToString(",") { "${it.key}@${it.value}" }} " +
                    "all_kg=${result.finalBiomassById.entries.joinToString(",") { "${it.key}:${"%.2e".format(it.value)}" }}",
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
            if (result.intendedStable && result.expectedExtinctions.isEmpty()) {
                val animalBiomass =
                    result.trophicBiomass.primaryConsumers +
                        result.trophicBiomass.predators +
                        result.trophicBiomass.apexPredators
                if (result.isLand &&
                    animalBiomass > 0.0 &&
                    result.trophicBiomass.producers / animalBiomass < 3.0
                ) {
                    failures += "${result.name} had less than a threefold terrestrial producer/animal pyramid"
                }
                if (!result.isLand) {
                    val plankton =
                        result.finalBiomassById.getOrDefault("invariant-plankton", 0.0)
                    if (plankton !in 1.0e8..1.0e11) {
                        failures +=
                            "${result.name} plankton biomass ${"%.3e".format(plankton)} kg " +
                            "was outside the broad one-tile target"
                    }
                    if (
                        plankton > 0.0 &&
                        result.evolvingFilterFeederBiomass > plankton * 0.10
                    ) {
                        failures +=
                            "${result.name} had evolving filter feeders above 10% of plankton biomass"
                    }
                }
                val predatoryBiomass =
                    result.trophicBiomass.predators +
                        result.trophicBiomass.apexPredators
                if (
                    result.trophicBiomass.primaryConsumers > 0.0 &&
                    predatoryBiomass > result.trophicBiomass.primaryConsumers
                ) {
                    failures += "${result.name} had more predatory than primary-consumer biomass"
                }
                if (result.poorClimateFits.isNotEmpty()) {
                    failures +=
                        "${result.name} retained species without a viable season: " +
                        result.poorClimateFits.joinToString()
                }
            }
            when (result.name) {
                "Boreal forest" -> {
                    val spruceBiomass =
                        result.finalBiomassById.getOrDefault("scots-pine", 0.0)
                    if (result.finalBiomassById.getOrDefault("red-squirrel", 0.0) >= spruceBiomass) {
                        failures += "${result.name} had more red squirrel biomass than its modeled pine"
                    }
                    if (
                        result.finalBiomassById.getOrDefault("red-fox", 0.0) >=
                        result.finalBiomassById.getOrDefault("snowshoe-hare", 0.0)
                    ) {
                        failures += "${result.name} had at least as much red fox biomass as snowshoe hare biomass"
                    }
                }
                "Himalayan alpine meadow" -> {
                    val namedPreyBiomass =
                        result.finalBiomassById.getOrDefault("himalayan-pika", 0.0)
                    if (result.finalBiomassById.getOrDefault("snow-leopard", 0.0) >= namedPreyBiomass) {
                        failures += "${result.name} had at least as much snow leopard biomass as named prey biomass"
                    }
                }
            }
        }
        val baselineLandRatios = results
            .filter { it.intendedStable && it.expectedExtinctions.isEmpty() && it.isLand }
            .mapNotNull { result ->
                val animals =
                    result.trophicBiomass.primaryConsumers +
                        result.trophicBiomass.predators +
                        result.trophicBiomass.apexPredators
                if (animals > 0.0) result.trophicBiomass.producers / animals else null
            }
            .sorted()
        if (
            baselineLandRatios.isEmpty() ||
            baselineLandRatios[baselineLandRatios.size / 2] < 50.0
        ) {
            failures += "Median terrestrial producer/animal biomass ratio was below 50"
        }
        assertTrue(failures.isEmpty(), failures.joinToString(separator = "\n"))
    }

    private fun simulate(scenario: AuthoredEcosystemScenario): HealthResult {
        val ecology = EcologyCompiler.compile(
            scenario.species + invariantGuilds(scenario.tile),
        )
        val runtime = EcologyRuntime(ecology)
        val initialMarineSnow =
            if (!scenario.tile.isLand || scenario.tile.adjacentToOcean > 0.0) 0.12 else 0.0
        val seedEnvironment = environmentAt(
            scenario.climate.sampleAt(0.5),
            scenario.climate.averageTemperature,
            scenario.tile,
            scenario.climate.tileId,
            0.5,
            resources = FunctionalResources(marineSnow = initialMarineSnow),
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
            val startingReserves = biomass * minOf(0.40, species.lifeHistory.reserveCapacity * 0.60)
            community.add(species.index, nicheIndex, biomass, startingReserves)
        }

        val fluxes = CellTurnFluxes()
        val totals = DoubleArray(SIMULATION_SEASONS)
        var resources = FunctionalResources(marineSnow = initialMarineSnow)
        var activeClimate = scenario.climate
        var activeTile = scenario.tile
        val extinctionSeasons = linkedMapOf<String, Int>()
        repeat(SIMULATION_SEASONS) { season ->
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
                resources = resources,
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
                val startingReserves = biomass * minOf(0.40, species.lifeHistory.reserveCapacity * 0.60)
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
            resources = FunctionalResourceDynamics.update(
                previous = resources,
                fluxes = fluxes,
                areaKm2 = 40_000.0,
                hasMarineCompartment = !activeTile.isLand || activeTile.adjacentToOcean > 0.0,
            )
            totals[season] = community.totalBiomass()
        }

        // Stability is year-to-year persistence, not the absence of ordinary
        // seasonality. Comparing raw seasons incorrectly classifies a stable
        // polar plankton bloom as an oscillating ecosystem.
        val tail = totals
            .copyOfRange(SIMULATION_SEASONS * 4 / 5, SIMULATION_SEASONS)
            .asList()
            .chunked(4)
            .map { year -> year.average() }
        val mean = tail.average()
        val cv =
            if (mean > 0.0) {
                sqrt(tail.sumOf { (it - mean) * (it - mean) } / tail.size) / mean
            } else {
                Double.POSITIVE_INFINITY
            }
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
        val trophicBiomass = trophicBiomass(ecology, community, finalBiomassById)
        val evolvingFilterFeederBiomass = (0 until community.size).sumOf { populationIndex ->
            val species = ecology.species[community.speciesIndices[populationIndex]]
            if (
                species.kind == SpeciesKind.EVOLVING &&
                species.niche.supportFor(EcoStrategy.FILTER_FEEDING) > 0.0
            ) {
                finalBiomassById.getValue(species.id)
            } else {
                0.0
            }
        }
        val poorClimateFits = ecology.species
            .filter { species ->
                species.kind == SpeciesKind.EVOLVING &&
                    finalBiomassById.getOrDefault(species.id, 0.0) > 0.0
            }
            .filter { species ->
                val niche = ecology.niches[nicheBySpecies.getValue(species.index)]
                (0 until 48).maxOf { sampleIndex ->
                    val year = sampleIndex / 48.0
                    val environment = environmentAt(
                        scenario.climate.sampleAt(year),
                        scenario.climate.averageTemperature,
                        scenario.tile,
                        scenario.climate.tileId,
                        year,
                        resources = FunctionalResources(marineSnow = initialMarineSnow),
                    )
                    EcologyFitness.combined(species, environment, niche.habitat)
                } < 0.35
            }
            .map { it.displayName }
        return HealthResult(
            scenario.name,
            scenario.tile.isLand,
            community.size,
            ecology.species.size,
            cv,
            scenario.intendedStable,
            survivors,
            extinctionSeasons,
            missingRoles,
            scenario.expectedExtinctions,
            finalBiomassById,
            trophicBiomass,
            evolvingFilterFeederBiomass,
            poorClimateFits,
        )
    }

    private fun trophicBiomass(
        ecology: CompiledEcology,
        community: TileCommunity,
        finalBiomassById: Map<String, Double>,
    ): TrophicBiomass {
        val extantIndices = (0 until community.size)
            .mapTo(hashSetOf()) { community.speciesIndices[it] }
        var producers = 0.0
        var primaryConsumers = 0.0
        var predators = 0.0
        var apexPredators = 0.0
        var other = 0.0
        extantIndices.forEach { speciesIndex ->
            val species = ecology.species[speciesIndex]
            val biomass = finalBiomassById.getValue(species.id)
            when {
                species.supports(TrophicRole.PRODUCER) -> producers += biomass
                species.supports(TrophicRole.PREDATOR) -> {
                    val eatsExtantPredator =
                        extantIndices.any { consumerIndex ->
                            consumerIndex != speciesIndex &&
                                ecology.species[consumerIndex].supports(TrophicRole.PREDATOR) &&
                                ecology.interactions.get(speciesIndex, consumerIndex).kind ==
                                InteractionKind.PREDATION
                        }
                    if (eatsExtantPredator) apexPredators += biomass else predators += biomass
                }
                species.supports(TrophicRole.PRIMARY_CONSUMER) -> primaryConsumers += biomass
                else -> other += biomass
            }
        }
        return TrophicBiomass(producers, primaryConsumers, predators, apexPredators, other)
    }

    private fun invariantGuilds(tile: AuthoredEcosystemTile): List<SpeciesDefinition> = buildList {
        if (tile.includeAeroplankton) add(InvariantSpecies.AEROPLANKTON)
        if (tile.isLand) {
            add(InvariantSpecies.CARPET_PLANTS)
            add(InvariantSpecies.BUGS)
        }
        if (!tile.isLand || tile.adjacentToOcean > 0.0 || tile.adjacentToMajorRiver > 0.0) {
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
            EcologyBiomass.carryingCapacityKg(species, niche, environment)
        val viableSeedMultiplier = when (niche.strategy) {
            EcoStrategy.AMBUSH_PREDATION,
            EcoStrategy.PURSUIT_PREDATION,
            EcoStrategy.COLONY_RAIDING,
            EcoStrategy.GENERALIST_FORAGING,
            EcoStrategy.FILTER_FEEDING,
            EcoStrategy.NECTAR_FEEDING,
            EcoStrategy.SCAVENGING -> 20.0
            else -> 100.0
        }
        val trophicSeedFraction = when (niche.strategy) {
            EcoStrategy.PHOTOSYNTHESIS, EcoStrategy.ABSORPTION -> 0.65
            EcoStrategy.FILTER_FEEDING,
            EcoStrategy.GRAZING,
            EcoStrategy.FRUGIVORY,
            EcoStrategy.NECTAR_FEEDING,
            EcoStrategy.DEPOSIT_FEEDING,
            EcoStrategy.DECOMPOSITION,
            EcoStrategy.COPROPHAGY -> 0.45
            EcoStrategy.AMBUSH_PREDATION,
            EcoStrategy.PURSUIT_PREDATION,
            EcoStrategy.COLONY_RAIDING,
            EcoStrategy.GENERALIST_FORAGING,
            EcoStrategy.SCAVENGING,
            EcoStrategy.PARASITISM -> 0.015
        }
        return maxOf(
            species.physiology.massKg * viableSeedMultiplier,
            carryingBiomass * trophicSeedFraction / speciesSharingNiche.coerceAtLeast(1),
        )
    }

    private fun environmentAt(
        sample: ClimateDatumSample,
        annualAverageTemperature: Double,
        tile: AuthoredEcosystemTile,
        climateTileId: Int,
        year: Double,
        resources: FunctionalResources = FunctionalResources(),
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
            adjacentToLand = tile.adjacentToLand,
            adjacentToMajorRiver = tile.adjacentToMajorRiver,
            elevationM = tile.elevationM,
            waterDepthM = tile.waterDepthM,
            usefulSunlightReachesWater = tile.usefulSunlightReachesWater,
            canopyCover = tile.canopyCover,
            reefCover = tile.reefCover,
        )
        return base.withResources(resources)
    }

    private data class HealthResult(
        val name: String,
        val isLand: Boolean,
        val extant: Int,
        val total: Int,
        val tailCv: Double,
        val intendedStable: Boolean,
        val survivors: List<String>,
        val extinctionSeasons: Map<String, Int>,
        val missingRoles: List<TrophicRole>,
        val expectedExtinctions: Set<String>,
        val finalBiomassById: Map<String, Double>,
        val trophicBiomass: TrophicBiomass,
        val evolvingFilterFeederBiomass: Double,
        val poorClimateFits: List<String>,
    )

    private data class TrophicBiomass(
        val producers: Double,
        val primaryConsumers: Double,
        val predators: Double,
        val apexPredators: Double,
        val other: Double,
    )

    private enum class TrophicRole {
        PRODUCER,
        PRIMARY_CONSUMER,
        PREDATOR,
    }

    private fun CompiledSpecies.supports(role: TrophicRole): Boolean = when (role) {
        TrophicRole.PRODUCER ->
            niche.supportFor(EcoStrategy.PHOTOSYNTHESIS) > 0.0
        TrophicRole.PRIMARY_CONSUMER ->
            niche.supportFor(EcoStrategy.FILTER_FEEDING) > 0.0 ||
                niche.supportFor(EcoStrategy.GRAZING) > 0.0 ||
                niche.supportFor(EcoStrategy.NECTAR_FEEDING) > 0.0 ||
                niche.supportFor(EcoStrategy.DEPOSIT_FEEDING) > 0.0
        TrophicRole.PREDATOR ->
            niche.supportFor(EcoStrategy.AMBUSH_PREDATION) > 0.0 ||
                niche.supportFor(EcoStrategy.PURSUIT_PREDATION) > 0.0 ||
                niche.supportFor(EcoStrategy.COLONY_RAIDING) > 0.0
    }

    private companion object {
        const val SIMULATION_SEASONS = 4_000
    }
}