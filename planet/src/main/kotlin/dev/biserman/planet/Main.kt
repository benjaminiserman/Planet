package dev.biserman.planet

import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.climate.ClimateSimulation
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
			Tectonics.stepTectonicsSimulation(planet)
			planet.terrainChangeCount++
			planetRenderer.update(planet)
		}

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

	val timerStep = 0.1
	var timerActive = false
		set(value) {
			field = value
			timerTime = timerStep
		}
	var timerTime = timerStep

	@RegisterFunction
	override fun _process(delta: Double) {
		if (timerActive && ::planet.isInitialized && ::planetRenderer.isInitialized) {
			timerTime += delta
			if (timerTime >= timerStep) {
				timerTime = 0.0
				simulations[Gui.instance.selectedSimulation]!!.invoke(planet)
				planetRenderer.update(planet)
			}
		}
	}

	val hasPlanet get() = ::planet.isInitialized

	fun updatePlanet(newPlanet: Planet) {
		GD.print("updating planet: $newPlanet")
		planet = newPlanet
		planetRenderer.update(newPlanet)
		Gui.instance.statsGraph.planet = newPlanet
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
