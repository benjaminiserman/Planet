package dev.biserman.planet.rendering.renderers

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.DebugRenderer
import dev.biserman.planet.rendering.MeshData
import godot.api.Node
import godot.api.StandardMaterial3D

class CellWireframeRenderer(parent: Node, val lift: Double, override val visibleByDefault: Boolean = false) :
    DebugRenderer<Planet>(parent) {
    override val name = "cell_wireframe"

    override fun generateMeshes(input: Planet): List<MeshData> = listOf(
        MeshData(
            input.topology.makeMesh().apply { this.verts.forEach { it.position *= lift } }.toWireframe(),
            StandardMaterial3D().apply {
                this.pointSize = 3.5f
            }
        )
    )
}