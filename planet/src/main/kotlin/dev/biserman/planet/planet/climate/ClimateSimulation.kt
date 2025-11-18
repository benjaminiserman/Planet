package dev.biserman.planet.planet.climate

import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.backwardsWind
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxMoistureSteps
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxPrecipitationSlope
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxWindBlocking
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.minPrecipitation
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.minStartingMoisture
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.saturationThreshold
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.startingMoistureMultiplier
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.windBlockingSlope
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.utils.UtilityExtensions.signPow
import dev.biserman.planet.utils.sum
import godot.common.util.lerp
import godot.core.Vector3
import kotlin.math.absoluteValue
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

data class ClimateDatumMonth(
    val minTemperature: Double, // minT, °C
    val maxTemperature: Double, // maxT, °C
    val insolation: Double, // W/m²
    val precipitation: Double // mm
) {
    val averageTemperature = (minTemperature + maxTemperature) * 0.5 // °C
}

class ClimateDatum(val months: List<ClimateDatumMonth>)

object ClimateSimulation {
    data class Band(val latitude: Double, val pressureDelta: Double)

    @Suppress("UnusedUnaryOperator")
    val bands = listOf(
        +90 to +5,
        +60 to -10,
        +30 to +10,
        0 to +5,
        -30 to +10,
        -60 to -10,
        -90 to +5,
    ).map { Band(it.first.toDouble(), it.second.toDouble()) }
    val basePressure = 1010.0

    fun estimateMonth(planet: Planet, daysPassed: Number): String =
        when (((daysPassed.toDouble() % Insolation.yearLength) / (365.242 / 12.0)).toInt() + 1) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Unknown"
        }

    fun stepClimateSimulation(planet: Planet) {
        planet.daysPassed += 45
        Gui.instance.daysPassedLabel.setText("${planet.daysPassed} — ${estimateMonth(planet, planet.daysPassed)}")
        Gui.instance.updateInfobox()

        planet.oceanCurrents = OceanCurrents.viaEarthlikeHeuristic(planet, 7)
            .distinctBy { it.planetTile }
            .associate { it.planetTile.tileId to it }
            .toMutableMap()

        simulateMoisture(planet)

//        Main.instance.timerActive = "none"
    }

    fun (PlanetTile).calculatePrevailingWind(): Vector3 {
        val pressureGradientForce = neighbors
            .filter { it.airPressure < airPressure }
            .map { (it.tile.position - tile.position).tangent(tile.position) * (airPressure - it.airPressure) * 0.1 }
            .sum()

        val latitudeRadians = asin(tile.position.y)
        val coriolisParameter = 2.0 * planet.rotationRate * -sin(latitudeRadians)

        // Deflection is perpendicular to both motion and local vertical
        val coriolisDeflection = tile.position.cross(pressureGradientForce).normalized() *
                coriolisParameter * pressureGradientForce.length()

        val direction = pressureGradientForce + coriolisDeflection
        return direction.normalized() * direction.length()
            .coerceIn(planet.topology.averageRadius * 0.25, planet.topology.averageRadius * 0.66)
    }

    fun (PlanetTile).calculateAirPressure(): Double {
        val latitude = tile.position.toGeoPoint().latitudeDegrees
        val nearestBandAbove = bands.last { it.latitude >= latitude }
        val nearestBandBelow = bands.first { it.latitude <= latitude }

        val adjustedContinentiality = continentiality.toDouble() + 2.0

        val seasonalAdjustment = -(2 / (1 + exp(
            -(insolation - 0.6).signPow(1.01) * (adjustedContinentiality + 1).signPow(3.0) * 0.01
        )) - 1) * adjustedContinentiality.scaleAndCoerceIn(-2.0..2.0, 5.0..15.0)

        val elevationAdjustment = if (elevation < 2000) 0.0 else (elevation - 2000) * 0.002

        val latitudeAdjustment =
            tile.position.y.absoluteValue.pow(2).scaleAndCoerceIn(
                0.0..1.0,
                -5.0..2.0
            ) * (1 / (1 + exp(-adjustedContinentiality.signPow(0.5) * 0.3)))

        return basePressure + lerp(
            nearestBandBelow.pressureDelta,
            nearestBandAbove.pressureDelta,
            (latitude - nearestBandBelow.latitude) / (nearestBandAbove.latitude - nearestBandBelow.latitude)
        ) + seasonalAdjustment + latitudeAdjustment + elevationAdjustment
    }

    fun simulateMoisture(planet: Planet) {
        var currentMoisture = planet.planetTiles.values.associateWith { tile ->
            if (tile.isAboveWater) 0.0
            else max(
                (tile.insolation.pow(2) + (planet.oceanCurrents[tile.tileId]?.temperature
                    ?: 0.0)) * startingMoistureMultiplier, minStartingMoisture
            )
        }
        val finalMoisture = planet.planetTiles.values.associateWith { 0.0 }.toMutableMap()

        var steps = 0
        while (currentMoisture.values.any { it > 0.01 } && steps < maxMoistureSteps) {
            steps += 1
            val nextStep = planet.planetTiles.values.associateWith { 0.0 }.toMutableMap()

            currentMoisture.forEach { (tile, moisture) ->
                val neighborWeights = if (tile.prevailingWind == Vector3.ZERO) {
                    tile.neighbors.associateWith { 1.0 }
                } else {
                    tile.neighbors.associateWith { neighbor ->
                        val delta = (neighbor.tile.position - tile.tile.position).normalized()
                        (tile.prevailingWind.dot(delta) + backwardsWind)
                    }
                }.filter { (_, weight) -> weight > 0.0 }
                    .mapValues { (neighbor, weight) ->
                        weight *
                                (1 - (tile.slopeAboveWaterTo(neighbor) / windBlockingSlope))
                                    .coerceIn(1 - maxWindBlocking..1.0) *
                                (1 - ((finalMoisture[neighbor] ?: 0.0) / saturationThreshold).pow(2))
                                    .coerceIn(0.01..1.0) *
                                tile.tile.borderFor(neighbor.tile).length.pow(0.1)
                    }

                val totalWeight = neighborWeights.values.sum()
                neighborWeights.forEach { (neighbor, weight) ->
                    val moistureProvided = if (totalWeight == 0.0) 0.0 else moisture * weight / totalWeight
                    val precipitation =
                        moistureProvided *
                                (tile.slopeAboveWaterTo(neighbor) / maxPrecipitationSlope)
                                    .pow(2.0)
                                    .coerceIn(minPrecipitation..1.0) *
                                (1 - ((finalMoisture[tile] ?: 0.0) / saturationThreshold).pow(2))
                                    .coerceIn(0.01..1.0) *
                                (1 - tile.prevailingWind.length().pow(0.1))
                    finalMoisture[tile] = (finalMoisture[tile] ?: 0.0) + precipitation
                    nextStep[neighbor] = (nextStep[neighbor] ?: 0.0) + moistureProvided - precipitation
                }
            }

            currentMoisture = nextStep
        }

        planet.planetTiles.values.forEach { tile ->
            tile.moisture = tile.neighbors
                .plus(tile)
                .filter { it.isAboveWater == tile.isAboveWater }
                .map { neighbor -> finalMoisture[neighbor] ?: 0.0 }
                .average()
        }
    }
}