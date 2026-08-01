package dev.biserman.planet.planet.ecology

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

data class FunctionalResources(
    val carrion: Double = 0.0,
    val detritus: Double = 0.0,
    val waste: Double = 0.0,
    val marineSnow: Double = 0.0,
    val fruit: Double = 0.0,
)

/**
 * Standalone ecology input. A later adapter can construct this from PlanetTile,
 * ClimateDatumSample, surface StoneComponent, and river/ocean topology.
 */
class SeasonalCellEnvironment private constructor(
    val areaKm2: Double,
    val temperatureC: Double,
    val annualAverageTemperatureC: Double,
    val insolation: Double,
    val waterAvailability: Double,
    val fertility: Double,
    val acidity: Double,
    val canopyCover: Double,
    val reefCover: Double,
    val snowOrIce: Boolean,
    val starLight: StarLight,
    val isLand: Boolean,
    val adjacentToLand: Boolean,
    val elevationM: Double,
    val waterDepthM: Double,
    val resources: FunctionalResources,
    private val habitatAvailability: DoubleArray,
) {
    init {
        require(areaKm2 > 0.0)
        require(habitatAvailability.size == Habitat.entries.size)
    }

    fun habitatAvailability(habitat: Habitat): Double =
        habitatAvailability[habitat.ordinal]

    fun resourceSupport(
        niche: NicheDefinition,
        consumerSize: SizeClass,
    ): Double = niche.strategy.resourceSupport(this, niche.habitat, consumerSize)

    fun lightAt(habitat: Habitat): Double =
        habitat.availableLight(insolation, canopyCover)

    fun withResources(resources: FunctionalResources): SeasonalCellEnvironment =
        SeasonalCellEnvironment(
            areaKm2 = areaKm2,
            temperatureC = temperatureC,
            annualAverageTemperatureC = annualAverageTemperatureC,
            insolation = insolation,
            waterAvailability = waterAvailability,
            fertility = fertility,
            acidity = acidity,
            canopyCover = canopyCover,
            reefCover = reefCover,
            snowOrIce = snowOrIce,
            starLight = starLight,
            isLand = isLand,
            adjacentToLand = adjacentToLand,
            elevationM = elevationM,
            waterDepthM = waterDepthM,
            resources = resources,
            habitatAvailability = habitatAvailability.copyOf(),
        )

    companion object {
        fun create(
            areaKm2: Double,
            temperatureC: Double,
            annualAverageTemperatureC: Double = temperatureC,
            insolation: Double,
            precipitationMm: Double,
            surfaceFertilityModifier: Double = 0.0,
            surfaceMoistureCapacityMultiplier: Double = 1.0,
            surfaceAcidityModifier: Double = 0.0,
            isLand: Boolean,
            adjacentToOcean: Boolean = false,
            adjacentToLand: Boolean = false,
            adjacentToMajorRiver: Boolean = false,
            elevationM: Double = 0.0,
            waterDepthM: Double = 0.0,
            usefulSunlightReachesWater: Boolean = true,
            permanentSeaIce: Boolean = false,
            canopyCover: Double = 0.0,
            reefCover: Double = 0.0,
            starLight: StarLight = StarLight.YELLOW,
            resources: FunctionalResources = FunctionalResources(),
            habitatOverrides: Map<Habitat, Double> = emptyMap(),
        ): SeasonalCellEnvironment {
            require(insolation in 0.0..1.0)
            require(precipitationMm >= 0.0)
            require(surfaceMoistureCapacityMultiplier > 0.0)
            require(canopyCover in 0.0..1.0)
            require(reefCover in 0.0..1.0)
            // Exposed land can lie below mean sea level in enclosed basins.
            // Water depth remains the separate non-negative aquatic measure.
            require(elevationM.isFinite())
            require(waterDepthM >= 0.0)
            require(!permanentSeaIce || !isLand)

            val evaporationDemand = 45.0 + max(temperatureC, 0.0) * 3.2 + insolation * 95.0
            val retainedPrecipitation = precipitationMm * surfaceMoistureCapacityMultiplier
            var water = retainedPrecipitation / (retainedPrecipitation + evaporationDemand)
            if (adjacentToMajorRiver) water += 0.30
            if (isLand && adjacentToOcean) water += 0.06
            water = water.coerceIn(0.0, 1.0)

            val habitats = DoubleArray(Habitat.entries.size)
            if (isLand) {
                habitats[Habitat.LAND_SURFACE.ordinal] = 1.0
                habitats[Habitat.CANOPY.ordinal] = canopyCover
                habitats[Habitat.AERIAL.ordinal] = 0.45
                if (adjacentToMajorRiver) habitats[Habitat.FRESHWATER.ordinal] = 0.42
                if (adjacentToOcean) habitats[Habitat.COASTAL.ordinal] = 0.48
            } else {
                habitats[Habitat.AERIAL.ordinal] = 0.30
                if (adjacentToLand) {
                    habitats[Habitat.COASTAL.ordinal] = 1.0
                }
                if (permanentSeaIce) {
                    habitats[Habitat.SEA_ICE.ordinal] = 0.80
                }
                if (usefulSunlightReachesWater) {
                    habitats[Habitat.SUNLIT_WATER.ordinal] = 1.0
                }
                if (!usefulSunlightReachesWater || waterDepthM >= 180.0) {
                    habitats[Habitat.DARK_WATER.ordinal] =
                        if (usefulSunlightReachesWater) 0.75 else 1.0
                }
            }
            habitatOverrides.forEach { (habitat, availability) ->
                habitats[habitat.ordinal] = availability.coerceIn(0.0, 1.0)
            }

            val snowOrIce = permanentSeaIce || (temperatureC < 0.0 && water > annualAverageTemperatureC / 100.0)

            return SeasonalCellEnvironment(
                areaKm2 = areaKm2,
                temperatureC = temperatureC,
                annualAverageTemperatureC = annualAverageTemperatureC,
                insolation = insolation,
                waterAvailability = water,
                fertility = (0.55 + surfaceFertilityModifier * 0.30).coerceIn(0.05, 1.0),
                acidity = surfaceAcidityModifier.coerceIn(-1.0, 1.0),
                canopyCover = canopyCover,
                reefCover = reefCover,
                snowOrIce = snowOrIce,
                starLight = starLight,
                isLand = isLand,
                adjacentToLand = adjacentToLand,
                elevationM = elevationM,
                waterDepthM = waterDepthM,
                resources = resources,
                habitatAvailability = habitats,
            )
        }
    }
}

object EcologyFitness {
    fun reefAssociationMultiplier(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
    ): Double =
        (1.0 - species.reefUse) + environment.reefCover * species.reefUse * 2.0

    fun temperature(species: CompiledSpecies, temperatureC: Double): Double = when {
        temperatureC <= species.temperatureOuterLow -> 0.0
        temperatureC < species.temperatureOptimalLow ->
            (temperatureC - species.temperatureOuterLow) /
                    (species.temperatureOptimalLow - species.temperatureOuterLow)

        temperatureC <= species.temperatureOptimalHigh -> 1.0
        temperatureC < species.temperatureOuterHigh ->
            (species.temperatureOuterHigh - temperatureC) /
                    (species.temperatureOuterHigh - species.temperatureOptimalHigh)

        else -> 0.0
    }.coerceIn(0.0, 1.0)

    fun seasonalTemperature(
        species: CompiledSpecies,
        temperatureC: Double,
        insolation: Double,
    ): Double {
        val trigger = species.seasonalColdTriggerInsolation
        if (species.seasonalColdToleranceC <= 0.0 || trigger <= 0.0 || insolation >= trigger) {
            return temperature(species, temperatureC)
        }
        val coatFraction = ((trigger - insolation) / trigger).coerceIn(0.0, 1.0)
        val coldBonus = species.seasonalColdToleranceC * coatFraction
        val adjustedOuterLow = species.temperatureOuterLow - coldBonus
        val adjustedOptimalLow = species.temperatureOptimalLow - coldBonus * 0.45
        return when {
            temperatureC <= adjustedOuterLow -> 0.0
            temperatureC < adjustedOptimalLow ->
                (temperatureC - adjustedOuterLow) / (adjustedOptimalLow - adjustedOuterLow)

            temperatureC <= species.temperatureOptimalHigh -> 1.0
            temperatureC < species.temperatureOuterHigh ->
                (species.temperatureOuterHigh - temperatureC) /
                        (species.temperatureOuterHigh - species.temperatureOptimalHigh)

            else -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    fun water(species: CompiledSpecies, environment: SeasonalCellEnvironment, habitat: Habitat): Double {
        if (habitat.aquatic) return 1.0
        val frozenWaterAvailable =
            environment.snowOrIce ||
                    (
                            environment.waterAvailability > 0.0 &&
                                    (
                                            environment.temperatureC <= 0.0 ||
                                                    environment.annualAverageTemperatureC <= 0.0
                                            )
                            )
        val water =
            if (species.snowHydration && frozenWaterAvailable) {
                max(environment.waterAvailability, species.minimumWater)
            } else {
                environment.waterAvailability
            }
        if (water < species.minimumWater) {
            if (species.minimumWater <= 0.0) return 1.0
            return (water / species.minimumWater).coerceIn(0.0, 1.0)
        }
        if (water <= species.optimalMaximumWater) return 1.0
        if (species.maximumWater <= species.optimalMaximumWater) return 0.0
        return (
                (species.maximumWater - water) /
                        (species.maximumWater - species.optimalMaximumWater)
                ).coerceIn(0.0, 1.0)
    }

    fun light(species: CompiledSpecies, environment: SeasonalCellEnvironment, habitat: Habitat): Double {
        if (species.strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] <= 0.0) return 1.0
        var available = environment.lightAt(habitat)
        if (habitat == Habitat.LAND_SURFACE && environment.canopyCover > 0.0) {
            available += environment.canopyCover * species.canopyLightEfficiency
        }
        val distance = abs(available - species.insolationOptimum)
        val quantityFit = (1.0 - distance / 0.65).coerceIn(0.0, 1.0)
        val spectrumFit = LightColorModel.photosyntheticMatch(
            environment.starLight,
            species.photosyntheticColor ?: BiologicalColor.GREEN,
        )
        return quantityFit * spectrumFit
    }

    fun combined(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
    ): Double =
        habitat(species, environment, habitat) *
                elevation(species, environment, habitat) *
                thermal(species, environment) *
                water(species, environment, habitat) *
                light(species, environment, habitat) *
                vegetationStructure(species, environment, habitat)

    fun elevation(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
    ): Double {
        if (!species.motile || !environment.isLand || habitat == Habitat.AERIAL || habitat.aquatic) {
            return 1.0
        }
        // Elevation adaptations shift the whole viable band upward. This
        // grants access to thin-air habitats while making a high-altitude
        // specialist progressively less fit below its adapted range.
        val optimalMinimumM =
            EcologyGlobals.normalMinimumElevationM + species.elevationToleranceShiftM
        val lethalMinimumM =
            EcologyGlobals.lethalMinimumElevationM + species.elevationToleranceShiftM
        val optimalMaximumM =
            EcologyGlobals.normalElevationLimitM + species.elevationToleranceShiftM
        val lethalMaximumM =
            EcologyGlobals.lethalElevationLimitM + species.elevationToleranceShiftM
        return when {
            environment.elevationM <= lethalMinimumM -> 0.0
            environment.elevationM < optimalMinimumM ->
                (environment.elevationM - lethalMinimumM) /
                        (optimalMinimumM - lethalMinimumM)
            environment.elevationM <= optimalMaximumM -> 1.0
            environment.elevationM >= lethalMaximumM -> 0.0
            else ->
                (lethalMaximumM - environment.elevationM) /
                        (lethalMaximumM - optimalMaximumM)
        }.coerceIn(0.0, 1.0)
    }

    fun vegetationStructure(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
    ): Double {
        if (habitat != Habitat.LAND_SURFACE) return 1.0
        return (
                1.0 -
                        environment.canopyCover *
                        species.denseCanopyForagingPenalty
                ).coerceIn(0.0, 1.0)
    }

    fun habitat(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
    ): Double {
        if (species.requiresAdjacentLand && !environment.adjacentToLand) return 0.0
        if (!habitat.aquatic || species.absoluteMaximumWaterDepthM.isInfinite()) return 1.0
        return waterDepth(
            environment.waterDepthM,
            species.optimalMaximumWaterDepthM,
            species.absoluteMaximumWaterDepthM,
        )
    }

    fun waterDepth(
        depthM: Double,
        optimalMaximumM: Double,
        absoluteMaximumM: Double,
    ): Double = when {
        depthM <= optimalMaximumM -> 1.0
        depthM >= absoluteMaximumM -> 0.0
        else ->
            (absoluteMaximumM - depthM) /
                    (absoluteMaximumM - optimalMaximumM)
    }.coerceIn(0.0, 1.0)

    fun thermal(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
    ): Double {
        if (environment.temperatureC < species.minimumActiveTemperatureC) {
            return 0.0
        }
        val passiveFit = seasonalTemperature(
            species,
            environment.temperatureC,
            environment.insolation,
        )
        return when (species.thermalStrategy) {
            ThermalStrategy.ECTOTHERMY -> passiveFit.pow(1.15)
            ThermalStrategy.ENDOTHERMY ->
                when {
                    passiveFit <= 0.0 -> 0.0
                    environment.temperatureC < species.temperatureOptimalLow ->
                        passiveFit.pow(0.55)

                    environment.temperatureC > species.temperatureOptimalHigh ->
                        passiveFit.pow(0.80)

                    else -> 1.0
                }

            ThermalStrategy.HETEROTHERMY ->
                when {
                    passiveFit <= 0.0 -> 0.0
                    environment.temperatureC < species.temperatureOptimalLow ->
                        0.35 + passiveFit * 0.65

                    environment.temperatureC > species.temperatureOptimalHigh ->
                        passiveFit.pow(0.95)

                    else -> 1.0
                }

            null -> passiveFit
        }
    }

    val aquaticHabitats: Set<Habitat> =
        Habitat.entries.filterTo(linkedSetOf()) { it.aquatic }
}

object LightColorModel {
    data class PhotosyntheticCompatibility(
        val byPigment: Map<BiologicalColor, Double>,
    ) {
        init {
            require(byPigment.values.all { it in 0.0..1.0 })
        }
    }

    val authoredCompatibility: Map<StarLight, PhotosyntheticCompatibility> = mapOf(
        StarLight.BLUE_WHITE to compatibility(0.84, 0.64, 0.76, 0.94, 0.58, 0.82, 0.38),
        StarLight.WHITE to compatibility(0.90, 0.70, 0.88, 0.90, 0.72, 0.86, 0.42),
        StarLight.YELLOW to compatibility(0.90, 0.72, 1.00, 0.86, 0.78, 0.84, 0.42),
        StarLight.ORANGE to compatibility(0.88, 0.76, 0.90, 0.74, 0.94, 0.82, 0.40),
        StarLight.RED to compatibility(0.82, 0.74, 0.66, 0.54, 1.00, 0.88, 0.36),
    )

    private val compiledCompatibility: Array<DoubleArray> by lazy {
        Array(StarLight.entries.size) { starIndex ->
            val starLight = StarLight.entries[starIndex]
            val compatibility = requireNotNull(authoredCompatibility[starLight]) {
                "Missing photosynthetic compatibility for $starLight"
            }
            DoubleArray(BiologicalColor.entries.size) { colorIndex ->
                val color = BiologicalColor.entries[colorIndex]
                requireNotNull(compatibility.byPigment[color]) {
                    "Missing $color photosynthetic compatibility for $starLight"
                }
            }
        }
    }

    fun photosyntheticMatch(starLight: StarLight, pigment: BiologicalColor): Double =
        compiledCompatibility[starLight.ordinal][pigment.ordinal]

    private fun compatibility(
        black: Double,
        brown: Double,
        green: Double,
        blueGreen: Double,
        red: Double,
        purple: Double,
        pale: Double,
        white: Double = pale,
        countershade: Double = pale,
    ) = PhotosyntheticCompatibility(
        mapOf(
            BiologicalColor.BLACK to black,
            BiologicalColor.BROWN to brown,
            BiologicalColor.GREEN to green,
            BiologicalColor.BLUE to blueGreen,
            BiologicalColor.RED to red,
            BiologicalColor.PURPLE to purple,
            BiologicalColor.PALE to pale,
            BiologicalColor.WHITE to white,
            BiologicalColor.COUNTERSHADE to countershade,
        ).let { it.plus(BiologicalColor.ADAPTIVE to it.values.max()) },
    )
}

object NicheSelection {
    fun choose(
        species: CompiledSpecies,
        ecology: CompiledEcology,
        environment: SeasonalCellEnvironment,
        competitionByNiche: DoubleArray = DoubleArray(ecology.niches.size),
        minimumRelativeIntrinsicFit: Double = 0.0,
        competitionAffectsSelection: Boolean = true,
    ): Int {
        require(competitionByNiche.size == ecology.niches.size)
        require(minimumRelativeIntrinsicFit in 0.0..1.0)
        val bestIntrinsicFit = ecology.niches.indices
            .asSequence()
            .filter { nicheIndex ->
                val habitat = ecology.niches[nicheIndex].habitat
                environment.habitatAvailability(habitat) > 0.0 &&
                        EcologyFitness.habitat(species, environment, habitat) > 0.0 &&
                        !(
                                !environment.isLand &&
                                        habitat == Habitat.AERIAL &&
                                        !species.pelagicAerialResident
                                ) &&
                        !(
                                !environment.isLand &&
                                        species.pelagicAerialResident &&
                                        habitat != Habitat.AERIAL
                                ) &&
                        !(habitat == Habitat.DARK_WATER && !species.darkWaterAdapted)
            }
            .maxOfOrNull { species.nicheFit[it] }
            ?: 0.0
        var bestIndex = -1
        var bestScore = 0.0
        ecology.niches.indices.forEach { nicheIndex ->
            val niche = ecology.niches[nicheIndex]
            if (
                species.nicheFit[nicheIndex] <
                bestIntrinsicFit * minimumRelativeIntrinsicFit
            ) {
                return@forEach
            }
            if (EcologyFitness.habitat(species, environment, niche.habitat) <= 0.0) {
                return@forEach
            }
            if (
                !environment.isLand &&
                niche.habitat == Habitat.AERIAL &&
                !species.pelagicAerialResident
            ) {
                return@forEach
            }
            if (
                !environment.isLand &&
                species.pelagicAerialResident &&
                niche.habitat != Habitat.AERIAL
            ) {
                return@forEach
            }
            if (niche.habitat == Habitat.DARK_WATER && !species.darkWaterAdapted) {
                return@forEach
            }
            // A temporarily empty carrion, marine-snow, or seasonal resource
            // pool should not make an otherwise valid niche impossible to
            // establish. Reserves and subsequent resource production decide
            // whether the population actually persists.
            val establishmentResource =
                max(0.01, environment.resourceSupport(niche, species.sizeClass))
            val score =
                species.nicheFit[nicheIndex] *
                        environment.habitatAvailability(niche.habitat) *
                        establishmentResource /
                        if (competitionAffectsSelection) {
                            1.0 + competitionByNiche[nicheIndex]
                        } else {
                            1.0
                        }
            if (score > bestScore) {
                bestScore = score
                bestIndex = nicheIndex
            }
        }
        return bestIndex
    }
}
