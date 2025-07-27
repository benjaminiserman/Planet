package dev.biserman.planet

import dev.biserman.planet.geometry.distortTriangles
import dev.biserman.planet.geometry.makeIcosahedron
import dev.biserman.planet.geometry.reorderVerts
import dev.biserman.planet.geometry.subdivideIcosahedron
import dev.biserman.planet.geometry.toMesh
import dev.biserman.planet.topology.toTopology
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.BaseMaterial3D
import godot.api.Material
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.global.GD
import kotlin.random.Random


@RegisterClass
class Main : Node() {

	@RegisterFunction
	override fun _ready() {
		GD.print("Hello World!")
		val planet = MeshInstance3D().also { it.setName("Planet") }
		val planet2 = MeshInstance3D().also { it.setName("Planet2") }
		addChild(planet, forceReadableName = true)
		addChild(planet2, forceReadableName = true)
		val icos = makeIcosahedron()
		val sub = icos.subdivideIcosahedron(4)
//        sub.distortTriangles(0.99)
		sub.reorderVerts()
//        sub.duplicateSharedVerts()
//        sub.recalculateNormals()
		val topologyMesh = sub.toTopology().mesh
		topologyMesh.recalculateNormals()
		planet.setMesh(topologyMesh.toArrayMesh())
//        planet.setMesh(sub.toWireframe())
//        planet2.setMesh(sub.tris.map { it.centroid(sub) }.toMesh())
//        planet2.setSurfaceOverrideMaterial(0, StandardMaterial3D().apply {
//            this.setAlbedo(Color.red)
//            this.usePointSize = true
//            this.setPointSize(10f)
//        })
	}


	companion object {

		val random = Random(0)
	}
}
