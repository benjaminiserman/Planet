package dev.biserman.planet.planet.tectonics

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.github.davidmoten.rtreemulti.RTree
import com.github.davidmoten.rtreemulti.geometry.Point
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverage
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.PointForce
import dev.biserman.planet.planet.tectonics.TectonicGlobals.convergingElevationStrengthScale
import dev.biserman.planet.planet.tectonics.TectonicGlobals.oceanOceanArcDistance
import dev.biserman.planet.planet.tectonics.TectonicGlobals.oceanOceanArcElevationStrength
import dev.biserman.planet.planet.tectonics.TectonicGlobals.oceanOceanArcMaxContinentalFraction
import dev.biserman.planet.planet.tectonics.TectonicGlobals.oceanOceanArcWidth
import dev.biserman.planet.planet.tectonics.TectonicGlobals.overridingElevationStrengthScale
import dev.biserman.planet.planet.tectonics.TectonicGlobals.subductingElevationStrengthScale
import dev.biserman.planet.topology.Tile
import godot.core.Vector3
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class ConvergenceInteraction(
    val plate: TectonicPlate,
    val movement: Vector3,
    val density: Double,
    val continentalFraction: Double = 0.0
) {
    constructor(plateGroup: Map.Entry<TectonicPlate, List<Tectonics.MovedTile>>) : this(
        plateGroup.key,
        plateGroup.value.map { it.newPosition - it.tile.tile.position }.average(),
        plateGroup.value.map { it.tile.density }.average(),
        plateGroup.value.count { it.tile.isContinentalCrust }.toDouble() / plateGroup.value.size
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
    val subductingMass: Double,
    val oceanOceanArcDirection: Vector3 = Vector3.ZERO,
    val oceanOceanArcStrength: Double = 0.0
) {
    @get:JsonIgnore
    val tile get() = planet.topology.tiles[tileId]

    fun unscaledElevationAdjustment(planetTile: PlanetTile): Double {
        val subductionStrength = subductionStrengths[planetTile.tectonicPlate?.id ?: return 0.0] ?: 0.0
        val convergenceAdjustment = when (planetTile.tectonicPlate?.id) {
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
        return convergenceAdjustment + oceanOceanArcAdjustment(planetTile)
    }

    private fun oceanOceanArcAdjustment(planetTile: PlanetTile): Double {
        if (oceanOceanArcStrength <= 0.0 || planetTile.tectonicPlate?.id != overridingPlate.plate.id) {
            return 0.0
        }

        val arcDirection = oceanOceanArcDirection
        if (arcDirection.lengthSquared() == 0.0) return 0.0

        val offset = (planetTile.tile.position - tile.position).tangent(tile.position)
        val alongArcDirection = offset.dot(arcDirection)
        if (alongArcDirection <= 0.0) return 0.0

        val targetDistance = planet.topology.averageRadius * oceanOceanArcDistance
        val arcWidth = planet.topology.averageRadius * oceanOceanArcWidth
        if (arcWidth <= 0.0) return 0.0

        val alongFalloff = 1.0 - ((alongArcDirection - targetDistance).absoluteValue / arcWidth).coerceIn(0.0, 1.0)
        val crossDistance = (offset - arcDirection * alongArcDirection).length()
        val crossFalloff = 1.0 - (crossDistance / (arcWidth * 1.5)).coerceIn(0.0, 1.0)
        return oceanOceanArcStrength * alongFalloff * crossFalloff
    }

    companion object {
        val subductionZoneSearchRadius
            get() = Main.instance.planet.topology.averageRadius *
                    max(1.25, oceanOceanArcDistance + oceanOceanArcWidth)

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
            val oceanOceanSubductingPlate = subductingPlates.values
                .filter { it.continentalFraction <= oceanOceanArcMaxContinentalFraction }
                .maxByOrNull { it.density }
            val oceanOceanArcDirection =
                if (
                    overridingPlate.continentalFraction <= oceanOceanArcMaxContinentalFraction &&
                    oceanOceanSubductingPlate != null
                ) {
                    val nearestSubductingTile = involvedTiles[oceanOceanSubductingPlate.plate]!!
                        .minBy { it.newPosition.distanceTo(tile.position) }
                    val arcDirection = (tile.position - nearestSubductingTile.tile.tile.position)
                        .tangent(tile.position)
                    if (arcDirection.lengthSquared() == 0.0) Vector3.ZERO else arcDirection.normalized()
                } else {
                    Vector3.ZERO
                }
            val oceanOceanArcStrength =
                if (oceanOceanArcDirection.lengthSquared() > 0.0) {
                    speed.absoluteValue * oceanOceanArcElevationStrength * subductingMass
                } else {
                    0.0
                }

            val slabPull = involvedTiles
                .filterKeys { subductionStrengths[it]!! > 0 }
                .mapValues { (plate, tiles) ->
                    tiles.mapNotNull { otherTile ->
                        val pullDirection = tile.position - otherTile.tile.tile.position
                        if (pullDirection.lengthSquared() == 0.0) return@mapNotNull null
                        PointForce(
                            tile.position,
                            pullDirection.normalized() * TectonicGlobals.slabPullStrength *
                                tile.area * subductionStrengths[plate]!!
                        )
                    }
                }

            val convergencePush = mutableMapOf<TectonicPlate, MutableList<PointForce>>()
            val searchRadius = planet.topology.averageRadius
            for ((plate, tiles) in involvedTiles) {
                if (plate == overridingPlate.plate) continue

                val interaction = subductingPlates[plate] ?: continue
                val nearestTile = tiles.minBy { it.newPosition.distanceTo(tile.position) }
                val outward = (nearestTile.tile.tile.position - tile.position).tangent(tile.position)
                if (outward.lengthSquared() == 0.0) continue

                val normal = outward.normalized()
                val penetration = (
                    1.0 - nearestTile.newPosition.distanceTo(tile.position) / searchRadius
                ).coerceIn(0.0, 1.0)
                val relativeMovement = interaction.movement - overridingPlate.movement
                val closingSpeed = max(0.0, -relativeMovement.dot(normal) / searchRadius)
                val densityDifference = (interaction.density - overridingPlate.density).absoluteValue
                val subductionBypass = (
                    (densityDifference - TectonicGlobals.collisionDensityBypassThreshold) /
                        (1.0 - TectonicGlobals.collisionDensityBypassThreshold)
                ).coerceIn(0.0, 1.0)
                val collisionResistance = 1.0 -
                    subductionBypass * (1.0 - TectonicGlobals.minCollisionResistance)
                val continentalContact =
                    interaction.continentalFraction * overridingPlate.continentalFraction
                val stiffnessMultiplier = 1.0 + continentalContact *
                    (TectonicGlobals.continentalCollisionStiffnessMultiplier - 1.0)
                val response =
                    TectonicGlobals.collisionStiffness * stiffnessMultiplier * penetration +
                        TectonicGlobals.collisionDamping * closingSpeed *
                        (1.0 + TectonicGlobals.collisionRestitution)
                val force = normal * TectonicGlobals.convergencePushStrength * tile.area *
                    subductingMass * collisionResistance * response

                convergencePush.getOrPut(plate) { mutableListOf() }
                    .add(PointForce(tile.position, force))
                convergencePush.getOrPut(overridingPlate.plate) { mutableListOf() }
                    .add(PointForce(tile.position, -force))
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
                subductingMass,
                oceanOceanArcDirection,
                oceanOceanArcStrength
            )
        }
    }
}
