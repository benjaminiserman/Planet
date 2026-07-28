package dev.biserman.planet.planet.ecology.v2

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import kotlin.math.max
import kotlin.random.Random

/**
 * Production owner and Planet adapter for Ecology V2.
 *
 * Trait compilation remains global and immutable. Tiles serialize only local
 * population/resource state and lazily rebuild primitive runtime communities.
 */
object PlanetEcology {
    const val TARGET_RANDOM_EVOLVING_SPECIES = 10

    val definitions: List<SpeciesDefinition> =
        EarthSpeciesCatalog.ALL + InvariantSpecies.ALL
    val compiled: CompiledEcology by lazy {
        EcologyCompiler.compile(definitions)
    }

    private var runtimeConfig = EcologyRuntimeConfig()
    private var runtimeInstance: EcologyRuntime? = null
    private val runtime: EcologyRuntime
        get() = runtimeInstance ?: newRuntime().also { runtimeInstance = it }
    private var cachedPlanet: Planet? = null
    private var cachedWorld: WorldCache? = null
    private var radiationScratch: MovementScratch? = null

    /**
     * Makes values reloaded into [EcologyGlobals] effective on the next turn.
     * Existing ecosystem state is preserved; only the runtime parameter
     * snapshot is replaced.
     */
    fun refreshRuntimeConfig() {
        EcologyGlobals.validate()
        runtimeConfig = EcologyRuntimeConfig()
        runtimeInstance = null
    }

    private fun newRuntime() = EcologyRuntime(
        ecology = compiled,
        config = runtimeConfig,
        maximumPopulationsPerCell = TileEcosystem.MAXIMUM_POPULATIONS,
    )

    fun clearEcosystems(planet: Planet) {
        planet.planetTiles.values.forEach { it.ecosystem.clear() }
    }

    fun suitability(
        speciesId: String,
        tile: PlanetTile,
        habitat: Habitat? = null,
    ): SpeciesSuitability {
        val climate = requireNotNull(tile.planet.climateMap[tile.tileId]) {
            "Climate must be calculated for tile ${tile.tileId}"
        }
        val species = compiled.species[compiled.speciesIndex(speciesId)]
        return EcologySuitability.evaluate(
            species,
            compiled,
            PlanetEcologyEnvironment.annualEnvironments(
                tile,
                climate,
                worldCache(tile.planet).context,
            ),
            habitat,
        )
    }

    fun randomizeEcosystems(planet: Planet) {
        require(planet.planetTiles.keys.all { it in planet.climateMap }) {
            "Climate must be calculated for every tile before randomizing ecosystems"
        }
        val context = worldCache(planet).context
        planet.ecologyRandomizationCount++
        planet.planetTiles.values.forEach { tile ->
            val random = Random(
                scopedSeed(
                    planet.seed,
                    planet.ecologyRandomizationCount,
                    tile.tileId,
                    RANDOMIZATION_PROCESS,
                ),
            )
            randomizeEcosystem(tile, context, random)
        }
    }

    fun randomizeEcosystem(
        tile: PlanetTile,
        context: PlanetEcologyEnvironmentContext =
            PlanetEcologyEnvironment.context(tile.planet),
        random: Random = Random(
            scopedSeed(
                tile.planet.seed,
                tile.planet.ecologyRandomizationCount,
                tile.tileId,
                RANDOMIZATION_PROCESS,
            ),
        ),
    ) {
        val climate = requireNotNull(tile.planet.climateMap[tile.tileId]) {
            "Climate must be calculated for tile ${tile.tileId}"
        }
        tile.ecosystem.clear()
        tile.ecosystem.reefCover = initialReefCover(tile, climate.averageTemperature)
        val annualEnvironments =
            PlanetEcologyEnvironment.annualEnvironments(tile, climate, context)
        val currentEnvironment = PlanetEcologyEnvironment.environment(
            tile,
            climate,
            tile.planet.historyTurn / 4.0,
            context,
        )

        val invariantSpecies = relevantInvariantSpecies(currentEnvironment)
        val scoredCandidates = EarthSpeciesCatalog.ALL.map { definition ->
            val species = compiled.species[compiled.speciesIndex(definition.id)]
            species to EcologySuitability.evaluate(
                species,
                compiled,
                annualEnvironments,
            )
        }.filter { (_, suitability) -> suitability.suitable }
        val evolvingCount = (
            TARGET_RANDOM_EVOLVING_SPECIES - 2 +
                random.nextInt(5)
            ).coerceAtMost(scoredCandidates.size)
        val selectedEvolving = weightedSampleWithoutReplacement(
            candidates = scoredCandidates,
            count = evolvingCount,
            random = random,
        )
        val selected = (
            invariantSpecies.map { species ->
                species to EcologySuitability.evaluate(
                    species,
                    compiled,
                    annualEnvironments,
                )
            } + selectedEvolving
            ).distinctBy { (species, _) -> species.id }
        val nicheCounts = selected
            .groupingBy { (_, suitability) -> suitability.nicheIndex }
            .eachCount()

        selected.forEach { (species, suitability) ->
            val niche = compiled.niches[suitability.nicheIndex]
            val carryingCapacity =
                EcologyBiomass.carryingCapacityKg(species, niche, currentEnvironment)
            val capacityFraction = when (niche.strategy) {
                EcoStrategy.PHOTOSYNTHESIS, EcoStrategy.ABSORPTION ->
                    random.nextDouble(0.45, 0.75)
                EcoStrategy.FILTER_FEEDING,
                EcoStrategy.GRAZING,
                EcoStrategy.DEPOSIT_FEEDING,
                EcoStrategy.DECOMPOSITION,
                EcoStrategy.COPROPHAGY -> random.nextDouble(0.12, 0.32)
                EcoStrategy.AMBUSH_PREDATION,
                EcoStrategy.PURSUIT_PREDATION,
                EcoStrategy.GENERALIST_FORAGING,
                EcoStrategy.SCAVENGING,
                EcoStrategy.PARASITISM -> random.nextDouble(0.006, 0.018)
            }
            val biomass = max(
                species.massKg * 20.0,
                carryingCapacity * capacityFraction /
                    nicheCounts.getValue(suitability.nicheIndex),
            )
            tile.ecosystem.populations += EcosystemPopulation(
                speciesId = species.id,
                habitat = niche.habitat,
                strategy = niche.strategy,
                activeBiomassKg = biomass,
                reservesKg = biomass * minOf(0.40, species.reserveCapacity * 0.60),
            )
        }
        tile.ecosystem.invalidateRuntimeCache()
    }

    fun advanceAllOneSeason(planet: Planet) {
        val hasEcologyState = planet.planetTiles.values.any {
            it.ecosystem.populations.isNotEmpty() ||
                it.ecosystem.resources != FunctionalResources() ||
                it.ecosystem.reefCover > 0.0
        }
        if (!hasEcologyState) return
        require(planet.planetTiles.keys.all { it in planet.climateMap }) {
            "Climate must be calculated for every ecology tile"
        }

        val cache = worldCache(planet)
        val tiles = cache.tiles
        val context = cache.context
        val year = planet.historyTurn / 4.0
        val communities = Array(tiles.size) { index ->
            tiles[index].ecosystem.community(compiled)
        }
        val environments = Array(tiles.size) { index ->
            val tile = tiles[index]
            PlanetEcologyEnvironment.environment(
                tile,
                planet.climateMap.getValue(tile.tileId),
                year,
                context,
            )
        }
        val neighbors = cache.neighbors
        val fluxes = CellTurnFluxes()
        tiles.indices.forEach { index ->
            runtime.advanceSeason(
                community = communities[index],
                environment = environments[index],
                fluxes = fluxes,
                finalizeExtinctions = false,
            )
            val tile = tiles[index]
            tile.ecosystem.resources = updateResources(
                previous = tile.ecosystem.resources,
                fluxes = fluxes,
                areaKm2 = environments[index].areaKm2,
                hasMarineCompartment =
                    !tile.isAboveWater || tile.neighbors.any { !it.isAboveWater },
            )
            tile.ecosystem.reefCover =
                (tile.ecosystem.reefCover + fluxes.reefCoverDelta).coerceIn(0.0, 1.0)
        }

        val transferCapacity =
            communities.sumOf { it.size }.coerceAtLeast(1)
        val scratch = radiationScratch
            ?.takeIf { it.capacity >= transferCapacity }
            ?: MovementScratch(transferCapacity, compiled.niches.size).also {
                radiationScratch = it
            }
        EcologyMovement.applyRadiation(
            ecology = compiled,
            communities = communities,
            environments = environments,
            neighbors = neighbors,
            seasonIndex = planet.historyTurn,
            planetSeed = planet.seed,
            scratch = scratch,
            config = runtimeConfig,
            canEstablish = { speciesIndex, destinationIndex, nicheIndex ->
                val destination = communities[destinationIndex]
                if (destination.find(speciesIndex) >= 0) {
                    true
                } else {
                    val niche = compiled.niches[nicheIndex]
                    val habitatPopulationCount =
                        (0 until destination.size).count { populationIndex ->
                            compiled.niches[destination.nicheIndices[populationIndex]].habitat ==
                                niche.habitat
                        }
                    val establishmentCapacity =
                        EcologyDiversity.softHabitatCapacity(
                            environments[destinationIndex],
                            niche.habitat,
                        ) * EcologyGlobals.establishmentCapacityMultiplier
                    habitatPopulationCount < establishmentCapacity &&
                        annuallySuitable(
                            cache,
                            destinationIndex,
                            speciesIndex,
                            niche.habitat,
                        )
                }
            },
        )
        communities.forEach(runtime::finalizeLocalExtinctions)
        tiles.indices.forEach { index ->
            tiles[index].ecosystem.replaceWith(communities[index], compiled)
        }
    }

    private fun relevantInvariantSpecies(
        environment: SeasonalCellEnvironment,
    ): List<CompiledSpecies> = buildList {
        if (environment.isLand) {
            add(compiled.species[compiled.speciesIndex(InvariantSpecies.CARPET_PLANTS.id)])
            add(compiled.species[compiled.speciesIndex(InvariantSpecies.BUGS.id)])
        }
        val aquaticHabitatAvailable = EcologyFitness.aquaticHabitats.any {
            environment.habitatAvailability(it) > 0.0
        }
        if (aquaticHabitatAvailable) {
            add(compiled.species[compiled.speciesIndex(InvariantSpecies.SMALL_AQUATIC_LIFE.id)])
        }
        val photosyntheticWaterAvailable =
            listOf(Habitat.FRESHWATER, Habitat.COASTAL, Habitat.SUNLIT_WATER).any {
                environment.habitatAvailability(it) > 0.0
            }
        if (photosyntheticWaterAvailable) {
            add(compiled.species[compiled.speciesIndex(InvariantSpecies.PLANKTON.id)])
        }
    }

    private fun weightedSampleWithoutReplacement(
        candidates: List<Pair<CompiledSpecies, SpeciesSuitability>>,
        count: Int,
        random: Random,
    ): List<Pair<CompiledSpecies, SpeciesSuitability>> {
        val remaining = candidates.toMutableList()
        return buildList {
            repeat(count.coerceAtMost(remaining.size)) {
                val totalWeight = remaining.sumOf { (_, suitability) ->
                    suitability.score * suitability.score + 0.001
                }
                var choice = random.nextDouble(totalWeight)
                var selectedIndex = remaining.lastIndex
                for (index in remaining.indices) {
                    val suitability = remaining[index].second
                    choice -= suitability.score * suitability.score + 0.001
                    if (choice <= 0.0) {
                        selectedIndex = index
                        break
                    }
                }
                add(remaining.removeAt(selectedIndex))
            }
        }
    }

    private fun updateResources(
        previous: FunctionalResources,
        fluxes: CellTurnFluxes,
        areaKm2: Double,
        hasMarineCompartment: Boolean,
    ): FunctionalResources = FunctionalResources(
        carrion = OrganicPoolDynamics.update(
            previous.carrion,
            fluxes.carrionBiomass,
            fluxes.carrionConsumedBiomass,
            areaKm2 * 10.0,
            0.55,
        ),
        detritus = OrganicPoolDynamics.update(
            previous.detritus,
            fluxes.detritusBiomass,
            fluxes.detritusConsumedBiomass,
            areaKm2 * 12.0,
            0.68,
            maximumAccessibleFraction = 0.75,
        ),
        waste = OrganicPoolDynamics.update(
            previous.waste,
            fluxes.wasteBiomass,
            fluxes.wasteConsumedBiomass,
            areaKm2 * 10.0,
            0.60,
            maximumAccessibleFraction = 0.80,
        ),
        marineSnow =
            if (hasMarineCompartment) {
                OrganicPoolDynamics.update(
                    previous.marineSnow,
                    fluxes.marineSnowBiomass,
                    fluxes.marineSnowConsumedBiomass,
                    areaKm2 * 15.0,
                    0.72,
                )
            } else {
                0.0
            },
    )

    private fun initialReefCover(tile: PlanetTile, averageTemperatureC: Double): Double {
        val depth = (tile.planet.seaLevel - tile.elevation).coerceAtLeast(0.0)
        return if (
            !tile.isAboveWater &&
            depth <= 80.0 &&
            averageTemperatureC in 20.0..31.0
        ) {
            0.30
        } else {
            0.0
        }
    }

    private fun scopedSeed(
        planetSeed: Int,
        event: Long,
        tileId: Int,
        process: Int,
    ): Int {
        var value = planetSeed
        value = value * 31 + event.hashCode()
        value = value * 31 + tileId
        value = value * 31 + process
        value = (value xor (value ushr 16)) * 0x45D9F3B
        return value xor (value ushr 16)
    }

    private fun annuallySuitable(
        cache: WorldCache,
        tileIndex: Int,
        speciesIndex: Int,
        habitat: Habitat,
    ): Boolean {
        val cacheIndex = tileIndex * compiled.species.size + speciesIndex
        val habitatBit = 1 shl habitat.ordinal
        val knownMask = cache.knownSuitabilityHabitats[cacheIndex].toInt() and 0xFF
        if (knownMask and habitatBit == 0) {
            val tile = cache.tiles[tileIndex]
            val climate = tile.planet.climateMap.getValue(tile.tileId)
            val result = EcologySuitability.evaluate(
                species = compiled.species[speciesIndex],
                ecology = compiled,
                annualEnvironments = PlanetEcologyEnvironment.annualEnvironments(
                    tile,
                    climate,
                    cache.context,
                ),
                habitat = habitat,
            )
            cache.knownSuitabilityHabitats[cacheIndex] =
                (knownMask or habitatBit).toByte()
            if (result.suitable) {
                val suitableMask =
                    cache.suitableHabitats[cacheIndex].toInt() and 0xFF
                cache.suitableHabitats[cacheIndex] =
                    (suitableMask or habitatBit).toByte()
            }
        }
        return (
            cache.suitableHabitats[cacheIndex].toInt() and 0xFF and habitatBit
            ) != 0
    }

    @Synchronized
    private fun worldCache(planet: Planet): WorldCache {
        val existing = if (cachedPlanet === planet) cachedWorld else null
        if (
            existing != null &&
            existing.terrainRevision == planet.terrainChangeCount &&
            existing.climateMap === planet.climateMap &&
            existing.tiles.size == planet.planetTiles.size
        ) {
            return existing
        }

        val tiles = planet.planetTiles.values.sortedBy { it.tileId }
        val maximumTileId = tiles.maxOfOrNull { it.tileId } ?: -1
        val arrayIndexByTileId = IntArray(maximumTileId + 1) { -1 }
        tiles.forEachIndexed { index, tile ->
            arrayIndexByTileId[tile.tileId] = index
        }
        val neighbors = Array(tiles.size) { index ->
            val origin = tiles[index]
            origin.neighbors
                .asSequence()
                .filterNot(origin::hasImpassableEdgeWith)
                .map { adjacent ->
                    arrayIndexByTileId.getOrElse(adjacent.tileId) { -1 }
                }
                .filter { it >= 0 }
                .toList()
                .toIntArray()
        }
        return WorldCache(
            terrainRevision = planet.terrainChangeCount,
            climateMap = planet.climateMap,
            tiles = tiles,
            context = PlanetEcologyEnvironment.context(planet),
            neighbors = neighbors,
            knownSuitabilityHabitats =
                ByteArray(tiles.size * compiled.species.size),
            suitableHabitats =
                ByteArray(tiles.size * compiled.species.size),
        ).also {
            cachedPlanet = planet
            cachedWorld = it
        }
    }

    private data class WorldCache(
        val terrainRevision: ULong,
        val climateMap: Any,
        val tiles: List<PlanetTile>,
        val context: PlanetEcologyEnvironmentContext,
        val neighbors: Array<IntArray>,
        val knownSuitabilityHabitats: ByteArray,
        val suitableHabitats: ByteArray,
    )

    private const val RANDOMIZATION_PROCESS = 0x2C71_4A19
}
