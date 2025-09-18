package dev.biserman.planet

import dev.biserman.planet.geometry.*
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.NoiseMaps
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile.Companion.floodFillGroupBy
import dev.biserman.planet.planet.Tectonics
import dev.biserman.planet.rendering.PlanetRenderer
import dev.biserman.planet.topology.toTopology
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Input
import godot.api.InputEvent
import godot.api.Node
import godot.core.Key
import godot.global.GD
import kotlin.random.Random

@RegisterClass
class Main : Node() {
	lateinit var planet: Planet private set
	lateinit var planetRenderer: PlanetRenderer

	@RegisterFunction
	override fun _ready() {
		instance = this

		val icos = makeIcosahedron()
		val sub = icos.subdivideIcosahedron(35)
		sub.distortTriangles(0.5)
		sub.relaxRepeatedly(500)
		sub.reorderVerts()
		val topology = sub.toTopology()
		GD.print("average radius: ${topology.averageRadius}")
		GD.print("tiles: ${topology.tiles.size}")
		val newPlanet = Planet(topology)

		Tectonics.stepTectonicPlateForces(newPlanet)
		planetRenderer = PlanetRenderer(this, newPlanet)

		Gui.addToggle("Run Tectonics Simulation", defaultValue = false) {
			timerActive = it
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
	var timerActive = false
		set(value) {
			field = value
			timerTime = timerStep
		}
	var timerTime = timerStep

	@RegisterFunction
	override fun _process(delta: Double) {
		if (timerActive) {
			timerTime += delta
			if (timerTime >= timerStep) {
				timerTime = 0.0
				Tectonics.stepTectonicsSimulation(planet)
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
		var random = Random(12)
		val noise = NoiseMaps(random.nextInt(), random)
		lateinit var instance: Main
	}
}
