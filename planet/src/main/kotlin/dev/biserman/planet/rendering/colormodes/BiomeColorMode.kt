package dev.biserman.planet.rendering.colormodes

import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.PlanetColorMode
import dev.biserman.planet.rendering.PlanetRenderer
import godot.core.Color

class BiomeColorMode(planetRenderer: PlanetRenderer, override val visibleByDefault: Boolean) : PlanetColorMode(planetRenderer) {
    override val name = "biome"

    fun levelIt(level: Float) = when {
        level < 0.8f -> level * 0.5f
        level >= 1.0f -> 1.0f
        else -> level - 0.25f
    }

    fun saturation(level: Float) = when {
        level >= 1.0f -> 0.0f
        else -> 0.9f
    }

    override fun colorsFor(planetTile: PlanetTile) = sequence {
        val tile = planetTile.tile
        val level = planetTile.elevation.adjustRange(-0.5f..0.5f, 0.0f..1.0f)
        val hue = planetTile.tectonicPlate?.biomeColor?.h ?: 0.0

        var color = Color.fromHsv(hue, saturation(level).toDouble(), levelIt(level).toDouble(), level.toDouble())
        if (level < 0.7) {
            color = Color.fromHsv(0.65, saturation(level).toDouble(), levelIt(level).toDouble(), level.toDouble())
        }
        yield(color)

        yieldAll((0..<tile.corners.size).map {
            val level =
                (tile.corners[it].tiles.map { tile -> planetTile.planet.planetTiles[tile]!!.elevation }.toFloatArray()
                    .sum() / tile.corners[it].tiles.size)
                    .adjustRange(-0.5f..0.5f, 0.0f..1.0f)
            val color = Color.fromHsv(
                color.h,
                color.s,
                levelIt(level).toDouble(),
                level.toDouble()
            )

            color
        })
    }
}