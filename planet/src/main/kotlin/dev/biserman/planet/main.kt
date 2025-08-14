package dev.biserman.planet

import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.distortTriangles
import dev.biserman.planet.geometry.makeIcosahedron
import dev.biserman.planet.geometry.relaxRepeatedly
import dev.biserman.planet.geometry.reorderVerts
import dev.biserman.planet.geometry.subdivideIcosahedron
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.topology.toTopology
import dev.biserman.planet.utils.VectorWarpNoise
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.FastNoiseLite
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.core.Vector3
import godot.global.GD
import kotlin.random.Random


@RegisterClass
class Main : Node() {

	@RegisterFunction
	override fun _ready() {
		GD.print("Hello World!")
		val planetMeshInstance = MeshInstance3D().also { it.setName("Planet") }
		val planet2 = MeshInstance3D().also { it.setName("Planet2") }
		addChild(planetMeshInstance, forceReadableName = true)
		addChild(planet2, forceReadableName = true)
		val icos = makeIcosahedron()
		val sub = icos.subdivideIcosahedron(30)
		sub.distortTriangles(0.5)
		sub.relaxRepeatedly(500)
		sub.reorderVerts()
		val topology = sub.toTopology()
		GD.print("tiles: ${topology.tiles.size}")
		val planet = Planet(topology)

		fun levelIt(level: Double) = when{
			level < 0.8 -> level * 0.5
			level >= 1.0 -> 1.0
			else -> level - 0.25
		}

		fun saturation(level: Double) = when {
			level >= 1.0 -> 0.0
			else -> 0.9
		}

		planetMeshInstance.setMesh(topology.makeMesh(enrich = { mesh, tile ->
			val planetTile = planet.planetTiles[tile] ?: return@makeMesh
			val level = planetTile.elevation.adjustRange(-0.5..0.5, 0.0..1.0)
			val hue = planetTile.tectonicPlate?.debugColor?.h ?: 0.0
//			val hue = noise.getNoise3dv(planetTile.tile.position * 100).toDouble()
			var color = Color.fromHsv(hue, saturation(level), levelIt(level), level)
			if (level < 0.8) {
				color = Color.fromHsv(0.7, saturation(level), levelIt(level), level)
			}
			mesh.colors.add(color)

			mesh.colors.addAll((0..<tile.corners.size).map {
				val level =
					(tile.corners[it].tiles.sumOf { tile -> planet.planetTiles[tile]!!.elevation } / tile.corners[it].tiles.size)
						.adjustRange(-0.5..0.5, 0.0..1.0)
				val color = Color.fromHsv(
					color.h,
					color.s,
					levelIt(level),
					level
				)

				color
			})
		}).apply {
			this.recalculateNormals()
		}.toArrayMesh())
		planetMeshInstance.setSurfaceOverrideMaterial(0, GD.load<StandardMaterial3D>("res://planet_mat.tres"))
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
		val noise = FastNoiseLite().apply { this.setSeed(2) }
		var random = Random(2)
	}
}
