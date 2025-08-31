package dev.biserman.planet

import dev.biserman.planet.geometry.*
import dev.biserman.planet.planet.NoiseMaps
import dev.biserman.planet.planet.Planet
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
			GD.print(planet.subductionTiles.size)
		}
	}

	companion object {
		var random = Random(11)
		val noise = NoiseMaps(random.nextInt(), random)
		lateinit var instance: Main
	}
}
