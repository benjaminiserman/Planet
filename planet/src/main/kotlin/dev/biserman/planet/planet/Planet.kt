package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.toWeightedBag

class Planet(val topology: Topology) {
    val tiles = mutableListOf<PlanetTile>()
    val tectonicPlates = mutableListOf<TectonicPlate>()
    val random = Main.random

    fun addTiles(topographicTiles: Iterable<Tile>) {
        tiles.addAll(topographicTiles.map { PlanetTile(it) })
    }


    fun generateRandomTectonicPlates(): MutableList<TectonicPlate> {
        val plateCount = random.nextInt(10, 20)
        val plates = (1..plateCount).map { TectonicPlate(generationWeight = random.nextInt(1, 10)) }
        val plateEdges = plates.associate { it to mutableListOf<PlanetTile>() }
        val plateBag = plates.toWeightedBag(random) { it.generationWeight }

        val remainingTiles = tiles.shuffled(random).toMutableList()
        for (plate in plates) {
            val selectedTile = remainingTiles.first()
            selectedTile.tectonicPlate = plate
            remainingTiles.remove(selectedTile)
            plate.tiles.add(selectedTile)
            plateEdges[plate]!!.add(selectedTile)
        }


    }
}