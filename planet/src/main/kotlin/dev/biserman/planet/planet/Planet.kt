package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Topology
import godot.global.GD

class Planet(val topology: Topology) {
    val random by lazy { Main.random }
    val noise by lazy { Main.noise }
    val planetTiles = topology.tiles.associateWith { PlanetTile(this, it) }
    @Suppress("JoinDeclarationAndAssignment")
    var tectonicPlates: MutableList<TectonicPlate>
    var subductionZones: MutableList<PlanetTile> = mutableListOf()
    var divergenceZones: MutableList<PlanetTile> = mutableListOf()
    var tectonicAge = 4000 + random.nextInt(1000)

    val seaLevel: Double = 0.0

    init {
        tectonicPlates = Tectonics.seedPlates(this, random.nextInt(35, 50))
        Tectonics.voronoiPlates(this)
        Tectonics.assignDensities(this)
    }
}