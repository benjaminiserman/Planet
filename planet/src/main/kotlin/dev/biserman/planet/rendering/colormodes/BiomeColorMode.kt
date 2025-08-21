package dev.biserman.planet.rendering.colormodes

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.PlanetColorMode
import dev.biserman.planet.rendering.PlanetRenderer
import dev.biserman.planet.topology.Corner
import godot.core.Color
import kotlin.math.max

class BiomeColorMode(planetRenderer: PlanetRenderer, override val visibleByDefault: Boolean) :
    PlanetColorMode(planetRenderer) {
    override val name = "biome"

    enum class RenderMode {
        BIOME,
        SNOW,
        WATER
    }

    fun getMode(elevation: Float) = when {
        elevation < planetRenderer.planet!!.seaLevel -> RenderMode.WATER
        elevation >= 300f -> RenderMode.SNOW
        else -> RenderMode.BIOME
    }

    fun hue(biomeHue: Double, elevation: Float) = when (getMode(elevation)) {
//        RenderMode.BIOME -> biomeHue
        RenderMode.BIOME -> 0.25
        RenderMode.SNOW -> 0.0
        RenderMode.WATER -> 0.65
    }

    fun value(elevation: Float) = when (getMode(elevation)) {
        RenderMode.BIOME -> elevation / 1000.0 * 0.75 + 0.15
        RenderMode.SNOW -> 1.0
        RenderMode.WATER -> max(elevation / 1000.0 + 0.5, 0.0) * 0.33 + 0.05
    }

    fun saturation(elevation: Float) = when (getMode(elevation)) {
        RenderMode.BIOME -> 0.95
        RenderMode.SNOW -> 0.0
        RenderMode.WATER -> 0.9
    }

    fun averageAroundPoint(corner: Corner, planetTile: PlanetTile, getFn: (PlanetTile) -> Double): Double {
        val matchingTiles = corner.tiles.map { tile -> planetTile.planet.planetTiles[tile]!! }
            .filter { getMode(it.elevation) == getMode(planetTile.elevation) }

        return matchingTiles.sumOf { getFn(it) } / matchingTiles.size
    }

    override fun colorsFor(planetTile: PlanetTile) = sequence {
        val tile = planetTile.tile
        val hue = hue(planetTile.tectonicPlate?.biomeColor?.h ?: 0.0, planetTile.elevation)

        yield(Color.fromHsv(hue, saturation(planetTile.elevation), value(planetTile.elevation), 1.0))

        yieldAll((0..<tile.corners.size).map {
            val corner = tile.corners[it]
            val color = Color.fromHsv(
                hue,
                averageAroundPoint(corner, planetTile) { tile -> saturation(tile.elevation) },
                averageAroundPoint(corner, planetTile) { tile -> value(tile.elevation) },
                1.0
            )

            color
        })
    }
}