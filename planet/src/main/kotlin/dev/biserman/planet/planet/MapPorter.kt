package dev.biserman.planet.planet

import godot.common.util.RealT
import godot.core.Color
import kotlin.math.min
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties

object MapPorter {
    data class Variable<T>(
        val name: String,
        val fetch: (PlanetTile) -> T,
        val apply: (PlanetTile, T) -> Unit
    ) {
        constructor(prop: KMutableProperty1<PlanetTile, T>) : this(
            prop.name,
            { prop.get(it) },
            { tile, value -> prop.set(tile, value) }
        )
    }

    data class Channel<T>(
        val name: String,
        val fetch: (Color) -> T,
        val apply: (Color, T) -> Color
    ) {
        constructor(prop: KMutableProperty1<Color, T>) : this(
            prop.name,
            { prop.get(it) },
            { color, value -> prop.set(color, value); color }
        )
    }

    val colorChannel = Channel("c", { it }, { _, value -> value })
    val doubleChannels = mapOf(
        "r" to Channel(Color::r),
        "g" to Channel(Color::g),
        "b" to Channel(Color::b),
        "a" to Channel(Color::a),

        "h" to Channel(Color::h),
        "s" to Channel(Color::s),
        "v" to Channel(Color::v),
        "l" to Channel("l", { it.l }, { color, value -> color.l = value; color })
    )

    @Suppress("UNCHECKED_CAST")
    val doubleVariables = listOf(
        (PlanetTile::class).memberProperties
            .filter { it.returnType == Double::class.createType() }
            .filter { it is KMutableProperty1<*, *> }
            .map { it as KMutableProperty1<PlanetTile, Double> }
            .map { Variable(it) },
        (PlanetTile::class).memberProperties
            .filter { it.returnType == Int::class.createType() }
            .filter { it is KMutableProperty1<*, *> }
            .map { it as KMutableProperty1<PlanetTile, Int> }
            .map {
                Variable(
                    it.name,
                    { planetTile -> it.get(planetTile).toDouble() },
                    { planetTile, value -> it.set(planetTile, value.toInt()) })
            },
    ).flatten().associateBy { it.name }

    fun parseFilenameToSteps(filename: String): List<Pair<Variable<*>, Channel<*>>> {
        val parts = filename.take(filename.indexOf('.')).split("_")

        val steps = parts
            .filter { "-" in it }
            .mapNotNull {
                val (start, end) = it.split("-")
                if (start in doubleVariables && (end in doubleChannels || end == colorChannel.name)) {
                    Pair(doubleVariables[start]!!, doubleChannels[end] ?: colorChannel)
                } else {
                    null
                }
            }

        return steps
    }

    fun import(planet: Planet, filename: String) {
        val color = Color(1, 1, 1)
    }

    fun export(planet: Planet, filename: String) {

    }

    fun hslToHsv(h: RealT, s: RealT, l: RealT, a: Double = 1.0): Color {
        val v = l + s * min(l, 1 - l)
        return Color.fromHsv(
            h,
            if (v == 0.0) 0.0 else 2 * (1 - l / v),
            l + s * min(l, 1 - l),
            a
        )
    }

    fun hsvToHsl(h: RealT, s: RealT, v: RealT, a: Double = 1.0): Color {
        val l = 2 * v - min(v, 1 - v)
        return Color.fromHsv(
            h,
            if (l == 0.0 || l == 1.0) 0.0 else (v - l) / min(l, 1 - l),
            v * (1 - s / 2),
            a
        )
    }

    var (Color).l: Double
        get() = hsvToHsl(this.h, this.s, this.v, this.a).l
        set(l) {
            val s = hsvToHsl(this.h, this.s, this.v, this.a).s
            val hsv = hslToHsv(this.h, s, l, this.a)
            this.s = hsv.s
            this.v = hsv.v
        }
}