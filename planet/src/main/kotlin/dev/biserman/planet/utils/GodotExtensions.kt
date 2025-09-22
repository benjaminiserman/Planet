package dev.biserman.planet.utils

import dev.biserman.planet.Main
import godot.api.FastNoiseLite
import godot.core.Color
import godot.core.Vector2
import godot.core.Vector3
import kotlin.random.Random

fun Color.Companion.randomRgb(a: Int = 255, random: Random = Main.random) =
    Color.fromRgba(random.nextInt(256), random.nextInt(256), random.nextInt(256), a)

fun Color.Companion.randomHsv(
    s: Double = 1.0,
    v: Double = 1.0,
    a: Double = 1.0,
    random: Random = Main.random
) =
    Color.fromHsv(random.nextDouble(), s, v, a)

fun (FastNoiseLite).withSeed(seed: Int): FastNoiseLite = this.apply { this.setSeed(seed) }

operator fun (Vector2).component1() = this.x
operator fun (Vector2).component2() = this.y

operator fun (Vector3).component1() = this.x
operator fun (Vector3).component2() = this.y
operator fun (Vector3).component3() = this.z
