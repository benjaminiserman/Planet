package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.eulerPole
import dev.biserman.planet.planet.Tectonics.random
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.memo
import dev.biserman.planet.utils.randomHsv
import godot.core.Color
import godot.core.Vector3
import godot.global.GD

class TectonicPlate(val planet: Planet, val age: Int = 0, val region: PlanetRegion = PlanetRegion(planet)) {
    val biomeColor = Color.fromHsv(Main.random.nextDouble(0.15, 0.4), Main.random.nextDouble(0.7, 0.9), 0.5, 1.0)
    val debugColor = Color.randomHsv()
    val tiles by region::tiles
    var density: Double? = null

    val basalDrag = 0.33
    var torque = Vector3.ZERO

    val formationTime = planet.tectonicAge

    val eulerPole by memo({ torque }) {
        try {
            eulerPole(
                torque,
                tiles.map { tile -> tile.tile.position to tile.tile.area })
                .let {
                    it.normalized() * it.length().coerceIn(0.03, 0.07)
                }
        } catch (e: Exception) {
            GD.print("Failed to calculate euler pole: ${tiles.size} ${torque.length()}")
            throw e
        }
    }

    val edgeTiles by memo({ planet.tectonicAge }) {
        tiles.filter { tile ->
            tile.tile.borders.any { border ->
                tile.oppositeTile(border)?.tectonicPlate != this
            }
        }
    }

    val area by memo({ planet.tectonicAge }) { tiles.sumOf { it.tile.area } }

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
        val tilesToClean =
            region.tiles.filter { planet.planetTiles[it.tile] != it || planet.planetTiles[it.tile]!!.tectonicPlate != this }
        for (tile in tilesToClean) {
            region.tiles.remove(tile)
        }
    }

    fun rift(): List<TectonicPlate> {
        planet.tectonicPlates.remove(this)

        val warpNoise = VectorWarpNoise(random.nextInt(), 0.75f)
        val warp: (Vector3) -> Vector3 = { warpNoise.warp(it, 0.5) }

        val points = region.tiles.shuffled(random).take(random.nextInt(3, 5)).map { warp(it.tile.position) }
        GD.print("Rifting $debugColor in ${points.size}")
        val newPlates = region.voronoi(points, warp).map { region ->
            val plate = TectonicPlate(planet, planet.tectonicAge, region)
            plate.tiles.forEach { it.tectonicPlate = plate }
            plate
        }

        planet.tectonicPlates.addAll(newPlates)
        return newPlates
    }
}