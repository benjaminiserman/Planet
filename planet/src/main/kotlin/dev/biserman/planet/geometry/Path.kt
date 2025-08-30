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



    fun toMesh(): MutMesh = addToMesh(MutMesh())

    fun addToMesh(mutMesh: MutMesh): MutMesh {
        val startCount = mutMesh.verts.size

        mutMesh.verts.addAll(points.map { MutVertex(it.position) })

        for (i in 1..<points.size) {
            mutMesh.edges.add(MutEdge(mutableListOf(i - 1 + startCount, i + startCount)))
        }

        if (isLoop) {
            mutMesh.edges.add(MutEdge(mutableListOf(points.size - 1 + startCount, startCount)))
        }

        for (i in 2..<points.size) {
            mutMesh.tris.add(MutTri(mutableListOf(i - 2 + startCount, i - 1 + startCount, i + startCount)))
        }

        return mutMesh
    }

    // no idea if this works
    fun isClockwise(observeDirection: Vector3): Boolean {
        val u = points[1].position - points[0].position
        val v = points[2].position - points[0].position

        val normal = u.cross(v).normalized()
        return observeDirection.dot(normal) >= 0
    }

    companion object {
        fun (List<Path>).toMesh(): MutMesh {
            val mutMesh = MutMesh()
            for (path in this) {
                path.addToMesh(mutMesh)
            }

            return mutMesh
        }

        fun (List<Border>).toPaths(): List<Path> {
            val pointsToEdges = mutableMapOf<Corner, MutableList<Border>>()
            for (border in this) {
                for (corner in border.corners) {
                    pointsToEdges.computeIfAbsent(corner) { mutableListOf() }.add(border)
                }
            }

            if (pointsToEdges.values.any { it.size > 2 }) {
                return listOf()
                throw IllegalStateException("List of borders contains an invalid path")
            }

            val visitedEdges = mutableSetOf<Border>()
            val visitedCorners = mutableSetOf<Corner>()
            val paths = mutableListOf<Path>()

            while (visitedEdges.size < pointsToEdges.size) {
                val start =
                    pointsToEdges.keys.firstOrNull { pointsToEdges[it]!!.size == 1 && it !in visitedCorners }
                        ?: pointsToEdges.keys.first { it !in visitedCorners }
                val path = mutableListOf(start)
                val theseVisitedEdges = mutableSetOf<Border>()
                var current = start
                while (true) {
                    val newEdge = pointsToEdges[current]!!.firstOrNull { it !in theseVisitedEdges } ?: break
                    current = newEdge.oppositeCorner(current)
                    path.add(current)
                    visitedCorners.add(current)
                    theseVisitedEdges.add(newEdge)
                }
                visitedEdges.addAll(theseVisitedEdges)
                val isLoop = theseVisitedEdges.all { it.corners.size == 2 }
                paths.add(Path(path, isLoop))
            }

            return paths.toList()
        }
    }
}