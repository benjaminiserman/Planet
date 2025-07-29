package dev.biserman.planet.planet

import dev.biserman.planet.topology.Tile

class PlanetTile(var tile: Tile) {
    //    var elevation = Main.noise.getNoise3dv(tile.averagePosition * 100)
    //    var elevation = sin(tile.averagePosition.y * 90)
    var elevation = 0.0
    var temperature = 0.0
    var moisture = 0.0
    var tectonicPlate: TectonicPlate? = null
}