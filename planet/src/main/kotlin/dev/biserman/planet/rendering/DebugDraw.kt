package dev.biserman.planet.rendering

import dev.biserman.planet.geometry.DebugVector
import godot.api.Material
import godot.api.Mesh
import godot.api.MeshInstance3D
import godot.api.Node
import godot.core.Color

object DebugDraw {
    fun (Node).drawMesh(name: String, mesh: Mesh, material: Material? = null): MeshInstance3D {
        val meshInstance = MeshInstance3D().also { it.setName(name) }
        this.addChild(meshInstance, forceReadableName = true)
        meshInstance.setMesh(mesh)
        if (material != null) {
            meshInstance.setSurfaceOverrideMaterial(0, material)
        }

        return meshInstance
    }

    fun (Node).drawVectorMesh(
        name: String,
        vectors: List<DebugVector>,
        color: Color = Color.red,
        drawDot: Boolean = true
    ) =
        vectorMesh(vectors, color, drawDot).withIndex().map { (index, data) ->
            this.drawMesh("${name}_$index", data.mesh, data.material)
        }

    fun (Node).drawRotVectorMesh(
        name: String,
        vectors: List<Pair<DebugVector, Double>>,
        color: Color = Color.red,
        drawDot: Boolean = true
    ) = rotVectorMesh(vectors, color, drawDot).withIndex().map { (index, data) ->
        this.drawMesh("${name}_$index", data.mesh, data.material)
    }
}