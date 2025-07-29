package dev.biserman.planet

import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.distortTriangles
import dev.biserman.planet.geometry.makeIcosahedron
import dev.biserman.planet.geometry.relaxRepeatedly
import dev.biserman.planet.geometry.reorderVerts
import dev.biserman.planet.geometry.subdivideIcosahedron
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.topology.toTopology
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.FastNoiseLite
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.core.Vector3
import godot.global.GD
import kotlin.math.floor
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
		val sub = icos.subdivideIcosahedron(30)
		sub.distortTriangles(0.5)
		sub.relaxRepeatedly(500)
		sub.reorderVerts()
		val topology = sub.toTopology()
		GD.print("tiles: ${topology.tiles.size}")
		planet.setMesh(topology.makeMesh(enrich = { mesh, tile ->
			val level = PlanetTile(tile).elevation.toDouble().adjustRange(-1.0..1.0, 0.0..1.0)
			val hue = random.nextDouble()
			val color = Color.fromHsv(hue, 0.7, level, level)
			mesh.colors.add(color)

			mesh.colors.addAll((0..<tile.corners.size).map {
				val level =
					(tile.corners[it].tiles.sumOf { PlanetTile(it).elevation.toDouble() } / tile.corners[it].tiles.size)
						.adjustRange(-1.0..1.0, 0.0..1.0)
				val color = Color.fromHsv(hue, 0.7, level, level)
				color
			})
		}).apply {
			this.recalculateNormals()
		}.toArrayMesh())
		planet.setSurfaceOverrideMaterial(0, GD.load<StandardMaterial3D>("res://planet_mat.tres"))
//        planet.setMesh(sub.toWireframe())

		sub.duplicateSharedVerts()
		sub.recalculateNormals()
		planet2.setMesh(topology.makeMesh().toWireframe())
		planet2.setPosition(Vector3(-3.0, 0.0, 0.0))
//        planet2.setSurfaceOverrideMaterial(0, StandardMaterial3D().apply {
//            this.setAlbedo(Color.red)
//            this.usePointSize = true
//            this.setPointSize(10f)
//        })
	}


	companion object {
		val random = Random(0)
		val noise = FastNoiseLite().apply { this.setSeed(0) }
	}
}
