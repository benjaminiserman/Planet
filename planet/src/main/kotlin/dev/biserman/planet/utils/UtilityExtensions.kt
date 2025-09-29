package dev.biserman.planet.utils

import godot.core.Vector3

object UtilityExtensions {
    fun (Double).formatDigits(digits: Int = 2) = "%.${digits}f".format(this)
    fun (Float).formatDigits(digits: Int = 2) = "%.${digits}f".format(this)
    fun (Vector3).formatDigits(digits: Int = 2) = "(%.${digits}f, %.${digits}f, %.${digits}f)".format(x, y, z)
    fun (Double).degToRad() = this * Math.PI / 180.0
    fun (Double).radToDeg() = this * 180.0 / Math.PI
}