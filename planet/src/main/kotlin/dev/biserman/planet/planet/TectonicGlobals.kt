package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.sigmoid
import dev.biserman.planet.planet.Tectonics.random
import godot.common.util.lerp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("MayBeConstant")
object TectonicGlobals {
    val slabPullStrength = 0.02
    val ridgePushStrength = 0.005
    val mantleConvectionStrength = 0.0007
    val edgeForceStrength = 800.0
    val springPlateContributionStrength = 0.007

    val plateTorqueScalar = 0.1
    val riftCutoff = 0.35
    val minElevation = -12000.0
    val maxElevation = 12000.0
    val plateMergeCutoff = 0.33
    val minPlateSize = 5
    val continentElevationCutoff = -250.0

    val tectonicElevationVariogram = Kriging.variogram(Main.instance.planet.topology.averageRadius * 1.5, 1e10, 1e11)

    // desmos: f\left(x\right)\ =\ \frac{80}{1+e^{\left(0.005x+7\right)}}-\frac{70}{1+e^{\left(0.0015x+7\right)}}
    fun oceanicSubsidence(elevation: Double) =
        80 * sigmoid(elevation, 0.005, 7.0) - 70 * sigmoid(elevation, 0.0015, 7.0)

    val hotspotEruptionChance = 0.5
    val hotspotStrength = 7500f.pow(2)
    fun tryHotspotEruption(tile: PlanetTile): Double {
        val planet = tile.planet
        if (random.nextFloat() >= hotspotEruptionChance) {
            val hotspot =
                planet.noise.hotspots.sample4d(tile.tile.position, planet.tectonicAge.toDouble()) * hotspotStrength
            if (hotspot > 0) {
                return lerp(tile.elevation, sqrt(hotspot), 0.66)
            }
        }

        return tile.elevation
    }

}