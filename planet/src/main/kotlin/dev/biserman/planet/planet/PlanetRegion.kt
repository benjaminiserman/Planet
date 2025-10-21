package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.TrackedMutableSet
import dev.biserman.planet.utils.TrackedMutableSet.Companion.toTracked
import dev.biserman.planet.utils.memo
import godot.core.Plane
import godot.core.Vector3
import kotlin.math.PI
import kotlin.math.absoluteValue

class PlanetRegion(
    val planet: Planet,
    var tiles: TrackedMutableSet<PlanetTile> = mutableSetOf<PlanetTile>().toTracked()
) {
    constructor(planet: Planet, tiles: Iterable<PlanetTile>) : this(planet, tiles.toMutableSet().toTracked())
    constructor(planet: Planet, tiles: Sequence<PlanetTile>) : this(planet, tiles.toMutableSet().toTracked())

    @get:JsonIgnore
    val border by memo({ tiles.mutationCount }) {
        tiles.flatMap { planetTile ->
            planetTile.tile.borders.filter { border ->
                planet.getTile(
                    border.oppositeTile(
                        planetTile.tile
                    )
                ) !in tiles
            }
        }
    }

    val center by memo({ tiles.mutationCount }) {
        tiles.fold(Vector3.ZERO) { sum, tile -> sum + tile.tile.position }.normalized()
    }

    val edgeTiles by memo({ tiles.mutationCount }) {
        tiles.filter { tile ->
            tile.neighbors.any { it !in tiles }
        }
    }

    fun toTopology(): Topology = Topology(
        tiles.map { it.tile },
        tiles.flatMap { it.tile.borders },
        tiles.flatMap { it.tile.corners }
    )

    fun <T> calculateNeighborLengths(
        planetTileFn: (Tile) -> PlanetTile = { planet.getTile(it) },
        getFn: (PlanetTile) -> T
    ): Map<T, Double> {
        val neighborsBorderLengths = mutableMapOf<T, Double>()

        fun Border.oppositeTile(tile: PlanetTile) = planetTileFn(this.oppositeTile(tile.tile))
        val thisValue = getFn(tiles.first())

        for (tile in tiles) {
            val neighborBorders =
                tile.tile.borders.filter { getFn(it.oppositeTile(tile)) != thisValue }
            for (border in neighborBorders) {
                val neighbor = getFn(border.oppositeTile(tile))
                neighborsBorderLengths[neighbor] = (neighborsBorderLengths[neighbor] ?: 0.0) + border.length
            }
        }

        return neighborsBorderLengths
    }

    fun voronoi(points: List<Vector3>, warp: (Vector3) -> Vector3 = { it }): List<PlanetRegion> {
        val remainingTiles =
            tiles.shuffled(planet.random).toMutableList()

        val regions = points.associateWith { PlanetRegion(planet) }

        for (planetTile in remainingTiles) {
            val warpedPosition = warp(planetTile.tile.position)
            val closestRegion = regions.minBy { warpedPosition.distanceTo(warp(it.key)) }.value
            closestRegion.tiles.add(planetTile)
        }

        return regions.values.toList()
    }

    fun <T> calculateEdgeDepthMap(classify: (PlanetTile) -> T): Map<PlanetTile, Int> {
        val edgeTiles = tiles.filter { tile ->
            val classification = classify(tile)
            tile.neighbors.any { classify(it) != classification }
        }
        val results = mutableMapOf<PlanetTile, Int>()
        val queue = ArrayDeque<Pair<PlanetTile, Int>>()
        queue.addAll(edgeTiles.map { it to 0 })

        while (queue.isNotEmpty()) {
            val (current, depth) = queue.removeFirst()
            if (current !in results) {
                results[current] = depth
                val classification = classify(current)
                current.neighbors
                    .filter { classify(it) == classification }
                    .filter { it !in results }
                    .forEach { queue.add(it to depth + 1) }
            }
        }

        return results
    }

    fun <T> floodFillGroupBy(
        planetTileFn: ((Tile) -> PlanetTile) = { planet.getTile(it) }, keyFn: (PlanetTile) -> T
    ): Map<T, List<PlanetRegion>> {
        val visited = mutableSetOf<PlanetTile>()
        val results = mutableMapOf<T, MutableList<PlanetRegion>>()

        for (tile in tiles) {
            if (visited.contains(tile)) {
                continue
            }
            val tileValue = keyFn(tile)
            val found = tile.floodFill(planetTileFn = planetTileFn, planetRegion = this) { tile, _ ->
                keyFn(tile) == tileValue
            }
            visited.addAll(found)
            results[tileValue] = (results[tileValue] ?: mutableListOf()).also { it.add(PlanetRegion(planet, found)) }
        }

        return results
    }

    fun raycastClockwise(tile: PlanetTile, normal: Vector3) = sequence {
        val results = mutableSetOf<PlanetTile>()
        var current = tile
        while (current in tiles && current !in results) {
            results.add(current)
            yield(current)
            val rightward = current.tile.position.cross(-normal)
            current = current.neighbors
                .filter { (it.tile.position - current.tile.position).dot(rightward) >= 0 }
                .minBy {
                    val toPoint = it.tile.position - tile.tile.position
                    val projection = toPoint.dot(-normal)
                    projection.absoluteValue
                }
        }
    }

    fun sortedClockwiseFrom(tile: PlanetTile, normal: Vector3) = tiles.sortedBy {
        val normalDistance = normal.dot(tile.tile.position)
        val projectedPoint = it.tile.position - normal * (normal.dot(it.tile.position) - normalDistance)
        val referenceVector = tile.tile.position - normal * (normal.dot(tile.tile.position) - normalDistance)
        val angle = projectedPoint.angleTo(referenceVector)
        if (projectedPoint.cross(referenceVector).dot(normal) < 0) 2 * PI - angle else angle
    }

    fun parallelCross(tile: PlanetTile, normal: Vector3) = sequence {
        yieldAll(raycastClockwise(tile, normal))
        yieldAll(raycastClockwise(tile, -normal))
    }

    operator fun plus(other: PlanetRegion): PlanetRegion {
        if (planet != other.planet) {
            throw IllegalArgumentException("Can't add regions from different planets")
        }

        return PlanetRegion(planet, tiles + other.tiles)
    }
}