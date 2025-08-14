package dev.biserman.planet.utils

import godot.api.FastNoiseLite
import godot.core.Vector3

class VectorWarpNoise(seed: Int, val frequency: Float = 100f) {

    fun (FastNoiseLite).applyDefaults() {
        this.setFrequency(this@VectorWarpNoise.frequency)
    }

    val xNoise = FastNoiseLite().apply { this.setSeed(seed); this.applyDefaults() }
    val yNoise = FastNoiseLite().apply { this.setSeed(seed + 1); this.applyDefaults() }
    val zNoise = FastNoiseLite().apply { this.setSeed(seed + 2); this.applyDefaults() }

    fun warp(vector: Vector3, magnitude: Double = 1.0): Vector3 {
        val warpVector = Vector3(
            xNoise.getNoise3dv(vector),
            yNoise.getNoise3dv(vector),
            zNoise.getNoise3dv(vector)
        )

        return vector + warpVector * magnitude
    }
}