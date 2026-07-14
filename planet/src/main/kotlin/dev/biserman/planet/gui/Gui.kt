package dev.biserman.planet.gui

import com.fasterxml.jackson.module.kotlin.readValue
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.Path.Companion.toMesh
import dev.biserman.planet.geometry.Path.Companion.toPaths
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.planet.MapProjections
import dev.biserman.planet.planet.MapProjections.applyValueTo
import dev.biserman.planet.planet.MapProjections.projectTiles
import dev.biserman.planet.planet.climate.OceanCurrents
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.climate.ClimateClassifier
import dev.biserman.planet.planet.climate.ClimateSimulation
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals
import dev.biserman.planet.planet.climate.HersfeldtReference
import dev.biserman.planet.planet.climate.OceanCurrents.updateCurrentDistanceMap
import dev.biserman.planet.planet.tectonics.TectonicGlobals
import dev.biserman.planet.rendering.MeshData
import dev.biserman.planet.rendering.SimpleDebugRenderer
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.Serialization
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.*
import godot.core.Color
import godot.core.PackedByteArray
import godot.core.connect
import godot.global.GD
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale.getDefault
import kotlin.math.min
import kotlin.random.Random

@RegisterClass
class Gui() : Node() {
    val infoboxContainer by lazy { findChild("InfoboxContainer") as TabContainer }
    val daysPassedLabel by lazy { findChild("DaysPassed") as Label }
    val tectonicAgeLabel by lazy { findChild("TectonicAge") as Label }
    val statsGraph by lazy { StatsGraph(findChild("DebugGraph") as CanvasItem) }

    val showSettingsButton by lazy { findChild("ShowSettingsButton") as ShowSettingsButton }
    val clearMapButton by lazy { findChild("ClearMapButton") as Button }

    val saveButton by lazy { findChild("SaveButton") as Button }
    val loadButton by lazy { findChild("LoadButton") as Button }
    val projectButton by lazy { findChild("ProjectButton") as Button }
    val importButton by lazy { findChild("ImportButton") as Button }
    val refreshConfigButton by lazy { findChild("RefreshConfigButton") as Button }
    val calculateClimateButton by lazy { findChild("CalculateClimateButton") as Button }
    val playButton by lazy { findChild("PlayButton") as Button }
    val mapPreviewContainer by lazy { findChild("MapPreviewContainer") as PanelContainer }
    val mapPreview by lazy { findChild("MapPreview") as TextureRect }
    val recenterMapPreviewButton by lazy { findChild("RecenterMapPreviewButton") as Button }

    val simulationOptionButton by lazy { findChild("SimulationOptionButton") as OptionButton }
    val selectedSimulation get() = simulationOptions[simulationOptionButton.selected]

    val saveDialog by lazy { findChild("SaveDialog") as FileDialog }
    val loadDialog by lazy { findChild("LoadDialog") as FileDialog }
    val importDialog by lazy { findChild("ImportDialog") as FileDialog }
    val exportDialog by lazy { findChild("ExportDialog") as FileDialog }

    val seedSelection by lazy { findChild("SeedSelection") as Control }
    val seedInput by lazy { findChild("SeedInput") as LineEdit }
    val randomizeSeedButton by lazy { findChild("RandomizeSeedButton") as Button }
    val generatePlanetButton by lazy { findChild("GeneratePlanetButton") as Button }
    val brushTool by lazy { BrushTool(this) }

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
        infoboxContainer.getChildren().forEach { tab ->
            val label = tab.findChild("Label") as? Label
            if (tab is ScrollContainer && label is Label) {
                label.text = if (selectedTile == null) "" else Main.instance.planet.getTile(selectedTile!!)
                    .getInfoText(tab.name.toString().lowercase())
            }
        }
    }

    private var statsGraphVisibleBeforeTileInspection = false

    var selectedTile: Tile? = null
        set(value) {
            if (field == null && value != null) {
                statsGraphVisibleBeforeTileInspection = statsGraph.visible
                showSettingsButton.setHiddenForTileInspection(true)
                statsGraph.visible = false
            } else if (field != null && value == null) {
                showSettingsButton.setHiddenForTileInspection(false)
                statsGraph.visible = statsGraphVisibleBeforeTileInspection
            }
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

    val isPlayToggled get() = playButton.text == "■"
    fun togglePlayButton(shouldToggleOn: Boolean = !isPlayToggled) {
        playButton.text = if (shouldToggleOn) "■" else "▶"
        Main.instance.timerActive = shouldToggleOn
    }

    fun showSeedSelection() {
        seedInput.text = Random.nextInt().toString()
        seedInput.editable = true
        generatePlanetButton.disabled = false
        seedSelection.visible = true
        seedInput.grabFocus()
        seedInput.selectAll()
    }

    private fun submitSeed() {
        val seed = seedInput.text.trim().hashCode()
        seedInput.editable = false
        generatePlanetButton.disabled = true
        Main.instance.generatePlanet(seed)
        seedSelection.visible = false
    }

    private var mapPreviewDateLine: Double? = null
    private var mapPreviewTexture: ImageTexture? = null

    fun resetMapPreviewCenter() {
        mapPreviewDateLine = null
    }

    private fun BufferedImage.toGodotImage(): Image? {
        val pixels = getRGB(0, 0, width, height, null, 0, width)
        val rgba = ByteArray(pixels.size * 4)
        pixels.forEachIndexed { index, argb ->
            val outputIndex = index * 4
            rgba[outputIndex] = (argb shr 16).toByte()
            rgba[outputIndex + 1] = (argb shr 8).toByte()
            rgba[outputIndex + 2] = argb.toByte()
            rgba[outputIndex + 3] = (argb shr 24).toByte()
        }
        return Image.createFromData(
            MAP_PREVIEW_WIDTH,
            MAP_PREVIEW_HEIGHT,
            false,
            Image.Format.RGBA8,
            PackedByteArray(rgba)
        )
    }

    fun updateMapPreview() {
        if (!mapPreviewContainer.visible || !Main.instance.hasPlanet) return

        val planet = Main.instance.planet
        val dateLine = mapPreviewDateLine ?: planet.internationalDateLine.also { mapPreviewDateLine = it }
        val projectedMap = MapProjections.EQUIRECTANGULAR.projectTiles(
            planet,
            null,
            MAP_PREVIEW_WIDTH,
            MAP_PREVIEW_HEIGHT,
            useKriging = false,
            sampleRadius = planet.topology.averageRadius * 1.5,
            dateLine = dateLine
        ) { tile -> Main.instance.planetRenderer.getColor(tile) }
        val image = projectedMap.toGodotImage() ?: return

        val texture = mapPreviewTexture
        if (texture == null) {
            mapPreviewTexture = ImageTexture.createFromImage(image)?.also { mapPreview.texture = it }
        } else {
            texture.update(image)
        }
    }

    @RegisterFunction
    override fun _ready() {
        instance = this
        randomizeSeedButton.pressed.connect {
            seedInput.text = Random.nextInt().toString()
            seedInput.grabFocus()
            seedInput.selectAll()
        }
        generatePlanetButton.pressed.connect { submitSeed() }
        seedInput.textSubmitted.connect { submitSeed() }
        brushTool.initialize()

        showSettingsButton.addToggle("Show Stats", listOf("debug", "default")) { statsGraph.visible = it }
        showSettingsButton.addToggle("Track Stats", listOf("debug", "default")) { statsGraph.trackStats = it }
        showSettingsButton.addToggle("Show Map Preview", listOf("debug", "default")) {
            mapPreviewContainer.visible = it
            if (it) updateMapPreview()
        }
        recenterMapPreviewButton.pressed.connect {
            if (Main.instance.hasPlanet) {
                mapPreviewDateLine = Main.instance.planet.internationalDateLine
                updateMapPreview()
            }
        }

        clearMapButton.pressed.connect { showSettingsButton.resetToggles() }

        simulationOptions.clear()
        Main.simulations.keys.forEachIndexed { index, simulationName ->
            simulationOptionButton.addItem(
                "Run ${
                    simulationName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            getDefault()
                        ) else it.toString()
                    }
                } Simulation")
            simulationOptions[index] = simulationName
        }
        simulationOptionButton.select(0)

        simulationOptionButton.itemSelected.connect {
            togglePlayButton(false)
        }

        playButton.pressed.connect { togglePlayButton() }

        saveDialog.fileSelected.connect { filename ->
            Serialization.save(filename.removePrefix("res:\\"), Main.instance.planet)
            GD.print("Saved!")
        }
        loadDialog.fileSelected.connect { filename ->
            val loadedPlanet = Serialization.load(filename.removePrefix("res:\\"))
            Main.instance.updatePlanet(loadedPlanet)
            GD.print("Loaded!")
            GD.print("Tectonic age: ${loadedPlanet.tectonicAge}")
        }
        saveButton.pressed.connect { saveDialog.popup() }
        loadButton.pressed.connect { loadDialog.popup() }
        fun reloadConfigFiles() {
            val tectonicsConfig = File("tectonics_config.json")
            if (tectonicsConfig.exists()) {
                Serialization.configMapper.readValue<TectonicGlobals>(tectonicsConfig)
                GD.print("Config refreshed!")
            } else {
                Serialization.configMapper.writeValue(tectonicsConfig, TectonicGlobals)
                GD.print("No tectonics_config.json file found, created one with default values.")
            }

            val climateConfig = File("climate_config.json")
            if (climateConfig.exists()) {
                Serialization.configMapper.readValue<ClimateSimulationGlobals>(climateConfig)
                GD.print("Config refreshed!")
            } else {
                Serialization.configMapper.writeValue(climateConfig, ClimateSimulationGlobals)
                GD.print("No climate_config.json file found, created one with default values.")
            }
        }
        refreshConfigButton.pressed.connect { reloadConfigFiles() }
        calculateClimateButton.pressed.connect {
            Main.instance.planet.climateMap =
                ClimateSimulation.calculateClimate(Main.instance.planet).mapKeys { it.key.tileId }
            ClimateClassifier.printCachedStats(Main.instance.planet)
            Main.instance.planetRenderer.update(Main.instance.planet)
        }

        projectButton.pressed.connect {
            MapProjections.EQUIRECTANGULAR.projectTiles(
                Main.instance.planet,
                "map.png",
                450,
                225,
                useKriging = false,
                Main.instance.planet.topology.averageRadius * 1.5
            ) { tile: PlanetTile -> Main.instance.planetRenderer.getColor(tile) }
            GD.print("Image created")
        }
        importButton.pressed.connect {
            importDialog.popup()
        }
        importDialog.fileSelected.connect { filename ->
            MapProjections.EQUIDISTANT.applyValueTo(
                Main.instance.planet,
                filename,
            ) { value ->
                val threshold = 61.0
                this.elevation = if (value.r8 <= threshold) {
                    value.r8.toDouble().adjustRange(0.0..threshold, -8000.0..-1.0)
                } else {
                    value.r8.toDouble().adjustRange(threshold..255.0, 1.0..6400.0)
                }
            }
            Main.instance.planet.tectonicAge = 0
            Main.instance.planet.terrainChangeCount++
            OceanCurrents.viaEarthlikeHeuristic(Main.instance.planet, 7)
            Main.instance.planetRenderer.update(Main.instance.planet)
            GD.print("Elevation map imported. Tune climate controls, then choose Simulate & Score.")
        }
    }

    class MapLayerCheckButton(val button: ToggleButton, val categories: List<String>)

    companion object {
        const val MAP_PREVIEW_WIDTH = 256
        const val MAP_PREVIEW_HEIGHT = 128
        lateinit var instance: Gui private set
        val simulationOptions = mutableMapOf<Int, String>()
    }
}
