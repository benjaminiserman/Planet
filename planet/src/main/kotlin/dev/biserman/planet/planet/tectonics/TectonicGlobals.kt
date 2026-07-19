package dev.biserman.planet.planet.tectonics

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.sigmoid
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.things.StonePlacementType
import godot.common.util.lerp
import godot.global.GD
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("MayBeConstant")
object TectonicGlobals {
    var slabPullStrength = 0.03
    var convergencePushStrength = 0.5
    var collisionStiffness = 0.5
    var continentalCollisionStiffnessMultiplier = 3.0
    var collisionDamping = 0.5
    var collisionRestitution = 0.1
    var collisionDensityBypassThreshold = 0.25
    var minCollisionResistance = 0.2
    var plateOverrideElevationAdvantage = 500.0
    var ridgePushStrength = 0.003
    var mantleConvectionStrength = 0.0005
    var springPlateContributionStrength = 0.007
    var edgeInteractionStrength = 0.08
    var tileInertia = 0.25
    var tectonicSimulationStop = 100000

    var plateTorqueScalar = 0.1
    // Least-squares fit of corrected degree-35 tile inertia tensors to the legacy area
    // calculation across representative 10-14 plate partitions. Apply it to every
    // area-weighted tectonic term so forces and inertia remain in the same calibrated units.
    var tectonicAreaScale = 0.11513827487177024
    var riftCutoff = 0.4
    var riftSeparationStrength = 0.1
    var minElevation = -12000.0
    var maxElevation = 12000.0
    var plateMergeCutoff = 0.39
    var minPlateSize = 10
    var continentElevationCutoff = -250.0
    var boundarySmoothingPasses = 1
    var boundarySmoothingMinSamePlateNeighbors = 2

    var convergenceSearchRadius = 1.5 // multiple of average tile radius
    var divergenceSearchRadius = 1.5 // multiple of average tile radius
    var divergenceContinuityStrength = 0.75
    var divergenceMinConnectedEdges = 2
    var divergenceMinSeparationSpeed = 0.1 // tile radii per step
    var divergenceFullSeparationSpeed = 0.5 // tile radii per step
    var divergenceMinNormalMotion = 0.5 // reject boundaries dominated by transform motion
    var searchMaxResults = 7
    var divergencePatchUplift = -1000

    var continentSpringStiffness = 1.0
    var continentSpringDamping = 0.1
    var continentSpringSearchRadius = 2.0 // multiple of average tile radius

    var overridingElevationStrengthScale = 1600.0
    var subductingElevationStrengthScale = -1200.0
    var convergingElevationStrengthScale = 1800.0
    var subductionDensityThreshold = 0.5
    var oceanOceanArcElevationStrength = 30000.0
    var oceanOceanArcDistance = 1.5
    var oceanOceanArcWidth = 0.75
    var oceanOceanArcMaxContinentalFraction = 0.2

    var divergenceCutoff = 0.25
    var divergedCrustHeight = -2000.0
    var divergedCrustLerp = 1.0

    var depositStrength = 0.6
    var depositLoss = 0.01
    var depositMultiplier = 1.4
    var desiredLandPercent = 0.3
    var prominenceErosion = 0.15
    var elevationErosion = 1e-06
    var waterErosion = 2.0
    var depositionStartHeight = 1000
    var maxErosionProportion = 1.0

    var accruedDepositThreshold = 800.0
    var accruedErosionThreshold = -400.0
    var orogenicMetamorphosisThreshold = 0.05
    var tectonicVolcanismThreshold = 0.6
    var hotspotEruptionAccretionThreshold = 200.0
    var intrusionStrengthAccretionThreshold = 200.0
    var depositionContinentialityThreshold = -0.4

    var minAverageContinentalHeightGuardrail = 750.0
    var maxAverageContinentalHeightGuardrail = 1000.0
    var guardrailStrictness = 0.15

    var meteorImpactChance = 0.2
    var minMeteorElevationChange = 200.0
    var maxMeteorElevationChange = 2000.0

    var biotaDistributionCount = 10
    var biotaDistributionClearChance = 0.025
    var biotaDistributionTerrestrialMaxSlope = 750.0

    var estimatedAverageRadius = 0.020775855876950022
    @JsonIgnore
    val tectonicElevationVariogram = Kriging.variogram(estimatedAverageRadius * 0.001, 10.0, 1000.0)

    // desmos: f\left(x\right)\ =\ \frac{110}{1+e^{0.005\left(x+1400\right)}}-\frac{100}{1+e^{0.003\left(x+5500\right)}}
    fun oceanicSubsidence(elevation: Double) =
        110 * sigmoid(elevation, 0.005, 1400.0) - 100 * sigmoid(elevation, 0.003, 5500.0)

    var hotspotEruptionChance = 0.45
    var hotspotStrength = 7500f.pow(2)
    var hotspotLerp = 0.66
    fun tryHotspotEruption(tile: PlanetTile): Double {
        val planet = tile.planet
        val hotspot =
            planet.noise.hotspots.sample4d(tile.tile.position, planet.tectonicAge.toDouble()) * hotspotStrength
        if (hotspot > 0) {
            val elevationIncrease = sqrt(hotspot)
            if (planet.random.nextFloat() <= hotspotEruptionChance) {
                if (elevationIncrease > hotspotEruptionAccretionThreshold) {
                    tile.stoneColumn.accreteLayer(tile, StonePlacementType.MantleVolcanic)
                }
                return lerp(tile.elevation, max(tile.elevation, elevationIncrease), hotspotLerp)
            } else {
                if (elevationIncrease > intrusionStrengthAccretionThreshold) {
                    tile.stoneColumn.igneousIntrude(tile)
                }
            }
        }

        return tile.elevation
    }
}

fun Tile.tectonicArea(): Double = area * TectonicGlobals.tectonicAreaScale
