package dev.biserman.planet.geometry

import godot.core.Vector3
import kotlin.math.exp

// nigh-entirely vibe-coded by GPT-5
object Kriging {
    // Exponential semivariogram
    fun variogram(range: Double, sill: Double, nugget: Double): (Double) -> Double = { h ->
        nugget + sill * (1.0 - exp(-h / range))
    }

    fun interpolate(
        samples: List<Pair<Vector3, Double>>,
        target: Vector3,
        variogram: (Double) -> Double = variogram(1.0, 1.0, 0.0)
    ): Double {
        val n = samples.size
        if (n == 0) return 0.0
        if (n == 1) return samples[0].second

        // Build Kriging matrix (n+1 x n+1)
        val K = Array(n + 1) { DoubleArray(n + 1) }
        val y = DoubleArray(n + 1)

        for (i in 0 until n) {
            for (j in 0 until n) {
                val h = samples[i].first.distanceTo(samples[j].first)
                K[i][j] = variogram(h)
            }
            K[i][n] = 1.0
            K[n][i] = 1.0

            val hTarget = samples[i].first.distanceTo(target)
            y[i] = variogram(hTarget)
        }
        K[n][n] = 0.0
        y[n] = 1.0

        // Solve linear system K * λ = y
        val lambda = solveLinearSystem(K, y)

        // Interpolated value
        var estimate = 0.0
        for (i in 0 until n) {
            estimate += lambda[i] * samples[i].second
        }
        return estimate
    }

    // Simple Gaussian elimination solver
    private fun solveLinearSystem(A: Array<DoubleArray>, b: DoubleArray): DoubleArray {
        val n = b.size
        val M = Array(n) { A[it].clone() }
        val x = b.clone()

        for (i in 0 until n) {
            // Pivot
            var max = i
            for (j in i + 1 until n) {
                if (kotlin.math.abs(M[j][i]) > kotlin.math.abs(M[max][i])) max = j
            }
            val tmpRow = M[i]; M[i] = M[max]; M[max] = tmpRow
            val tmpVal = x[i]; x[i] = x[max]; x[max] = tmpVal

            // Normalize pivot row
            val pivot = M[i][i]
            for (j in i until n) M[i][j] /= pivot
            x[i] /= pivot

            // Eliminate column
            for (j in 0 until n) {
                if (j != i) {
                    val factor = M[j][i]
                    for (k in i until n) M[j][k] -= factor * M[i][k]
                    x[j] -= factor * x[i]
                }
            }
        }
        return x
    }
}
