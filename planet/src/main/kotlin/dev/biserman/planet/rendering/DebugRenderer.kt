package dev.biserman.planet.rendering

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.geometry.toMesh
import dev.biserman.planet.gui.Gui
import godot.api.*
import godot.core.Color

class MeshData(val mesh: Mesh, val material: Material? = null)
abstract class DebugRenderer<T>(val parent: Node) {
    val meshInstances = mutableListOf<MeshInstance3D>()
    abstract val name: String

    open val displayName: String
        get() = name.split("_").joinToString(" ") { it.capitalize() }


    open val visibleByDefault: Boolean = false
    var visible: Boolean = false
        set(value) {
            field = value
            if (visible && dirty && lastInput != null) {
                update(lastInput!!)
            }
            meshInstances.forEach { it.visible = value }
        }

    var dirty = false
    var lastInput: T? = null

    fun init() {
        visible = visibleByDefault
        Gui.addToggle("Show $displayName", defaultValue = visibleByDefault) { visible = it }
    }

    fun update(input: T) {
        lastInput = input
        if (!visible) {
            dirty = true
            return
        }

        val meshData = generateMeshes(input)
        while (meshInstances.size < meshData.size) {
            meshInstances.add(MeshInstance3D().also {
                it.setName("${name}_${meshInstances.size}")
                it.setVisible(visible)
                parent.addChild(it, forceReadableName = true)
            })
        }

        while (meshInstances.size > meshData.size) {
            meshInstances.removeLast().also {
                it.queueFree()
            }
        }

        meshInstances.zip(meshData).forEach { (meshInstance, data) ->
            meshInstance.setMesh(data.mesh)
            if (data.material != null) {
                meshInstance.setSurfaceOverrideMaterial(0, data.material)
            }
        }

        dirty = false
    }

    abstract fun generateMeshes(input: T): List<MeshData>
}

class SimpleDebugRenderer<T>(
    parent: Node, override val name: String, val generateMesh: (T) -> List<MeshData>
) : DebugRenderer<T>(parent) {
    override fun generateMeshes(input: T): List<MeshData> = generateMesh(input)
}

fun vectorMesh(
    vectors: List<DebugVector>, color: Color = Color.red, drawDot: Boolean = true
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
    vectors: List<Pair<DebugVector, Double>>, color: Color = Color.red, drawDot: Boolean = true
): List<MeshData> = vectorMesh(vectors.map { it.first }, color, drawDot).plus(
    vectorMesh(
        vectors.map { it.first.crossOff(it.second) },
        color,
        drawDot = false
    )
)