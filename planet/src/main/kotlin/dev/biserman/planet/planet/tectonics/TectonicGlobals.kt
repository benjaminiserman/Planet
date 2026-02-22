package dev.biserman.planet.planet.tectonics

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.sigmoid
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.things.StonePlacementType
import godot.common.util.lerp
import godot.global.GD
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("MayBeConstant")
object TectonicGlobals {
    var slabPullStrength = 0.015
    var convergencePushStrength = 0.5
    var ridgePushStrength = 0.003
    var mantleConvectionStrength = 0.0008
    var springPlateContributionStrength = 0.007
    var edgeInteractionStrength = 0.08
    var tileInertia = 0.25

    var plateTorqueScalar = 0.1
    var riftCutoff = 0.5
    var minElevation = -12000.0
    var maxElevation = 12000.0
    var plateMergeCutoff = 0.39
    var minPlateSize = 10
    var continentElevationCutoff = -250.0

    var convergenceSearchRadius = 1.5 // multiple of average tile radius
    var divergenceSearchRadius = 1.5 // multiple of average tile radius
    var searchMaxResults = 7
    var divergencePatchUplift = -1000

    var continentSpringStiffness = 1.0
    var continentSpringDamping = 0.1
    var continentSpringSearchRadius = 2.0 // multiple of average tile radius

    var overridingElevationStrengthScale = 1400.0
    var subductingElevationStrengthScale = -1000.0
    var convergingElevationStrengthScale = 1500.0

    var divergenceCutoff = 0.25
    var divergedCrustHeight = -2000.0
    var divergedCrustLerp = 1.0

    var depositStrength = 0.6
    var depositLoss = 0.01
    var prominenceErosion = 0.1
    var elevationErosion = 6e-07
    var waterErosion = 12.5
    var depositionStartHeight = 1000

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
            if (planet.random.nextFloat() <= hotspotEruptionChance) {
                if (hotspot > 1000) {
                    tile.stoneColumn.accreteLayer(tile, StonePlacementType.MantleVolcanic)
                }
                return lerp(tile.elevation, sqrt(hotspot), hotspotLerp)
            } else {
                tile.stoneColumn.igneousIntrude(tile)
            }
        }

        return tile.elevation
    }
}