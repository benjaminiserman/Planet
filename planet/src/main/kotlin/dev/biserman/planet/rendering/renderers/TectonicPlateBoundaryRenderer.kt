package dev.biserman.planet.rendering.renderers

import dev.biserman.planet.geometry.Path.Companion.toMesh
import dev.biserman.planet.geometry.Path.Companion.toPaths
import dev.biserman.planet.geometry.toMesh
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.DebugRenderer
import dev.biserman.planet.rendering.MeshData
import godot.api.Node
import godot.api.StandardMaterial3D

class TectonicPlateBoundaryRenderer(parent: Node, val lift: Double, override val visibleByDefault: Boolean = false) :
    DebugRenderer<Planet>(parent) {
    override val name = "tectonic_plate_boundary"

    override fun generateMeshes(input: Planet): List<MeshData> =
        input.tectonicPlates.withIndex().map { (index, plate) ->
            val borderMesh = plate.region.border.toPaths().toMesh().apply {
                this.verts.forEach { it.position *= (lift + 0.0001 * index) }
            }

            MeshData(
                borderMesh.toWireframe(), StandardMaterial3D().apply {
                    this.setAlbedo(plate.debugColor)
                }
            )
        }
}