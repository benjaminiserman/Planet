package dev.biserman.planet.planet.tectonics

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.eulerPole
import dev.biserman.planet.planet.DebugNameGenerator
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetRegion
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.tectonics.TectonicGlobals.riftSeparationStrength
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.memo
import dev.biserman.planet.utils.randomHsv
import godot.core.Color
import godot.core.Vector3
import godot.global.GD

@JsonIdentityInfo(
    generator = ObjectIdGenerators.IntSequenceGenerator::class,
    scope = TectonicPlate::class,
    property = "serId"
)
class TectonicPlate(
    val planet: Planet,
    val region: PlanetRegion = PlanetRegion(planet),
    var name: String = DebugNameGenerator.generateName(planet.random)
) {
    val biomeColor = Color.fromHsv(Main.debugRandom.nextDouble(0.15, 0.4), Main.debugRandom.nextDouble(0.7, 0.9), 0.5, 1.0)
    val debugColor = Color.randomHsv()

    @get:JsonIgnore
    val tiles by region::tiles
    var density: Double? = null

    var torque = Vector3.ZERO

    val formationTime = planet.tectonicAge
    val id = planet.nextPlateId++

    val eulerPole by memo({ torque }) {
        try {
            val unconstrainedPole = eulerPole(
                torque,
                tiles.map { tile -> tile.tile.position to tile.tile.area }
            )
            if (!unconstrainedPole.isFinite()) {
                GD.print("Discarding non-finite Euler pole for plate $id (${tiles.size} tiles)")
                Vector3.ZERO
            } else if (unconstrainedPole.lengthSquared() == 0.0) {
                Vector3.ZERO
            } else {
                unconstrainedPole.normalized() *
                    unconstrainedPole.length().coerceIn(0.0, planet.topology.averageRadius * 2.5)
            }
        } catch (e: Exception) {
            GD.print("Failed to calculate euler pole: ${tiles.size} ${torque.length()}")
            throw e
        }
    }

    @get:JsonIgnore
    val edgeTiles by memo({ planet.tectonicAge }) {
        tiles.filter { tile ->
            tile.tile.borders.any { border ->
                tile.oppositeTile(border).tectonicPlate != this
            }
        }
    }

    val area by memo({ planet.tectonicAge }) { tiles.sumOf { it.tile.area } }

    fun calculateNeighborLengths(): Map<TectonicPlate, Double> {
        val neighborsBorderLengths = mutableMapOf<TectonicPlate, Double>()

        fun Border.oppositeTile(tile: PlanetTile) = planet.getTile(this.oppositeTile(tile.tile)).tile
        fun Tile.planetTile() = planet.getTile(this)

        for (tile in tiles) {
            val neighborBorders =
                tile.tile.borders.filter { it.oppositeTile(tile).planetTile().tectonicPlate != this }
            for (border in neighborBorders) {
                val neighbor = border.oppositeTile(tile).planetTile().tectonicPlate!!
                neighborsBorderLengths[neighbor] = (neighborsBorderLengths[neighbor] ?: 0.0) + border.length
            }
        }

        return neighborsBorderLengths
    }

    fun mergeInto(other: TectonicPlate) {
        tiles.toList().forEach { it.tectonicPlate = other }
        planet.tectonicPlates.remove(this)
    }

    fun clean() {
        val tilesToClean =
            region.tiles.filter { planet.getTile(it.tile) != it || planet.getTile(it.tile).tectonicPlate != this }
        for (tile in tilesToClean) {
            region.tiles.remove(tile)
        }
    }

    fun rift(): List<TectonicPlate> {
        planet.tectonicPlates.remove(this)

        val warpNoise = VectorWarpNoise(planet.random.nextInt(), 0.75f)
        val warp: (Vector3) -> Vector3 = { warpNoise.warp(it, 0.5) }

        val points = region.tiles.shuffled(planet.random).take(planet.random.nextInt(3, 5)).map { warp(it.tile.position) }
        GD.print("Rifting $debugColor in ${points.size}")
        val riftRegions = region.voronoi(points, warp)
        val regionAreas = riftRegions.associateWith { riftRegion ->
            riftRegion.tiles.sumOf { it.tile.area }
        }
        val totalArea = regionAreas.values.sum()
        val separationAxes = riftRegions.associateWith { riftRegion ->
            val axis = region.center.cross(riftRegion.center)
            if (axis.lengthSquared() == 0.0) Vector3.ZERO else axis.normalized()
        }
        val meanSeparationAxis = separationAxes.entries.fold(Vector3.ZERO) { sum, (riftRegion, axis) ->
            sum + axis * (regionAreas.getValue(riftRegion) / totalArea)
        }

        val newPlates = riftRegions.map { riftRegion ->
            val plate = TectonicPlate(planet, riftRegion)
            val separationTorque = (separationAxes.getValue(riftRegion) - meanSeparationAxis) *
                regionAreas.getValue(riftRegion) * riftSeparationStrength
            plate.torque = this.torque + separationTorque
            plate.tiles.forEach { it.tectonicPlate = plate }
            plate
        }

        planet.tectonicPlates.addAll(newPlates)
        return newPlates
    }
}
