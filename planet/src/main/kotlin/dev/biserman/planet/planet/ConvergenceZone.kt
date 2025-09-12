package dev.biserman.planet.planet

import com.github.davidmoten.rtreemulti.RTree
import com.github.davidmoten.rtreemulti.geometry.Point
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverage
import dev.biserman.planet.planet.TectonicGlobals.convergingElevationStrengthScale
import dev.biserman.planet.planet.TectonicGlobals.overridingElevationStrengthScale
import dev.biserman.planet.planet.TectonicGlobals.subductingElevationStrengthScale
import dev.biserman.planet.topology.Tile
import godot.core.Vector3
import godot.global.GD
import kotlin.collections.average
import kotlin.collections.component1
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

data class ConvergenceInteraction(val plate: TectonicPlate, val movement: Vector3, val density: Double) {
    constructor(plateGroup: Map.Entry<TectonicPlate, List<Tectonics.MovedTile>>) : this(
        plateGroup.key,
        plateGroup.value.map { it.newPosition - it.tile.tile.position }.average(),
        plateGroup.value.map { it.tile.density }.average()
    )
}

class ConvergenceZone(
    val tile: Tile,
    val speed: Double,
    val overridingPlate: ConvergenceInteraction,
    val subductingPlates: Map<TectonicPlate, ConvergenceInteraction>,
    involvedTiles: Map<TectonicPlate, List<Tectonics.MovedTile>>
) {

    val overridingDensity = involvedTiles[overridingPlate.plate]!!.map { it.tile.density }.average()
    val subductionStrengths =
        involvedTiles.mapValues { tiles -> tiles.value.map { it.tile.density }.average() - overridingDensity - 0.5 }

    val slabPull = involvedTiles
        .filterKeys { subductionStrengths[it]!! > 0 }
        .mapValues { (plate, tiles) ->
            tiles.map { otherTile ->
                Pair(
                    tile.position,
                    (tile.position - otherTile.tile.tile.position).normalized() * TectonicGlobals.slabPullStrength * tile.area // * strength
                )
            }
        }

    val convergencePush = involvedTiles
        .filterKeys { subductionStrengths[it]!! < 0 }
        .mapValues { (plate, tiles) ->
            tiles.map { otherTile ->
                Pair(
                    tile.position,
                    (otherTile.tile.tile.position - tile.position).normalized() * TectonicGlobals.convergencePushStrength * tile.area // * -strength
                )
            }
        }

    val subductingMass = involvedTiles.filter { it.key != overridingPlate.plate }.values.flatten()
        .map { 2 - it.tile.density.scaleAndCoerceIn(-1.0..1.0, 0.5..1.5) }
        .average()

    fun unscaledElevationAdjustment(planetTile: PlanetTile): Double {
        val subductionStrength = subductionStrengths[planetTile.tectonicPlate] ?: 0.0
        return when (planetTile.tectonicPlate) {
            overridingPlate.plate -> {
                speed * if (subductionStrength > 0) {
                    overridingElevationStrengthScale * subductingMass
                } else {
                    convergingElevationStrengthScale * subductingMass
                }
            }
            in subductingPlates -> {
                speed * if (subductionStrength > 0) {
                    subductingElevationStrengthScale * subductionStrength * (2 - subductingMass)
                } else {
                    convergingElevationStrengthScale * -subductionStrength * subductingMass
                }
            }
            else -> 0.0
        }
    }

    companion object {
        val subductionZoneSearchRadius = Main.instance.planet.topology.averageRadius * 1.25
        fun adjustElevation(planetTile: PlanetTile, zoneRTree: RTree<ConvergenceZone, Point>) =
            zoneRTree.nearest(planetTile.tile.position.toPoint(), subductionZoneSearchRadius, 25)
                .map { it.value() to it.value().unscaledElevationAdjustment(planetTile) }
                .weightedAverage(planetTile.tile.position) { zone ->
                    (1 - min(1.0, (zone.tile.position.distanceTo(planetTile.tile.position) / zone.speed))).pow(
                        planetTile.movement.length()
                    )
                }
    }
}
