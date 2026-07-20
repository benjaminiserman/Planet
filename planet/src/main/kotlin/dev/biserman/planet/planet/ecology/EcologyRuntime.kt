package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.BiotaDistributionAquatic
import dev.biserman.planet.planet.BiotaDistributionMethod
import dev.biserman.planet.planet.BiotaDistributionTerrestrial
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.climate.ClimateDatum
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random
import java.util.concurrent.ForkJoinPool
import kotlin.jvm.optionals.getOrNull

object EcologyRuntime {
    internal fun SpeciesDefinition.matchesHabitat(method: BiotaDistributionMethod): Boolean =
        if (method === BiotaDistributionAquatic) {
            Trait.AQUATIC in traits
        } else {
            Trait.AQUATIC !in traits || Trait.AMPHIBIOUS in traits
        }

    fun ordersCompatibleWith(method: BiotaDistributionMethod): List<TaxonomicOrder> =
        speciesCatalogByTaxonomicOrder
            .filterValues { species -> species.any { it.matchesHabitat(method) } }
            .keys
            .toList()

    fun clearEcosystems(planet: Planet) {
        planet.planetTiles.values.forEach { it.ecosystem.clear() }
    }

    fun randomizeEcosystems(planet: Planet) {
        EcologyConfig.validate()
        require(planet.planetTiles.keys.all { it in planet.climateMap }) {
            "Climate must be calculated for every tile before randomizing ecosystems"
        }
        planet.planetTiles.values.forEach { randomizeEcosystemUnchecked(it, planet.random) }
    }

    fun randomizeEcosystem(tile: PlanetTile, random: Random = tile.planet.random) {
        EcologyConfig.validate()
        require(tile.tileId in tile.planet.climateMap) {
            "Climate must be calculated for tile ${tile.tileId} before randomizing its ecosystem"
        }
        randomizeEcosystemUnchecked(tile, random)
    }

    private fun randomizeEcosystemUnchecked(tile: PlanetTile, random: Random) {
        tile.ecosystem.clear()
        val localOrders = tile.planet.biotaDistributions.asSequence()
            .filter { tile in it.region.tiles }
            .map { it.taxonomicOrder }
        val availableOrders = (EcologyConfig.globallyDistributedOrders.asSequence() + localOrders).toSet()
        val habitatSpecies = availableOrders.asSequence()
            .flatMap { speciesCatalogByTaxonomicOrder[it].orEmpty().asSequence() }
            .filter { species ->
                species.matchesHabitat(if (tile.isAboveWater) BiotaDistributionTerrestrial else BiotaDistributionAquatic)
            }
            .distinctBy { it.id }
            .toList()
        if (habitatSpecies.isEmpty()) return

        val climate = tile.planet.climateMap[tile.tileId] ?: return
        val environment = tile.hersfeldt.getOrNull()
            ?.let { hersfeldtEnvironmentProfile(it, climate) }
            ?: EnvironmentProfile(emptyMap())
        val averageStress = habitatSpecies.associateWith { species ->
            averageAnnualClimateStress(species, climate, tile.elevationAboveSeaLevel)
        }
        val suitedSpecies = habitatSpecies
            .filter { species -> isSpeciesSuitedTo(species, climate, tile.elevationAboveSeaLevel) }
            .filter { species ->
                species.environmentAffinity(environment) >= EcologyConfig.minimumEnvironmentAffinity
            }
        val preferredPool = suitedSpecies
        val producerPool = suitedSpecies.filter { it.autotrophy != null }.ifEmpty {
            leastUnsuitableProducers(
                habitatSpecies.filter { it.autotrophy != null },
                climate,
                tile.elevationAboveSeaLevel,
            )
        }
        val selected = selectSpeciesForEcosystem(
            preferredPool = preferredPool,
            producerPool = producerPool,
            limit = EcologyConfig.maxSpeciesPerEcosystem,
            random = random,
            weight = { species ->
                speciesEstablishmentWeight(
                    species,
                    environment,
                    averageStress.getValue(species),
                )
            },
        )
        if (selected.isEmpty()) return

        val model = modelFor(tile, selected)
        tile.ecosystem.biomass.putAll(initialBiomass(model, random))
    }

    fun advanceAllOneSeason(planet: Planet) {
        EcologyConfig.validate()
        val wallStart = System.nanoTime()
        val startYear = planet.historyTurn / 4.0
        val tiles = planet.planetTiles.values.toList()
        val profile = EcologyProfiler.begin(planet.historyTurn, tiles.size)
        val advanceTile: (PlanetTile) -> Unit = { tile ->
            advanceOneSeason(
                tile,
                startYear,
                Random(seasonalNoiseSeed(planet.seed, planet.historyTurn, tile.tileId)),
            )
        }
        try {
            if (EcologyConfig.parallelTileSimulation) {
                val pool = ForkJoinPool(EcologyConfig.parallelTileWorkers)
                try {
                    pool.submit { tiles.parallelStream().forEach(advanceTile) }.get()
                } finally {
                    pool.shutdown()
                }
            } else {
                tiles.forEach(advanceTile)
            }
        } finally {
            EcologyProfiler.finish(profile, System.nanoTime() - wallStart)
        }
    }

    fun advanceOneSeason(tile: PlanetTile, startYear: Double, random: Random = tile.planet.random) {
        val profile = EcologyProfiler.current
        if (tile.ecosystem.biomass.isEmpty()) {
            profile?.emptyTiles?.increment()
            return
        }
        val tileStart = if (profile != null) System.nanoTime() else 0L
        val definitions = tile.ecosystem.speciesIds.mapNotNull { speciesId ->
            speciesCatalogById[speciesId]
        }
        if (definitions.isEmpty()) {
            profile?.emptyTiles?.increment()
            tile.ecosystem.clear()
            return
        }
        profile?.activeTiles?.increment()
        profile?.totalSpecies?.add(definitions.size.toLong())
        val modelStart = if (profile != null) System.nanoTime() else 0L
        val model = modelFor(tile, definitions)
        if (profile != null) profile.modelNanos.add(System.nanoTime() - modelStart)
        val steps = EcologyConfig.integrationSubstepsPerSeason.coerceAtLeast(13)
        val solverStart = if (profile != null) System.nanoTime() else 0L
        val result = simulate(
            model = model,
            initial = definitions.associate { it.id to (tile.ecosystem.biomass[it.id] ?: 0.0) },
            years = 0.25,
            dt = 0.25 / steps,
            sampleEverySteps = steps,
            noise = PopulationNoise(
                environmentalVolatility = EcologyConfig.environmentalVolatility,
                speciesVolatility = EcologyConfig.speciesVolatility,
            ),
            random = random,
            minimumIndividuals = EcologyConfig.minimumViableIndividuals,
            startYear = startYear,
        ).last().biomass
        if (profile != null) profile.solverNanos.add(System.nanoTime() - solverStart)
        val finalizationStart = if (profile != null) System.nanoTime() else 0L
        tile.ecosystem.biomass = result.filterValues { it > 0.0 }.toMutableMap()
        if (profile != null) {
            profile.finalizationNanos.add(System.nanoTime() - finalizationStart)
            profile.recordTile(tile.tileId, definitions.size, System.nanoTime() - tileStart)
        }
    }

    fun biomassDensity(tile: PlanetTile, speciesId: String): Double =
        (tile.ecosystem.biomass[speciesId] ?: 0.0) / simulationArea(tile)

    /** Effective solver area; the default 1e-6 scale converts square metres to square kilometres. */
    fun simulationArea(tile: PlanetTile): Double = max(
        1.0,
        tile.tile.area * tile.planet.radiusMeters.pow(2) * EcologyConfig.simulationAreaPerSquareMeter,
    )

    private fun modelFor(tile: PlanetTile, definitions: List<SpeciesDefinition>): EcosystemModel {
        val area = simulationArea(tile)
        val climate = tile.planet.climateMap[tile.tileId] ?: oceanicTemperateClimate
        val altitude = tile.elevationAboveSeaLevel
        val speciesIds = definitions.mapTo(mutableSetOf()) { it.id }
        tile.ecosystem.cachedModel?.let { cached ->
            if (cached.speciesById.keys == speciesIds &&
                cached.landArea == area &&
                cached.climate === climate &&
                cached.altitudeMeters == altitude
            ) {
                EcologyProfiler.current?.modelCacheHits?.increment()
                return cached
            }
        }
        EcologyProfiler.current?.modelCacheMisses?.increment()
        return EcosystemModel(
            species = definitions,
            seasonsEnabled = true,
            landArea = area,
            climate = climate,
            altitudeMeters = altitude,
        ).also { tile.ecosystem.cachedModel = it }
    }

    private fun initialBiomass(model: EcosystemModel, random: Random): Biomass =
        model.species.associate { definition ->
            val density = when {
                definition.isSessileProducer() -> {
                    val guildSize = model.sessileProducerGroups.getValue(definition.sizeClass).size
                    model.sessileCarryingCapacityDensity(definition.sizeClass, 0.0) *
                        random.nextDouble(
                            EcologyConfig.initialProducerCapacityFractionMin,
                            EcologyConfig.initialProducerCapacityFractionMax,
                        ) / guildSize
                }
                definition.autotrophy != null -> definition.autotrophy.baseCarryingCapacity *
                    random.nextDouble(
                        EcologyConfig.initialProducerCapacityFractionMin,
                        EcologyConfig.initialProducerCapacityFractionMax,
                    )
                else -> random.nextDouble(
                    EcologyConfig.initialConsumerDensityMin,
                    EcologyConfig.initialConsumerDensityMax,
                )
            }
            definition.id to max(density * model.landArea, 4.0 * definition.individualBiomass)
        }
}

internal fun seasonalNoiseSeed(planetSeed: Int, historyTurn: Long, tileId: Int): Int {
    var hash = planetSeed
    hash = 31 * hash + historyTurn.hashCode()
    hash = 31 * hash + tileId
    return hash
}

internal fun selectSpeciesForEcosystem(
    preferredPool: List<SpeciesDefinition>,
    producerPool: List<SpeciesDefinition>,
    limit: Int,
    random: Random,
    weight: (SpeciesDefinition) -> Double = { 1.0 },
): List<SpeciesDefinition> {
    require(limit >= 1)
    val producer = producerPool.weightedRandomOrNull(random, weight) ?: return emptyList()
    return buildList {
        add(producer)
        addAll((preferredPool - producer).weightedSampleWithoutReplacement(limit - 1, random, weight))
    }
}

internal fun <T> List<T>.weightedRandomOrNull(
    random: Random,
    weight: (T) -> Double,
): T? {
    if (isEmpty()) return null
    val usableWeights = map { weight(it).takeIf { value -> value.isFinite() && value > 0.0 } ?: 0.0 }
    val total = usableWeights.sum()
    if (total <= 0.0) return randomOrNull(random)
    var remaining = random.nextDouble(total)
    for (index in indices) {
        remaining -= usableWeights[index]
        if (remaining <= 0.0) return this[index]
    }
    return last()
}

internal fun <T> List<T>.weightedSampleWithoutReplacement(
    count: Int,
    random: Random,
    weight: (T) -> Double,
): List<T> {
    if (count <= 0 || isEmpty()) return emptyList()
    val remaining = toMutableList()
    return buildList {
        repeat(count.coerceAtMost(remaining.size)) {
            val selected = remaining.weightedRandomOrNull(random, weight) ?: return@repeat
            add(selected)
            remaining.remove(selected)
        }
    }
}

internal fun leastUnsuitableProducers(
    producers: List<SpeciesDefinition>,
    climate: ClimateDatum,
    elevationMeters: Double,
): List<SpeciesDefinition> {
    val scores = producers.associateWith { species ->
        speciesSuitabilityIssueCount(species, climate, elevationMeters)
    }
    val bestScore = scores.values.minOrNull() ?: return emptyList()
    return scores.filterValues { it == bestScore }.keys.toList()
}
