package dev.biserman.planet.rendering.renderers

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.DebugRenderer
import dev.biserman.planet.rendering.MeshData
import dev.biserman.planet.rendering.vectorMesh
import godot.api.Node
import godot.core.Color
import godot.core.Vector3

class TileVectorRenderer(
    parent: Node,
    override val name: String,
    val lift: Double,
    val getFn: (PlanetTile) -> Vector3,
    val color: Color = Color(1.0, 1.0, 1.0),
    override val visibleByDefault: Boolean = false
) :
    DebugRenderer<Planet>(parent) {

    override fun generateMeshes(input: Planet): List<MeshData> {
        val movementVectors = input.planetTiles.values.map {
            DebugVector(
                it.tile.position * lift,
                getFn(it)
            )
        }

        return vectorMesh(movementVectors, color)
    }
}