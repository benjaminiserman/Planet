package dev.biserman.planet.planet

import com.github.davidmoten.rtreemulti.RTree
import com.github.davidmoten.rtreemulti.geometry.Point
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverageInverse
import dev.biserman.planet.topology.Tile
import kotlin.math.pow

class SubductionZone(
    val tile: Tile,
    val strength: Double,
    val overridingPlate: TectonicPlate,
    val subductingPlates: List<TectonicPlate>,
) {
    val slabPull
        get() = subductingPlates.map { subductingPlate ->
            Pair(
                tile.position,
                (tile.position - subductingPlate.region.center).normalized() * tile.area * TectonicGlobals.slabPullStrength
            )
        }

    fun unscaledElevationAdjustment(planetTile: PlanetTile): Double =
        when (planetTile.tectonicPlate) {
            overridingPlate -> strength * 150 * (1 - planetTile.density
                .adjustRange(-1.0..1.0, 0.0..1.0)
                .coerceIn(0.0..1.0)).pow(2)
            in subductingPlates -> strength * -150 * planetTile.density
                .adjustRange(-1.0..1.0, 0.0..1.0)
                .coerceIn(0.0..1.0).pow(2)
            else -> 0.0
        }

    companion object {
        val subductionZoneSearchRadius = Main.instance.planet.topology.averageRadius * 5
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