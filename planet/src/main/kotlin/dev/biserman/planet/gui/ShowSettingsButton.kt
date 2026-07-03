package dev.biserman.planet.gui

import dev.biserman.planet.gui.Gui.Companion.instance
import dev.biserman.planet.gui.Gui.MapLayerCheckButton
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Control
import godot.api.OptionButton
import godot.api.PanelContainer
import godot.api.VBoxContainer
import godot.core.connect

@RegisterClass
class ShowSettingsButton() : OptionButton() {
    val mapLayerButtons = mutableListOf<MapLayerCheckButton>()
    val categoriesIdMap = mutableMapOf<Int, String>()
    var settingsCategory = "none"
        set(value) {
            field = value
            mapLayerButtons.forEach { it.button.setVisible(value in it.categories) }
            settingsOptionsPanel.visible = !hiddenForTileInspection && value != "none"
        }

    private var hiddenForTileInspection = false
    private val settingsButtons by lazy {
        instance.findChild("SettingsButtons") as Control
    }

    private val settingsOptionsPanel by lazy {
        instance.findChild("SettingsOptionsPanel") as PanelContainer
    }
    private val settingsOptionsList by lazy {
        instance.findChild("SettingsOptionsList") as VBoxContainer
    }

    fun setHiddenForTileInspection(hidden: Boolean) {
        hiddenForTileInspection = hidden
        settingsButtons.visible = !hidden
        settingsOptionsPanel.visible = !hidden && settingsCategory != "none"
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
            it.setVisible(false)
            settingsOptionsList.addChild(it)
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
            "terrain",
            "biome",
            "climate",
            "stats",
            "tectonics",
            "debug"
        ).forEachIndexed { index, category ->
            this.addItem("Settings: ${category.split("_").joinToString(" ") { it.capitalize() }}", index)
            categoriesIdMap[index] = category
        }
    }
}
