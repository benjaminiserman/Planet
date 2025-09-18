package dev.biserman.planet.rendering.colormodes

import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.PlanetColorMode
import dev.biserman.planet.rendering.PlanetRenderer
import godot.core.Color

class SimpleDoubleColorMode(
    planetRenderer: PlanetRenderer,
    override val name: String,
    val colorFn: (Double?) -> Color = defaultColorFn,
    override val visibleByDefault: Boolean = false,
    val getFn: (PlanetTile) -> Double?,
) : PlanetColorMode(planetRenderer) {
    override fun colorsFor(planetTile: PlanetTile): Sequence<Color> = sequence {
        yield(colorFn(getFn(planetTile)))

        yieldAll((0..<planetTile.tile.corners.size).map {
            val validTilesValues =
                planetTile.tile.corners[it].tiles.mapNotNull { tile -> getFn(planetTile.planet.getTile(tile)) }
            colorFn(validTilesValues.sum() / validTilesValues.size)
        })
    }

    companion object {
        val defaultColorFn = redWhenNull { Color(it, it, it, 1.0) }

        fun redOutsideRange(range: ClosedRange<Double>, colorFn: (Double) -> Color = defaultColorFn) =
            redWhenNull { if (it in range) colorFn(it) else Color.red }

        fun redWhenNull(colorFn: (Double) -> Color) = { level: Double? ->
            if (level == null) Color.red else colorFn(level)
        }
    }
}