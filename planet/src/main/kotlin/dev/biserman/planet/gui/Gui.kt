package dev.biserman.planet.gui

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.Path.Companion.toMesh
import dev.biserman.planet.geometry.Path.Companion.toPaths
import dev.biserman.planet.rendering.DebugDraw.drawMesh
import dev.biserman.planet.rendering.MeshData
import dev.biserman.planet.rendering.SimpleDebugRenderer
import dev.biserman.planet.topology.Tile
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.CheckButton
import godot.api.Label
import godot.api.Material
import godot.api.Mesh
import godot.api.Node
import godot.api.ScrollContainer
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.core.Vector2
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle

@RegisterClass
class Gui() : Node() {
    val infoboxContainer by lazy { findChild("InfoboxContainer") as ScrollContainer }
    val infoboxLabel by lazy { infoboxContainer.findChild("Label") as Label }

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
    var selectedTile: Tile? = null
        set(value) {
            field = value
            selectedTileRenderer.update(value)
            if (value == null) {
                infoboxContainer.visible = false
                selectedTileRenderer.visible = false
            } else {
                infoboxLabel.text = Main.instance.planet.planetTiles[value]?.getInfoText() ?: "null"
                infoboxContainer.visible = true
                selectedTileRenderer.visible = true
            }
        }

    @RegisterFunction
    override fun _ready() {
        instance = this
        buttons.forEach { addChild(it) }
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
                it.setPosition(Vector2(0, 40 * (buttons.size + 1)))
                it.setVisible(settingsVisible)
                instance.addChild(it)
            }
        }
    }
}