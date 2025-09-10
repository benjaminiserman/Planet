package dev.biserman.planet.planet

import com.github.davidmoten.rtreemulti.RTree
import com.github.davidmoten.rtreemulti.geometry.Point
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.scaleAndCoerce01
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverage
import dev.biserman.planet.topology.Tile
import godot.core.Vector3
import jdk.internal.org.jline.utils.Colors.s
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
    val subductionStrength = subductingPlates.values.maxOf { it.density } - overridingPlate.density - 0.5
    val slabPull
        get() = subductingPlates.values.map { interaction ->
            Pair(
                tile.position,
                (tile.position - interaction.plate.region.center).normalized() * tile.area * TectonicGlobals.slabPullStrength * subductionStrength.sign
            )
        }

    val subductingMass =
        involvedTiles.filter { it.key != overridingPlate.plate }.values.flatten()
            .map { 1 - it.tile.density.scaleAndCoerceIn(-1.0..1.0, 0.0..0.66) }
            .average()
            .pow(3)

    val overridingElevationStrengthScale = 14000.0
    val subductingElevationStrengthScale = -22000.0
    val convergingElevationStrengthScale = 16000.0
    fun unscaledElevationAdjustment(planetTile: PlanetTile): Double =
        when (planetTile.tectonicPlate) {
            overridingPlate.plate -> {
                val overridingMultiplier =
                    speed * if (subductionStrength > 0) {
                        overridingElevationStrengthScale
                    } else {
                        convergingElevationStrengthScale * -subductionStrength
                    }
                overridingMultiplier * subductingMass
            }
            in subductingPlates -> {
                speed * if (subductionStrength > 0) {
                    speed * subductingElevationStrengthScale
                } else {
                    speed * convergingElevationStrengthScale * -subductionStrength
                }
            }
            else -> 0.0
        }

    companion object {
        val subductionZoneSearchRadius = Main.instance.planet.topology.averageRadius * 1.5
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

// subduction elevation
//                            val speedScale = (moveDelta.length() / planet.topology.averageRadius).toFloat()
//                            val elevationDifference = groups
//                                .filter { it != overridingPlate }
//                                .entries
//                                .flatMap { list ->
//                                    list.value.map {
//                                        it.tile.tile.position to it.tile.elevation.adjustRange(
//                                            -5000f..5000f,
//                                            0f..200f
//                                        ).toDouble()
//                                    }
//                                }
//                                .weightedAverageInverse(tile.position, searchRadius)
//                                .toFloat()
//                            this.elevation += elevationDifference * speedScale
//
//                            // subduction pulldown (or up for convergence)
//                            groups.filter { it != overridingPlate }.forEach { (plate, tiles) ->
//                                tiles.forEach { entry ->
//                                    val pulledTile = entry.tile
//                                    val densityScale =
//                                        max(pulledTile.density, this.density).adjustRange(-1f..1f, 0.2f..1f)
//                                    val distanceScale =
//                                        pulledTile.tile.position.distanceTo(tile.position) / searchRadius
//                                    val direction = min(pulledTile.density, this.density)
//
//                                    val current = subductPulldown.computeIfAbsent(pulledTile.tile) { 0.0 }
//                                    subductPulldown[pulledTile.tile] =
//                                        current + densityScale * distanceScale * direction * 100
//                                }
//                            }