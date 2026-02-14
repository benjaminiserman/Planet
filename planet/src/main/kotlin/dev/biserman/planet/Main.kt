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

	@RegisterFunction
	override fun _ready() {
		instance = this

		val newPlanet = Planet(seed = 3, size = 35)
		GD.print("tiles: ${newPlanet.topology.tiles.size}")
		GD.print("average radius: ${newPlanet.topology.averageRadius}, area: ${newPlanet.topology.averageArea}")

		Tectonics.stepTectonicPlateForces(newPlanet)
		planetRenderer = PlanetRenderer(this, newPlanet)

		Gui.addToggle("Run Tectonics Simulation", defaultValue = false) {
			timerActive = if (it) "tectonics" else "none"
		}
		Gui.addToggle("Run Climate Simulation", defaultValue = false) {
			timerActive = if (it) "climate" else "none"
		}

		updatePlanet(newPlanet)
	}

	@RegisterFunction
	override fun _unhandledInput(event: InputEvent?) {
		if (event == null) {
			return
		}

		if (Input.isActionJustPressed("next")) {
			Tectonics.stepTectonicsSimulation(planet)
			planetRenderer.update(planet)
		}

		if (Input.isActionJustPressed("erode")) {
			Tectonics.performErosion(planet)
			planetRenderer.update(planet)
		}

//        if (Input.isActionJustPressed("play")) {
//            timerActive = !timerActive
//            timerTime = 0.33
//        }
	}

	val timerStep = 0.1
	var timerActive = "none"
		set(value) {
			field = value
			timerTime = timerStep
		}
	var timerTime = timerStep

	@RegisterFunction
	override fun _process(delta: Double) {
		if (timerActive != "none") {
			timerTime += delta
			if (timerTime >= timerStep) {
				timerTime = 0.0
				when (timerActive) {
					"tectonics" -> Tectonics.stepTectonicsSimulation(planet)
					"climate" -> ClimateSimulation.stepClimateSimulation(planet)
				}
				planetRenderer.update(planet)
			}
		}
	}

	fun updatePlanet(newPlanet: Planet) {
		GD.print("updating planet: $newPlanet")
		planet = newPlanet
		planetRenderer.update(newPlanet)
		Gui.instance.statsGraph.planet = newPlanet
	}

	companion object {
		lateinit var instance: Main
		val debugRandom = Random(0)
	}
}
