package dev.biserman.planet.planet

import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.cache

class PlanetRegion(val planet: Planet, var tiles: MutableList<PlanetTile> = mutableListOf()) {

    val border by cache {

    }

    fun toTopology(): Topology = Topology(
        tiles.map { it.tile },
        tiles.flatMap { it.tile.borders },
        tiles.flatMap { it.tile.corners }
    )
}