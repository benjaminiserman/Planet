package dev.biserman.planet.planet

@Suppress("MayBeConstant")
object ClimateSimulationGlobals {
    val maxMoistureSteps = 50
    val startingMoistureMultiplier = 3.0

    val windBlockingSlope = 1500.0
    val maxWindBlocking = 1.0
    val backwardsWind = 0.025

    val maxPrecipitationSlope = 1500.0
    val minPrecipitation = 0.1
    val saturationThreshold = 1.0
}