package dev.biserman.planet.planet.climate

@Suppress("MayBeConstant")
object ClimateSimulationGlobals {

    // ITCZ PATHFINDING

    val itczPathfindingNowVsAnnualInsolationLerp = 0.4
    val itczPathfindingContinentialityWeight = 0.015

    // AIR PRESSURE

    val airPressureSolarDeclinationScalar = -0.75 //-0.85

    // the furthest away in tiles from the shore that warm currents can affect air pressure
    val warmCurrentAirPressureMaxContinentiality = 5.0

    // the furthest away that a warm current can affect air pressure (in tiles)
    val warmCurrentAirPressureMaxDistance = 5.0

    // where in tiles from the shore is the air pressure effect centered for warm currents
    val warmCurrentAirPressureContinentialityCenter = 0.0
    val warmCurrentAirPressureStrength = -2.5 // maximum impact of warm currents on air pressure in mb

    // the furthest away in tiles from the shore that cool currents can affect air pressure
    val coolCurrentAirPressureMaxContinentiality = 5.0

    // the furthest away that a cool current can affect air pressure (in tiles)
    val coolCurrentAirPressureMaxDistance = 6.0

    // where in tiles from the shore is the air pressure effect centered for cool currents
    val coolCurrentAirPressureContinentialityCenter = -3.0
    val coolCurrentAirPressureStrength = 5.0 // maximum impact of cool currents on air pressure in mb

    // the following section is complex & silly, I recommend ignoring it
    val airPressureSeasonalContinentialityExp = 3.2 // higher exp -> more seasonality & faster drop-off
    val airPressureSeasonalInsolationCenter = 0.6 // the expected insolation during the mid-latitude equinox
    val airPressureSeasonalInsolationExp = 1.01 // the expected insolation during the mid-latitude equinox
    val airPressureSeasonalAdjustmentScalar = 0.008 // a scalar on seasonal air pressure adjustment
    val airPressureSeasonalScalarMin = 5.0 // the minimum scalar effect of seasonality on air pressure in mb
    val airPressureSeasonalScalarMax = 20.0 // the maximum scalar effect of seasonality on air pressure in mb
    val airPressureSeasonalExpectedMin = -2.0 // see usage
    val airPressureSeasonalExpectedMax = 2.0 // see usage

    val airPressureElevationFallStart = 2000.0 // at what elevation does air pressure start falling
    val airPressureElevationFallStrength = 0.001 // rate of air pressure fall, mb/m

    val itczAirPressureStrength = -12.5 // maximum impact of the ITCZ on air pressure in mb
    val itczAirPressureMaxDistance = 15.0 // max distance (in tiles) at which the ITCZ effects air pressure

    // WIND

    val windBlockingSlope = 750.0
    val maxWindBlocking = 0.98
    val backwardsWind = 0.025

    // TEMPERATURE

    val baseTemperature = 240.15 // °K, base temperature of land
    val baseTemperatureInsolationScalar = 83.0 // °K, increasing by 1°K ≈ 0.9°K increase in tropical summer temperatures

    val oceanMinBaseTemp = 271.34 // °K, freezing temperature of salt water
    val oceanBaseTemp = 248.55 // °K, increasing by 1°K ≈ 1°K increase in global ocean temperatures
    val oceanNowVsAnnualInsolationLerp = 0.66 // ∈[0,1], higher values -> less seasonality
    val oceanNowVsAnnualInsolationLerpPow = 0.75 // higher exp -> less insolation & more seasonality
    val oceanInsolationScale = 57.5 // °K, increasing by 1°K ≈ 1°K increase in tropical ocean temperatures

    val oceanWaterVsLandTemperatureLerp = 0.05 // lower values = more oceanic, higher values = more landlike temperature
    val shoreWaterVsLandTemperatureLerpExp = 0.5 // higher exp -> peninsularity matters more for temperature moderation
    val shoreWaterVsLandTemperatureLerpMin = 0.1 // min lerp like above but for shorelines, modulated by peninsularity
    val shoreWaterVsLandTemperatureLerpMax = 0.45 // max lerp like above but for shorelines, modulated by peninsularity
    val inlandWaterVsLandTemperatureContinentialityScalar = 0.5 // lerp scalar for inland tiles based on continentiality

    val dryLapseRate = -0.0098 // °C/meter
    val dryLapseRateScalar = 0.5 // linear scalar on dry lapse rate

    // the furthest away in tiles from the shore that ocean currents can affect temperature
    val maxOceanCurrentTemperatureContinentiality = 5.0
    val warmCurrentTemperatureStrength = 6.5 // °K, the max temperature increase from a warm current
    val warmCurrentTemperatureDistance = 3.0 // the furthest away that a warm current can affect temperature (in tiles)
    val coolCurrentTemperatureStrength = -1.0 // °K, the max temperature decrease from a cool current
    val coolCurrentTemperatureDistance = 3.0 // the furthest away that a cool current can affect temperature (in tiles)

    val moistureCoolingTargetTemperature = 273.15 // °K, the temperature that added moisture warms/cools towards
    val moistureCoolingExp = 1.0 // higher exp -> less moisture cooling & faster moisture cooling drop-off
    val maxMoistureCoolingLerp = 0.38 // max % that moisture can cool temperature towards target
    val maxMoistureForCooling = 3.0 // added moisture above this value has no impact on cooling

    // MOISTURE

    val maxMoistureSteps = 50 // the maximum amount of steps the moisture simulator can run
    val startingMoistureMultiplier = 0.9 // multiply all starting moisture by this value
    val minStartingMoisture = 0.15 // minimum starting moisture
    val maxStartingMoisture = 3.0 // maximum starting moisture
    val oceanMoistureInsolationExp = 1.2 // higher exp -> less ocean moisture & most seasonality

    // awful hack to help moisture propagate into continental interiors
    val moisturePropagationMultiplier = 1.05 // VERY sensitive. can cause exponential turbo-rain

    // ITCZ = inter-tropical convergence zone
    val itczMoistureMaxDistance = 5.0 // max distance (in tiles) at which the ITCZ effects moisture
    val itczMoistureExp = 3.0 // higher exp -> less ITCZ effect & faster effect decay with distance
    val itczMoistureScalar = 3.0 // how much is moisture multiplied by directly under the ITCZ

    val equatorMoistureEffectScalar = 2.8 // maximum amount that equator can effect moisture
    val equatorMoistureEffectInsolationExp = 4.0 // higher exp -> lower moisture effect at equator & faster drop-off
    val equatorMoistureEffectMaxDistance = 5.0 // max distance that equatorial updraft effects moisture, in °latitude

    val ferrelMoistureEffectScalar = 0.25 // maximum amount that ferrel cell updraft can effect moisture
    val ferrelMoistureEffectInsolationExp = 0.4 // higher exp -> lower ferrel moisture effect & faster drop-off
    val ferrelMoistureEffectMaxDistance = 17.5 // max distance that ferrel cell updraft effects moisture
    val ferrelMoistureEffectLatitude = 60.0 // °latitude at which ferrel moisture effect is centered

    val landPrecipitationScalar = 1.0 // scalar for all land precipitation
    val oceanPrecipitationScalar = 0.5 // scalar for all oceanic precipitation

    // the furthest away in tiles from the shore that ocean currents can affect moisture
    val maxOceanCurrentMoistureContinentiality = 5.0
    val warmCurrentMoistureStrength = 2.0 // °K, the max moisture increase from a warm current
    val warmCurrentMoistureDistance = 2.0 // the furthest away that a warm current can affect moisture
    val coolCurrentMoistureStrength = -0.3 // °K, the max moisture decrease from a cool current
    val coolCurrentMoistureDistance = 5.0 // the furthest away that a cool current can affect moisture

    val minPrecipitation = 0.01 // min % of moisture to precipitate per tile a cloud moves
    val upslopeOfMinMoisture = 750.0 // at what slope does moisture propagation drop to the % below
    val minUpslopeMoisture = 0.1 // proportional, must be ≤ 1.0
    val upslopeMoistureExp = 2.0 // higher exp -> less moisture & faster moisture loss upslope
    val saturationThreshold = 1.0 // simulator tries to push water away from tiles with moisture higher than this value
}