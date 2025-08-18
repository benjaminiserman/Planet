package dev.biserman.planet.planet

import godot.api.FastNoiseLite
import godot.core.Vector3
import opensimplex2.OpenSimplex2S
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

open class NoiseMap4D(val seed: Long) {
    open fun sample4d(v: Vector3, w: Double) = OpenSimplex2S.noise4_ImproveXYZ(seed, v.x, v.y, v.z, w).toDouble()
}

class NoiseMaps(val seed: Int, val random: Random) {
    val debug = FastNoiseLite().apply {
        setSeed(seed)
        setFrequency(0.01f)
    }

    val startingElevation = FastNoiseLite().apply {
        setSeed(random.nextInt())
        setFrequency(0.5f)
    }

    val hotspots = object : NoiseMap4D(random.nextLong()) {
        override fun sample4d(v: Vector3, w: Double) = max(0.0, super.sample4d(v * 15, w) - 0.7) * 1.42
    }
}