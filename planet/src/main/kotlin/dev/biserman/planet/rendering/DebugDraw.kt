package dev.biserman.planet.rendering

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.geometry.MutMesh
import dev.biserman.planet.geometry.toMesh
import godot.api.ArrayMesh
import godot.api.Material
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle

object DebugDraw {
    fun (Node).drawMesh(name: String, mesh: ArrayMesh, material: Material? = null): MeshInstance3D {
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
    ) {
        this.drawMesh("${name}_vector", vectors.toMesh().toWireframe(), StandardMaterial3D().apply {
            this.setAlbedo(color)
        })

        if (drawDot) {
            this.drawMesh("${name}_vector_origin", vectors.map { it.origin }.toMesh(), StandardMaterial3D().apply {
                this.setAlbedo(color)
                this.usePointSize = true
                this.setPointSize(3.5f)
            })
        }
    }

    fun (Node).drawRotVectorMesh(
        name: String,
        vectors: List<Pair<DebugVector, Double>>,
        color: Color = Color.red,
    ) {
        drawVectorMesh(name, vectors.map { it.first }, color)
        drawVectorMesh("${name}_rot", vectors.map { it.first.crossOff(it.second) }, color, drawDot = false)
    }
}