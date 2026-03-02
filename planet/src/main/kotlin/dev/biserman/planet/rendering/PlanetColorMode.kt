package dev.biserman.planet.rendering

import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.PlanetTile
import godot.core.Color

abstract class PlanetColorMode(val planetRenderer: PlanetRenderer) {
    abstract val name: String
    open val displayName: String get() = name.split("_").joinToString(" ") { it.capitalize() }

    open val categories: List<String> = listOf()
    var visible: Boolean = false
        set(value) {
            field = value
            planetRenderer.updateMesh()
        }

    fun init() {
        visible = "default" in categories
        Gui.instance.showSettingsButton.addToggle("$displayName Color Mode", categories) { visible = it }
    }

    abstract fun colorsFor(planetTile: PlanetTile): Sequence<Color?>
}