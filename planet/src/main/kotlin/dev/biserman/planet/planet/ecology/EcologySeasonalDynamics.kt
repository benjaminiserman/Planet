package dev.biserman.planet.planet.ecology

import kotlin.math.floor
import kotlin.math.min

data class ClimateAnomaly(
    val temperatureC: Double,
    val precipitationMultiplier: Double,
)

/**
 * Deterministic smooth noise keeps saved simulations reproducible. The faster
 * octave changes over seasons; the slower one produces multi-decade runs of
 * relatively warm/cool and wet/dry conditions.
 */
object EcologyClimateVariability {
    private const val SEASONAL_SCALE_YEARS = 0.75
    private const val DECADAL_SCALE_YEARS = 18.0

    fun anomaly(tileId: Int, year: Double): ClimateAnomaly {
        val seasonalTemperature = valueNoise(tileId xor 0x2A61_9D31, year, SEASONAL_SCALE_YEARS)
        val decadalTemperature = valueNoise(tileId xor 0x6D2B_79F5, year, DECADAL_SCALE_YEARS)
        val seasonalPrecipitation = valueNoise(tileId xor 0x1B56_C4E9, year, SEASONAL_SCALE_YEARS)
        val decadalPrecipitation = valueNoise(tileId xor 0x4C95_7F2D, year, DECADAL_SCALE_YEARS)
        return ClimateAnomaly(
            temperatureC = seasonalTemperature * 1.25 + decadalTemperature * 0.75,
            precipitationMultiplier =
            1.0 + seasonalPrecipitation * 0.15 + decadalPrecipitation * 0.10,
        )
    }

    private fun valueNoise(seed: Int, year: Double, scaleYears: Double): Double {
        val position = year / scaleYears
        val first = floor(position).toInt()
        val fraction = position - floor(position)
        val smoothFraction = fraction * fraction * (3.0 - 2.0 * fraction)
        val firstValue = hashToSignedUnit(seed, first)
        val secondValue = hashToSignedUnit(seed, first + 1)
        return firstValue + (secondValue - firstValue) * smoothFraction
    }

    private fun hashToSignedUnit(seed: Int, position: Int): Double {
        var value = position * 0x45D9F3B + seed
        value = (value xor (value ushr 16)) * 0x45D9F3B
        value = (value xor (value ushr 16)) * 0x45D9F3B
        value = value xor (value ushr 16)
        return value.toUInt().toDouble() / UInt.MAX_VALUE.toDouble() * 2.0 - 1.0
    }
}

/**
 * Carries an organic resource pool between seasons without allowing consumers
 * to eat biomass produced only at the end of the same season.
 */
object OrganicPoolDynamics {
    fun update(
        previousLevel: Double,
        producedBiomassKg: Double,
        consumedBiomassKg: Double,
        biomassKgPerLevel: Double,
        seasonalRetention: Double,
        maximumAccessibleFraction: Double = 1.0,
    ): Double {
        require(previousLevel in 0.0..1.0)
        require(producedBiomassKg >= 0.0)
        require(consumedBiomassKg >= 0.0)
        require(biomassKgPerLevel > 0.0)
        require(seasonalRetention in 0.0..1.0)
        require(maximumAccessibleFraction in 0.0..1.0)

        val retainedOldPool = previousLevel * seasonalRetention
        val requestedConsumption = consumedBiomassKg / biomassKgPerLevel
        val consumedOldPool =
            min(requestedConsumption, retainedOldPool * maximumAccessibleFraction)
        val newProduction = producedBiomassKg / biomassKgPerLevel
        return (retainedOldPool - consumedOldPool + newProduction).coerceIn(0.0, 1.0)
    }
}

/** Advances all climate-independent resource pools with production runtime semantics. */
object FunctionalResourceDynamics {
    fun update(
        previous: FunctionalResources,
        fluxes: CellTurnFluxes,
        areaKm2: Double,
        hasMarineCompartment: Boolean,
    ): FunctionalResources = FunctionalResources(
        carrion = OrganicPoolDynamics.update(
            previous.carrion,
            fluxes.carrionBiomass,
            fluxes.carrionConsumedBiomass,
            areaKm2 * EcologyGlobals.carrionFullLevelBiomassKgKm2,
            0.55,
            maximumAccessibleFraction = 0.90,
        ),
        detritus = OrganicPoolDynamics.update(
            previous.detritus,
            fluxes.detritusBiomass,
            fluxes.detritusConsumedBiomass,
            areaKm2 * EcologyGlobals.detritusFullLevelBiomassKgKm2,
            0.68,
            maximumAccessibleFraction = 0.75,
        ),
        waste = OrganicPoolDynamics.update(
            previous.waste,
            fluxes.wasteBiomass,
            fluxes.wasteConsumedBiomass,
            areaKm2 * EcologyGlobals.wasteFullLevelBiomassKgKm2,
            0.60,
            maximumAccessibleFraction = 0.80,
        ),
        marineSnow = if (hasMarineCompartment) {
            OrganicPoolDynamics.update(
                previous.marineSnow,
                fluxes.marineSnowBiomass,
                fluxes.marineSnowConsumedBiomass,
                areaKm2 * EcologyGlobals.marineSnowFullLevelBiomassKgKm2,
                0.72,
                maximumAccessibleFraction = 0.90,
            )
        } else {
            0.0
        },
        fruit = OrganicPoolDynamics.update(
            previous.fruit,
            fluxes.fruitBiomass,
            fluxes.fruitConsumedBiomass,
            areaKm2 * 2_500.0,
            0.20,
            maximumAccessibleFraction = 0.85,
        ),
    )
}