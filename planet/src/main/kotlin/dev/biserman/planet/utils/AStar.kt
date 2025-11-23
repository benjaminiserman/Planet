package dev.biserman.planet.utils

import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import godot.global.GD
import java.util.PriorityQueue

data class Path<T>(val nodes: List<T>, val distance: Double)
class QueueNode<T>(val node: T, val priority: Double)

object AStar {
    fun <T> path(
        start: T,
        goal: (T) -> Boolean,
        heuristic: (T) -> Double,
        distance: (T, T) -> Double,
        neighbors: (T) -> Iterable<T>,
        searchExhaustively: Boolean = false,
        cutoff: ((T) -> Boolean) = { false }
    ): Path<T> {
        val heuristicResult = heuristic(start)
        val openQueue = PriorityQueue<QueueNode<T>>(Comparator.comparing { it.priority })
            .also { it.add(QueueNode(start, heuristicResult)) }
        val openSet = mutableMapOf<T, Double>(start to heuristicResult)
        val cameFrom = mutableMapOf<T, T>()
        val bestPathTo = mutableMapOf<T, Double>(start to 0.0)
        val distanceFromGoalEstimate = mutableMapOf<T, Double>(start to heuristicResult)
        var bestPathThusFar = Path<T>(emptyList(), Double.POSITIVE_INFINITY)

        fun reconstructPath(current: T): Path<T> {
            val path = mutableListOf<T>()
            var node = current
            while (node in cameFrom) {
                path.add(0, node)
                node = cameFrom[node]!!
            }
            path.add(0, start)
            return Path(path, bestPathTo[current] ?: 0.0)
        }

        while (openQueue.isNotEmpty()) {
            val next = openQueue.poll()
            val current = next.node
            val gotPriority = next.priority

            if (gotPriority > (distanceFromGoalEstimate[current] ?: Double.POSITIVE_INFINITY)) {
                continue
            }

            if (goal(current)) {
                val path = reconstructPath(current)

                if (!searchExhaustively) {
                    return path
                }

                if (path.distance < bestPathThusFar.distance) {
                    bestPathThusFar = path
                }
            }

            if (cutoff(current)) {
                continue
            }

            for (neighbor in neighbors(current)) {
                val distanceFromStart = (bestPathTo[current] ?: Double.POSITIVE_INFINITY) + distance(current, neighbor)
                if (distanceFromStart < (bestPathTo[neighbor] ?: Double.POSITIVE_INFINITY)) {
                    cameFrom[neighbor] = current
                    bestPathTo[neighbor] = distanceFromStart
                    val distanceEstimate = distanceFromStart + heuristic(neighbor)
                    distanceFromGoalEstimate[neighbor] = distanceEstimate

                    val neighborPriority = openSet[neighbor]
                    if (neighborPriority == null || neighborPriority > distanceEstimate) {
                        println("$neighbor: $distanceFromStart, ${distanceFromGoalEstimate[neighbor]}")
                        openSet[neighbor] = distanceEstimate
                        openQueue.add(QueueNode(neighbor, distanceEstimate))
                    }
                }
            }
        }

        return bestPathThusFar
    }
}