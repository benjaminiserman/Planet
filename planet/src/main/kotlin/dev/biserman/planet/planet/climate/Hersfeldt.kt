package dev.biserman.planet.planet.climate

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.utils.UtilityExtensions.weightedAverage
import godot.common.util.lerp
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

// a climate classification scheme developed by Nikolai Hersfeldt
// see: https://worldbuildingpasta.blogspot.com/2025/03/beyond-koppen-geiger-climate.html#climateparameters
// included with permission
class Hersfeldt : ClimateClassifier {
    // I've provided simplified alternatives to some of Hersfeldt's climate names for accessibility
    var alternateNames = false

    fun winterType(averageTemperature: Double) = when {
        averageTemperature < -30 -> "frigid"
        averageTemperature < 0 -> "cold"
        averageTemperature < 17 -> "cool"
        else -> "mild"
    }

    fun summerType(averageTemperature: Double) = when {
        averageTemperature < 40 -> "warm"
        averageTemperature < 60 -> "hot"
        averageTemperature < 90 -> "torrid"
        else -> "boiling"
    }

    fun linearGraph(vararg points: Pair<Double, Double>): (Double) -> Double {
        val sorted = points.sortedBy { it.first }

        return ({ value ->
            when {
                value <= sorted.first().first -> sorted.first().second
                value >= sorted.last().first -> sorted.last().second
                else -> {
                    val index = sorted.indexOfFirst { it.first > value }
                    val (x1, y1) = sorted[index - 1]
                    val (x2, y2) = sorted[index]
                    lerp(y1, y2, (value - x1) / (x2 - x1))
                }
            }
        })
    }

    val gddGraph = linearGraph(5.0 to 0.0, 25.0 to 20.0, 40.0 to 20.0, 50.0 to 0.0)
    val gddzGraph = linearGraph(0.0 to 0.0, 20.0 to 20.0, 40.0 to 40.0, 60.0 to 0.0)
    val gddiGraph = linearGraph(20.0 to 0.0, 220.0 to 20.0)
    val gddizGraph = linearGraph(0.0 to 0.0, 200.0 to 20.0)
    val monthLength = 30
    val gintThreshold = 1250

    data class GddResults(
        val monthlyGdd: List<Double>,
        val monthlyGddz: List<Double>,
        val monthlyGint: List<Double>,
        val totalGdd: Double,
        val totalGddz: Double,
        val totalGint: Double,
    )
    fun gdd(datum: ClimateDatum): GddResults {
        val monthlyGdd = datum.months.map { minOf(gddGraph(it.averageTemperature), gddiGraph(it.insolation)) * monthLength }
        val monthlyGddz = datum.months.map { minOf(gddzGraph(it.averageTemperature), gddizGraph(it.insolation)) * monthLength }
        val monthlyGint = monthlyGddz.map { max(0.0, 15 * monthLength - it) }

        var totalGdd = 0.0
        var totalGddz = 0.0
        var totalGint = 0.0

        var accumulatedGdd = 0.0
        var accumulatedGddz = 0.0
        var accumulatedGint = 0.0
        for (i in 0..<(datum.months.size * 3)) {
            accumulatedGdd += monthlyGdd[i % datum.months.size]
            accumulatedGddz += monthlyGddz[i % datum.months.size]
            accumulatedGint += monthlyGint[i % datum.months.size]

            if (monthlyGdd[i % datum.months.size] <= 0.0 && accumulatedGint >= gintThreshold) {
                accumulatedGdd = 0.0
            }

            if (monthlyGddz[i % datum.months.size] <= 0.0) {
                accumulatedGddz = 0.0
            }

            if (monthlyGint[i % datum.months.size] <= 0.0) {
                accumulatedGint = 0.0
            }

            if (i >= datum.months.size * 2) {
                totalGdd = max(totalGdd, accumulatedGdd)
                totalGddz = max(totalGddz, accumulatedGddz)
                totalGint = max(totalGint, accumulatedGint)
            }
        }

        return GddResults(monthlyGdd, monthlyGddz, monthlyGint, totalGdd, totalGddz, totalGint)
    }

    // Hargreaves method for calculating potential evapotranspiration
    fun hargreavesPet(
        averageTemperature: Double, // °C
        insolation: Double // W/m²
    ): Double {
        val insolationMegajoules = insolation * 0.0864
        return 0.0135 *
                (averageTemperature + 17.8) *
                insolationMegajoules *
                (238.8 / (595.5 - 0.55 * averageTemperature))
    }

    // heuristic for estimating actual evapotranspiration
    fun estimateAet(
        datum: ClimateDatum,
        pet: List<Double>
    ): List<Double> {
        var soilMoisture = 0.0
        val aet = datum.months.map { 0.0 }.toMutableList()

        while (true) {
            val startSoilMoisture = soilMoisture
            for (i in 0..<datum.months.size) {
                val precipitation = datum.months[i].precipitation
                if (precipitation > pet[i]) {
                    soilMoisture += precipitation - pet[i]
                    soilMoisture = min(soilMoisture, 500.0)
                    aet[i] = pet[i]
                } else {
                    soilMoisture -= pet[i] - precipitation
                    soilMoisture = max(soilMoisture, 0.0)
                    val soilEvaporation = min(soilMoisture, (pet[i] - precipitation) * soilMoisture / 25.0)
                    aet[i] = precipitation + soilEvaporation
                }
            }

            if ((soilMoisture - startSoilMoisture).absoluteValue <= 1.0) {
                break
            }
        }

        return aet
    }

    fun aridityFactor(
        pet: List<Double>,
        aet: List<Double>
    ) = aet.sum() / pet.sum()

    fun growthAridityFactor(
        pet: List<Double>,
        gdd: List<Double>,
        aet: List<Double>,
    ) = aet.zip(gdd).weightedAverage() / pet.zip(gdd).weightedAverage()

    fun growthSupply(
        datum: ClimateDatum,
        gdd: List<Double>,
        aet: List<Double>,
    ) = datum.months.map { it.precipitation }.zip(gdd).weightedAverage() / aet.average()

    fun evaporationRatio(
        datum: ClimateDatum,
        aet: List<Double>
    ) = aet.sum() / datum.annualPrecipitation

    fun iceCover(planet: Planet, datum: ClimateDatum) =
        if (planet.planetTiles[datum.tileId]!!.isAboveWater) datum.months.maxOf { it.averageTemperature } <= 0.0
        else datum.months.maxOf { it.averageTemperature } <= -2.0

    override fun classify(
        planet: Planet,
        datum: ClimateDatum
    ): ClimateClassification {
        throw NotImplementedError("not yet implemented")
    }
}