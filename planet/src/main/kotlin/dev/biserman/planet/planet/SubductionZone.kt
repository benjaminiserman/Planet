package dev.biserman.planet.planet

import com.github.davidmoten.rtreemulti.RTree
import com.github.davidmoten.rtreemulti.geometry.Point
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverageInverse
import dev.biserman.planet.topology.Tile
import godot.core.Vector3
import kotlin.math.pow
import kotlin.math.sqrt

data class SubductionInteraction(val plate: TectonicPlate, val movement: Vector3, val density: Double) {
    constructor(plateGroup: Map.Entry<TectonicPlate, List<Tectonics.MovedTile>>) : this(
        plateGroup.key,
        plateGroup.value.map { it.newPosition - it.tile.tile.position }.average(),
        plateGroup.value.map { it.tile.density }.average()
    )
}

class SubductionZone(
    val tile: Tile,
    val strength: Double,
    val overridingPlate: SubductionInteraction,
    val subductingPlates: Map<TectonicPlate, SubductionInteraction>,
) {
    val slabPull
        get() = subductingPlates.values.map { interaction ->
            Pair(
                tile.position,
                (tile.position - interaction.plate.region.center).normalized() * tile.area * TectonicGlobals.slabPullStrength
            )
        }

    val overridingElevationStrengthScale = 2500.0
    val subductingElevationStrengthScale = -3600.0
    fun unscaledElevationAdjustment(planetTile: PlanetTile): Double =
        when (planetTile.tectonicPlate) {
            overridingPlate.plate -> strength * overridingElevationStrengthScale * overridingPlate.movement.length() * sqrt(
                1 - planetTile.density
                    .scaleAndCoerceIn(-1.0..1.0, 0.0..1.0)
            )
            in subductingPlates -> strength * subductingElevationStrengthScale * subductingPlates[planetTile.tectonicPlate]!!.movement.length() * planetTile.density
                .scaleAndCoerceIn(-1.0..1.0, 0.0..1.0)
                .pow(2)
            else -> 0.0
        }

    companion object {
        val subductionZoneSearchRadius = Main.instance.planet.topology.averageRadius * 2
        fun adjustElevation(planetTile: PlanetTile, zoneRTree: RTree<SubductionZone, Point>) =
            zoneRTree.nearest(planetTile.tile.position.toPoint(), subductionZoneSearchRadius, 25)
                .map { it.value().tile.position to it.value().unscaledElevationAdjustment(planetTile) }
                .weightedAverageInverse(planetTile.tile.position, subductionZoneSearchRadius)
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