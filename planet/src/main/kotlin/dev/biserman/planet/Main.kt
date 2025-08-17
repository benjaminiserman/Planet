package dev.biserman.planet

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.geometry.Path.Companion.toPath
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.distortTriangles
import dev.biserman.planet.geometry.makeIcosahedron
import dev.biserman.planet.geometry.relaxRepeatedly
import dev.biserman.planet.geometry.reorderVerts
import dev.biserman.planet.geometry.subdivideIcosahedron
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.planet.NoiseMaps
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.DebugDraw.drawMesh
import dev.biserman.planet.rendering.DebugDraw.drawRotVectorMesh
import dev.biserman.planet.topology.toTopology
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.global.GD
import kotlin.math.abs
import kotlin.random.Random


@RegisterClass
class Main : Node() {

	@RegisterFunction
	override fun _ready() {
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

		this.drawMesh("Planet", topology.makeMesh(enrich = { mesh, tile ->
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
		}.toArrayMesh(), GD.load<StandardMaterial3D>("res://planet_mat.tres"))

//        this.drawMesh("tile_wireframe", topology.makeMesh().apply { this.verts.forEach { it.position *= 1.005 } }.toWireframe(), StandardMaterial3D().apply {
//			this.usePointSize = true
//			this.setPointSize(10f)
//        })

		planet.tectonicPlates.withIndex().forEach { tuple ->
			val (index, plate) = tuple
			val hashOffset = abs(plate.debugColor.hashCode() / Int.MAX_VALUE.toDouble())
			val borderMesh = plate.region.border.toPath().toMesh().apply {
				this.verts.forEach { it.position *= (1.001 + 0.005 * hashOffset) }
			}

			this.drawMesh("border_$index", borderMesh.toWireframe(), StandardMaterial3D().apply {
				this.setAlbedo(plate.debugColor)
			})
		}

		planet.tectonicPlates.withIndex().forEach { tuple ->
			val lift = 1.005
			val (_, plate) = tuple

			val edgeVectors = plate.tiles.filter { it.plateBoundaryForces.length() > 0.005 }.flatMap {
				listOf(
					Pair(
						DebugVector(
							it.tile.position * lift,
							it.plateBoundaryForces
						), it.rotationalForce * 0.3
					)
				)
			}

			this.drawRotVectorMesh("tile_movement", edgeVectors, plate.debugColor)

			val overallMovementVectors = listOf(plate.averageForce).map {
				Pair(
					DebugVector(
						plate.region.center * lift,
						plate.averageForce.tangent(plate.region.center * lift) * 100,
					), plate.averageRotation * 10
				)
			}

			this.drawRotVectorMesh("plate_movement", overallMovementVectors, plate.debugColor)
		}
	}

	companion object {
		var random = Random(4)
		val noise = NoiseMaps(random.nextInt(), random)
	}
}
