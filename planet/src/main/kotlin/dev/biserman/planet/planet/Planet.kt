package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.memo
import godot.core.Vector3

class Planet(val topology: Topology) {
    val random by lazy { Main.random }
    val noise by lazy { Main.noise }
    var planetTiles = topology.tiles.associateWith { PlanetTile(this, it) }

    @Suppress("JoinDeclarationAndAssignment")
    var tectonicPlates: MutableList<TectonicPlate>
    var subductionZones: MutableMap<Tile, List<TectonicPlate>> = mutableMapOf()
    var subductedPoints: List<Triple<Vector3, TectonicPlate, Double>> = mutableListOf()
    var divergenceZones: MutableMap<Tile, List<TectonicPlate>> = mutableMapOf()
    var tectonicAge = 4000 + random.nextInt(1000)

    val seaLevel: Double = 0.0

    init {
        planetTiles.values.forEach {
            it.planetInit()
        }
        tectonicPlates = Tectonics.seedPlates(this, random.nextInt(15, 20))
        Tectonics.voronoiPlates(this)
        Tectonics.assignDensities(this)
    }
}