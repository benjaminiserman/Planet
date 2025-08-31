package dev.biserman.planet.planet

import dev.biserman.planet.geometry.tangent
import godot.api.FastNoiseLite
import godot.core.Vector3
import opensimplex2.OpenSimplex2S
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

open class NoiseMap4D(val seed: Long) {
    open fun sample4d(v: Vector3, w: Double) = OpenSimplex2S.noise4_ImproveXYZ(seed, v.x, v.y, v.z, w).toDouble()
}

open class VectorNoiseMap4D(val seed: Long) {
    open fun sample4d(v: Vector3, w: Double) = Vector3(
        OpenSimplex2S.noise4_Fallback(seed, v.x, v.y, v.z, w) * magnitudeScalar,
        OpenSimplex2S.noise4_Fallback(seed + 1, v.x, v.y, v.z, w) * magnitudeScalar,
        OpenSimplex2S.noise4_Fallback(seed + 2, v.x, v.y, v.z, w) * magnitudeScalar,
    )

    companion object {
        val magnitudeScalar = 1 / sqrt(3.0)
    }
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

    val mantleConvection = object : VectorNoiseMap4D(random.nextLong()) {
        override fun sample4d(v: Vector3, w: Double) = super.sample4d(v, w * 0.01).tangent(v) * 0.1
    }
}