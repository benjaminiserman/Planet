package dev.biserman.planet

import dev.biserman.planet.gui.Gui
import dev.biserman.planet.gui.Gui.Mode
import dev.biserman.planet.planet.climate.ClimateSimulation
import dev.biserman.planet.planet.ecology.EcologyRuntime
import dev.biserman.planet.planet.BiotaDistribution
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.tectonics.Tectonics
import dev.biserman.planet.rendering.PlanetRenderer
import dev.biserman.planet.things.Concept
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Input
import godot.api.InputEvent
import godot.api.Node
import godot.global.GD
import kotlin.random.Random

@RegisterClass
class Main : Node() {
	lateinit var planet: Planet private set
	lateinit var planetRenderer: PlanetRenderer

	var copyElevation: Double? = 0.0

	@RegisterFunction
	override fun _ready() {
		instance = this
		Gui.instance.showSeedSelection()
	}

	fun generatePlanet(seed: Int) {
		val newPlanet = Planet(seed = seed, size = 35)
		GD.print("tiles: ${newPlanet.topology.tiles.size}")
		GD.print("average radius: ${newPlanet.topology.averageRadius}, area: ${newPlanet.topology.averageArea}")

		Tectonics.stepTectonicPlateForces(newPlanet)
		planetRenderer = PlanetRenderer(this, newPlanet)

		updatePlanet(newPlanet)
	}

	@RegisterFunction
	override fun _unhandledInput(event: InputEvent?) {
		if (event == null || !::planet.isInitialized || !::planetRenderer.isInitialized) {
			return
		}

		if (Input.isActionJustPressed("next")) {
			if (Gui.instance.mode == Mode.PLAY) {
				advanceHistoryTurn()
			} else {
				runSelectedEditSimulation()
			}
		}

		if (Gui.instance.mode != Mode.EDIT) return

		val selectedTile = planet.planetTiles[Gui.instance.selectedTile?.id] ?: return
		if (Input.isActionJustPressed("place_land")) {
			selectedTile.elevation = 1.0
			planet.terrainChangeCount++
			planetRenderer.update(planet)
		}

		if (Input.isActionJustPressed("place_ocean")) {
			selectedTile.elevation = -1.0
			planet.terrainChangeCount++
			planetRenderer.update(planet)
		}

		if (Input.isActionJustPressed("paste")) {
			if (copyElevation != null) {
				selectedTile.elevation = copyElevation!!
				planet.terrainChangeCount++
				planetRenderer.update(planet)
			}
		}

		if (Input.isActionJustPressed("copy")) {
			copyElevation = selectedTile.elevation
		}
	}

	private val editSimulationTimerStep = 0.1
	var timerActive = false
		set(value) {
			field = value
			timerTime = editSimulationTimerStep
		}
	private var timerTime = editSimulationTimerStep

	private val historyTurnTimerStep = 0.5
	var historyTimerActive = false
		set(value) {
			field = value
			historyTimerTime = historyTurnTimerStep
		}
	private var historyTimerTime = historyTurnTimerStep

	@RegisterFunction
	override fun _process(delta: Double) {
		if (timerActive && Gui.instance.mode == Mode.EDIT && ::planet.isInitialized && ::planetRenderer.isInitialized) {
			timerTime += delta
			if (timerTime >= editSimulationTimerStep) {
				timerTime = 0.0
				runSelectedEditSimulation()
			}
		}

		if (historyTimerActive && Gui.instance.mode == Mode.PLAY && ::planet.isInitialized) {
			historyTimerTime += delta
			if (historyTimerTime >= historyTurnTimerStep) {
				historyTimerTime = 0.0
				advanceHistoryTurn()
			}
		}
	}

	private fun runSelectedEditSimulation() {
		simulations[Gui.instance.selectedSimulation]!!.invoke(planet)
		planetRenderer.update(planet)
		Gui.instance.brushTool.refreshOptions()
	}

	fun advanceHistoryTurn() {
		EcologyRuntime.advanceAllOneSeason(planet)
		planet.historyTurn++
		Gui.instance.statsGraph.updateHistory(planet)
		planetRenderer.update(planet)
		Gui.instance.updateHistoryDisplay()
	}

	val hasPlanet get() = ::planet.isInitialized

	fun updatePlanet(newPlanet: Planet) {
		GD.print("updating planet: $newPlanet")
		BiotaDistribution.ensureUniqueOrders(newPlanet)
		planet = newPlanet
		Gui.instance.resetMapPreviewCenter()
		planetRenderer.update(newPlanet)
		Gui.instance.statsGraph.planet = newPlanet
		Gui.instance.brushTool.refreshOptions()
		Gui.instance.updateHistoryDisplay()
	}

	companion object {
		lateinit var instance: Main
		val debugRandom = Random(0)

		val simulations = mapOf(
			"tectonics" to { planet: Planet -> Tectonics.stepTectonicsSimulation(planet) },
			"climate" to { planet: Planet -> ClimateSimulation.stepClimateSimulation(planet)},
			"erosion" to { planet: Planet -> Tectonics.performErosion(planet) }
		)
	}
}
