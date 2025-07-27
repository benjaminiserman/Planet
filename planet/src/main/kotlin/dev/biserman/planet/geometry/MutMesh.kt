package dev.biserman.planet.geometry

import godot.api.ArrayMesh
import godot.api.Mesh
import godot.core.PackedInt32Array
import godot.core.PackedVector3Array
import godot.core.VariantArray
import kotlin.collections.flatMap

data class MutMesh(
    val verts: MutableList<MutVertex>,
    val edges: MutableList<MutEdge>,
    val tris: MutableList<MutTri>
) {
    fun toArrayMesh(): ArrayMesh {
        val surfaceArray = VariantArray<Any?>()
        surfaceArray.resize(Mesh.ArrayType.MAX.ordinal)

        surfaceArray[Mesh.ArrayType.VERTEX.ordinal] = PackedVector3Array(this.verts.map { it.position })
        surfaceArray[Mesh.ArrayType.INDEX.ordinal] =
            PackedInt32Array(this.tris.flatMap { it.vertIndexes }.toIntArray())
        surfaceArray[Mesh.ArrayType.NORMAL.ordinal] = PackedVector3Array(this.verts.map { it.normal })

        val arrayMesh = ArrayMesh()
        arrayMesh.addSurfaceFromArrays(Mesh.PrimitiveType.TRIANGLES, surfaceArray)
        return arrayMesh
    }

    fun nextTriIndex(vertIndex: Int, faceIndex: Int): Int {
        val tri = this.tris[faceIndex]
        val vertTriIndex = tri.vertIndexes.indexOf(vertIndex)
        val edge = this.edges[tri.edgeIndexes[(vertTriIndex + 2) % 3]]
        return edge.oppositeTriIndex(faceIndex)
    }

    fun duplicateSharedVerts() {
        val set = mutableSetOf<Int>()

        for (tri in this.tris) {
            for (i in 0..<tri.vertIndexes.size) {
                val vertIndex = tri.vertIndexes[i]
                if (set.contains(vertIndex)) {
                    val newVertIndex = this.verts.size
                    tri.vertIndexes[i] = newVertIndex

                    for (edgeIndex in tri.edgeIndexes) {
                        this.edges[edgeIndex].vertIndexes.replaceAll { if (it == vertIndex) newVertIndex else it }
                    }

                    set.add(newVertIndex)
                    this.verts.add(this.verts[vertIndex].copy())
                } else {
                    set.add(vertIndex)
                }
            }
        }
    }

    fun recalculateNormals() {
        for (tri in this.tris) {
            val normal = calculateNormal(
                this.verts[tri.vertIndexes[0]].position,
                this.verts[tri.vertIndexes[1]].position,
                this.verts[tri.vertIndexes[2]].position
            )

            for (vertIndex in tri.vertIndexes) {
                this.verts[vertIndex].normal = normal
            }
        }
    }
}






