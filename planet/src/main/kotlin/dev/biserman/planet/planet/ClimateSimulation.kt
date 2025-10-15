package dev.biserman.planet.planet

object ClimateSimulation {
    data class Band(val latitude: Double, val pressureDelta: Double)

    @Suppress("UnusedUnaryOperator")
    val bands = listOf(
        +90 to +5,
        +60 to -10,
        +30 to +10,
        0 to +5,
        -30 to +10,
        -60 to -10,
        -90 to +5,
    ).map { Band(it.first.toDouble(), it.second.toDouble()) }
    val basePressure = 1010.0
}