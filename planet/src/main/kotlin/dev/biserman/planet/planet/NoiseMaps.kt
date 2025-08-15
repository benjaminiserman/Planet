package dev.biserman.planet.planet

import godot.api.FastNoiseLite
import kotlin.random.Random

class NoiseMaps(val seed: Int, val random: Random) {
    val debug = FastNoiseLite().apply {
        setSeed(seed)
        setFrequency(0.01f)
    }

    val startingElevation = FastNoiseLite().apply {
        setSeed(random.nextInt())
        setFrequency(0.5f)
    }
}