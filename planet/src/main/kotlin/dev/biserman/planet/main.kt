package dev.biserman.planet

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.geometry.Path.Companion.toPath
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.distortTriangles
import dev.biserman.planet.geometry.makeIcosahedron
import dev.biserman.planet.geometry.relaxRepeatedly
import dev.biserman.planet.geometry.reorderVerts
import dev.biserman.planet.geometry.subdivideIcosahedron
import dev.biserman.planet.geometry.toMesh
import dev.biserman.planet.planet.NoiseMaps
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.topology.toTopology
import dev.biserman.planet.utils.VectorWarpNoise
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.core.Vector3
import godot.global.GD
import kotlin.math.abs
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

		fun levelIt(level: Float) = when {
			level < 0.8f -> level * 0.5f
			level >= 1.0f -> 1.0f
			else -> level - 0.25f
		}

		fun saturation(level: Float) = when {
			level >= 1.0f -> 0.0f
			else -> 0.9f
		}

		planetMeshInstance.setMesh(topology.makeMesh(enrich = { mesh, tile ->
			val planetTile = planet.planetTiles[tile] ?: return@makeMesh
			val level = planetTile.elevation.adjustRange(-0.5f..0.5f, 0.0f..1.0f)
			val hue = planetTile.tectonicPlate?.biomeColor?.h ?: 0.0
//			val hue = noise.getNoise3dv(planetTile.tile.position * 100).toDouble()
			var color = Color.fromHsv(hue, saturation(level).toDouble(), levelIt(level).toDouble(), level.toDouble())
			if (level < 0.7) {
				color = Color.fromHsv(0.7, saturation(level).toDouble(), levelIt(level).toDouble(), level.toDouble())
			}
			mesh.colors.add(color)

			mesh.colors.addAll((0..<tile.corners.size).map {
				val level =
					(tile.corners[it].tiles.map { tile -> planet.planetTiles[tile]!!.elevation }.toFloatArray()
						.sum() / tile.corners[it].tiles.size)
						.adjustRange(-0.5f..0.5f, 0.0f..1.0f)
				val color = Color.fromHsv(
					color.h,
					color.s,
					levelIt(level).toDouble(),
					level.toDouble()
				)

				color
			})
		}).apply {
			this.recalculateNormals()
		}.toArrayMesh())
		planetMeshInstance.setSurfaceOverrideMaterial(0, GD.load<StandardMaterial3D>("res://planet_mat.tres"))

		sub.duplicateSharedVerts()
		sub.recalculateNormals()
//		planet2.setMesh(topology.makeMesh().apply { this.verts.forEach { it.position *= 1.005 } }.toWireframe())
//		planet2.setPosition(Vector3(-3.0, 0.0, 0.0))
//		planet2.setSurfaceOverrideMaterial(0, StandardMaterial3D().apply {
//            this.setAlbedo(Color.red)
//			this.usePointSize = true
//			this.setPointSize(10f)
//		})

		val warpNoise = VectorWarpNoise(random.nextInt(), 0.1f)

		planet.tectonicPlates.withIndex().forEach { tuple ->
			val (index, plate) = tuple
			val borderMeshInstance = MeshInstance3D().also { it.setName("border_${index}") }
			addChild(borderMeshInstance, forceReadableName = true)

			val hashOffset = abs(plate.debugColor.hashCode() / Int.MAX_VALUE.toDouble())
			val borderMesh = plate.region.border.toPath().toMesh().apply {
				this.verts.forEach { it.position *= (1.001 + 0.005 * hashOffset) }
			}
			borderMeshInstance.setMesh(borderMesh.toWireframe())

			borderMeshInstance.setSurfaceOverrideMaterial(0, StandardMaterial3D().apply {
				this.setAlbedo(plate.debugColor)
			})
		}

		planet.tectonicPlates.withIndex().forEach { tuple ->
			val lift = 1.005
			val (index, plate) = tuple
			val vectorMeshInstance = MeshInstance3D().also { it.setName("vector_${index}") }
			val vectorOriginMeshInstance = MeshInstance3D().also { it.setName("vector_origin_${index}") }
			addChild(vectorMeshInstance, forceReadableName = true)
			addChild(vectorOriginMeshInstance, forceReadableName = true)

			val plateVectors = plate.tiles.map {
				DebugVector(
					it.tile.position * lift,
//					it.tile.position.cross(warpNoise.warp(it.tile.position, 0.01) - it.tile.position)
//						.normalized() * 0.01
					it.getPlateBoundaryForces() * lift
				)
			}
			vectorMeshInstance.setMesh(plateVectors.toMesh().toWireframe())
			vectorMeshInstance.setSurfaceOverrideMaterial(0, StandardMaterial3D().apply {
				this.setAlbedo(plate.debugColor)
			})

			val origins = plate.tiles.map { it.tile.position * lift }
			vectorOriginMeshInstance.setMesh(origins.toMesh())
			vectorOriginMeshInstance.setSurfaceOverrideMaterial(0, StandardMaterial3D().apply {
				this.setAlbedo(plate.debugColor)
				this.usePointSize = true
				this.setPointSize(3.5f)
			})
		}
	}

	companion object {
		var random = Random(4)
		val noise = NoiseMaps(random.nextInt(), random)
	}
}
