package dev.biserman.planet

import dev.biserman.planet.geometry.*
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
	lateinit var planet: Planet
	lateinit var planetRenderer: PlanetRenderer

	@RegisterFunction
	override fun _ready() {
		instance = this

		val icos = makeIcosahedron()
		val sub = icos.subdivideIcosahedron(30)
		sub.distortTriangles(0.5)
		sub.relaxRepeatedly(500)
		sub.reorderVerts()
		val topology = sub.toTopology()
		GD.print("average radius: ${topology.averageRadius}")
		GD.print("tiles: ${topology.tiles.size}")
		planet = Planet(topology)

		Tectonics.stepTectonicPlateForces(planet)

		planetRenderer = PlanetRenderer(this, planet)
		planetRenderer.update(planet)
	}

	@RegisterFunction
	override fun _unhandledInput(event: InputEvent?) {
		if (event == null) {
			return
		}

		if (Input.isActionJustPressed("next")) {
			Tectonics.stepTectonicsSimulation(planet)
			planetRenderer.update(planet)
			GD.print("continental crust: ${(planet.planetTiles.values.filter { it.elevation >= planet.seaLevel }.size / planet.planetTiles.size.toFloat() * 100).toInt()}%")
		}

		if (Input.isActionJustPressed("erode")) {
			Tectonics.performErosion(planet)
			planetRenderer.update(planet)
		}

		if (Input.isActionJustPressed("play")) {
			timerActive = !timerActive
			timerTime = 0.0
		}
	}

	var timerActive = false
	var timerTime = 0.0

	@RegisterFunction
	override fun _process(delta: Double) {
		if (timerActive) {
			timerTime += delta
			if (timerTime >= 1.0) {
				timerTime = 0.0
				Tectonics.stepTectonicsSimulation(planet)
				planetRenderer.update(planet)
				GD.print("continental crust: ${(planet.planetTiles.values.filter { it.elevation >= planet.seaLevel }.size / planet.planetTiles.size.toFloat() * 100).toInt()}%")
			}
		}
	}

	companion object {
		var random = Random(11)
		val noise = NoiseMaps(random.nextInt(), random)
		lateinit var instance: Main
	}
}
