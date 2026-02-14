package dev.biserman.planet.rendering.colormodes

import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.PlanetColorMode
import dev.biserman.planet.rendering.PlanetRenderer
import dev.biserman.planet.topology.Corner
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.memo
import godot.common.util.lerp
import godot.core.Color
import kotlin.jvm.optionals.getOrNull
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class BiomeColorMode(planetRenderer: PlanetRenderer, override val visibleByDefault: Boolean) :
    PlanetColorMode(planetRenderer) {
    override val name = "biome"

    enum class RenderMode {
        BIOME,
        SNOW,
        WATER
    }

//    fun snowLine(tile: PlanetTile) = (1 - tile.tile.position.y.absoluteValue).pow(0.5) * 6500


    fun getMode(tile: PlanetTile) = when {
//        tile.elevation >= snowLine(tile) -> RenderMode.SNOW
//        warpNoise.warp(tile.tile.position, 0.075).y.absoluteValue >= 0.95 -> RenderMode.SNOW
        tile.isIceCap -> RenderMode.SNOW
        tile.elevation <= planetRenderer.planet.seaLevel -> RenderMode.WATER
        else -> RenderMode.BIOME
    }

    fun slopeScale(tile: PlanetTile): Double {
        val contiguousSlope = tile.prominence
        return if (contiguousSlope.isNaN()) 0.0
        else contiguousSlope.scaleAndCoerceIn(0.0..2500.0, 0.0..1.0).pow(0.75)
    }

    fun hue(tile: PlanetTile) = when (getMode(tile)) {
//        RenderMode.BIOME -> biomeHue
        RenderMode.BIOME -> lerp(0.25, 0.09, slopeScale(tile))
        RenderMode.SNOW -> 0.0
        RenderMode.WATER -> 0.65
    }

    fun value(tile: PlanetTile) = when (getMode(tile)) {
        RenderMode.BIOME -> lerp(
            tile.elevation.scaleAndCoerceIn(
                planetRenderer.planet.seaLevel..5000.0,
                0.15..0.9
            ),
            0.8,
            if (tile.nonContiguousSlope.isNaN()) 0.0 else {
                1 - tile.nonContiguousSlope.scaleAndCoerceIn(
                    0.0..200.0,
                    0.0..1.0
                ).pow(2)
            },
        )
        RenderMode.SNOW -> 1.0
        RenderMode.WATER -> tile.elevation
            .adjustRange(-6000.0..planetRenderer.planet.seaLevel, 0.05..0.2)
    }

    fun saturation(tile: PlanetTile) = when (getMode(tile)) {
        RenderMode.BIOME -> lerp(0.9, 0.5, slopeScale(tile))
        RenderMode.SNOW -> 0.0
        RenderMode.WATER -> 0.9
    }

    fun averageAroundPoint(corner: Corner, planetTile: PlanetTile, getFn: (PlanetTile) -> Double): Double {
        val matchingTiles = corner.tiles.map { tile -> planetTile.planet.getTile(tile) }
            .filter { getMode(it) == getMode(planetTile) }

        return matchingTiles.sumOf { getFn(it) } / matchingTiles.size
    }

    override fun colorsFor(planetTile: PlanetTile) = sequence {
        val tile = planetTile.tile
        val hue = hue(planetTile)

//        val color = Color.fromHsv(hue, saturation(planetTile), value(planetTile), 1.0)
        val biomeColor = planetTile.koppen.getOrNull()?.terrainColor
            ?.run { Color.fromHsv(h, min(1.0, s * 1.2), v, a) } ?: Color.fromHsv(0.25, 0.9, 0.15, 1.0)
        val color = when (getMode(planetTile)) {
            RenderMode.BIOME -> biomeColor.lerp(
                Color.fromHsv(0.06, min(biomeColor.s, 0.33), 0.5, 1.0),
                min(0.75, slopeScale(planetTile))
            )
            RenderMode.SNOW -> Color.fromHsv(0.0, 0.0, 1.0, 1.0)
            RenderMode.WATER -> Color.fromHsv(
                0.65,
                0.9,
                planetTile.elevation.adjustRange(-6000.0..planetRenderer.planet.seaLevel, 0.05..0.2),
                1.0
            )
        }

        yield(color)

        yieldAll((0..<tile.corners.size).map {
//            val corner = tile.corners[it]
//            val color = Color.fromHsv(
//                averageAroundPoint(corner, planetTile) { tile -> hue(tile) },
//                averageAroundPoint(corner, planetTile) { tile -> saturation(tile) },
//                averageAroundPoint(corner, planetTile) { tile -> value(tile) },
//                1.0
//            )

            color
        })
    }
}