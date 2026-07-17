package dev.biserman.planet.gui

import dev.biserman.planet.Main
import dev.biserman.planet.planet.BiotaDistribution
import dev.biserman.planet.planet.BiotaDistributionAquatic
import dev.biserman.planet.planet.BiotaDistributionTerrestrial
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.tectonics.TectonicPlate
import dev.biserman.planet.topology.Tile
import godot.api.Button
import godot.api.Control
import godot.api.OptionButton
import godot.api.SpinBox
import godot.core.connect

class BrushTool(private val gui: Gui) {
    private enum class Mode { SELECT, PAINT, PICK }
    private enum class Target { ELEVATION, TECTONIC_PLATE, BIOTA_DISTRIBUTION }

    private val paintButton by lazy { gui.findChild("PaintBrushButton") as Button }
    private val pickButton by lazy { gui.findChild("PickBrushButton") as Button }
    private val showToolbarButton by lazy { gui.findChild("ShowBrushToolbarButton") as Button }
    private val toolbar by lazy { gui.findChild("BrushToolbar") as Control }
    private val targetOptionButton by lazy { gui.findChild("BrushTargetOptionButton") as OptionButton }
    private val sizeValue by lazy { gui.findChild("BrushSizeValue") as SpinBox }
    private val elevationValue by lazy { gui.findChild("ElevationBrushValue") as SpinBox }
    private val elevationSmoothnessValue by lazy { gui.findChild("ElevationSmoothnessValue") as SpinBox }
    private val plateValue by lazy { gui.findChild("PlateBrushValue") as OptionButton }
    private val newPlateButton by lazy { gui.findChild("NewPlateButton") as Button }
    private val biotaValue by lazy { gui.findChild("BiotaBrushValue") as OptionButton }

    private var mode = Mode.SELECT
    private var target = Target.ELEVATION
    private var plates: List<TectonicPlate?> = listOf(null)
    private var biotaDistributions: List<BiotaDistribution?> = listOf(null)
    private var lastPaintedTileId: Int? = null
    private var suppressPaintingUntilRelease = false

    val isActive get() = mode != Mode.SELECT
    val isPainting get() = mode == Mode.PAINT && !suppressPaintingUntilRelease

    fun initialize() {
        showToolbarButton.pressed.connect {
            toolbar.visible = showToolbarButton.buttonPressed
            if (!toolbar.visible) setMode(Mode.SELECT)
        }

        Target.entries.forEach {
            targetOptionButton.addItem(
                it.name.lowercase().replace('_', ' ').replaceFirstChar { char -> char.titlecase() }
            )
        }
        targetOptionButton.select(0)
        targetOptionButton.itemSelected.connect { index ->
            target = Target.entries[index.toInt()]
            updateValueVisibility()
        }
        paintButton.pressed.connect {
            setMode(if (paintButton.buttonPressed) Mode.PAINT else Mode.SELECT)
        }
        pickButton.pressed.connect {
            setMode(if (pickButton.buttonPressed) Mode.PICK else Mode.SELECT)
        }
        newPlateButton.pressed.connect {
            val plate = TectonicPlate(Main.instance.planet)
            Main.instance.planet.tectonicPlates.add(plate)
            refreshOptions()
            plateValue.select(plates.indexOf(plate))
            setMode(Mode.PAINT)
        }
        updateValueVisibility()
    }

    fun setEditModeEnabled(enabled: Boolean) {
        showToolbarButton.visible = enabled
        if (!enabled) {
            showToolbarButton.buttonPressed = false
            toolbar.visible = false
            setMode(Mode.SELECT)
        }
    }

    private fun setMode(newMode: Mode) {
        mode = newMode
        paintButton.buttonPressed = newMode == Mode.PAINT
        pickButton.buttonPressed = newMode == Mode.PICK
        lastPaintedTileId = null
        suppressPaintingUntilRelease = false
        if (newMode != Mode.SELECT) gui.togglePlayButton(false)
    }

    private fun updateValueVisibility() {
        elevationValue.visible = target == Target.ELEVATION
        elevationSmoothnessValue.visible = target == Target.ELEVATION
        plateValue.visible = target == Target.TECTONIC_PLATE
        newPlateButton.visible = target == Target.TECTONIC_PLATE
        biotaValue.visible = target == Target.BIOTA_DISTRIBUTION
    }

    fun refreshOptions() {
        if (!Main.instance.hasPlanet) return
        val planet = Main.instance.planet

        val selectedPlate = plates.getOrNull(plateValue.selected)
        plates = listOf(null) + planet.tectonicPlates
        plateValue.clear()
        plateValue.addItem("None")
        planet.tectonicPlates.forEach { plateValue.addItem(it.name) }
        plateValue.select(plates.indexOf(selectedPlate).coerceAtLeast(0))

        val selectedDistribution = biotaDistributions.getOrNull(biotaValue.selected)
        biotaDistributions = listOf(null) + planet.biotaDistributions
        biotaValue.clear()
        biotaValue.addItem("None")
        planet.biotaDistributions.forEachIndexed { index, distribution ->
            val type = when (distribution.method) {
                BiotaDistributionTerrestrial -> "Terrestrial"
                BiotaDistributionAquatic -> "Aquatic"
                else -> "Biota"
            }
            biotaValue.addItem("${index + 1}: $type")
        }
        biotaValue.select(biotaDistributions.indexOf(selectedDistribution).coerceAtLeast(0))
    }

    fun apply(tile: Tile) {
        if (lastPaintedTileId == tile.id) return
        lastPaintedTileId = tile.id

        if (mode == Mode.PICK) {
            pickValue(tile)
            setMode(Mode.PAINT)
            suppressPaintingUntilRelease = true
            return
        }
        if (mode != Mode.PAINT) return

        val planet = Main.instance.planet
        val brushTiles = brushTiles(tile)
        val maxDistance = sizeValue.value.toInt() - 1
        var changed = false
        var selectedTileChanged = false
        for ((brushTile, distance) in brushTiles) {
            val planetTile = planet.getTile(brushTile)
            if (paintValue(planetTile, distance, maxDistance)) {
                changed = true
                if (gui.selectedTile == brushTile) selectedTileChanged = true
            }
        }

        if (changed) {
            planet.terrainChangeCount++
            Main.instance.planetRenderer.update(planet)
            if (selectedTileChanged) gui.updateInfobox()
        }
    }

    private fun brushTiles(center: Tile): List<Pair<Tile, Int>> {
        val maxDistance = sizeValue.value.toInt() - 1
        val result = ArrayList<Pair<Tile, Int>>()
        val visited = BooleanArray(Main.instance.planet.topology.tiles.size)
        val queue = ArrayDeque<Pair<Tile, Int>>()
        queue.addLast(center to 0)
        visited[center.id] = true

        while (queue.isNotEmpty()) {
            val (tile, distance) = queue.removeFirst()
            result.add(tile to distance)
            if (distance >= maxDistance) continue
            for (neighbor in tile.tiles) {
                if (!visited[neighbor.id]) {
                    visited[neighbor.id] = true
                    queue.addLast(neighbor to distance + 1)
                }
            }
        }
        return result
    }

    private fun paintValue(planetTile: PlanetTile, distance: Int, maxDistance: Int): Boolean =
        when (target) {
            Target.ELEVATION -> {
                val elevation = elevationValue.value
                val smoothness = elevationSmoothnessValue.value / 100.0
                val strength = if (maxDistance == 0) 1.0
                else 1.0 - smoothness * distance / (maxDistance + 1.0)
                val adjustedElevation = planetTile.elevation + (elevation - planetTile.elevation) * strength
                if (planetTile.elevation == adjustedElevation) false else {
                    planetTile.elevation = adjustedElevation
                    true
                }
            }
            Target.TECTONIC_PLATE -> {
                val plate = plates.getOrNull(plateValue.selected)
                if (planetTile.tectonicPlate == plate) false else {
                    planetTile.tectonicPlate = plate
                    true
                }
            }
            Target.BIOTA_DISTRIBUTION -> {
                val distribution = biotaDistributions.getOrNull(biotaValue.selected)
                val existing = planetTile.planet.biotaDistributions.filter { planetTile in it.region }
                if (existing.size == (if (distribution == null) 0 else 1) &&
                    existing.firstOrNull() == distribution
                ) {
                    false
                } else {
                    existing.forEach { it.region.tiles.remove(planetTile) }
                    distribution?.region?.tiles?.add(planetTile)
                    true
                }
            }
        }

    private fun pickValue(tile: Tile) {
        val planetTile = Main.instance.planet.getTile(tile)
        refreshOptions()
        when (target) {
            Target.ELEVATION -> elevationValue.value = planetTile.elevation
            Target.TECTONIC_PLATE ->
                plateValue.select(plates.indexOf(planetTile.tectonicPlate).coerceAtLeast(0))
            Target.BIOTA_DISTRIBUTION -> {
                val distribution = Main.instance.planet.biotaDistributions.firstOrNull { planetTile in it.region }
                biotaValue.select(biotaDistributions.indexOf(distribution).coerceAtLeast(0))
            }
        }
    }

    fun endStroke() {
        lastPaintedTileId = null
        suppressPaintingUntilRelease = false
    }
}
