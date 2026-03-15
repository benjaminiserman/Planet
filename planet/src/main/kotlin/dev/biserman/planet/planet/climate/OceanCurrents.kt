package dev.biserman.planet.planet.climate

import dev.biserman.planet.geometry.GeoPoint
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetRegion
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.minCurrentCirculationRadius
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.minCurrentScanlineProportion
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.minOceanTilesForCurrent
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanCurrentMinStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanCurrentStrengthDiagonalization
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanCurrentStrengthPow
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.targetCurrentRadiusProportion
import dev.biserman.planet.utils.UtilityExtensions.formatGeo
import dev.biserman.planet.utils.toCardinal
import godot.core.Color
import godot.core.Vector2
import godot.core.Vector3
import godot.global.GD
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class OceanCurrent(val planetTile: PlanetTile, val direction: Vector3, val temperature: Double)
class OceanBand(
    val bottomLatitudeDegrees: Double,
    val topLatitudeDegrees: Double,
    val polarity: Double,
    planet: Planet
) {
    val region = PlanetRegion(planet, planet.planetTiles.values.filter { tile ->
        val geoPoint = tile.tile.position.toGeoPoint()
        geoPoint.latitudeDegrees in bottomLatitudeDegrees..topLatitudeDegrees
    })

    val centerLatitudeDegrees get() = (bottomLatitudeDegrees + topLatitudeDegrees) * 0.5

    val centerParallel = let {
        val center =
            region.tiles.minBy { (it.tile.position.toGeoPoint().latitudeDegrees - centerLatitudeDegrees).absoluteValue }
        PlanetRegion(planet, region.parallelCross(center, Vector3.UP))
    }
}

object OceanCurrents {
    fun viaEarthlikeHeuristic(planet: Planet, numBands: Int): List<OceanCurrent> {
        val cells = numBands - 2
        val degreesPerCell = 150.0 / cells
        val equatorialCellIndex = cells / 2
        val bands = (0..<cells).map { i ->
            OceanBand(
                bottomLatitudeDegrees = max(-75 + i * degreesPerCell, -60.0),
                topLatitudeDegrees = min(-75 + (i + 1) * degreesPerCell, 60.0),
                polarity = when {
                    i == equatorialCellIndex -> 0.0
                    abs(equatorialCellIndex - i) % 2 == 0 -> -1.0
                    else -> 1.0
                },
                planet
            )
        }

        val currents = bands.flatMap { band ->
            if (band.centerLatitudeDegrees == 0.0) {
                return@flatMap listOf()
            }

            val oceans = band.region
                .floodFillGroupBy { it.continentiality >= 0 || it.isAboveWater }[false]
                ?.filter { waterBody -> waterBody.tiles.minOf { it.continentiality } <= -minCurrentCirculationRadius }
                ?.flatMap { ocean ->
                    val deepest =
                        ocean.tiles.filter { it.continentiality <= -minCurrentCirculationRadius }
                            .sortedBy { it.continentiality }
                    deepest.fold(mutableListOf<PlanetRegion>()) { acc, tile ->
                        if (!acc.any { it.tiles.contains(tile) }) {
                            val clockwiseExtent =
                                ocean.sortedClockwiseFrom(tile, Vector3.UP).takeWhile { bandTile ->
                                    val scanLine =
                                        band.region.parallelCross(bandTile, bandTile.tile.position.cross(Vector3.UP))
                                            .toList()
                                    scanLine.filter { it in ocean.tiles }.size >= scanLine.size * minCurrentScanlineProportion
                                }
                            val counterExtent =
                                ocean.sortedClockwiseFrom(tile, Vector3.DOWN).takeWhile { bandTile ->
                                    val scanLine =
                                        band.region.parallelCross(bandTile, bandTile.tile.position.cross(Vector3.UP))
                                            .toList()
                                    scanLine.filter { it in ocean.tiles }.size >= scanLine.size * minCurrentScanlineProportion
                                }

                            ocean.tiles.removeAll(clockwiseExtent + counterExtent)
                            acc.apply { add(PlanetRegion(planet, clockwiseExtent + counterExtent)) }
                        } else acc
                    }
                }
                ?.filter { it.tiles.size >= minOceanTilesForCurrent }
                ?: listOf()

            oceans.flatMap { ocean ->
                val oceanCenterGeoPoint = ocean.center.toGeoPoint()
                    .copy(latitude = PI * (band.topLatitudeDegrees + band.bottomLatitudeDegrees) * 0.5 / 180)
                val bandHeight = PI * (band.topLatitudeDegrees - band.bottomLatitudeDegrees) / 180
                ocean.edgeTiles.map { edgeTile ->
                    val averageNeighbor = edgeTile.neighbors
                        .filter { it !in ocean.tiles }
                        .map { it.tile.position }
                        .average()

                    val polarity = if (edgeTile.tile.position.y >= 0) 1 else -1
                    val edgeTileGeoPoint = edgeTile.tile.position.toGeoPoint()

                    val distance = bandHeight * targetCurrentRadiusProportion
                    val cosineLongitudeDifference =
                        (cos(distance) - sin(edgeTileGeoPoint.latitude) * sin(oceanCenterGeoPoint.latitude)) /
                                (cos(edgeTileGeoPoint.latitude) * cos(oceanCenterGeoPoint.latitude))
                    val longitudeDifference = acos(cosineLongitudeDifference)

                    val compareCirclePoint = listOf(
                        GeoPoint(oceanCenterGeoPoint.latitude, edgeTileGeoPoint.longitude + longitudeDifference),
                        GeoPoint(oceanCenterGeoPoint.latitude, edgeTileGeoPoint.longitude - longitudeDifference)
                    ).map { it.toVector3() }.minBy { it.distanceTo(ocean.center) }

                    val temperature = (
                            compareCirclePoint
                                .toCardinal(edgeTile.tile.position)
                                .normalized()
                                .dot((Vector2.RIGHT + Vector2.UP * -band.polarity * oceanCurrentStrengthDiagonalization).normalized()) * band.polarity
                            ).pow(oceanCurrentStrengthPow)

                    OceanCurrent(
                        edgeTile,
                        (averageNeighbor - edgeTile.tile.position).cross(edgeTile.tile.position) * polarity,
                        if (temperature.isNaN()) 0.0 else temperature
                    )
                }
            }
        }

        return currents
    }

    fun (Planet).updateCurrentDistanceMap() {
        warmCurrentDistanceMap = PlanetRegion(this, planetTiles.values.toMutableSet()).calculateEdgeDepthMap {
            val current = oceanCurrents[it.tileId]
            current != null && current.temperature >= oceanCurrentMinStrength
        }.mapKeys { it.key.tileId }
        coolCurrentDistanceMap = PlanetRegion(this, planetTiles.values.toMutableSet()).calculateEdgeDepthMap {
            val current = oceanCurrents[it.tileId]
            current != null && current.temperature <= -oceanCurrentMinStrength
        }.mapKeys { it.key.tileId }
    }
}