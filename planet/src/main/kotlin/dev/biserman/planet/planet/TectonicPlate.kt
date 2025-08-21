package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.Mat3
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.memo
import dev.biserman.planet.utils.randomHsv
import godot.core.Color
import godot.core.Vector3
import kotlin.math.absoluteValue

class TectonicPlate(val planet: Planet) {
    val biomeColor = Color.fromHsv(Main.random.nextDouble(0.15, 0.4), Main.random.nextDouble(0.7, 0.9), 0.5, 1.0)
    val debugColor = Color.randomHsv()
    val region = PlanetRegion(planet)
    val tiles by region::tiles
    var density: Float? = null

    val torque by memo({ tiles.mutationCount }) {
        tiles.fold(Vector3.ZERO) { sum, tile ->
            sum + tile.tile.position.cross(tile.plateBoundaryForces)
        }
    }

    val eulerPole by memo({ tiles.mutationCount }) {
        var inertiaTensor = Mat3.zero()
        for (tile in tiles) {
            val outer = Mat3.fromOuter(tile.tile.position, tile.tile.position)
            val contribution = Mat3.identity() - outer
            inertiaTensor += contribution * tile.tile.area
        }

        inertiaTensor.inverse() * torque
    }

    fun calculateNeighborLengths(): Map<TectonicPlate, Double> {
        val neighborsBorderLengths = mutableMapOf<TectonicPlate, Double>()

        fun Border.oppositeTile(tile: PlanetTile) = planet.planetTiles[this.oppositeTile(tile.tile)]?.tile
        fun Tile.planetTile() = planet.planetTiles[this]!!

        for (tile in tiles) {
            val neighborBorders =
                tile.tile.borders.filter { it.oppositeTile(tile) != this }
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
}