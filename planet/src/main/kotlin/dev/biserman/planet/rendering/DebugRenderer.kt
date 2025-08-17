package dev.biserman.planet.rendering

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.geometry.toMesh
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.Planet
import godot.api.Material
import godot.api.Mesh
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color

class MeshData(val mesh: Mesh, val material: Material? = null)
abstract class DebugRenderer<T>(val parent: Node) {
    val meshInstances = mutableListOf<MeshInstance3D>()
    abstract val name: String
    abstract val displayName: String
    open val visibleByDefault: Boolean = false

    fun init() {
        visible = visibleByDefault
        Gui.addToggle("Show $displayName", defaultValue = visibleByDefault) { visible = it }
    }

    fun update(input: T) {
        val meshData = generateMeshes(input)
        while (meshInstances.size < meshData.size) {
            meshInstances.add(MeshInstance3D().also {
                it.setName("${name}_${meshInstances.size}")
                it.setVisible(visible)
                parent.addChild(it, forceReadableName = true)
            })
        }

        meshInstances.zip(meshData).forEach { (meshInstance, data) ->
            meshInstance.setMesh(data.mesh)
            if (data.material != null) {
                meshInstance.setSurfaceOverrideMaterial(0, data.material)
            }
        }
    }

    abstract fun generateMeshes(input: T): List<MeshData>

    var visible: Boolean = false
        set(value) {
            field = value
            meshInstances.forEach { it.visible = value }
        }
}

fun vectorMesh(
    vectors: List<DebugVector>,
    color: Color = Color.red,
    drawDot: Boolean = true
): List<MeshData> {
    val meshData = mutableListOf<MeshData>()
    meshData.add(MeshData(vectors.toMesh().toWireframe(), StandardMaterial3D().apply {
        this.setAlbedo(color)
    }))

    if (drawDot) {
        meshData.add(MeshData(vectors.map { it.origin }.toMesh(), StandardMaterial3D().apply {
            this.setAlbedo(color)
            this.usePointSize = true
            this.setPointSize(3.5f)
        }))
    }

    return meshData
}

fun rotVectorMesh(
    vectors: List<Pair<DebugVector, Double>>,
    color: Color = Color.red,
    drawDot: Boolean = true
): List<MeshData> =
    vectorMesh(vectors.map { it.first }, color, drawDot)
        .plus(vectorMesh(vectors.map { it.first.crossOff(it.second) }, color, drawDot = false))