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
    val plateTorqueScalar = 0.1
    val riftCutoff = 0.35
    val minElevation = -12000.0
    val maxElevation = 12000.0
    val plateMergeCutoff = 0.33
    val minPlateSize = 5

    val tectonicElevationVariogram = Kriging.variogram(Main.instance.planet.topology.averageRadius * 1.5, 500.0, 5000.0)

    // desmos: f\left(x\right)\ =\ \frac{100}{1+e^{\left(0.003x+5\right)}}-\frac{100}{1+e^{\left(0.003x+10\right)}}
    fun oceanicSubsidence(elevation: Double) =
        100 * (sigmoid(elevation, 0.003, 5.0) - sigmoid(elevation, 0.003, 10.0))

    fun tectonicErosion(tile: PlanetTile) =
        oceanicSubsidence(tile.elevation) + 10 * max(0.0, tile.elevation * 0.0005).pow(2)

    val hotspotEruptionChance = 0.5
    val hotspotStrength = 20000f.pow(2)
    fun tryHotspotEruption(tile: PlanetTile): Double {
        val planet = tile.planet
        if (random.nextFloat() >= hotspotEruptionChance) {
            val hotspot =
                planet.noise.hotspots.sample4d(tile.tile.position, planet.tectonicAge.toDouble()) * hotspotStrength
            if (hotspot > 0) {
                return lerp(tile.elevation, sqrt(hotspot), 0.5)
            }
        }

        return tile.elevation
    }

}