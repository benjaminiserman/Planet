package dev.biserman.planet.planet

import godot.api.FastNoiseLite
import godot.core.Vector3
import opensimplex2.OpenSimplex2
import opensimplex2.OpenSimplex2S
import kotlin.random.Random
import kotlin.random.nextLong

class NoiseMaps(val seed: Int, val random: Random) {
    val debug = FastNoiseLite().apply {
        setSeed(seed)
        setFrequency(0.01f)
    }

    val startingElevation = FastNoiseLite().apply {
        setSeed(random.nextInt())
        setFrequency(0.5f)
    }

    val hotspots = object {
        val seed = random.nextLong()
        fun sample4d(v: Vector3, w: Double) = OpenSimplex2S.noise4_ImproveXYZ(seed, v.x, v.y, v.z, w)
    }
}