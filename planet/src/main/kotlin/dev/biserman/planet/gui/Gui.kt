package dev.biserman.planet.gui

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.Path.Companion.toMesh
import dev.biserman.planet.geometry.Path.Companion.toPaths
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.planet.MapProjections
import dev.biserman.planet.planet.MapProjections.applyValueTo
import dev.biserman.planet.planet.MapProjections.projectTiles
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.MeshData
import dev.biserman.planet.rendering.SimpleDebugRenderer
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.Serialization
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.*
import godot.core.Color
import godot.core.Vector2
import godot.core.connect
import godot.global.GD

@RegisterClass
class Gui() : Node() {
    val infoboxContainer by lazy { findChild("InfoboxContainer") as ScrollContainer }
    val infoboxLabel by lazy { infoboxContainer.findChild("Label") as Label }
    val tectonicAgeLabel by lazy { findChild("TectonicAge") as Label }
    val statsGraph by lazy { StatsGraph(findChild("DebugGraph") as CanvasItem) }

    val saveButton by lazy { findChild("SaveButton") as Button }
    val loadButton by lazy { findChild("LoadButton") as Button }
    val projectButton by lazy { findChild("ProjectButton") as Button }
    val importButton by lazy { findChild("ImportButton") as Button }

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
        infoboxLabel.text = if (selectedTile == null) "" else Main.instance.planet.getTile(selectedTile!!).getInfoText()
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

        saveButton.pressed.connect {
            Serialization.save(Main.instance.planet)
            GD.print("Saved!")
        }
        loadButton.pressed.connect {
            val loadedPlanet = Serialization.load()
            Main.instance.updatePlanet(loadedPlanet)
            GD.print("Loaded!")
            GD.print("Tectonic age: ${loadedPlanet.tectonicAge}")
        }
        projectButton.pressed.connect {
            MapProjections.EQUIDISTANT.projectTiles(
                Main.instance.planet,
                "map.png",
                450,
                225,
                useKriging = true,
                Main.instance.planet.topology.averageRadius * 1.5
            ) { tile: PlanetTile -> Main.instance.planetRenderer.getColor(tile) }
            GD.print("Image created")
        }
        importButton.pressed.connect {
            MapProjections.EQUIDISTANT.applyValueTo(
                Main.instance.planet,
                "import_elevation.png",
            ) { value ->
                this.elevation = value.adjustRange(0.0..1.0, -10000.0..10000.0)
            }
            Main.instance.planet.tectonicAge -= 1
            Main.instance.planetRenderer.update(Main.instance.planet)
        }
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