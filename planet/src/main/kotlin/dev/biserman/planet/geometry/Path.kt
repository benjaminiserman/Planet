package dev.biserman.planet.geometry

import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Corner
import godot.core.Vector3

// maintains invariant that points are unique and that adjacent point share an edge
// if isLoop is true, then the first and last points also share an edge
class Path {
    val points: List<Corner>
    var isLoop = false
        private set
    private constructor(points: List<Corner>, isLoop: Boolean) {
        this.points = points
        this.isLoop = isLoop
    }

    fun reversed(): Path = Path(points.reversed(), isLoop)

    fun toMesh(): MutMesh {
        val mutVerts = points.map { MutVertex(it.position) }.toMutableList()
        val mutEdges = mutableListOf<MutEdge>()
        val mutTris = mutableListOf<MutTri>()

        for (i in 1..<points.size) {
            mutEdges.add(MutEdge(mutableListOf(i - 1, i)))
        }

        if (isLoop) {
            mutEdges.add(MutEdge(mutableListOf(points.size - 1, 0)))
        }

        for (i in 2..<points.size) {
            mutTris.add(MutTri(mutableListOf(i - 2, i - 1, i)))
        }

        return MutMesh(mutVerts, mutEdges, mutTris)
    }

    // no idea if this works
    fun isClockwise(observeDirection: Vector3): Boolean {
        val u = points[1].position - points[0].position
        val v = points[2].position - points[0].position

        val normal = u.cross(v).normalized()
        return observeDirection.dot(normal) >= 0
    }

    companion object {
        fun (List<Border>).toPath(): Path {
            val pointsToEdges = mutableMapOf<Corner, MutableList<Border>>()
            for (border in this) {
                for (corner in border.corners) {
                    pointsToEdges.computeIfAbsent(corner) { mutableListOf() }.add(border)
                }
            }

            if (pointsToEdges.values.any { it.size > 2 } || pointsToEdges.values.filter { it.size < 2 }.size > 2) {
                throw IllegalStateException("List of borders is not a valid path")
            }

            val isLoop = pointsToEdges.values.all { it.size == 2 }

            val start = pointsToEdges.keys.firstOrNull { pointsToEdges[it]!!.size == 1 } ?: pointsToEdges.keys.first()
            val path = mutableListOf(start)
            val visitedEdges = mutableSetOf<Border>()
            var current = start
            while (true) {
                val newEdge = pointsToEdges[current]!!.firstOrNull { it !in visitedEdges } ?: break
                current = newEdge.oppositeCorner(current)
                path.add(current)
                visitedEdges.add(newEdge)
            }

            return Path(path, isLoop)
        }
    }
}