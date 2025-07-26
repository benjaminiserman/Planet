package dev.biserman.planet

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.ArrayMesh
import godot.api.Mesh
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.RenderingServer
import godot.api.VisualInstance3D
import godot.api.VisualShader
import godot.core.PackedInt32Array
import godot.core.PackedVector3Array
import godot.core.VariantArray
import godot.core.Vector3
import godot.global.GD
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

// Adapted from Andy Gainey, original license below:
// Copyright © 2014 Andy Gainey <andy@experilous.com>
//
// Usage of the works is permitted provided that this instrument
// is retained with the works, so that any entity that uses the
// works is notified of this instrument.
//
// DISCLAIMER: THE WORKS ARE WITHOUT WARRANTY.

data class MutMesh(val verts: MutableList<MutVertex>, val edges: MutableList<MutEdge>, val faces: MutableList<MutFace>)
data class MutVertex(
    var position: Vector3,
    val edgeIndexes: MutableList<Int> = mutableListOf(),
    val faceIndexes: MutableList<Int> = mutableListOf(),
    var normal: Vector3 = Vector3.ZERO
)

data class MutEdge(
    val vertIndexes: MutableList<Int> = mutableListOf(),
    val faceIndexes: MutableList<Int> = mutableListOf(),
    val subdividedVertexIndexes: MutableList<Int> = mutableListOf(),
    val subdividedEdgeIndexes: MutableList<Int> = mutableListOf(),
)

data class MutFace(
    val vertIndexes: MutableList<Int> = mutableListOf(),
    val edgeIndexes: MutableList<Int> = mutableListOf()
)

@RegisterClass
class Main : Node() {

    val random = Random(0)
    lateinit var planet: MeshInstance3D

    @RegisterFunction
    override fun _ready() {
        GD.print("Hello World!")
        planet = findChild("Planet") as MeshInstance3D
        val icos = makeIcosahedron()
        val sub = icos.subdivideIcosahedron(4)
        sub.distortTriangles(0.99)
        sub.duplicateSharedEdges()
        sub.recalculateNormals()
        planet.setMesh(sub.toArrayMesh())
    }

    fun (MutMesh).toArrayMesh(): ArrayMesh {
        val surfaceArray = VariantArray<Any?>()
        surfaceArray.resize(Mesh.ArrayType.MAX.ordinal)

        surfaceArray[Mesh.ArrayType.VERTEX.ordinal] = PackedVector3Array(this.verts.map { it.position })
        surfaceArray[Mesh.ArrayType.INDEX.ordinal] =
            PackedInt32Array(this.faces.flatMap { it.vertIndexes }.toIntArray())
        surfaceArray[Mesh.ArrayType.NORMAL.ordinal] = PackedVector3Array(this.verts.map { it.normal })

        val arrayMesh = ArrayMesh()
        arrayMesh.addSurfaceFromArrays(Mesh.PrimitiveType.TRIANGLES, surfaceArray)
        return arrayMesh
    }

    fun makeIcosahedron(): MutMesh {
        val phi = (1.0 + sqrt(5.0)) / 2.0;
        val du = 1.0 / sqrt(phi * phi + 1.0);
        val dv = phi * du;

        val verts = mutableListOf(
            MutVertex(Vector3(0, +dv, +du)),
            MutVertex(Vector3(0, +dv, -du)),
            MutVertex(Vector3(0, -dv, +du)),
            MutVertex(Vector3(0, -dv, -du)),
            MutVertex(Vector3(+du, 0, +dv)),
            MutVertex(Vector3(-du, 0, +dv)),
            MutVertex(Vector3(+du, 0, -dv)),
            MutVertex(Vector3(-du, 0, -dv)),
            MutVertex(Vector3(+dv, +du, 0)),
            MutVertex(Vector3(+dv, -du, 0)),
            MutVertex(Vector3(-dv, +du, 0)),
            MutVertex(Vector3(-dv, -du, 0)),
        )

        val edges = mutableListOf(
            MutEdge(mutableListOf(0, 1)),
            MutEdge(mutableListOf(0, 4)),
            MutEdge(mutableListOf(0, 5)),
            MutEdge(mutableListOf(0, 8)),
            MutEdge(mutableListOf(0, 10)),
            MutEdge(mutableListOf(1, 6)),
            MutEdge(mutableListOf(1, 7)),
            MutEdge(mutableListOf(1, 8)),
            MutEdge(mutableListOf(1, 10)),
            MutEdge(mutableListOf(2, 3)),
            MutEdge(mutableListOf(2, 4)),
            MutEdge(mutableListOf(2, 5)),
            MutEdge(mutableListOf(2, 9)),
            MutEdge(mutableListOf(2, 11)),
            MutEdge(mutableListOf(3, 6)),
            MutEdge(mutableListOf(3, 7)),
            MutEdge(mutableListOf(3, 9)),
            MutEdge(mutableListOf(3, 11)),
            MutEdge(mutableListOf(4, 5)),
            MutEdge(mutableListOf(4, 8)),
            MutEdge(mutableListOf(4, 9)),
            MutEdge(mutableListOf(5, 10)),
            MutEdge(mutableListOf(5, 11)),
            MutEdge(mutableListOf(6, 7)),
            MutEdge(mutableListOf(6, 8)),
            MutEdge(mutableListOf(6, 9)),
            MutEdge(mutableListOf(7, 10)),
            MutEdge(mutableListOf(7, 11)),
            MutEdge(mutableListOf(8, 9)),
            MutEdge(mutableListOf(10, 11)),
        )

        val faces = mutableListOf(
            MutFace(mutableListOf(0, 1, 8), mutableListOf(0, 7, 3)),
            MutFace(mutableListOf(0, 4, 5), mutableListOf(1, 18, 2)),
            MutFace(mutableListOf(0, 5, 10), mutableListOf(2, 21, 4)),
            MutFace(mutableListOf(0, 8, 4), mutableListOf(3, 19, 1)),
            MutFace(mutableListOf(0, 10, 1), mutableListOf(4, 8, 0)),
            MutFace(mutableListOf(1, 6, 8), mutableListOf(5, 24, 7)),
            MutFace(mutableListOf(1, 7, 6), mutableListOf(6, 23, 5)),
            MutFace(mutableListOf(1, 10, 7), mutableListOf(8, 26, 6)),
            MutFace(mutableListOf(2, 3, 11), mutableListOf(9, 17, 13)),
            MutFace(mutableListOf(2, 4, 9), mutableListOf(10, 20, 12)),
            MutFace(mutableListOf(2, 5, 4), mutableListOf(11, 18, 10)),
            MutFace(mutableListOf(2, 9, 3), mutableListOf(12, 16, 9)),
            MutFace(mutableListOf(2, 11, 5), mutableListOf(13, 22, 11)),
            MutFace(mutableListOf(3, 6, 7), mutableListOf(14, 23, 15)),
            MutFace(mutableListOf(3, 7, 11), mutableListOf(15, 27, 17)),
            MutFace(mutableListOf(3, 9, 6), mutableListOf(16, 25, 14)),
            MutFace(mutableListOf(4, 8, 9), mutableListOf(19, 28, 20)),
            MutFace(mutableListOf(5, 11, 10), mutableListOf(22, 29, 21)),
            MutFace(mutableListOf(6, 9, 8), mutableListOf(25, 28, 24)),
            MutFace(mutableListOf(7, 10, 11), mutableListOf(26, 29, 27)),
        )

        for (i in 0..<edges.size) {
            for (j in 0..<(edges[i].vertIndexes.size)) {
                verts[j].edgeIndexes.add(i)
            }
        }

        for (i in 0..<faces.size) {
            for (j in 0..<(faces[i].vertIndexes.size)) {
                verts[j].faceIndexes.add(i)
            }

            for (j in 0..<(faces[i].edgeIndexes.size)) {
                edges[j].faceIndexes.add(i)
            }
        }

        return MutMesh(verts, edges, faces)
    }

    fun (MutMesh).distortTriangles(distortionRate: Double = 0.5, iterations: Int = 6) {
        var totalDistortion = ceil(this.edges.size * distortionRate)
        for (remainingIterations in iterations downTo 1) {
            val iterationDistortion = floor(totalDistortion / remainingIterations).toInt()
            totalDistortion -= iterationDistortion

            this.distortMesh(iterationDistortion)
            // relaxMesh
        }
    }

    fun rotationPredicate(
        oldVert0: MutVertex,
        oldVert1: MutVertex,
        newVert0: MutVertex,
        newVert1: MutVertex
    ): Boolean {
        if (newVert0.faceIndexes.size >= 7
            || newVert1.faceIndexes.size >= 7
            || oldVert0.faceIndexes.size <= 5
            || oldVert1.faceIndexes.size <= 5
        ) {
            return false
        }

        val oldEdgeLength = oldVert0.position.distanceTo(oldVert1.position)
        val newEdgeLength = newVert0.position.distanceTo(newVert1.position)
        val ratio = oldEdgeLength / newEdgeLength

        if (ratio >= 2 || ratio <= 0.5) {
            return false
        }

        val v0 = (oldVert1.position - oldVert0.position).div(oldEdgeLength)
        val v1 = (newVert0.position - oldVert0.position).normalized()
        val v2 = (newVert1.position - oldVert0.position).normalized()

        if (v0.dot(v1) < 0.2 || v0.dot(v2) < 0.2) {
            return false
        }

        val v3 = (newVert0.position - oldVert1.position).normalized()
        val v4 = (newVert1.position - oldVert1.position).normalized()
        @Suppress("RedundantIf")
        if (-v0.dot(v3) < 0.2 || -v0.dot(v4) < 0.2) {
            return false
        }

        return true
    }

    fun (MutMesh).distortMesh(degree: Int): Boolean {
        (0..<degree).forEach {
            var consecutiveFailedAttempts = 0
            var edgeIndex = random.nextInt(0, this.edges.size)

            while (!this.conditionalRotateEdge(edgeIndex, ::rotationPredicate)) {
                consecutiveFailedAttempts += 1
                if (consecutiveFailedAttempts >= this.edges.size) {
                    return false
                }

                edgeIndex = (edgeIndex + 1) % this.edges.size
            }
        }

        return true
    }

    fun (MutEdge).oppositeFaceIndex(faceIndex: Int): Int = when (faceIndex) {
        this.faceIndexes[0] -> this.faceIndexes[1]
        this.faceIndexes[1] -> this.faceIndexes[0]
        else -> throw Error("Given face is not part of given edge.")
    }

    fun (MutFace).oppositeVertIndex(edge: MutEdge): Int = when {
        this.vertIndexes[0] != edge.vertIndexes[0] && this.vertIndexes[0] != edge.vertIndexes[1] -> 0
        this.vertIndexes[1] != edge.vertIndexes[0] && this.vertIndexes[1] != edge.vertIndexes[1] -> 1
        this.vertIndexes[2] != edge.vertIndexes[0] && this.vertIndexes[2] != edge.vertIndexes[1] -> 2
        else -> throw Error("Cannot find node of given face that is not also a node of given edge.")
    }

    fun (MutMesh).nextFaceIndex(vertIndex: Int, faceIndex: Int): Int {
        val face = this.faces[faceIndex]
        val vertFaceIndex = face.vertIndexes.indexOf(vertIndex)
        val edge = this.edges[face.edgeIndexes[(vertFaceIndex + 2) % 3]]
        return edge.oppositeFaceIndex(faceIndex)
    }

    fun (MutMesh).conditionalRotateEdge(
        edgeIndex: Int,
        predicate: (MutVertex, MutVertex, MutVertex, MutVertex) -> Boolean
    ): Boolean {
        val edge = this.edges[edgeIndex]
        val face0 = this.faces[edge.faceIndexes[0]]
        val face1 = this.faces[edge.faceIndexes[1]]
        val farVertFaceIndex0 = face0.oppositeVertIndex(edge)
        val farVertFaceIndex1 = face1.oppositeVertIndex(edge)
        val newVertIndex0 = face0.vertIndexes[farVertFaceIndex0]
        val oldVertIndex0 = face0.vertIndexes[(farVertFaceIndex0 + 1) % 3]
        val newVertIndex1 = face1.vertIndexes[farVertFaceIndex1]
        val oldVertIndex1 = face1.vertIndexes[(farVertFaceIndex1 + 1) % 3]
        val oldVert0 = this.verts[oldVertIndex0]
        val oldVert1 = this.verts[oldVertIndex1]
        val newVert0 = this.verts[newVertIndex0]
        val newVert1 = this.verts[newVertIndex1]
        val newEdgeIndex0 = face1.edgeIndexes[(farVertFaceIndex1 + 2) % 3]
        val newEdgeIndex1 = face0.edgeIndexes[(farVertFaceIndex0 + 2) % 3]
        val newEdge0 = this.edges[newEdgeIndex0]
        val newEdge1 = this.edges[newEdgeIndex1]

        if (!predicate(oldVert0, oldVert1, newVert0, newVert1)) {
            return false
        }

        oldVert0.edgeIndexes.remove(edgeIndex)
        oldVert1.edgeIndexes.remove(edgeIndex)
        newVert0.edgeIndexes.add(edgeIndex)
        newVert1.edgeIndexes.add(edgeIndex)

        edge.vertIndexes[0] = newVertIndex0
        edge.vertIndexes[1] = newVertIndex1

        newEdge0.faceIndexes.remove(edge.faceIndexes[1])
        newEdge1.faceIndexes.remove(edge.faceIndexes[0])
        newEdge0.faceIndexes.add(edge.faceIndexes[0])
        newEdge1.faceIndexes.add(edge.faceIndexes[1])

        oldVert0.faceIndexes.remove(edge.faceIndexes[1])
        oldVert1.faceIndexes.remove(edge.faceIndexes[0])
        newVert0.faceIndexes.add(edge.faceIndexes[1])
        newVert1.faceIndexes.add(edge.faceIndexes[0])

        face0.vertIndexes[(farVertFaceIndex0 + 2) % 3] = newVertIndex1
        face1.vertIndexes[(farVertFaceIndex1 + 2) % 3] = newVertIndex0

        face0.edgeIndexes[(farVertFaceIndex0 + 1) % 3] = newEdgeIndex0
        face1.edgeIndexes[(farVertFaceIndex1 + 1) % 3] = newEdgeIndex1
        face0.edgeIndexes[(farVertFaceIndex0 + 2) % 3] = edgeIndex
        face1.edgeIndexes[(farVertFaceIndex1 + 2) % 3] = edgeIndex

        return true
    }

    fun (MutMesh).duplicateSharedEdges() {
        val set = mutableSetOf<Int>()

        for (face in this.faces) {
            for (i in 0..<face.vertIndexes.size) {
                val vertIndex = face.vertIndexes[i]
                if (set.contains(vertIndex)) {
                    val newVertIndex = this.verts.size
                    face.vertIndexes[i] = newVertIndex

                    for (edgeIndex in face.edgeIndexes) {
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

    fun calculateNormal(p1: Vector3, p2: Vector3, p3: Vector3): Vector3 {
        val a = p2 - p1
        val b = p3 - p1

        return Vector3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        )
    }

    fun (MutMesh).recalculateNormals() {
        for (face in this.faces) {
            val normal = calculateNormal(
                this.verts[face.vertIndexes[0]].position,
                this.verts[face.vertIndexes[1]].position,
                this.verts[face.vertIndexes[2]].position
            )

            for (vertIndex in face.vertIndexes) {
                this.verts[vertIndex].normal = normal
            }
        }
    }

    fun (MutMesh).subdivideIcosahedron(degree: Int): MutMesh {
        val verts = this.verts.map { MutVertex(Vector3(it.position)) }.toMutableList()
        val edges = mutableListOf<MutEdge>()

        for (edge in this.edges) {
            val v0 = this.verts[edge.vertIndexes[0]]
            val v1 = this.verts[edge.vertIndexes[1]]
            verts[edge.vertIndexes[0]].edgeIndexes.add(edges.size)
            var priorVertIndex = edge.vertIndexes[0]
            for (i in 1..<degree) {
                val edgeIndex = edges.size
                val vertIndex = verts.size
                edge.subdividedEdgeIndexes.add(edgeIndex)
                edge.subdividedVertexIndexes.add(vertIndex)
                edges.add(MutEdge(mutableListOf(priorVertIndex, vertIndex)))
                priorVertIndex = vertIndex
                verts.add(
                    MutVertex(
                        v0.position.slerp(v1.position, i / degree.toDouble()),
                        mutableListOf(edgeIndex, edgeIndex + 1)
                    )
                )
            }
            edge.subdividedEdgeIndexes.add(edges.size)
            verts[edge.vertIndexes[1]].edgeIndexes.add(edges.size)
            edges.add(MutEdge(mutableListOf(priorVertIndex, edge.vertIndexes[1])))
        }

        val faces = mutableListOf<MutFace>()
        for (face in this.faces) {
            val edge0 = this.edges[face.edgeIndexes[0]]
            val edge1 = this.edges[face.edgeIndexes[1]]
            val edge2 = this.edges[face.edgeIndexes[2]]

            val getEdgeVert0: (Int) -> Int = if (face.vertIndexes[0] == edge0.vertIndexes[0]) ({
                edge0.subdividedVertexIndexes[it]
            }) else ({
                edge0.subdividedVertexIndexes[degree - 2 - it]
            })

            val getEdgeVert1: (Int) -> Int = if (face.vertIndexes[1] == edge1.vertIndexes[0]) ({
                edge1.subdividedVertexIndexes[it]
            }) else ({
                edge1.subdividedVertexIndexes[degree - 2 - it]
            })

            val getEdgeVert2: (Int) -> Int = if (face.vertIndexes[0] == edge2.vertIndexes[0]) ({
                edge2.subdividedVertexIndexes[it]
            }) else ({
                edge2.subdividedVertexIndexes[degree - 2 - it]
            })

            val faceVerts = listOf<Int>(face.vertIndexes[0])
                .plus((0..<edge0.subdividedVertexIndexes.size).map(getEdgeVert0))
                .plus(face.vertIndexes[1])
                .toMutableList()
            for (i in 1..<degree) {
                faceVerts.add(getEdgeVert2(i - 1))
                val start = verts[getEdgeVert2(i - 1)].position
                val end = verts[getEdgeVert1(i - 1)].position
                for (j in 1..<(degree - i)) {
                    faceVerts.add(verts.size)
                    verts.add(MutVertex(start.slerp(end, j.toDouble() / (degree - i).toDouble())))
                }
                faceVerts.add(getEdgeVert1(i - 1))
            }
            faceVerts.add(face.vertIndexes[2])

            val getEdgeEdge0: (Int) -> Int = if (face.vertIndexes[0] == edge0.vertIndexes[0]) ({
                edge0.subdividedEdgeIndexes[it]
            }) else ({
                edge0.subdividedEdgeIndexes[degree - 1 - it]
            })

            val getEdgeEdge1: (Int) -> Int = if (face.vertIndexes[1] == edge1.vertIndexes[0]) ({
                edge1.subdividedEdgeIndexes[it]
            }) else ({
                edge1.subdividedEdgeIndexes[degree - 1 - it]
            })

            val getEdgeEdge2: (Int) -> Int = if (face.vertIndexes[0] == edge2.vertIndexes[0]) ({
                edge2.subdividedEdgeIndexes[it]
            }) else ({
                edge2.subdividedEdgeIndexes[degree - 1 - it]
            })

            val faceEdges0 = (0..<degree).map(getEdgeEdge0).toMutableList()
            var vertIndex = degree + 1
            for (i in 1..<degree) {
                (0..<(degree - i)).forEach {
                    faceEdges0.add(edges.size)
                    val edge = MutEdge(mutableListOf(faceVerts[vertIndex], faceVerts[vertIndex + 1]))
                    verts[edge.vertIndexes[0]].edgeIndexes.add(edges.size)
                    verts[edge.vertIndexes[1]].edgeIndexes.add(edges.size)
                    edges.add(edge)
                    vertIndex += 1
                }
                vertIndex += 1
            }

            val faceEdges1 = mutableListOf<Int>()
            vertIndex = 1
            for (i in 0..<degree) {
                (1..<(degree - i)).forEach {
                    faceEdges1.add(edges.size)
                    val edge = MutEdge(mutableListOf(faceVerts[vertIndex], faceVerts[vertIndex + degree - i]))
                    verts[edge.vertIndexes[0]].edgeIndexes.add(edges.size)
                    verts[edge.vertIndexes[1]].edgeIndexes.add(edges.size)
                    edges.add(edge)
                    vertIndex += 1
                }
                faceEdges1.add(getEdgeEdge1(i))
                vertIndex += 2
            }

            val faceEdges2 = mutableListOf<Int>()
            vertIndex = 1
            for (i in 0..<degree) {
                faceEdges2.add(getEdgeEdge2(i))
                (1..<(degree - i)).forEach {
                    faceEdges2.add(edges.size)
                    val edge = MutEdge(mutableListOf(faceVerts[vertIndex], faceVerts[vertIndex + degree - i + 1]))
                    verts[edge.vertIndexes[0]].edgeIndexes.add(edges.size)
                    verts[edge.vertIndexes[1]].edgeIndexes.add(edges.size)
                    edges.add(edge)
                    vertIndex += 1
                }
                vertIndex += 2
            }

            vertIndex = 0
            var edgeIndex = 0
            for (i in 0..<degree) {
                (1..<(degree - i + 1)).forEach {
                    val subFace = MutFace(
                        mutableListOf(
                            faceVerts[vertIndex],
                            faceVerts[vertIndex + 1],
                            faceVerts[vertIndex + degree - i + 1]
                        ),
                        mutableListOf(faceEdges0[edgeIndex], faceEdges1[edgeIndex], faceEdges2[edgeIndex])
                    )
                    (0..2).forEach {
                        verts[subFace.vertIndexes[it]].faceIndexes.add(faces.size)
                        edges[subFace.edgeIndexes[it]].faceIndexes.add(faces.size)
                    }
                    faces.add(subFace)
                    vertIndex += 1
                    edgeIndex += 1
                }
                vertIndex += 1
            }

            vertIndex = 1
            edgeIndex = 0
            for (i in 1..<degree) {
                (1..<(degree - i + 1)).forEach {
                    val subFace = MutFace(
                        mutableListOf(
                            faceVerts[vertIndex],
                            faceVerts[vertIndex + degree - i + 2],
                            faceVerts[vertIndex + degree - i + 1]
                        ),
                        mutableListOf(
                            faceEdges2[edgeIndex + 1],
                            faceEdges0[edgeIndex + degree - i + 1],
                            faceEdges1[edgeIndex]
                        )
                    )
                    (0..2).forEach {
                        verts[subFace.vertIndexes[it]].faceIndexes.add(faces.size)
                        edges[subFace.edgeIndexes[it]].faceIndexes.add(faces.size)
                    }
                    faces.add(subFace)
                    vertIndex += 1
                    edgeIndex += 1
                }
                vertIndex += 2
                edgeIndex += 1
            }
        }

        return MutMesh(verts, edges, faces)
    }
}
