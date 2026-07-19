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
            mapLayerButtons.forEach { entry ->
                entry.button.setVisible(entry.available && value in entry.categories)
            }
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
        // Keep the permanent action buttons available while the tile infobox is open.
        settingsButtons.visible = true
        settingsOptionsPanel.visible = !hidden && settingsCategory != "none"
    }

    private val toggles = mutableMapOf<String, Boolean>()
    fun addToggle(
        toggle: String,
        categories: List<String>,
        onClick: (Boolean) -> Any,
    ): MapLayerCheckButton {
        val defaultValue = "default" in categories
        toggles[toggle] = defaultValue
        return MapLayerCheckButton(ToggleButton(default = defaultValue, onClick = {
            toggles[toggle] = it
            onClick(it)
        }).also {
            it.text = toggle
            it.setVisible(false)
            settingsOptionsList.addChild(it)
        }, categories).also { mapLayerButtons += it }
    }

    fun setAvailable(entry: MapLayerCheckButton, available: Boolean) {
        entry.available = available
        if (!available && entry.button.isPressed()) {
            entry.button.setPressed(false)
            entry.button.onClick?.invoke(false)
        }
        entry.button.visible = available && settingsCategory in entry.categories
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
            "ecology",
            "animal_ranges",
            "biota_distributions",
            "tectonics",
            "debug"
        ).forEachIndexed { index, category ->
            this.addItem("Settings: ${category.split("_").joinToString(" ") { it.capitalize() }}", index)
            categoriesIdMap[index] = category
        }
    }
}
