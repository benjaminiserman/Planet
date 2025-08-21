package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Topology
import godot.global.GD

class Planet(val topology: Topology) {
    val random by lazy { Main.random }
    val planetTiles = topology.tiles.associateWith { PlanetTile(this, it) }
    @Suppress("JoinDeclarationAndAssignment")
    val tectonicPlates: MutableList<TectonicPlate>

    val seaLevel: Double = 0.0

    init {
        tectonicPlates = Tectonics.seedPlates(this, random.nextInt(35, 50))
        Tectonics.voronoiPlates(this)
        Tectonics.assignDensities(this)
    }
}