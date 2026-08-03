package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatum
import kotlin.math.sqrt
import kotlin.random.Random

data class RandomEcosystemTile(
    val isLand: Boolean,
    val adjacentToOcean: Double,
    val adjacentToLand: Double,
    val adjacentToMajorRiver: Double,
    val waterDepthM: Double,
    val usefulSunlightReachesWater: Boolean,
    val canopyCover: Double,
    val reefCover: Double,
    val fertilityModifier: Double,
)

data class RandomEcosystemClimate(
    val presetId: String,
    val presetName: String,
    val ocean: Boolean,
    val annualMeanTemperatureC: Double,
    val annualTemperatureRangeC: Double,
    val annualPrecipitationMm: Double,
    val meanInsolation: Double,
    val starLight: StarLight,
)

data class RandomEcosystemResult(
    val seed: Int,
    val climate: RandomEcosystemClimate,
    val tile: RandomEcosystemTile,
    val selectedSpecies: List<String>,
    val invariantSpecies: List<String>,
    val chosenNiches: Map<String, String>,
    val extinctionSeasons: Map<String, Int>,
    val finalBiomassKg: Map<String, Double>,
    val biomassHistoryKg: Map<String, List<Double>>,
    val resourceHistory: Map<String, List<Double>>,
    val temperatureHistoryC: List<Double>,
    val precipitationHistoryMm: List<Double>,
    val unsupportedConsumers: List<String>,
    val maximumAnnualClimateFitness: Map<String, Double>,
    val anomalies: List<String>,
    val tailCoefficientOfVariation: Double,
) {
    val survivingSpecies: List<String>
        get() = finalBiomassKg.filterValues { it > 0.0 }.keys.toList()
}

/**
 * Reproducible randomized smoke test for arbitrary communities.
 *
 * Selection is filtered only for physical habitat support. Climate fitness and
 * food-web completeness are deliberately not pre-screened: those are exactly
 * the failure modes this experiment is meant to expose.
 */
object RandomEcosystemExperiment {
    private const val AREA_KM2 = 40_000.0
    private val catalogEcology by lazy { EcologyCompiler.compile(EarthSpeciesCatalog.ALL) }

    fun run(
        seed: Int,
        speciesCount: Int = 10,
        seasons: Int = 400,
    ): RandomEcosystemResult {
        require(speciesCount in 1..EarthSpeciesCatalog.ALL.size)
        require(seasons > 0)
        val random = Random(seed)
        val preset =
            HersfeldtClimatePresets.ALL[Math.floorMod(seed, HersfeldtClimatePresets.ALL.size)]
        val tile = randomTile(random, preset)
        val climate = presetClimate(seed, preset)
        val initialEnvironment = environmentAt(
            climate = climate.first,
            tile = tile,
            year = 0.5,
            resources = FunctionalResources(
                marineSnow = if (!tile.isLand || tile.adjacentToOcean > 0.0) 0.12 else 0.0,
            ),
        )
        val annualEnvironments = List(12) { month ->
            environmentAt(
                climate = climate.first,
                tile = tile,
                year = (month + 0.5) / 12.0,
                resources = FunctionalResources(),
            )
        }
        val birdIds = EarthSpeciesCatalog.BIRDS.mapTo(hashSetOf()) { it.id }
        val openOceanBirdIds = setOf("emperor-penguin", "wandering-albatross")
        val candidates = catalogEcology.species.filter { species ->
            val permittedOverOcean =
                tile.isLand || species.id !in birdIds || species.id in openOceanBirdIds
            permittedOverOcean &&
                EcologySuitability.evaluate(
                    species,
                    catalogEcology,
                    annualEnvironments,
                ).suitable
        }
        // Extremely restrictive habitats, especially permanent coastal sea
        // ice, may authentically have fewer than the requested ten candidates.
        val sampledCatalogSpecies = candidates.shuffled(random).take(speciesCount)
        val selectedCatalogSpecies = EcologyAssembly.completeRequiredTargets(
            ecology = catalogEcology,
            selected = sampledCatalogSpecies,
            availableTargets = candidates,
        )
        val selectedDefinitionsById = EarthSpeciesCatalog.ALL.associateBy { it.id }
        val selectedDefinitions = selectedCatalogSpecies.map { selectedDefinitionsById.getValue(it.id) }
        val selectedUsesAerialFilterFeeding = selectedCatalogSpecies.any { species ->
            val nicheIndex = NicheSelection.choose(species, catalogEcology, initialEnvironment)
            val niche = catalogEcology.niches[nicheIndex]
            niche.habitat == Habitat.AERIAL && niche.strategy == EcoStrategy.FILTER_FEEDING
        }
        val invariantDefinitions = invariantGuilds(tile, selectedUsesAerialFilterFeeding)
        val definitions = selectedDefinitions + invariantDefinitions
        val ecology = EcologyCompiler.compile(definitions)
        val runtime = EcologyRuntime(ecology)
        val nicheBySpecies = ecology.species.associate { species ->
            val nicheIndex = NicheSelection.choose(species, ecology, initialEnvironment)
            require(nicheIndex >= 0) { "No viable niche for ${species.displayName}" }
            species.index to nicheIndex
        }
        val nicheCounts = ecology.species
            .groupingBy { nicheBySpecies.getValue(it.index) }
            .eachCount()
        val community = TileCommunity(capacity = definitions.size + 4)
        ecology.species.forEach { species ->
            val nicheIndex = nicheBySpecies.getValue(species.index)
            val biomass = initialBiomass(
                species,
                ecology.niches[nicheIndex],
                initialEnvironment,
                nicheCounts.getValue(nicheIndex),
            )
            community.add(
                species.index,
                nicheIndex,
                biomass,
                reserves = biomass * minOf(0.40, species.lifeHistory.reserveCapacity * 0.60),
            )
        }

        val displayNames = ecology.species.associate { it.id to it.displayName }
        val biomassHistory = ecology.species.associate { it.id to ArrayList<Double>(seasons) }
        val resourceHistory = linkedMapOf(
            "Carrion" to ArrayList<Double>(seasons),
            "Detritus" to ArrayList<Double>(seasons),
            "Waste" to ArrayList<Double>(seasons),
            "Marine snow" to ArrayList<Double>(seasons),
            "Fruit" to ArrayList<Double>(seasons),
        )
        val temperatureHistory = ArrayList<Double>(seasons)
        val precipitationHistory = ArrayList<Double>(seasons)
        val extinctionSeasons = linkedMapOf<String, Int>()
        val totalHistory = DoubleArray(seasons)
        val fluxes = CellTurnFluxes()
        var resources = initialEnvironment.resources

        repeat(seasons) { season ->
            val year = season / 4.0
            val environment = environmentAt(climate.first, tile, year, resources)
            temperatureHistory += environment.temperatureC
            precipitationHistory += climate.first.sampleAt(year).precipitation *
                EcologyClimateVariability.anomaly(climate.first.tileId, year).precipitationMultiplier
            val presentBefore = presentSpeciesIds(community, ecology)
            runtime.advanceSeason(community, environment, fluxes)
            val presentAfter = presentSpeciesIds(community, ecology)
            (presentBefore - presentAfter).forEach { id ->
                extinctionSeasons.putIfAbsent(id, season)
            }
            resources = FunctionalResourceDynamics.update(
                previous = resources,
                fluxes = fluxes,
                areaKm2 = AREA_KM2,
                hasMarineCompartment = !tile.isLand || tile.adjacentToOcean > 0.0,
            )

            ecology.species.forEach { species ->
                val population = community.find(species.index)
                val biomass =
                    if (population < 0) {
                        0.0
                    } else {
                        community.activeBiomass[population] + community.dormantBiomass[population]
                    }
                biomassHistory.getValue(species.id) += biomass
            }
            resourceHistory.getValue("Carrion") += resources.carrion
            resourceHistory.getValue("Detritus") += resources.detritus
            resourceHistory.getValue("Waste") += resources.waste
            resourceHistory.getValue("Marine snow") += resources.marineSnow
            resourceHistory.getValue("Fruit") += resources.fruit
            totalHistory[season] = community.totalBiomass()
        }

        val finalBiomass = ecology.species.associate { species ->
            val population = community.find(species.index)
            displayNames.getValue(species.id) to
                if (population < 0) {
                    0.0
                } else {
                    community.activeBiomass[population] + community.dormantBiomass[population]
                }
        }
        val unsupportedConsumers = ecology.species
            .filter { it.kind == SpeciesKind.EVOLVING }
            .filter { species ->
                val niche = ecology.niches[nicheBySpecies.getValue(species.index)]
                niche.strategy in directConsumerStrategies &&
                    ecology.species.none { target ->
                        ecology.interactions.get(species.index, target.index).kind != InteractionKind.NONE
                    }
            }
            .map { it.displayName }
        val maximumAnnualClimateFitness = ecology.species.associate { species ->
            val niche = ecology.niches[nicheBySpecies.getValue(species.index)]
            species.displayName to (0 until 48).maxOf { sampleIndex ->
                EcologyFitness.combined(
                    species,
                    environmentAt(
                        climate = climate.first,
                        tile = tile,
                        year = sampleIndex / 48.0,
                        resources = initialEnvironment.resources,
                    ),
                    niche.habitat,
                )
            }
        }
        val anomalies = detectAnomalies(
            ecology,
            community,
            nicheBySpecies,
            totalHistory,
            maximumAnnualClimateFitness,
        )
        val tailStart = (seasons * 4 / 5).coerceAtMost(seasons - 1)
        val tail = totalHistory.copyOfRange(tailStart, seasons)
        val tailMean = tail.average()
        val tailCv =
            if (tailMean <= 0.0) {
                Double.POSITIVE_INFINITY
            } else {
                sqrt(tail.sumOf { (it - tailMean) * (it - tailMean) } / tail.size) / tailMean
            }

        return RandomEcosystemResult(
            seed = seed,
            climate = climate.second,
            tile = tile,
            selectedSpecies = selectedCatalogSpecies.map { it.displayName },
            invariantSpecies = invariantDefinitions.map { it.displayName },
            chosenNiches = ecology.species.associate { species ->
                species.displayName to ecology.niches[nicheBySpecies.getValue(species.index)].displayName
            },
            extinctionSeasons = extinctionSeasons.mapKeys { displayNames.getValue(it.key) },
            finalBiomassKg = finalBiomass,
            biomassHistoryKg = biomassHistory.mapKeys { displayNames.getValue(it.key) },
            resourceHistory = resourceHistory,
            temperatureHistoryC = temperatureHistory,
            precipitationHistoryMm = precipitationHistory,
            unsupportedConsumers = unsupportedConsumers,
            maximumAnnualClimateFitness = maximumAnnualClimateFitness,
            anomalies = anomalies,
            tailCoefficientOfVariation = tailCv,
        )
    }

    private fun randomTile(
        random: Random,
        preset: HersfeldtClimatePreset,
    ): RandomEcosystemTile {
        val land = !preset.ocean
        val coastal = land && random.nextDouble() < 0.24
        val adjacentLand = !land && random.nextDouble() < 0.24
        val river = land && random.nextDouble() < 0.20
        val depth = when (preset) {
            HersfeldtClimatePresets.TROPICAL_REEF -> randomBetween(random, 5.0, 65.0)
            HersfeldtClimatePresets.TEMPERATE_SHELF -> randomBetween(random, 20.0, 180.0)
            HersfeldtClimatePresets.POLAR_SEA -> randomBetween(random, 30.0, 800.0)
            HersfeldtClimatePresets.PERMANENT_SEA_ICE -> randomBetween(random, 30.0, 800.0)
            HersfeldtClimatePresets.DEEP_OCEAN -> randomBetween(random, 600.0, 3_500.0)
            else -> 0.0
        }
        val sunlight = when (preset) {
            HersfeldtClimatePresets.TROPICAL_REEF -> true
            HersfeldtClimatePresets.TEMPERATE_SHELF -> depth < 140.0
            HersfeldtClimatePresets.POLAR_SEA -> depth < 160.0
            HersfeldtClimatePresets.PERMANENT_SEA_ICE -> depth < 160.0
            HersfeldtClimatePresets.DEEP_OCEAN -> false
            else -> false
        }
        val reef = when (preset) {
            HersfeldtClimatePresets.TROPICAL_REEF -> randomBetween(random, 0.35, 0.90)
            HersfeldtClimatePresets.TEMPERATE_SHELF -> randomBetween(random, 0.0, 0.25)
            else -> if (coastal) randomBetween(random, 0.0, 0.85) else 0.0
        }
        return RandomEcosystemTile(
            isLand = land,
            adjacentToOcean = if (coastal) 1.0 else 0.0,
            adjacentToLand = if (adjacentLand) 1.0 else 0.0,
            adjacentToMajorRiver = if (river) 1.0 else 0.0,
            waterDepthM = depth,
            usefulSunlightReachesWater = sunlight,
            canopyCover = if (land) randomBetween(random, 0.0, 0.92) else 0.0,
            reefCover = reef,
            fertilityModifier = randomBetween(random, -0.55, 0.75),
        )
    }

    private fun presetClimate(
        seed: Int,
        preset: HersfeldtClimatePreset,
    ): Pair<ClimateDatum, RandomEcosystemClimate> {
        val starLight = StarLight.entries[Math.floorMod(seed, StarLight.entries.size)]
        val datum = preset.climateDatum(seed)
        return datum to RandomEcosystemClimate(
            presetId = preset.id,
            presetName = preset.displayName,
            ocean = preset.ocean,
            annualMeanTemperatureC = datum.averageTemperature,
            annualTemperatureRangeC = datum.temperatureRange,
            annualPrecipitationMm = datum.annualPrecipitation,
            meanInsolation = preset.months.map { it.insolation / 340.0 }.average(),
            starLight = starLight,
        )
    }

    private fun invariantGuilds(
        tile: RandomEcosystemTile,
        includeAeroplankton: Boolean,
    ): List<SpeciesDefinition> = buildList {
        if (tile.isLand) {
            add(InvariantSpecies.CARPET_PLANTS)
            add(InvariantSpecies.BUGS)
        }
        if (!tile.isLand || tile.adjacentToOcean > 0.0 || tile.adjacentToMajorRiver > 0.0) {
            if (
                tile.usefulSunlightReachesWater ||
                tile.adjacentToOcean > 0.0 ||
                tile.adjacentToMajorRiver > 0.0
            ) {
                add(InvariantSpecies.PLANKTON)
            }
            add(InvariantSpecies.SMALL_AQUATIC_LIFE)
        }
        if (includeAeroplankton) add(InvariantSpecies.AEROPLANKTON)
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
        climate: ClimateDatum,
        tile: RandomEcosystemTile,
        year: Double,
        resources: FunctionalResources,
    ): SeasonalCellEnvironment {
        val sample = climate.sampleAt(year)
        val anomaly = EcologyClimateVariability.anomaly(climate.tileId, year)
        return SeasonalCellEnvironment.create(
            areaKm2 = AREA_KM2,
            temperatureC = sample.averageTemperature + anomaly.temperatureC,
            annualAverageTemperatureC = climate.averageTemperature,
            insolation = (sample.insolation / 340.0).coerceIn(0.0, 1.0),
            precipitationMm = sample.precipitation * anomaly.precipitationMultiplier,
            surfaceFertilityModifier = tile.fertilityModifier,
            isLand = tile.isLand,
            adjacentToOcean = tile.adjacentToOcean,
            adjacentToLand = tile.adjacentToLand,
            adjacentToMajorRiver = tile.adjacentToMajorRiver,
            waterDepthM = tile.waterDepthM,
            usefulSunlightReachesWater = tile.usefulSunlightReachesWater,
            permanentSeaIce = !tile.isLand && PlanetEcologyEnvironment.supportsSeaIceHabitat(climate),
            canopyCover = tile.canopyCover,
            reefCover = tile.reefCover,
            starLight = StarLight.entries[Math.floorMod(climate.tileId, StarLight.entries.size)],
            resources = resources,
        )
    }

    private fun presentSpeciesIds(
        community: TileCommunity,
        ecology: CompiledEcology,
    ): Set<String> = buildSet {
        repeat(community.size) { population ->
            add(ecology.species[community.speciesIndices[population]].id)
        }
    }

    private fun detectAnomalies(
        ecology: CompiledEcology,
        community: TileCommunity,
        nicheBySpecies: Map<Int, Int>,
        totalHistory: DoubleArray,
        maximumAnnualClimateFitness: Map<String, Double>,
    ): List<String> = buildList {
        if (totalHistory.any { !it.isFinite() || it < 0.0 }) {
            add("Non-finite or negative total biomass")
        }
        ecology.species.forEach { consumer ->
            val population = community.find(consumer.index)
            if (population < 0 || consumer.kind == SpeciesKind.INVARIANT) return@forEach
            if (community.activeBiomass[population] < consumer.physiology.massKg * 2.0) return@forEach
            val climateFitness = maximumAnnualClimateFitness.getValue(consumer.displayName)
            if (climateFitness < 0.35) {
                add(
                    "${consumer.displayName} survived despite maximum annual climate fitness " +
                        "${"%.2f".format(climateFitness)}",
                )
            }
            val niche = ecology.niches[nicheBySpecies.getValue(consumer.index)]
            if (niche.strategy !in directConsumerStrategies) return@forEach
            val extantFood = ecology.species.any { target ->
                community.find(target.index) >= 0 &&
                    ecology.interactions.get(consumer.index, target.index).kind != InteractionKind.NONE
            }
            if (!extantFood) add("${consumer.displayName} survived without an extant modeled food source")
        }
    }

    private fun randomBetween(random: Random, minimum: Double, maximum: Double): Double =
        minimum + random.nextDouble() * (maximum - minimum)

    private val directConsumerStrategies = setOf(
        EcoStrategy.FILTER_FEEDING,
        EcoStrategy.GRAZING,
        EcoStrategy.AMBUSH_PREDATION,
        EcoStrategy.PURSUIT_PREDATION,
        EcoStrategy.COLONY_RAIDING,
        EcoStrategy.GENERALIST_FORAGING,
    )
}