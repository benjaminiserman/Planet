package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.ClimateSimulationGlobals.backwardsWind
import dev.biserman.planet.planet.ClimateSimulationGlobals.maxMoistureSteps
import dev.biserman.planet.planet.ClimateSimulationGlobals.maxPrecipitationSlope
import dev.biserman.planet.planet.ClimateSimulationGlobals.maxWindBlocking
import dev.biserman.planet.planet.ClimateSimulationGlobals.minPrecipitation
import dev.biserman.planet.planet.ClimateSimulationGlobals.saturationThreshold
import dev.biserman.planet.planet.ClimateSimulationGlobals.startingMoistureMultiplier
import dev.biserman.planet.planet.ClimateSimulationGlobals.windBlockingSlope
import godot.core.Vector3
import godot.global.GD
import kotlin.math.pow

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
        planet.daysPassed += 10
        Gui.instance.daysPassedLabel.setText("${planet.daysPassed} — ${estimateMonth(planet, planet.daysPassed)}")
        Gui.instance.updateInfobox()

        planet.oceanCurrents = OceanCurrents.viaEarthlikeHeuristic(planet, 7)
            .distinctBy { it.planetTile }
            .associate { it.planetTile.tileId to it }
            .toMutableMap()

        simulateMoisture(planet)
//        Gui.instance.statsGraph.update(planet)

        Main.instance.timerActive = "none"
    }

    fun simulateMoisture(planet: Planet) {
        var currentMoisture = planet.planetTiles.values.associateWith { tile ->
            if (tile.isAboveWater) 0.0
            else (tile.insolation + (planet.oceanCurrents[tile.tileId]?.temperature
                ?: 0.0)) * startingMoistureMultiplier
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
                                    .coerceIn(0.01..1.0)
                    finalMoisture[tile] = (finalMoisture[tile] ?: 0.0) + precipitation
                    nextStep[neighbor] = (nextStep[neighbor] ?: 0.0) + moistureProvided - precipitation
                }
            }

            currentMoisture = nextStep
        }

        GD.print(steps)
        planet.planetTiles.values.forEach { tile ->
            tile.moisture =
                (tile.neighbors.sumOf { neighbor -> finalMoisture[neighbor] ?: 0.0 } + (finalMoisture[tile] ?: 0.0)) /
                        (tile.neighbors.size + 1)
        }
    }
}