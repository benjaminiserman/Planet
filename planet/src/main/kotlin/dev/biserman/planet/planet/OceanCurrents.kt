package dev.biserman.planet.planet

import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.utils.randomHsv
import godot.core.Color
import godot.core.Vector3
import godot.global.GD
import kotlin.math.abs
import kotlin.math.absoluteValue

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

    fun viaEarthlikeHeuristic(planet: Planet, numBands: Int) {
        val cells = numBands - 2
        val degreesPerCell = 150.0 / cells
        val equatorialCellIndex = cells / 2
        val bands = (0..<cells).map { i ->
            OceanBand(
                bottomLatitudeDegrees = -75 + i * degreesPerCell,
                topLatitudeDegrees = -75 + (i + 1) * degreesPerCell,
                polarity = when {
                    i == equatorialCellIndex -> 0.0
                    abs(equatorialCellIndex - i) % 2 == 1 -> 1.0
                    else -> -1.0
                },
                planet
            )
        }

        planet.planetTiles.values.forEach { it.debugColor = Color.black }

        bands.forEach { band ->
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
                                    scanLine.filter { it in ocean.tiles }.size >= scanLine.size * 0.5
                                }
                            val counterExtent =
                                ocean.sortedClockwiseFrom(tile, Vector3.DOWN).takeWhile { bandTile ->
                                    val scanLine =
                                        band.region.parallelCross(bandTile, bandTile.tile.position.cross(Vector3.UP))
                                            .toList()
                                    scanLine.filter { it in ocean.tiles }.size >= scanLine.size * 0.5
                                }

                            ocean.tiles.removeAll(clockwiseExtent + counterExtent)
                            acc.apply { add(PlanetRegion(planet, clockwiseExtent + counterExtent)) }
                        } else acc
                    }
                }
                ?.filter { it.tiles.size >= minOceanTiles }
                ?: listOf()

            oceans.flatMap { ocean ->
                ocean.edgeTiles.map { edgeTile ->
                    val averageNeighbor = edgeTile.neighbors
                        .filter { it !in ocean.tiles }
                        .map { it.tile.position }
                        .average()

                    OceanCurrent(
                        edgeTile,
                        (averageNeighbor - edgeTile.tile.position).cross(edgeTile.tile.position) *
                                if (edgeTile.tile.position.y >= 0) 1 else -1,
                        0.0
                    )
                }

            }

            oceans.forEach { region ->
                val color = Color.randomHsv()
                region.tiles.forEach {
                    it.debugColor = color
                }
            }
        }
    }
}