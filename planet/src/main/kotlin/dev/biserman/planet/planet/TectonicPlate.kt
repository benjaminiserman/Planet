package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.eulerPole
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.memo
import dev.biserman.planet.utils.randomHsv
import godot.core.Color
import godot.core.Vector3

class TectonicPlate(val planet: Planet) {
    val biomeColor = Color.fromHsv(Main.random.nextDouble(0.15, 0.4), Main.random.nextDouble(0.7, 0.9), 0.5, 1.0)
    val debugColor = Color.randomHsv()
    val region = PlanetRegion(planet)
    val tiles by region::tiles
    var density: Float? = null

    val basalDrag = 0.5
    var torque = Vector3.ZERO

    val eulerPole by memo({ torque }) {
        eulerPole(
            torque,
            tiles.map { tile -> tile.tile.position to 1.0 })
    }

    val edgeTiles by memo({ planet.tectonicAge }) {
        tiles.filter { tile ->
            tile.tile.borders.any { border ->
                tile.oppositeTile(border)?.tectonicPlate != this
            }
        }
    }

    fun calculateNeighborLengths(): Map<TectonicPlate, Double> {
        val neighborsBorderLengths = mutableMapOf<TectonicPlate, Double>()

        fun Border.oppositeTile(tile: PlanetTile) = planet.planetTiles[this.oppositeTile(tile.tile)]?.tile
        fun Tile.planetTile() = planet.planetTiles[this]!!

        for (tile in tiles) {
            val neighborBorders =
                tile.tile.borders.filter { it.oppositeTile(tile)!!.planetTile().tectonicPlate != this }
            for (border in neighborBorders) {
                val neighbor = border.oppositeTile(tile)!!.planetTile().tectonicPlate!!
                neighborsBorderLengths[neighbor] = (neighborsBorderLengths[neighbor] ?: 0.0) + border.length
            }
        }

        return neighborsBorderLengths
    }

    fun merge(other: TectonicPlate) {
        other.tiles.forEach { it.tectonicPlate = this }
        planet.tectonicPlates.remove(other)
    }

    fun clean() {
        val tilesToClean = region.tiles.filter { planet.planetTiles[it.tile] != it }
        for (tile in tilesToClean) {
            region.tiles.remove(tile)
        }
    }
}