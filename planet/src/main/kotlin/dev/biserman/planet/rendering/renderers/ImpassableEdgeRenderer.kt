package dev.biserman.planet.rendering.renderers

import dev.biserman.planet.geometry.MutEdge
import dev.biserman.planet.geometry.MutMesh
import dev.biserman.planet.geometry.MutVertex
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.DebugRenderer
import dev.biserman.planet.rendering.MeshData
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color

class ImpassableEdgeRenderer(
    parent: Node,
    private val lift: Double,
    override val categories: List<String> = listOf()
) : DebugRenderer<Planet>(parent) {
    override val name = "impassable_edges"

    override fun generateMeshes(input: Planet): List<MeshData> {
        val impassableBorders = input.topology.borders.filter { border ->
            val tileA = input.getTile(border.tiles[0])
            val tileB = input.getTile(border.tiles[1])
            tileA.hasImpassableEdgeWith(tileB)
        }
        val vertices = impassableBorders
            .flatMap { border -> border.corners.map { MutVertex(it.position * lift) } }
            .toMutableList()
        val edges = impassableBorders.indices
            .map { index -> MutEdge(mutableListOf(index * 2, index * 2 + 1)) }
            .toMutableList()

        return listOf(
            MeshData(
                MutMesh(vertices, edges).toWireframe(),
                StandardMaterial3D().apply { albedoColor = Color(1.0, 0.15, 0.05, 1.0) }
            )
        )
    }
}
