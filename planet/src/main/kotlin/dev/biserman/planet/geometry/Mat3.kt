package dev.biserman.planet.geometry

import godot.core.Vector3
import kotlin.math.abs

// not gonna lie this is entirely vibe-coded
class Mat3(val m: Array<DoubleArray>) {

    init {
        require(m.size == 3 && m.all { it.size == 3 }) {
            "Mat3 must be 3x3"
        }
    }

    // Matrix + Matrix
    operator fun plus(other: Mat3): Mat3 {
        val r = Array(3) { DoubleArray(3) }
        for (i in 0..2) {
            for (j in 0..2) {
                r[i][j] = m[i][j] + other.m[i][j]
            }
        }
        return Mat3(r)
    }

    // Matrix - Matrix 
    operator fun minus(other: Mat3): Mat3 {
        val r = Array(3) { DoubleArray(3) }
        for (i in 0..2) {
            for (j in 0..2) {
                r[i][j] = m[i][j] - other.m[i][j]
            }
        }
        return Mat3(r)
    }

    // Matrix * scalar
    operator fun times(s: Double): Mat3 {
        val r = Array(3) { DoubleArray(3) }
        for (i in 0..2) {
            for (j in 0..2) {
                r[i][j] = m[i][j] * s
            }
        }
        return Mat3(r)
    }

    // Matrix * Vector3
    operator fun times(v: Vector3): Vector3 {
        val x = m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z
        val y = m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z
        val z = m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z
        return Vector3(x, y, z)
    }

    // Matrix * Matrix
    operator fun times(other: Mat3): Mat3 {
        val r = Array(3) { DoubleArray(3) }
        for (i in 0..2) {
            for (j in 0..2) {
                r[i][j] = (0..2).sumOf { k -> m[i][k] * other.m[k][j] }
            }
        }
        return Mat3(r)
    }

    // Inverse of a 3x3
    fun inverse(): Mat3 {
        val det =
            m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
                    m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
                    m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])

        require(abs(det) > 1e-12) { "Matrix is singular (det ~ 0)" }

        val invDet = 1.0 / det
        val r = Array(3) { DoubleArray(3) }

        r[0][0] = (m[1][1] * m[2][2] - m[1][2] * m[2][1]) * invDet
        r[0][1] = (m[0][2] * m[2][1] - m[0][1] * m[2][2]) * invDet
        r[0][2] = (m[0][1] * m[1][2] - m[0][2] * m[1][1]) * invDet

        r[1][0] = (m[1][2] * m[2][0] - m[1][0] * m[2][2]) * invDet
        r[1][1] = (m[0][0] * m[2][2] - m[0][2] * m[2][0]) * invDet
        r[1][2] = (m[0][2] * m[1][0] - m[0][0] * m[1][2]) * invDet

        r[2][0] = (m[1][0] * m[2][1] - m[1][1] * m[2][0]) * invDet
        r[2][1] = (m[0][1] * m[2][0] - m[0][0] * m[2][1]) * invDet
        r[2][2] = (m[0][0] * m[1][1] - m[0][1] * m[1][0]) * invDet

        return Mat3(r)
    }

    companion object {
        fun identity(): Mat3 = Mat3(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0)
            )
        )

        fun zero(): Mat3 = Mat3(
            arrayOf(
                doubleArrayOf(0.0, 0.0, 0.0),
                doubleArrayOf(0.0, 0.0, 0.0),
                doubleArrayOf(0.0, 0.0, 0.0)
            )
        )

        // Outer product u ⊗ v
        fun fromOuter(u: Vector3, v: Vector3): Mat3 {
            return Mat3(
                arrayOf(
                    doubleArrayOf(u.x * v.x, u.x * v.y, u.x * v.z),
                    doubleArrayOf(u.y * v.x, u.y * v.y, u.y * v.z),
                    doubleArrayOf(u.z * v.x, u.z * v.y, u.z * v.z)
                )
            )
        }
    }
}