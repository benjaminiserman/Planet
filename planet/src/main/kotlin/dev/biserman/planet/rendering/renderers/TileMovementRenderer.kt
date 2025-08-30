package dev.biserman.planet.rendering.renderers

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.DebugRenderer
import dev.biserman.planet.rendering.MeshData
import dev.biserman.planet.rendering.vectorMesh
import godot.api.Node

class TileMovementRenderer(parent: Node, val lift: Double, override val visibleByDefault: Boolean = false) :
    DebugRenderer<Planet>(parent) {
    override val name = "tile_movement"

    override fun generateMeshes(input: Planet): List<MeshData> =
        input.tectonicPlates.flatMap { plate ->
            val movementVectors = plate.tiles.filter { it.movement.length() > 0.005 }.map {
                DebugVector(
                    it.tile.position * lift,
                    it.movement
                )
            }

            vectorMesh(movementVectors, plate.debugColor)
        }
}