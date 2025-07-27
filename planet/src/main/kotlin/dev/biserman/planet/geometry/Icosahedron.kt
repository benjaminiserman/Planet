package dev.biserman.planet.geometry

import dev.biserman.planet.Main
import godot.core.Plane
import godot.core.Vector3
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

// Adapted from Andy Gainey, original license below:
// Copyright © 2014 Andy Gainey <andy@experilous.com>
//
// Usage of the works is permitted provided that this instrument
// is retained with the works, so that any entity that uses the
// works is notified of this instrument.
//
// DISCLAIMER: THE WORKS ARE WITHOUT WARRANTY.

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

    val tris = mutableListOf(
        MutTri(mutableListOf(0, 1, 8), mutableListOf(0, 7, 3)),
        MutTri(mutableListOf(0, 4, 5), mutableListOf(1, 18, 2)),
        MutTri(mutableListOf(0, 5, 10), mutableListOf(2, 21, 4)),
        MutTri(mutableListOf(0, 8, 4), mutableListOf(3, 19, 1)),
        MutTri(mutableListOf(0, 10, 1), mutableListOf(4, 8, 0)),
        MutTri(mutableListOf(1, 6, 8), mutableListOf(5, 24, 7)),
        MutTri(mutableListOf(1, 7, 6), mutableListOf(6, 23, 5)),
        MutTri(mutableListOf(1, 10, 7), mutableListOf(8, 26, 6)),
        MutTri(mutableListOf(2, 3, 11), mutableListOf(9, 17, 13)),
        MutTri(mutableListOf(2, 4, 9), mutableListOf(10, 20, 12)),
        MutTri(mutableListOf(2, 5, 4), mutableListOf(11, 18, 10)),
        MutTri(mutableListOf(2, 9, 3), mutableListOf(12, 16, 9)),
        MutTri(mutableListOf(2, 11, 5), mutableListOf(13, 22, 11)),
        MutTri(mutableListOf(3, 6, 7), mutableListOf(14, 23, 15)),
        MutTri(mutableListOf(3, 7, 11), mutableListOf(15, 27, 17)),
        MutTri(mutableListOf(3, 9, 6), mutableListOf(16, 25, 14)),
        MutTri(mutableListOf(4, 8, 9), mutableListOf(19, 28, 20)),
        MutTri(mutableListOf(5, 11, 10), mutableListOf(22, 29, 21)),
        MutTri(mutableListOf(6, 9, 8), mutableListOf(25, 28, 24)),
        MutTri(mutableListOf(7, 10, 11), mutableListOf(26, 29, 27)),
    )

    for (i in 0..<edges.size) {
        for (j in 0..<(edges[i].vertIndexes.size)) {
            verts[j].edgeIndexes.add(i)
        }
    }

    for (i in 0..<tris.size) {
        for (j in 0..<(tris[i].vertIndexes.size)) {
            verts[j].triIndexes.add(i)
        }

        for (j in 0..<(tris[i].edgeIndexes.size)) {
            edges[j].triIndexes.add(i)
        }
    }

    return MutMesh(verts, edges, tris)
}

fun (MutMesh).distortTriangles(distortionRate: Double = 0.5, iterations: Int = 6) {
    var totalDistortion = ceil(this.edges.size * distortionRate)
    for (remainingIterations in iterations downTo 1) {
        val iterationDistortion = floor(totalDistortion / remainingIterations).toInt()
        totalDistortion -= iterationDistortion

        this.distortMesh(iterationDistortion)
        this.relaxMesh(0.5)
    }
}

fun rotationPredicate(
    oldVert0: MutVertex,
    oldVert1: MutVertex,
    newVert0: MutVertex,
    newVert1: MutVertex
): Boolean {
    if (newVert0.triIndexes.size >= 7
        || newVert1.triIndexes.size >= 7
        || oldVert0.triIndexes.size <= 5
        || oldVert1.triIndexes.size <= 5
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
        var edgeIndex = Main.random.nextInt(0, this.edges.size)

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

fun (MutMesh).relaxMesh(multiplier: Double) {
    val totalSurfaceArea = 4 * PI
    val idealTriArea = totalSurfaceArea / this.tris.size
    val idealEdgeLength = sqrt(idealTriArea * 4 / sqrt(3.0))
    val idealDistanceToCentroid = idealEdgeLength * sqrt(3.0) / 3 * 0.9

    val pointShifts = (1..this.verts.size).map { Vector3.ZERO }.toMutableList()
    for (tri in this.tris) {
        val centroid = tri.centroid(this).normalized()
        val v0 = centroid - this.verts[tri.vertIndexes[0]].position
        val v1 = centroid - this.verts[tri.vertIndexes[1]].position
        val v2 = centroid - this.verts[tri.vertIndexes[2]].position
        pointShifts[tri.vertIndexes[0]] += v0 * (multiplier * (v0.length() - idealDistanceToCentroid) / v0.length())
        pointShifts[tri.vertIndexes[1]] += v1 * (multiplier * (v1.length() - idealDistanceToCentroid) / v1.length())
        pointShifts[tri.vertIndexes[2]] += v2 * (multiplier * (v2.length() - idealDistanceToCentroid) / v2.length())
    }

    pointShifts.zip(this.verts).mapTo(pointShifts) {
        val (pointShift, vert) = it
        val plane = Plane(vert.position, Vector3.ZERO)
        (vert.position + plane.project(pointShift)).normalized()
    }

    val rotationSuppressions = this.verts.map { 0.0 }.toMutableList()
    for (edge in this.edges) {
        val oldPoint0 = this.verts[edge.vertIndexes[0]].position
        val oldPoint1 = this.verts[edge.vertIndexes[1]].position
        val newPoint0 = pointShifts[edge.vertIndexes[0]]
        val newPoint1 = pointShifts[edge.vertIndexes[1]]
        val oldVector = (oldPoint1 - oldPoint0).normalized()
        val newVector = (newPoint1 - newPoint0).normalized()
        val suppression = (1 - oldVector.dot(newVector)) * 0.5
        rotationSuppressions[edge.vertIndexes[0]] = max(rotationSuppressions[edge.vertIndexes[0]], suppression)
        rotationSuppressions[edge.vertIndexes[1]] = max(rotationSuppressions[edge.vertIndexes[1]], suppression)
    }

    var totalShift = 0.0
    for (i in 0..<this.verts.size) {
        val original = Vector3(verts[i].position)
        verts[i].position = original.lerp(pointShifts[i], 1 - sqrt((rotationSuppressions[i]))).normalized()
        totalShift += (verts[i].position - original).length()
    }
}

fun (MutMesh).reorderVerts() {
    for (i in 0..<this.verts.size) {
        val vert = this.verts[i]
        var faceIndex = vert.triIndexes[0]
        for (j in 1..<(vert.triIndexes.size - 1)) {
            faceIndex = this.nextTriIndex(i, faceIndex)
            val k = vert.triIndexes.indexOf(faceIndex)
            vert.triIndexes[k] = vert.triIndexes[j]
            vert.triIndexes[j] = faceIndex
        }
    }
}

fun (MutMesh).conditionalRotateEdge(
    edgeIndex: Int,
    predicate: (MutVertex, MutVertex, MutVertex, MutVertex) -> Boolean
): Boolean {
    val edge = this.edges[edgeIndex]
    val face0 = this.tris[edge.triIndexes[0]]
    val face1 = this.tris[edge.triIndexes[1]]
    val farVertTriIndex0 = face0.oppositeVertIndex(edge)
    val farVertTriIndex1 = face1.oppositeVertIndex(edge)
    val newVertIndex0 = face0.vertIndexes[farVertTriIndex0]
    val oldVertIndex0 = face0.vertIndexes[(farVertTriIndex0 + 1) % 3]
    val newVertIndex1 = face1.vertIndexes[farVertTriIndex1]
    val oldVertIndex1 = face1.vertIndexes[(farVertTriIndex1 + 1) % 3]
    val oldVert0 = this.verts[oldVertIndex0]
    val oldVert1 = this.verts[oldVertIndex1]
    val newVert0 = this.verts[newVertIndex0]
    val newVert1 = this.verts[newVertIndex1]
    val newEdgeIndex0 = face1.edgeIndexes[(farVertTriIndex1 + 2) % 3]
    val newEdgeIndex1 = face0.edgeIndexes[(farVertTriIndex0 + 2) % 3]
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

    newEdge0.triIndexes.remove(edge.triIndexes[1])
    newEdge1.triIndexes.remove(edge.triIndexes[0])
    newEdge0.triIndexes.add(edge.triIndexes[0])
    newEdge1.triIndexes.add(edge.triIndexes[1])

    oldVert0.triIndexes.remove(edge.triIndexes[1])
    oldVert1.triIndexes.remove(edge.triIndexes[0])
    newVert0.triIndexes.add(edge.triIndexes[1])
    newVert1.triIndexes.add(edge.triIndexes[0])

    face0.vertIndexes[(farVertTriIndex0 + 2) % 3] = newVertIndex1
    face1.vertIndexes[(farVertTriIndex1 + 2) % 3] = newVertIndex0

    face0.edgeIndexes[(farVertTriIndex0 + 1) % 3] = newEdgeIndex0
    face1.edgeIndexes[(farVertTriIndex1 + 1) % 3] = newEdgeIndex1
    face0.edgeIndexes[(farVertTriIndex0 + 2) % 3] = edgeIndex
    face1.edgeIndexes[(farVertTriIndex1 + 2) % 3] = edgeIndex

    return true
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

    val tris = mutableListOf<MutTri>()
    for (tri in this.tris) {
        val edge0 = this.edges[tri.edgeIndexes[0]]
        val edge1 = this.edges[tri.edgeIndexes[1]]
        val edge2 = this.edges[tri.edgeIndexes[2]]

        val getEdgeVert0: (Int) -> Int = if (tri.vertIndexes[0] == edge0.vertIndexes[0]) ({
            edge0.subdividedVertexIndexes[it]
        }) else ({
            edge0.subdividedVertexIndexes[degree - 2 - it]
        })

        val getEdgeVert1: (Int) -> Int = if (tri.vertIndexes[1] == edge1.vertIndexes[0]) ({
            edge1.subdividedVertexIndexes[it]
        }) else ({
            edge1.subdividedVertexIndexes[degree - 2 - it]
        })

        val getEdgeVert2: (Int) -> Int = if (tri.vertIndexes[0] == edge2.vertIndexes[0]) ({
            edge2.subdividedVertexIndexes[it]
        }) else ({
            edge2.subdividedVertexIndexes[degree - 2 - it]
        })

        val faceVerts = listOf<Int>(tri.vertIndexes[0])
            .plus((0..<edge0.subdividedVertexIndexes.size).map(getEdgeVert0))
            .plus(tri.vertIndexes[1])
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
        faceVerts.add(tri.vertIndexes[2])

        val getEdgeEdge0: (Int) -> Int = if (tri.vertIndexes[0] == edge0.vertIndexes[0]) ({
            edge0.subdividedEdgeIndexes[it]
        }) else ({
            edge0.subdividedEdgeIndexes[degree - 1 - it]
        })

        val getEdgeEdge1: (Int) -> Int = if (tri.vertIndexes[1] == edge1.vertIndexes[0]) ({
            edge1.subdividedEdgeIndexes[it]
        }) else ({
            edge1.subdividedEdgeIndexes[degree - 1 - it]
        })

        val getEdgeEdge2: (Int) -> Int = if (tri.vertIndexes[0] == edge2.vertIndexes[0]) ({
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
                val subTri = MutTri(
                    mutableListOf(
                        faceVerts[vertIndex],
                        faceVerts[vertIndex + 1],
                        faceVerts[vertIndex + degree - i + 1]
                    ),
                    mutableListOf(faceEdges0[edgeIndex], faceEdges1[edgeIndex], faceEdges2[edgeIndex])
                )
                (0..2).forEach {
                    verts[subTri.vertIndexes[it]].triIndexes.add(tris.size)
                    edges[subTri.edgeIndexes[it]].triIndexes.add(tris.size)
                }
                tris.add(subTri)
                vertIndex += 1
                edgeIndex += 1
            }
            vertIndex += 1
        }

        vertIndex = 1
        edgeIndex = 0
        for (i in 1..<degree) {
            (1..<(degree - i + 1)).forEach {
                val subTri = MutTri(
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
                    verts[subTri.vertIndexes[it]].triIndexes.add(tris.size)
                    edges[subTri.edgeIndexes[it]].triIndexes.add(tris.size)
                }
                tris.add(subTri)
                vertIndex += 1
                edgeIndex += 1
            }
            vertIndex += 2
            edgeIndex += 1
        }
    }

    return MutMesh(verts, edges, tris)
}