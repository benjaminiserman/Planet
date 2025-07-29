package dev.biserman.planet.planet

data class TectonicPlate(var generationWeight: Int) {
    var tiles: MutableList<PlanetTile> = mutableListOf()

}