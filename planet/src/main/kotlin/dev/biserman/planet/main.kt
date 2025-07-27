package dev.biserman.planet

import dev.biserman.planet.geometry.MutEdge
import dev.biserman.planet.geometry.MutTri
import dev.biserman.planet.geometry.MutMesh
import dev.biserman.planet.geometry.MutVertex
import dev.biserman.planet.geometry.calculateNormal
import dev.biserman.planet.geometry.distortTriangles
import dev.biserman.planet.geometry.makeIcosahedron
import dev.biserman.planet.geometry.reorderVerts
import dev.biserman.planet.geometry.subdivideIcosahedron
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.MeshInstance3D
import godot.api.Node
import godot.core.Plane
import godot.core.Vector3
import godot.global.GD
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random



@RegisterClass
class Main : Node() {

    lateinit var planet: MeshInstance3D

    @RegisterFunction
    override fun _ready() {
        GD.print("Hello World!")
        planet = findChild("Planet") as MeshInstance3D
        val icos = makeIcosahedron()
        val sub = icos.subdivideIcosahedron(4)
        sub.distortTriangles(0.99)
        sub.reorderVerts()
        sub.duplicateSharedVerts()
        sub.recalculateNormals()
        planet.setMesh(sub.toArrayMesh())
    }


    companion object {

        val random = Random(0)
    }
}
