package dev.biserman.planet.rendering.colormodes

import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.PlanetColorMode
import dev.biserman.planet.rendering.PlanetRenderer
import godot.core.Color

class SimpleColorMode(
    planetRenderer: PlanetRenderer,
    override val name: String,
    override val visibleByDefault: Boolean = false,
    val getFn: (PlanetTile) -> Color?,
) : PlanetColorMode(planetRenderer) {
    override fun colorsFor(planetTile: PlanetTile): Sequence<Color?> = sequence {
        val tileColor = getFn(planetTile)
        yield(tileColor)

        yieldAll((0..<planetTile.tile.corners.size).map {
            tileColor
        })
    }
}