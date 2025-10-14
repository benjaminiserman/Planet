package dev.biserman.planet.planet

object ClimateSimulation {
    data class Band(val latitude: Double, val pressureDelta: Double)

    @Suppress("UnusedUnaryOperator")
    val bands = listOf(
        +90 to 0,
        +60 to -15,
        +30 to +15,
        0 to 0,
        -30 to +15,
        -60 to -15,
        -90 to 0,
    ).map { Band(it.first.toDouble(), it.second.toDouble()) }
    val basePressure = 1010.0
}