package dev.biserman.planet

import dev.biserman.planet.geometry.*
import dev.biserman.planet.planet.NoiseMaps
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.DebugDraw.drawMesh
import dev.biserman.planet.rendering.PlanetRenderer
import dev.biserman.planet.rendering.renderers.TectonicForcesRenderer
import dev.biserman.planet.rendering.renderers.CellWireframeRenderer
import dev.biserman.planet.rendering.renderers.TectonicPlateBoundaryRenderer
import dev.biserman.planet.topology.toTopology
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.global.GD
import kotlin.random.Random

@RegisterClass
class Main : Node() {
    val planetRenderer = PlanetRenderer(this)

	val planetDebugRenders = listOf(
		TectonicForcesRenderer(this, lift = 1.005, visibleByDefault = true),
		CellWireframeRenderer(this, lift = 1.005, visibleByDefault = false),
		TectonicPlateBoundaryRenderer(this, lift = 1.005, visibleByDefault = true),
	)

	@RegisterFunction
	override fun _ready() {
		instance = this

		val icos = makeIcosahedron()
		val sub = icos.subdivideIcosahedron(30)
		sub.distortTriangles(0.5)
		sub.relaxRepeatedly(500)
		sub.reorderVerts()
		val topology = sub.toTopology()
		GD.print("tiles: ${topology.tiles.size}")
		val planet = Planet(topology)

		planetRenderer.update(planet)

		planetDebugRenders.forEach {
			it.init()
			it.update(planet)
		}
	}

	companion object {
		var random = Random(4)
		val noise = NoiseMaps(random.nextInt(), random)
		lateinit var instance: Main
	}
}
