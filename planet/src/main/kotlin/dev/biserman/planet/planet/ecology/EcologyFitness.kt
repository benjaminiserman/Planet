package dev.biserman.planet.planet.ecology

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

object EcologyFitness {
    fun reefAssociationMultiplier(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
    ): Double = (1.0 - species.interactions.reefUse) + environment.reefCover * species.interactions.reefUse * 2.0

    fun temperature(species: CompiledSpecies, temperatureC: Double): Double = when {
        temperatureC <= species.physiology.thermal.outerLowC -> 0.0
        temperatureC < species.physiology.thermal.optimalLowC -> (temperatureC - species.physiology.thermal.outerLowC) / (species.physiology.thermal.optimalLowC - species.physiology.thermal.outerLowC)

        temperatureC <= species.physiology.thermal.optimalHighC -> 1.0
        temperatureC < species.physiology.thermal.outerHighC -> (species.physiology.thermal.outerHighC - temperatureC) / (species.physiology.thermal.outerHighC - species.physiology.thermal.optimalHighC)

        else -> 0.0
    }.coerceIn(0.0, 1.0)

    fun seasonalTemperature(
        species: CompiledSpecies,
        temperatureC: Double,
        insolation: Double,
    ): Double {
        val trigger = species.physiology.thermal.seasonalColdTriggerInsolation
        if (species.physiology.thermal.seasonalColdToleranceC <= 0.0 || trigger <= 0.0 || insolation >= trigger) {
            return temperature(species, temperatureC)
        }
        val coatFraction = ((trigger - insolation) / trigger).coerceIn(0.0, 1.0)
        val coldBonus = species.physiology.thermal.seasonalColdToleranceC * coatFraction
        val adjustedOuterLow = species.physiology.thermal.outerLowC - coldBonus
        val adjustedOptimalLow = species.physiology.thermal.optimalLowC - coldBonus * 0.45
        return when {
            temperatureC <= adjustedOuterLow -> 0.0
            temperatureC < adjustedOptimalLow -> (temperatureC - adjustedOuterLow) / (adjustedOptimalLow - adjustedOuterLow)

            temperatureC <= species.physiology.thermal.optimalHighC -> 1.0
            temperatureC < species.physiology.thermal.outerHighC -> (species.physiology.thermal.outerHighC - temperatureC) / (species.physiology.thermal.outerHighC - species.physiology.thermal.optimalHighC)

            else -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    fun water(species: CompiledSpecies, environment: SeasonalCellEnvironment, habitat: Habitat): Double {
        if (habitat.aquatic) return 1.0
        val frozenWaterAvailable =
            environment.snowOrIce || (environment.waterAvailability > 0.0 && (environment.temperatureC <= 0.0 || environment.annualAverageTemperatureC <= 0.0))
        val water = if (species.physiology.hydration.snowHydration && frozenWaterAvailable) {
            max(environment.waterAvailability, species.physiology.hydration.minimumWater)
        } else {
            environment.waterAvailability
        }
        if (water < species.physiology.hydration.minimumWater) {
            if (species.physiology.hydration.minimumWater <= 0.0) return 1.0
            return (water / species.physiology.hydration.minimumWater).coerceIn(0.0, 1.0)
        }
        if (water <= species.physiology.hydration.optimalMaximumWater) return 1.0
        if (species.physiology.hydration.maximumWater <= species.physiology.hydration.optimalMaximumWater) return 0.0
        return ((species.physiology.hydration.maximumWater - water) / (species.physiology.hydration.maximumWater - species.physiology.hydration.optimalMaximumWater)).coerceIn(
            0.0,
            1.0
        )
    }

    fun light(species: CompiledSpecies, environment: SeasonalCellEnvironment, habitat: Habitat): Double {
        if (species.niche.supportFor(EcoStrategy.PHOTOSYNTHESIS) <= 0.0) return 1.0
        var available = environment.lightAt(habitat)
        if (habitat == Habitat.LAND_SURFACE && environment.canopyCover > 0.0) {
            available += environment.canopyCover * species.environment.canopyLightEfficiency
        }
        val distance = abs(available - species.environment.insolationOptimum)
        val quantityFit = (1.0 - distance / 0.65).coerceIn(0.0, 1.0)
        val spectrumFit = LightColorModel.photosyntheticMatch(
            environment.starLight,
            species.niche.photosyntheticColor ?: BiologicalColor.GREEN,
        )
        return quantityFit * spectrumFit
    }

    fun combined(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
    ): Double = habitat(species, environment, habitat) * elevation(species, environment, habitat) * thermal(
        species,
        environment
    ) * water(species, environment, habitat) * light(species, environment, habitat) * vegetationStructure(
        species,
        environment,
        habitat
    )

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
        val optimalMinimumM = EcologyGlobals.normalMinimumElevationM + species.environment.elevationToleranceShiftM
        val lethalMinimumM = EcologyGlobals.lethalMinimumElevationM + species.environment.elevationToleranceShiftM
        val optimalMaximumM = EcologyGlobals.normalElevationLimitM + species.environment.elevationToleranceShiftM
        val lethalMaximumM = EcologyGlobals.lethalElevationLimitM + species.environment.elevationToleranceShiftM
        return when {
            environment.elevationM <= lethalMinimumM -> 0.0
            environment.elevationM < optimalMinimumM -> (environment.elevationM - lethalMinimumM) / (optimalMinimumM - lethalMinimumM)

            environment.elevationM <= optimalMaximumM -> 1.0
            environment.elevationM >= lethalMaximumM -> 0.0
            else -> (lethalMaximumM - environment.elevationM) / (lethalMaximumM - optimalMaximumM)
        }.coerceIn(0.0, 1.0)
    }

    fun vegetationStructure(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
    ): Double {
        if (habitat != Habitat.LAND_SURFACE) return 1.0
        return (1.0 - environment.canopyCover * species.environment.denseCanopyForagingPenalty).coerceIn(0.0, 1.0)
    }

    fun habitat(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
    ): Double {
        val landAccess =
            if (species.environment.requiresAdjacentLand) environment.adjacentToLand else 1.0
        val depthFitness =
            if (!habitat.aquatic || species.environment.absoluteMaximumWaterDepthM.isInfinite()) {
                1.0
            } else {
                waterDepth(
                    environment.waterDepthM,
                    species.environment.optimalMaximumWaterDepthM,
                    species.environment.absoluteMaximumWaterDepthM,
                )
            }
        return landAccess * depthFitness
    }

    fun waterDepth(
        depthM: Double,
        optimalMaximumM: Double,
        absoluteMaximumM: Double,
    ): Double = when {
        depthM <= optimalMaximumM -> 1.0
        depthM >= absoluteMaximumM -> 0.0
        else -> (absoluteMaximumM - depthM) / (absoluteMaximumM - optimalMaximumM)
    }.coerceIn(0.0, 1.0)

    fun thermal(
        species: CompiledSpecies,
        environment: SeasonalCellEnvironment,
    ): Double {
        if (environment.temperatureC < species.physiology.thermal.minimumActiveC) {
            return 0.0
        }
        val passiveFit = seasonalTemperature(
            species,
            environment.temperatureC,
            environment.insolation,
        )
        return when (species.physiology.thermal.regulation) {
            ThermalStrategy.ECTOTHERMY -> passiveFit.pow(1.15)
            ThermalStrategy.ENDOTHERMY -> when {
                passiveFit <= 0.0 -> 0.0
                environment.temperatureC < species.physiology.thermal.optimalLowC -> passiveFit.pow(0.55)

                environment.temperatureC > species.physiology.thermal.optimalHighC -> passiveFit.pow(0.80)

                else -> 1.0
            }

            ThermalStrategy.HETEROTHERMY -> when {
                passiveFit <= 0.0 -> 0.0
                environment.temperatureC < species.physiology.thermal.optimalLowC -> 0.35 + passiveFit * 0.65

                environment.temperatureC > species.physiology.thermal.optimalHighC -> passiveFit.pow(0.95)

                else -> 1.0
            }

            null -> passiveFit
        }
    }

    val aquaticHabitats: Set<Habitat> = Habitat.entries.filterTo(linkedSetOf()) { it.aquatic }
}