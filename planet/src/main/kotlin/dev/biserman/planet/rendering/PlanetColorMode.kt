package dev.biserman.planet.rendering

import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.PlanetTile
import godot.core.Color

abstract class PlanetColorMode(val planetRenderer: PlanetRenderer) {
    abstract val name: String
    open val displayName: String get() = name.split("_").joinToString(" ") { it.capitalize() }

    open val visibleByDefault: Boolean = false
    var visible: Boolean = false
        set(value) {
            field = value
            planetRenderer.updateMesh()
        }

    fun init() {
        visible = visibleByDefault
        Gui.addToggle("$displayName Color Mode", defaultValue = visibleByDefault) { visible = it }
    }

    abstract fun colorsFor(planetTile: PlanetTile): Sequence<Color?>
}