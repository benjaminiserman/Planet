package dev.biserman.planet.gui

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.Path.Companion.toMesh
import dev.biserman.planet.geometry.Path.Companion.toPaths
import dev.biserman.planet.rendering.MeshData
import dev.biserman.planet.rendering.SimpleDebugRenderer
import dev.biserman.planet.topology.Tile
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.CanvasItem
import godot.api.CheckButton
import godot.api.Label
import godot.api.MenuButton
import godot.api.Mesh
import godot.api.Node
import godot.api.RefCounted
import godot.api.ScrollContainer
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.core.NativeCallable
import godot.core.PackedVector2Array
import godot.core.Vector2
import godot.global.GD
import kotlin.math.sin

@RegisterClass
class Gui() : Node() {
    val infoboxContainer by lazy { findChild("InfoboxContainer") as ScrollContainer }
    val infoboxLabel by lazy { infoboxContainer.findChild("Label") as Label }
    val tectonicAgeLabel by lazy { findChild("TectonicAge") as Label }
    val statsGraph by lazy { StatsGraph(findChild("DebugGraph") as CanvasItem) }

    val selectedTileMaterial = StandardMaterial3D().apply {
        this.setAlbedo(Color.white)
    }
    val selectedTileRenderer = SimpleDebugRenderer<Tile?>(this, "selected_tile") { tile ->
        listOf(
            if (tile == null) {
                MeshData(Mesh(), selectedTileMaterial)
            } else {
                MeshData(
                    tile.borders.toPaths().toMesh()
                        .apply {
                            this.verts.forEach { it.position *= 1.001 }
                        }
                        .toWireframe(), selectedTileMaterial)
            }
        )
    }

    fun updateInfobox() {
        infoboxLabel.text = Main.instance.planet.planetTiles[selectedTile]?.getInfoText() ?: "null"
    }

    var selectedTile: Tile? = null
        set(value) {
            field = value
            selectedTileRenderer.update(value)
            if (value == null) {
                infoboxContainer.visible = false
                selectedTileRenderer.visible = false
            } else {
                updateInfobox()
                infoboxContainer.visible = true
                selectedTileRenderer.visible = true
            }
        }


    @RegisterFunction
    override fun _ready() {
        instance = this
        buttons.forEach { addChild(it) }
        addToggle("Show Stats", defaultValue = statsGraph.visible) { statsGraph.visible = it }
        addToggle("Track Stats", defaultValue = statsGraph.trackStats) { statsGraph.trackStats = it }
    }

    companion object {
        lateinit var instance: Gui private set
        val buttons = mutableListOf<CheckButton>()

        var settingsVisible = false
            set(value) {
                field = value
                buttons.forEach { it.setVisible(value) }
            }

        private val toggles = mutableMapOf<String, Boolean>()
        fun addToggle(toggle: String, defaultValue: Boolean = false, onClick: (Boolean) -> Any) {
            toggles[toggle] = defaultValue
            buttons += ToggleButton(default = defaultValue, onClick = {
                toggles[toggle] = it
                onClick(it)
            }).also {
                it.text = toggle
                it.setPosition(Vector2(0, 25 * (buttons.size + 1)))
                it.setVisible(settingsVisible)
                instance.addChild(it)
            }
        }
    }
}