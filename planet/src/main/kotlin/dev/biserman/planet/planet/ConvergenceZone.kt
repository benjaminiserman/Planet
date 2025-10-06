package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
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
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow

data class ConvergenceInteraction(val plate: TectonicPlate, val movement: Vector3, val density: Double) {
    constructor(plateGroup: Map.Entry<TectonicPlate, List<Tectonics.MovedTile>>) : this(
        plateGroup.key,
        plateGroup.value.map { it.newPosition - it.tile.tile.position }.average(),
        plateGroup.value.map { it.tile.density }.average()
    )
}

@JsonIdentityInfo(
    generator = ObjectIdGenerators.IntSequenceGenerator::class,
    scope = ConvergenceZone::class,
    property = "id"
)
class ConvergenceZone(
    val planet: Planet,
    val tileId: Int,
    val speed: Double,
    val overridingPlate: ConvergenceInteraction,
    val subductingPlates: Map<Int, ConvergenceInteraction>,
    val overridingDensity: Double,
    val subductionStrengths: Map<Int, Double>,
    val slabPull: Map<Int, List<PointForce>>,
    val convergencePush: Map<Int, List<PointForce>>,
    val subductingMass: Double
) {
    @get:JsonIgnore
    val tile get() = planet.topology.tiles[tileId]

    fun unscaledElevationAdjustment(planetTile: PlanetTile): Double {
        val subductionStrength = subductionStrengths[planetTile.tectonicPlate?.id ?: return 0.0] ?: 0.0
        return when (planetTile.tectonicPlate?.id) {
            overridingPlate.plate.id -> {
                speed * if (subductionStrength >= 0) {
                    overridingElevationStrengthScale * subductingMass
                } else {
                    convergingElevationStrengthScale * subductingMass
                }
            }
            in subductingPlates -> {
                speed * if (subductionStrength >= 0) {
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
                    (1 - min(
                        1.0,
                        zone.tile.position.distanceTo(planetTile.tile.position) / zone.speed
                    )).pow(
                        planetTile.movement.length()
                    )
                }

        fun make(
            planet: Planet,
            tile: Tile,
            speed: Double,
            overridingPlate: ConvergenceInteraction,
            subductingPlates: Map<TectonicPlate, ConvergenceInteraction>,
            involvedTiles: Map<TectonicPlate, List<Tectonics.MovedTile>>
        ): ConvergenceZone {
            val overridingDensity = involvedTiles[overridingPlate.plate]!!.map { it.tile.density }.average()
            val averageDensity = involvedTiles.values.flatten().map { it.tile.density }.average()
            val subductionStrengths =
                involvedTiles.mapValues { tiles ->
                    (tiles.value.map { it.tile.density }.average() - averageDensity).absoluteValue - 0.5
                }

            val subductingMass = involvedTiles.filter { it.key != overridingPlate.plate }.values.flatten()
                .map { 2 - it.tile.density.scaleAndCoerceIn(-1.0..1.0, 0.75..1.25) }
                .average()

            val slabPull = involvedTiles
                .filterKeys { subductionStrengths[it]!! > 0 }
                .mapValues { (plate, tiles) ->
                    tiles.map { otherTile ->
                        PointForce(
                            tile.position,
                            (tile.position - otherTile.tile.tile.position).normalized() * TectonicGlobals.slabPullStrength * tile.area * subductionStrengths[plate]!!
                        )
                    }
                }

            val convergencePush = involvedTiles
                .filterKeys { subductionStrengths[it]!! <= 0 }
                .mapValues { (plate, tiles) ->
                    tiles.map { otherTile ->
                        PointForce(
                            tile.position,
                            (otherTile.tile.tile.position - tile.position).normalized() * TectonicGlobals.convergencePushStrength * tile.area * subductingMass * -subductionStrengths[plate]!!
                        )
                    }
                }

            return ConvergenceZone(
                planet,
                tile.id,
                speed,
                overridingPlate,
                subductingPlates.mapKeys { it.key.id },
                overridingDensity,
                subductionStrengths.mapKeys { it.key.id },
                slabPull.mapKeys { it.key.id },
                convergencePush.mapKeys { it.key.id },
                subductingMass
            )
        }
    }
}
