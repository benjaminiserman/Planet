package dev.biserman.planet.rendering.colormodes

import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.PlanetColorMode
import dev.biserman.planet.rendering.PlanetRenderer
import dev.biserman.planet.topology.Corner
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.memo
import godot.core.Color
import godot.core.Vector3
import kotlin.math.absoluteValue
import kotlin.math.max

class BiomeColorMode(planetRenderer: PlanetRenderer, override val visibleByDefault: Boolean) :
    PlanetColorMode(planetRenderer) {
    override val name = "biome"

    enum class RenderMode {
        BIOME,
        SNOW,
        WATER
    }

    val warpNoise by memo({ planetRenderer.planet.tectonicAge }) { VectorWarpNoise(planetRenderer.planet.tectonicAge, 3f) }
    fun getMode(tile: PlanetTile) = when {
        tile.elevation >= 5000 -> RenderMode.SNOW
        warpNoise.warp(tile.tile.position, 0.075).y.absoluteValue >= 0.9 -> RenderMode.SNOW
        tile.elevation < planetRenderer.planet.seaLevel -> RenderMode.WATER
        else -> RenderMode.BIOME
    }

    fun hue(tile: PlanetTile) = when (getMode(tile)) {
//        RenderMode.BIOME -> biomeHue
        RenderMode.BIOME -> 0.25
        RenderMode.SNOW -> 0.0
        RenderMode.WATER -> 0.65
    }

    fun value(tile: PlanetTile) = when (getMode(tile)) {
        RenderMode.BIOME -> tile.elevation.adjustRange(
            planetRenderer.planet.seaLevel..5000.0,
            0.15..0.9
        )
        RenderMode.SNOW -> 1.0
        RenderMode.WATER -> tile.elevation
            .adjustRange(-6000.0..planetRenderer.planet.seaLevel, 0.05..0.2)
    }

    fun saturation(tile: PlanetTile) = when (getMode(tile)) {
        RenderMode.BIOME -> 0.95
        RenderMode.SNOW -> 0.0
        RenderMode.WATER -> 0.9
    }

    fun averageAroundPoint(corner: Corner, planetTile: PlanetTile, getFn: (PlanetTile) -> Double): Double {
        val matchingTiles = corner.tiles.map { tile -> planetTile.planet.planetTiles[tile]!! }
            .filter { getMode(it) == getMode(planetTile) }

        return matchingTiles.sumOf { getFn(it) } / matchingTiles.size
    }

    override fun colorsFor(planetTile: PlanetTile) = sequence {
        val tile = planetTile.tile
        val hue = hue(planetTile)

        yield(Color.fromHsv(hue, saturation(planetTile), value(planetTile), 1.0))

        yieldAll((0..<tile.corners.size).map {
            val corner = tile.corners[it]
            val color = Color.fromHsv(
                hue,
                averageAroundPoint(corner, planetTile) { tile -> saturation(tile) },
                averageAroundPoint(corner, planetTile) { tile -> value(tile) },
                1.0
            )

            color
        })
    }
}