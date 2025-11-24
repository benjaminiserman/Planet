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
}