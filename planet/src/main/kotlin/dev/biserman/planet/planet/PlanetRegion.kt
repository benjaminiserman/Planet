package dev.biserman.planet.planet

import dev.biserman.planet.geometry.MutMesh
import dev.biserman.planet.geometry.MutVertex
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.TrackedMutableSet
import dev.biserman.planet.utils.TrackedMutableSet.Companion.toTracked
import dev.biserman.planet.utils.memo

class PlanetRegion(
    val planet: Planet,
    var tiles: TrackedMutableSet<PlanetTile> = mutableSetOf<PlanetTile>().toTracked()
) {

    val border by memo({ tiles.mutationCount }) {
        tiles.flatMap { planetTile ->
            planetTile.tile.borders.filter { border ->
                planet.planetTiles[border.oppositeTile(
                    planetTile.tile
                )] !in tiles
            }
        }
    }

    fun toTopology(): Topology = Topology(
        tiles.map { it.tile },
        tiles.flatMap { it.tile.borders },
        tiles.flatMap { it.tile.corners }
    )
}