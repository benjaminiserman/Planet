package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.memo

class Planet(val topology: Topology) {
    val random by lazy { Main.random }
    val noise by lazy { Main.noise }
    var planetTiles = topology.tiles.associateWith { PlanetTile(this, it) }
    @Suppress("JoinDeclarationAndAssignment")
    var tectonicPlates: MutableList<TectonicPlate>
    var subductionZones: MutableSet<PlanetTile> = mutableSetOf()
    val subductionTiles by memo({ tectonicAge }) { subductionZones.map { it.tile }}
    var divergenceZones: MutableSet<PlanetTile> = mutableSetOf()
    var tectonicAge = 4000 + random.nextInt(1000)

    val seaLevel: Double = 0.0

    init {
        tectonicPlates = Tectonics.seedPlates(this, random.nextInt(35, 50))
        Tectonics.voronoiPlates(this)
        Tectonics.assignDensities(this)
    }
}