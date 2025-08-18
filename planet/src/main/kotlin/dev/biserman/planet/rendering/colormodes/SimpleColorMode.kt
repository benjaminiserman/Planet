package dev.biserman.planet.rendering.colormodes

import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.PlanetColorMode
import dev.biserman.planet.rendering.PlanetRenderer
import godot.core.Color

class SimpleColorMode(
    planetRenderer: PlanetRenderer,
    override val name: String,
    val colorFn: (Double) -> Color = { Color(it, it, it, 1.0) },
    override val visibleByDefault: Boolean = false,
    val getFn: (PlanetTile) -> Double,
) : PlanetColorMode(planetRenderer) {
    override fun colorsFor(planetTile: PlanetTile): Sequence<Color> = sequence {
        yield(colorFn(getFn(planetTile)))

        yieldAll((0..<planetTile.tile.corners.size).map {
            val level =
                planetTile.tile.corners[it].tiles
                    .sumOf { tile ->
                        getFn(planetTile.planet.planetTiles[tile]!!)
                    } / planetTile.tile.corners[it].tiles.size

            colorFn(level)
        })
    }

    companion object {
        fun redOutsideRange(range: ClosedRange<Double>) =
            fun(level: Double) = if (level in range) Color(level, level, level, 1.0) else Color.red
    }
}