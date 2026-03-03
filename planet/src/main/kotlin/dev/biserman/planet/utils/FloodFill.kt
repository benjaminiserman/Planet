package dev.biserman.planet.utils

fun <T> floodFill(starts: List<T>, neighborFn: (T) -> List<T>): Set<T> {
    val visited = mutableSetOf<T>()
    val queue = ArrayDeque<T>()
    queue.addAll(starts)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (current !in visited) {
            visited.add(current)
            neighborFn(current)
                .forEach { queue.add(it) }
        }
    }

    return visited
}

fun <T> floodFill(start: T, neighborFn: (T) -> List<T>): Set<T> = floodFill(listOf(start), neighborFn)

fun <T> floodFillPartitionForest(nodes: List<T>, neighborFn: (T) -> List<T>): Map<T, Set<T>> {
    val visited = mutableSetOf<T>()
    val results = mutableMapOf<T, Set<T>>()

    nodes.forEach { node ->
        if (node !in visited) {
            results[node] = floodFill(node, neighborFn)
            visited.addAll(results[node]!!)
        }
    }

    return results
}