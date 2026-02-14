package dev.biserman.planet.planet

import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.memo
import godot.api.FastNoiseLite
import godot.core.Vector3
import opensimplex2.OpenSimplex2S
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

open class NoiseMap4D(val seed: Long) {
    open fun sample4d(v: Vector3, w: Double) = OpenSimplex2S.noise4_ImproveXYZ(seed, v.x, v.y, v.z, w).toDouble()
    fun sampleAt(tile: PlanetTile) = sample4d(tile.tile.position, tile.planet.tectonicAge.toDouble())
}

open class NoiseMap3D(val seed: Long) {
    open fun sample3d(v: Vector3) = OpenSimplex2S.noise3_ImproveXY(seed, v.x, v.y, v.z).toDouble()
    fun sampleAt(tile: PlanetTile) = sample3d(tile.tile.position)
}

open class VectorNoiseMap4D(val seed: Long) {
    open fun sample4d(v: Vector3, w: Double) = Vector3(
        OpenSimplex2S.noise4_Fallback(seed, v.x, v.y, v.z, w) * magnitudeScalar,
        OpenSimplex2S.noise4_Fallback(seed + 1, v.x, v.y, v.z, w) * magnitudeScalar,
        OpenSimplex2S.noise4_Fallback(seed + 2, v.x, v.y, v.z, w) * magnitudeScalar,
    )

    fun sampleAt(tile: PlanetTile) = sample4d(tile.tile.position, tile.planet.tectonicAge.toDouble())

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
        override fun sample4d(v: Vector3, w: Double) = (max(0.0, super.sample4d(v * 7, w * 0.001) - 0.65) * 2.85).pow(6)
    }

    val mantleConvection = object : VectorNoiseMap4D(random.nextLong()) {
        override fun sample4d(v: Vector3, w: Double) = super.sample4d(v, w * 0.01).tangent(v) * 0.1
    }

    val essence = NoiseMap3D(random.nextLong())
}