package dev.biserman.planet.planet.climate

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.adjustRange
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
import dev.biserman.planet.planet.PlanetRegion
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.climate.OceanCurrents.updateCurrentDistanceMap
import dev.biserman.planet.utils.AStar
import dev.biserman.planet.utils.AStar.path
import dev.biserman.planet.utils.Path
import dev.biserman.planet.utils.UtilityExtensions.signPow
import dev.biserman.planet.utils.sum
import godot.common.util.lerp
import godot.core.Vector3
import godot.global.GD
import kotlin.math.absoluteValue
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin


object ClimateSimulation {
    data class Band(val latitude: Double, val pressureDelta: Double)

    @Suppress("UnusedUnaryOperator")
    val bands = listOf(
        +90 to +2.5,
        +60 to -7.5,
        +30 to +5.0,
        0 to +0.0,
        -30 to +5.0,
        -60 to -7.5,
        -90 to +2.5,
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

        updatePlanetClimate(planet)
        Main.instance.timerActive = "none"
    }

    fun updatePlanetClimate(planet: Planet) {
        planet.oceanCurrents = OceanCurrents.viaEarthlikeHeuristic(planet, 7)
            .distinctBy { it.planetTile }
            .associate { it.planetTile.tileId to it }
            .toMutableMap()
        planet.updateCurrentDistanceMap()

        val itcz = planet.calculateItcz().nodes.toSet()
        planet.itczDistanceMap = PlanetRegion(planet, planet.planetTiles.values.toMutableSet())
            .calculateEdgeDepthMap { it in itcz }
            .mapValues { it.value + if (it.key in itcz) -1 else 0 }
            .mapKeys { it.key.tileId }

        simulateMoisture(planet)
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
        val adjustedLatitude = latitude + Insolation.solarDeclination(planet.daysPassed % Insolation.yearLength) * 2.0
        val nearestBandAbove = bands.last { it.latitude >= adjustedLatitude }
        val nearestBandBelow = bands.first { it.latitude <= adjustedLatitude }

        val adjustedContinentiality = continentiality.toDouble()
        val seasonalAdjustment = -(2 / (1 + exp(
            -(insolation - 0.6).signPow(1.01) * (adjustedContinentiality + 1).signPow(3.0) * 0.01
        )) - 1) * adjustedContinentiality.scaleAndCoerceIn(-2.0..2.0, 5.0..15.0)

        val elevationAdjustment = if (elevation < 2000) 0.0 else (elevation - 2000) * 0.001

        val ictzAdjustment = -20.0 * ((10 - planet.itczDistanceMap[tileId]!!) / 10.0).coerceIn(0.0..1.0)

        return basePressure + lerp(
            nearestBandBelow.pressureDelta,
            nearestBandAbove.pressureDelta,
            (adjustedLatitude - nearestBandBelow.latitude) / (nearestBandAbove.latitude - nearestBandBelow.latitude)
        ) + seasonalAdjustment + elevationAdjustment + ictzAdjustment
    }

    fun simulateMoisture(planet: Planet) {
        var currentMoisture = planet.planetTiles.values.associateWith { tile ->
            val geoPoint = tile.tile.position.toGeoPoint()
            val equatorEffect =
                2.8 * tile.insolation.pow(5) * max(0.0, 1 - geoPoint.latitudeDegrees.absoluteValue / 5.0)
            val ferrelEffect = 0.8 * tile.insolation.pow(0.5) * max(
                0.0,
                1 - ((geoPoint.latitudeDegrees.absoluteValue - 60).absoluteValue) / 15.0
            )
            val oceanEffect = if (tile.isAboveWater) 0.0
            else {
                val coolCurrentEffect =
                    -0.3 * tile.insolation * max(5 - (planet.coolCurrentDistanceMap[tile.tileId] ?: 10), 0)
                val warmCurrentEffect =
                    2.0 * tile.insolation * max(2 - (planet.warmCurrentDistanceMap[tile.tileId] ?: 10), 0)
                max(
                    min(
                        (tile.insolation.pow(1.5) + warmCurrentEffect + coolCurrentEffect) * startingMoistureMultiplier,
                        3.0
                    ),
                    minStartingMoisture
                )
            }
            equatorEffect + ferrelEffect + oceanEffect
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
                                tile.tile.borderFor(neighbor.tile).length.pow(0.1)
                    }

                val totalWeight = neighborWeights.values.sum()
                neighborWeights.forEach { (neighbor, weight) ->
                    val moistureProvided = if (totalWeight == 0.0) 0.0 else moisture * weight / totalWeight
                    val precipitation =
                        moistureProvided *
                                max(
                                    minPrecipitation,
                                    (tile.slopeAboveWaterTo(neighbor) / maxPrecipitationSlope)
                                        .pow(2.0)
                                        .coerceIn(0.0..1.0) *
                                            (1 - ((finalMoisture[tile] ?: 0.0) / saturationThreshold).pow(2))
                                                .coerceIn(0.0..1.0)
//                                            (1 - tile.prevailingWind.length().pow(0.1))
                                )
                    finalMoisture[tile] = (finalMoisture[tile] ?: 0.0) + precipitation
                    nextStep[neighbor] = (nextStep[neighbor] ?: 0.0) + moistureProvided * 1.03 - precipitation
                }
            }

            currentMoisture = nextStep
        }

        finalMoisture.mapValuesTo(finalMoisture) { (tile, moisture) ->
            val itczEffect = max(0.0, 1 - planet.itczDistanceMap[tile.tileId]!! / 5.0)
                .adjustRange(0.0..1.0, 1.0..2.0)
            moisture * itczEffect
        }
        planet.planetTiles.values.forEach { tile ->
            tile.moisture = tile.neighbors
                .plus(tile)
                .filter { it.isAboveWater == tile.isAboveWater }
                .map { neighbor -> finalMoisture[neighbor] ?: 0.0 }
                .average()
        }
    }

    val (PlanetTile).averageTemperature: Double
        get() {
            val geoPoint = tile.position.toGeoPoint()
            val baseTemperature =
                240.15 + insolation * 83.0
            val moistureAdjustedTemperature =
                lerp(
                    273.15,
                    baseTemperature,
                    max(0.0, 1 - moisture)
                        .pow(1.5)
                        .scaleAndCoerceIn(0.0..1.0, 0.75..1.0)
                )

            val currentContinentialityFactor = if (continentiality >= 0) 1.0 else {
                max(0.0, 1.2 + continentiality * 0.2)
            }
            val warmCurrentAdjustment =
                5.5 * max(
                    3 - (planet.warmCurrentDistanceMap[tileId] ?: return 0.0),
                    0
                ) * insolation * (1.0 - averageInsolation) * currentContinentialityFactor
            val coolCurrentAdjustment =
                -1.5 * max(
                    3 - (planet.coolCurrentDistanceMap[tileId] ?: return 0.0),
                    0
                ) * insolation * averageInsolation * currentContinentialityFactor

            val elevationAdjustment = -0.0098 * 0.66 * max(0.0, elevation)

            val oceanTemperature = max(
                271.1,
                249.55 + lerp(insolation, annualInsolation.average(), 0.66).pow(0.75) * 57.5
            ) + warmCurrentAdjustment + coolCurrentAdjustment + elevationAdjustment

            val adjustedTemperature =
                moistureAdjustedTemperature + warmCurrentAdjustment + coolCurrentAdjustment + elevationAdjustment

            val averageTemperature = if (continentiality < 0) {
                oceanTemperature
            } else if (continentiality == 0) {
                lerp(
                    oceanTemperature,
                    adjustedTemperature,
                    (neighbors.filter { it.isAboveWater }.size / neighbors.size.toDouble())
                        .adjustRange(0.0..1.0, 0.0..0.25)
                )
            } else {
                lerp(oceanTemperature, adjustedTemperature, min(continentiality * 0.4, 1.0))
            }

            val celsius = averageTemperature - 273.15
            return celsius
        }

    fun (PlanetTile).calculateClimateDatumMonth(): ClimateDatumMonth {
        return ClimateDatumMonth(
            averageTemperature,
            insolation * 500.0,
            moisture * 120.0
        )
    }

    fun calculateClimate(planet: Planet): Map<PlanetTile, ClimateDatum> {
        val startDate = planet.daysPassed

        val monthToTileClimate = (0..<12).map { i ->
            GD.print("${MonthIndex.values()[i].name}...")
            planet.daysPassed = i * 30
            updatePlanetClimate(planet)
            planet.planetTiles.values.associateWith { it.calculateClimateDatumMonth() }
        }

        return planet.planetTiles.values.associateWith { tile ->
            ClimateDatum(tile.tileId, monthToTileClimate.map { it[tile]!! })
        }

        planet.daysPassed = startDate
        updatePlanetClimate(planet)
    }

    // inter-tropical convergence zone
    fun (Planet).calculateItcz(): Path<PlanetTile> {
        fun costFn(_1: PlanetTile, tile: PlanetTile): Double =
            1 - lerp(tile.insolation, tile.averageInsolation, 0.4) - max(0.0, tile.continentiality * 0.015)

        fun neighborFn(tile: PlanetTile): List<PlanetTile> = tile.neighbors.filter {
            val tileToNeighbor = (it.tile.position - tile.tile.position).normalized()
            val crossProduct = tile.tile.position.cross(tileToNeighbor)
            val dotProduct = crossProduct.dot(Vector3.UP)
            dotProduct > 0
        }

        val startTile = planetTiles.values.minBy { costFn(it, it) }
        val validStartNeighbors = neighborFn(startTile)
        val goal = startTile.neighbors.filter { it !in validStartNeighbors }.maxBy { costFn(it, it) }

        fun goalFn(tile: PlanetTile): Boolean = tile == goal
        fun heuristic(tile: PlanetTile) = costFn(tile, tile)

        val path = AStar.path(startTile, ::goalFn, ::heuristic, ::costFn, ::neighborFn)
        if (path.nodes.size == 0) {
            throw Exception("No path found!")
        }
        return path
    }
}