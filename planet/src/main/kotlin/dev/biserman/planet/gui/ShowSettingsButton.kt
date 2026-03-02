package dev.biserman.planet.gui

import dev.biserman.planet.gui.Gui.Companion.instance
import dev.biserman.planet.gui.Gui.MapLayerCheckButton
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.OptionButton
import godot.core.Vector2
import godot.core.connect

@RegisterClass
class ShowSettingsButton() : OptionButton() {
    val mapLayerButtons = mutableListOf<MapLayerCheckButton>()
    val categoriesIdMap = mutableMapOf<Int, String>()
    var settingsCategory = "none"
        set(value) {
            field = value
            mapLayerButtons.forEach { it.button.setVisible(value in it.categories) }
            mapLayerButtons.filter { it.button.isVisible() }
                .sortedBy { it.button.text }
                .forEachIndexed { index, mapLayerCheckButton ->
                    mapLayerCheckButton.button.setPosition(Vector2(0, 20 * index + 40))
                }
        }

    private val toggles = mutableMapOf<String, Boolean>()
    fun addToggle(toggle: String, categories: List<String>, onClick: (Boolean) -> Any) {
        val defaultValue = "default" in categories
        toggles[toggle] = defaultValue
        mapLayerButtons += MapLayerCheckButton(ToggleButton(default = defaultValue, onClick = {
            toggles[toggle] = it
            onClick(it)
        }).also {
            it.text = toggle
            it.setPosition(Vector2(0, 20 * mapLayerButtons.size + 40))
//            it.scale = Vector2(0.6, 0.6)
            it.setVisible(false)
            instance.addChild(it)
        }, categories)
    }

    fun resetToggles() {
        mapLayerButtons.forEach {
            if (it.button.isPressed()) {
                it.button.setPressed(false)
                it.button.onClick?.invoke(false)
            }
        }
    }

    @RegisterFunction
    override fun _ready() {
        this.itemSelected.connect { selectedId ->
            settingsCategory = categoriesIdMap[selectedId.toInt()]!!
        }

        listOf(
            "none",
            "default",
            "feature",
            "overlay",
            "base_layer",
            "biome",
            "climate",
            "tectonics",
            "debug"
        ).forEachIndexed { index, category ->
            this.addItem("Settings: ${category.split("_").joinToString(" ") { it.capitalize() }}", index)
            categoriesIdMap[index] = category
        }
    }
}