package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.memo

class Planet(val topology: Topology) {
    val random by lazy { Main.random }
    val noise by lazy { Main.noise }
    var planetTiles = topology.tiles.associateWith { PlanetTile(this, it) }

    @Suppress("JoinDeclarationAndAssignment")
    var tectonicPlates: MutableList<TectonicPlate>
    var convergenceZones: MutableMap<Tile, ConvergenceZone> = mutableMapOf()
    var divergenceZones: MutableMap<Tile, DivergenceZone> = mutableMapOf()

    //    var tectonicAge = 4000 + random.nextInt(1000)
    var tectonicAge = 0

    val seaLevel: Double = 0.0

    val oldestCrust by memo({ tectonicAge }) { planetTiles.values.minOf { it.formationTime } }
    val youngestCrust by memo({ tectonicAge }) { planetTiles.values.maxOf { it.formationTime } }

    init {
        planetTiles.values.forEach {
            it.planetInit()
        }
        tectonicPlates = Tectonics.seedPlates(this, random.nextInt(15, 20))
        Tectonics.voronoiPlates(this)
        Tectonics.assignDensities(this)
    }
}