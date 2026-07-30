package dev.biserman.planet.geometry

import godot.core.Vector3
import kotlin.math.exp

// nigh-entirely vibe-coded by GPT-5
object Kriging {
    // Exponential semivariogram
    fun variogram(range: Double, sill: Double, nugget: Double): (Double) -> Double = { h ->
        nugget + sill * (1.0 - exp(-h / range))
    }

    fun interpolate(samples: List<Pair<Vector3, Double>>, target: Vector3, variogram: (Double) -> Double = variogram(1.0, 1.0, 0.0)): Double {
        val n = samples.size
        if (n == 0) return 0.0
        if (n == 1) return samples[0].second

        // Build Kriging matrix (n+1 x n+1)
        val k = Array(n + 1) { DoubleArray(n + 1) }
        val y = DoubleArray(n + 1)

        for (i in 0 until n) {
            for (j in 0 until n) {
                val h = samples[i].first.distanceTo(samples[j].first)
                k[i][j] = variogram(h)
            }
            k[i][n] = 1.0
            k[n][i] = 1.0

            val hTarget = samples[i].first.distanceTo(target)
            y[i] = variogram(hTarget)
        }
        k[n][n] = 0.0
        y[n] = 1.0

        // Solve linear system K * λ = y
        val lambda = solveLinearSystem(k, y)

        // Interpolated value
        var estimate = 0.0
        for (i in 0 until n) {
            estimate += lambda[i] * samples[i].second
        }
        return estimate
    }

    // Simple Gaussian elimination solver
    private fun solveLinearSystem(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
        val n = b.size
        val m = Array(n) { a[it].clone() }
        val x = b.clone()

        for (i in 0 until n) {
            // Pivot
            var max = i
            for (j in i + 1 until n) {
                if (kotlin.math.abs(m[j][i]) > kotlin.math.abs(m[max][i])) max = j
            }
            val tmpRow = m[i]
            m[i] = m[max]
            m[max] = tmpRow
            val tmpVal = x[i]
            x[i] = x[max]
            x[max] = tmpVal

            // Normalize pivot row
            val pivot = m[i][i]
            for (j in i until n) m[i][j] /= pivot
            x[i] /= pivot

            // Eliminate column
            for (j in 0 until n) {
                if (j != i) {
                    val factor = m[j][i]
                    for (k in i until n) m[j][k] -= factor * m[i][k]
                    x[j] -= factor * x[i]
                }
            }
        }
        return x
    }
}
