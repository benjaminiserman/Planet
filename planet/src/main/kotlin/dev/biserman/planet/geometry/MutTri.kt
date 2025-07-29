package dev.biserman.planet.geometry

import godot.core.Vector3

data class MutTri(
    val vertIndexes: MutableList<Int> = mutableListOf(),
    val edgeIndexes: MutableList<Int> = mutableListOf()
) {
    fun centroid(mesh: MutMesh): Vector3 =
        centroid(vertIndexes.map { mesh.verts[it].position })

    fun oppositeVertIndex(edge: MutEdge): Int = when {
        this.vertIndexes[0] != edge.vertIndexes[0] && this.vertIndexes[0] != edge.vertIndexes[1] -> 0
        this.vertIndexes[1] != edge.vertIndexes[0] && this.vertIndexes[1] != edge.vertIndexes[1] -> 1
        this.vertIndexes[2] != edge.vertIndexes[0] && this.vertIndexes[2] != edge.vertIndexes[1] -> 2
        else -> throw Error("Cannot find node of given tri that is not also a node of given edge.")
    }

    fun area(mesh: MutMesh): Double {
        val (p0, p1, p2) = vertIndexes.map { mesh.verts[it].position }
        return triArea(p0, p1, p2)
    }
}
