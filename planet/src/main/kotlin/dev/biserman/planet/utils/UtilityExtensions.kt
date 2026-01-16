package dev.biserman.planet.utils

import dev.biserman.planet.geometry.toGeoPoint
import godot.core.Vector3
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.withSign

object UtilityExtensions {
    fun (Double).formatDigits(digits: Int = 2) = "%.${digits}f".format(this)
    fun (Float).formatDigits(digits: Int = 2) = "%.${digits}f".format(this)
    fun (Vector3).formatDigits(digits: Int = 2) = "(%.${digits}f, %.${digits}f, %.${digits}f)".format(x, y, z)
    fun (Vector3).formatGeo(digits: Int = 2) = this.toGeoPoint().formatDigits(digits)
    fun (Double).degToRad() = this * Math.PI / 180.0
    fun (Double).radToDeg() = this * 180.0 / Math.PI
    fun (Double).signPow(exp: Double) = abs(this).pow(exp).withSign(this)

    fun (List<Pair<Double, Double>>).weightedAverage(): Double {
        val totalWeight = this.sumOf { it.second }
        val contributions = this.map { (value, weight) -> value * weight }
        return contributions.sum() / totalWeight
    }

    // adapted from: https://gist.github.com/erikhuizinga/d2ca2b501864df219fd7f25e4dd000a4

    /**
     * Create the cartesian product of any number of sets of any size. Useful for parameterized tests
     * to generate a large parameter space with little code. Note that any type information is lost, as
     * the returned set contains list of any combination of types in the input set.
     *
     * @param sets The sets.
     */
    fun <T> cartesianProduct(vararg sets: Set<T>) =
        sets
            .fold(listOf(listOf<T>())) { acc, set ->
                acc.flatMap { list -> set.map { element -> list + element } }
            }
            .toSet()

    fun <T> (Collection<T>).cartesianProduct(vararg collections: Collection<T>): Set<List<T>> =
        cartesianProduct(this.toSet(), *collections.map { it.toSet() }.toTypedArray())

    fun <T> (Set<T>).cartesianProduct(vararg collections: Set<T>): Set<List<T>> =
        cartesianProduct(this.toSet(), *collections)
}