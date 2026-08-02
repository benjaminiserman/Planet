package dev.biserman.planet.rendering

import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.utils.UtilityExtensions.toTitleCase
import godot.core.Color

abstract class PlanetColorMode(val planetRenderer: PlanetRenderer) {
    abstract val name: String
    open val displayName get() = name.toTitleCase()

    open val categories: List<String> = listOf()
    private lateinit var toggleEntry: Gui.MapLayerCheckButton
    var visible: Boolean = false
        set(value) {
            field = value
            planetRenderer.updateMesh()
        }

    fun init() {
        visible = "default" in categories
        toggleEntry = Gui.instance.showSettingsButton.addToggle("$displayName Color Mode", categories) { visible = it }
    }

    fun setAvailable(available: Boolean) {
        if (::toggleEntry.isInitialized) Gui.instance.showSettingsButton.setAvailable(toggleEntry, available)
    }

    abstract fun colorsFor(planetTile: PlanetTile): Sequence<Color?>
}