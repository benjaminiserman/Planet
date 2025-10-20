package dev.biserman.planet.planet

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
        val center = region.tiles.minBy { (it.tile.position.toGeoPoint().latitudeDegrees - centerLatitudeDegrees).absoluteValue }
        PlanetRegion(planet, region.parallelCross(center, Vector3.UP))
    }
}

object OceanCurrents {
    val minOceanRadius = 3

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

        GD.print("bands: $bands")

        planet.planetTiles.values.forEach { it.debugColor = Color.black }

        bands.forEach { band ->
            val oceans = band.region
                .floodFillGroupBy { it.continentiality >= 0 }[false]
                ?.filter { waterBody -> waterBody.tiles.minOf { it.continentiality } <= -minOceanRadius }
                ?.flatMap { ocean ->
                    val oceanBandTiles = PlanetRegion(planet, band.centerParallel.tiles.filter { it in ocean.tiles })
                    val first = oceanBandTiles.tiles.first()
                    oceanBandTiles.raycastClockwise(first, Vector3.UP).forEach { bandTile ->
                        val scanLine = band.region.raycastClockwise(bandTile, bandTile.tile.position.cross(Vector3.UP)).toList()
                        if (scanLine.filter { it in ocean.tiles }.size >= scanLine.size * 0.5) {

                        }
                    }
                }
                ?: listOf()
            val oceans = listOf(band.centerParallel)

            oceans.forEach { region ->
                val color = Color.randomHsv()
                region.tiles.forEach {
                    if (it.debugColor == Color.black) it.debugColor = color
                }
            }

            GD.print("oceans: ${oceans.size}, ${oceans.sumOf { it.tiles.size }}")
        }
    }
}