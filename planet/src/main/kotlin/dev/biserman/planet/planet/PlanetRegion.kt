package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.TrackedMutableSet.Companion.toTracked
import dev.biserman.planet.utils.memo
import godot.core.Vector3

class PlanetRegion(
    val planet: Planet,
    var tiles: MutableSet<PlanetTile> = mutableSetOf<PlanetTile>().toTracked()
) {
    constructor(planet: Planet, tiles: Collection<PlanetTile>) : this(planet, tiles.toMutableSet())

    @get:JsonIgnore
    val border by memo({ planet.tectonicAge }) {
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

    val center by memo({ planet.tectonicAge }) {
        tiles.fold(Vector3.ZERO) { sum, tile -> sum + tile.tile.position }.normalized()
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
        planetTileFn: ((Tile) -> PlanetTile)? = null, keyFn: (PlanetTile) -> T
    ): Map<T, List<PlanetRegion>> {
        val visited = mutableSetOf<PlanetTile>()
        val results = mutableMapOf<T, MutableList<PlanetRegion>>()

        for (tile in tiles) {
            if (visited.contains(tile)) {
                continue
            }
            val tileValue = keyFn(tile)
            val found = if (planetTileFn == null) {
                tile.floodFill { keyFn(it) == tileValue }
            } else {
                tile.floodFill(planetTileFn = planetTileFn) {
                    keyFn(it) == tileValue
                }
            }
            visited.addAll(found)
            results[tileValue] = (results[tileValue] ?: mutableListOf()).also { it.add(PlanetRegion(planet, found)) }
        }

        return results
    }
}