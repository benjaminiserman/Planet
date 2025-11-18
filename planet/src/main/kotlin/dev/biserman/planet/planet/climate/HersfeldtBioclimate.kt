package dev.biserman.planet.planet.climate

import kotlin.math.max

// a climate classification system, Nikolai Hersfeldt 2025
// see: https://github.com/hersfeldtn/koppenpasta/blob/main/koppenpasta.py
object HersfeldtBioclimate {
    // temperatures in °C
    // insolation in W/m²

    fun growingDegreeDayCurve(averageTemperature: Double) = when { // GDD
        averageTemperature <= 5.0 -> 0
        averageTemperature in 5.0..25.0 -> averageTemperature - 5.0
        averageTemperature in 25.0..40.0 -> 20
        averageTemperature in 40.0..50.0 -> 20 - 2 * (averageTemperature - 40)
        averageTemperature >= 50.0 -> 0
        else -> throw IllegalStateException("Unreachable")
    }

    fun growingDegreeDayZeroCurve(averageTemperature: Double) = when {
        averageTemperature <= 0.0 -> 0
        averageTemperature in 0.0..20.0 -> averageTemperature
        averageTemperature in 20.0..40.0 -> 20
        averageTemperature in 40.0..60.0 -> 20 - (averageTemperature - 40)
        averageTemperature >= 60.0 -> 0
        else -> throw IllegalStateException("Unreachable")
    }

    fun lightLimitedGrowingDegreeDayCurve(insolation: Double) = when {
        insolation <= 20.0 -> 0
        insolation in 20.0..220.0 -> (insolation - 20.0) / 10.0
        insolation >= 220.0 -> 20
        else -> throw IllegalStateException("Unreachable")
    }

    fun lightLimitedGrowingDegreeDayZeroCurve(insolation: Double) = lightLimitedGrowingDegreeDayCurve(insolation + 20)

    fun calculateGdd(
        temperature: Double,
        baseline: Double,
        plateauStart: Double,
        plateauEnd: Double,
        compensation: Double
    ): Double {
        val gddMax = plateauStart - baseline
        val backSlope = gddMax / (compensation - plateauEnd)

        val gdd = max(0.0, when {
            temperature >= plateauEnd -> gddMax - backSlope * (temperature - plateauEnd)
            temperature >= plateauStart -> gddMax
            else -> temperature - baseline
        })

        return gdd * 30
    }



    fun calculateAnnualGdd(gdd: List<Double>, gInt: List<Double>) {

    }
}