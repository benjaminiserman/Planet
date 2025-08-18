package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Topology

class Planet(val topology: Topology) {
    val random by lazy { Main.random }
    val planetTiles = topology.tiles.associateWith { PlanetTile(this, it) }
    @Suppress("JoinDeclarationAndAssignment")
    val tectonicPlates: MutableList<TectonicPlate>

    init {
        tectonicPlates = Tectonics.seedPlates(this, random.nextInt(15, 20))
        Tectonics.voronoiPlates(this)
        Tectonics.assignDensities(this)
    }
}