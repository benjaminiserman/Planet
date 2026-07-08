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
            if (isEmpty()) return emptyList()

            val pointsToEdges = mutableMapOf<Corner, MutableList<Border>>()
            for (border in this) {
                for (corner in border.corners) {
                    pointsToEdges.computeIfAbsent(corner) { mutableListOf() }.add(border)
                }
            }

            if (pointsToEdges.values.any { it.size > 2 }) {
                throw IllegalStateException("List of borders contains an invalid path")
            }

            val remainingEdges = toMutableSet()
            val paths = mutableListOf<Path>()

            while (remainingEdges.isNotEmpty()) {
                val componentEdges = mutableSetOf<Border>()
                val pendingEdges = ArrayDeque<Border>().apply { add(remainingEdges.first()) }
                while (pendingEdges.isNotEmpty()) {
                    val edge = pendingEdges.removeFirst()
                    if (!componentEdges.add(edge)) continue
                    edge.corners.flatMap { pointsToEdges.getValue(it) }
                        .filter { it in remainingEdges && it !in componentEdges }
                        .forEach(pendingEdges::addLast)
                }

                val start = componentEdges.flatMap { it.corners }
                    .firstOrNull { corner -> pointsToEdges.getValue(corner).count { it in componentEdges } == 1 }
                    ?: componentEdges.first().corners.first()
                val points = mutableListOf(start)
                var current = start
                var isLoop = false

                while (true) {
                    val edge = pointsToEdges.getValue(current).firstOrNull { it in componentEdges && it in remainingEdges }
                        ?: break
                    remainingEdges.remove(edge)
                    val next = edge.oppositeCorner(current)
                    if (next == start) {
                        isLoop = true
                        break
                    }
                    points.add(next)
                    current = next
                }

                paths.add(Path(points, isLoop))
            }

            return paths
        }
    }
}
