package dev.biserman.planet.planet

import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.TrackedMutableSet.Companion.toTracked
import dev.biserman.planet.utils.memo
import godot.core.Vector3

class PlanetRegion(
    val planet: Planet,
    var tiles: MutableSet<PlanetTile> = mutableSetOf<PlanetTile>().toTracked()
) {
    val border by memo({ planet.tectonicAge }) {
        tiles.flatMap { planetTile ->
            planetTile.tile.borders.filter { border ->
                planet.planetTiles[border.oppositeTile(
                    planetTile.tile
                )] !in tiles
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
}