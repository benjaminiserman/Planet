package dev.biserman.planet.planet.climate

@Suppress("MayBeConstant")
object ClimateSimulationGlobals {
    val maxMoistureSteps = 50
    val startingMoistureMultiplier = 3.0
    val minStartingMoisture = 0.1

    val windBlockingSlope = 750.0
    val maxWindBlocking = 1.0
    val backwardsWind = 0.025

    val maxPrecipitationSlope = 750.0
    val minPrecipitation = 0.1
    val saturationThreshold = 2.0
}