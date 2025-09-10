package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.sigmoid
import dev.biserman.planet.planet.Tectonics.random
import godot.common.util.lerp
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("MayBeConstant")
object TectonicGlobals {
    val slabPullStrength = 0.015
    val ridgePushStrength = 0.003
    val mantleConvectionStrength = 0.0005
    val edgeForceStrength = 800.0
    val springPlateContributionStrength = 0.007

    val plateTorqueScalar = 0.1
    val riftCutoff = 0.25
    val minElevation = -12000.0
    val maxElevation = 12000.0
    val plateMergeCutoff = 0.35
    val minPlateSize = 5
    val continentElevationCutoff = -250.0

    val convergenceSearchRadius = 1.5 // multiple of average tile radius
    val divergenceSearchRadius = 1.5 // multiple of average tile radius
    val searchMaxResults = 7

    val continentSpringStiffness = 2.0
    val continentSpringDamping = 0.1
    val continentSpringSearchRadius = 2.0 // multiple of average tile radius

    val depositStrength = 0.5
    val erosionStrength = 0.015

    val tectonicElevationVariogram = Kriging.variogram(Main.instance.planet.topology.averageRadius * 1.5, 1e4, 1e5)

    // desmos: f\left(x\right)\ =\ \frac{90}{1+e^{0.005\left(x+1400\right)}}-\frac{80}{1+e^{0.0015\left(x+4700\right)}}
    fun oceanicSubsidence(elevation: Double) =
        90 * sigmoid(elevation, 0.005, 1400.0) - 80 * sigmoid(elevation, 0.0015, 4700.0)

    val hotspotEruptionChance = 0.45
    val hotspotStrength = 7500f.pow(2)
    fun tryHotspotEruption(tile: PlanetTile): Double {
        val planet = tile.planet
        if (random.nextFloat() <= hotspotEruptionChance) {
            val hotspot =
                planet.noise.hotspots.sample4d(tile.tile.position, planet.tectonicAge.toDouble()) * hotspotStrength
            if (hotspot > 0) {
                return lerp(tile.elevation, sqrt(hotspot), 0.66)
            }
        }

        return tile.elevation
    }

}