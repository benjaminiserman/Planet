package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Topology

class Planet(val topology: Topology) {
    val random by lazy { Main.random }
    val planetTiles = topology.tiles.associateWith { PlanetTile(it) }
    val tectonicPlates: MutableList<TectonicPlate> = Tectonics.seedPlates(this, random.nextInt(10, 20))

    init {
        Tectonics.voronoiPlates(this)
        Tectonics.assignStartingElevation(this)
    }
}