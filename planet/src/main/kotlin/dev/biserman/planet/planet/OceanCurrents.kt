package dev.biserman.planet.planet

import dev.biserman.planet.geometry.GeoPoint
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.utils.randomHsv
import dev.biserman.planet.utils.toCardinal
import godot.core.Color
import godot.core.Vector2
import godot.core.Vector3
import godot.global.GD
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

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
    val minOceanRadius = 4
    val minOceanTiles = 50
    val minScanlineProportion = 0.3

    fun viaEarthlikeHeuristic(planet: Planet, numBands: Int): List<OceanCurrent> {
        val cells = numBands - 2
        val degreesPerCell = 150.0 / cells
        val equatorialCellIndex = cells / 2
        val bands = (0..<cells).map { i ->
            OceanBand(
                bottomLatitudeDegrees = -75 + i * degreesPerCell,
                topLatitudeDegrees = -75 + (i + 1) * degreesPerCell,
                polarity = when {
                    i == equatorialCellIndex -> 0.0
                    abs(equatorialCellIndex - i) % 2 == 1 -> -1.0
                    else -> 1.0
                },
                planet
            )
        }

//        planet.planetTiles.values.forEach { it.debugColor = Color.black }

        val currents = bands.flatMap { band ->
            val oceans = band.region
                .floodFillGroupBy { it.continentiality >= 0 }[false]
                ?.filter { waterBody -> waterBody.tiles.minOf { it.continentiality } <= -minOceanRadius }
                ?.flatMap { ocean ->
                    val deepest =
                        ocean.tiles.filter { it.continentiality <= -minOceanRadius }.sortedBy { it.continentiality }
                    deepest.fold(mutableListOf<PlanetRegion>()) { acc, tile ->
                        if (!acc.any { it.tiles.contains(tile) }) {
                            val clockwiseExtent =
                                ocean.sortedClockwiseFrom(tile, Vector3.UP).takeWhile { bandTile ->
                                    val scanLine =
                                        band.region.parallelCross(bandTile, bandTile.tile.position.cross(Vector3.UP))
                                            .toList()
                                    scanLine.filter { it in ocean.tiles }.size >= scanLine.size * minScanlineProportion
                                }
                            val counterExtent =
                                ocean.sortedClockwiseFrom(tile, Vector3.DOWN).takeWhile { bandTile ->
                                    val scanLine =
                                        band.region.parallelCross(bandTile, bandTile.tile.position.cross(Vector3.UP))
                                            .toList()
                                    scanLine.filter { it in ocean.tiles }.size >= scanLine.size * minScanlineProportion
                                }

                            ocean.tiles.removeAll(clockwiseExtent + counterExtent)
                            acc.apply { add(PlanetRegion(planet, clockwiseExtent + counterExtent)) }
                        } else acc
                    }
                }
                ?.filter { it.tiles.size >= minOceanTiles }
                ?: listOf()

            if (band.centerLatitudeDegrees == 0.0) {
                return@flatMap listOf()
            }

            oceans.flatMap { ocean ->
                ocean.edgeTiles.map { edgeTile ->
                    val averageNeighbor = edgeTile.neighbors
                        .filter { it !in ocean.tiles }
                        .map { it.tile.position }
                        .average()

                    val polarity = if (edgeTile.tile.position.y >= 0) 1 else -1

                    val delta = (edgeTile.tile.position - ocean.center)
                    val bandHeight = PI * (band.topLatitudeDegrees - band.bottomLatitudeDegrees) / 180
                    val compareVector =
                        (ocean.center + delta * min(delta.length(), bandHeight * 0.5) / delta.length()).normalized()

                    OceanCurrent(
                        edgeTile,
                        (averageNeighbor - edgeTile.tile.position).cross(edgeTile.tile.position) * polarity,
                        ((edgeTile.tile.position - compareVector)
                            .toCardinal(edgeTile.tile.position)
                            .normalized()
                            .dot(Vector2.RIGHT) * band.polarity).pow(5)
                    )
                }
            }


//            currents.forEach { current ->
//                current.planetTile.debugColor =
//                    if (current.temperature > 0) Color.red * current.temperature
//                    else Color.blue * current.temperature
//            }

//            oceans.forEach { region ->
//                val color = Color.randomHsv()
//                region.tiles.forEach {
//                    it.debugColor = color
//                }
//            }
        }

        return currents
    }
}