package dev.biserman.planet.planet.climate

@Suppress("MayBeConstant")
object ClimateSimulationGlobals {

    var climateSimulationSamplesPerMonth = 1

    // INSOLATION

    var solarConstant = 1.0 // 1361.0 // W/m^2
    var orbitEccentricity = 0.016718
    var axialTiltDeg = 23.45
    var periapsis = 3.5
    var northSpringEquinox = 286.0
    var yearLength = 365.242
    var opticalDepthConstant = -0.14
    var insolationToWm2 = 340.0 // convert [0, 1] range insolation to W/m^2

    // ITCZ PATHFINDING

    var itczPathfindingNowVsAnnualInsolationLerp = 0.6
    var itczPathfindingContinentialityWeight = 0.01

    // OCEAN CURRENTS IDENTIFICATION

    var minCurrentCirculationRadius = 4
    var minOceanTilesForCurrent = 50
    var minCurrentScanlineProportion = 0.3
    var targetCurrentRadiusProportion = 0.35
    var oceanCurrentStrengthPow = 3.0
    var oceanCurrentStrengthDiagonalization = 0.66
    var oceanCurrentMinStrength = 0.35

    // AIR PRESSURE

    var airPressureSolarDeclinationScalar = -0.33 //-0.85

    // the furthest away in tiles from the shore that warm currents can affect air pressure
    var warmCurrentAirPressureMaxContinentiality = 5.0

    // the furthest away that a warm current can affect air pressure (in tiles)
    var warmCurrentAirPressureMaxDistance = 5.0

    // where in tiles from the shore is the air pressure effect centered for warm currents
    var warmCurrentAirPressureContinentialityCenter = 0.0
    var warmCurrentAirPressureStrength = -1.0 // maximum impact of warm currents on air pressure in mb

    // the furthest away in tiles from the shore that cool currents can affect air pressure
    var coolCurrentAirPressureMaxContinentiality = 5.0

    // the furthest away that a cool current can affect air pressure (in tiles)
    var coolCurrentAirPressureMaxDistance = 6.0

    // where in tiles from the shore is the air pressure effect centered for cool currents
    var coolCurrentAirPressureContinentialityCenter = 0.0
    var coolCurrentAirPressureStrength = 12.5 // maximum impact of cool currents on air pressure in mb

    // the following section is complex & silly, I recommend ignoring it
    var airPressureSeasonalContinentialityExp = 1.0 // higher exp -> more seasonality & faster drop-off
    var airPressureSeasonalInsolationCenter = 0.6 // the expected insolation during the mid-latitude equinox
    var airPressureSeasonalInsolationExp = 1.01 // the expected insolation during the mid-latitude equinox
    var airPressureSeasonalAdjustmentScalar = 1.0 // a scalar on seasonal air pressure adjustment
    var airPressureSeasonalScalarMin = 5.0 // the minimum scalar effect of seasonality on air pressure in mb
    var airPressureSeasonalScalarMax = 25.0 // the maximum scalar effect of seasonality on air pressure in mb
    var airPressureSeasonalExpectedMin = -2.0 // see usage
    var airPressureSeasonalExpectedMax = 10.0 // see usage

    var airPressureElevationFallStart = 2000.0 // at what elevation does air pressure start falling
    var airPressureElevationFallStrength = 0.00066 // rate of air pressure fall, mb/m

    var itczAirPressureStrength = -7.5 // maximum impact of the ITCZ on air pressure in mb
    var itczAirPressureMaxDistance = 12.5 // max distance (in tiles) at which the ITCZ effects air pressure

    // WIND

    var windBlockingSlope = 750.0
    var maxWindBlocking = 0.9999
    var backwardsWind = 0.02

    // TEMPERATURE

    var baseTemperature = 245.0 // °K, base temperature of land
    var baseTemperatureInsolationScalar = 74.0 // °K, increasing by 1°K ≈ 0.9°K increase in tropical summer temperatures

    var oceanMinBaseTemp = 254.5 // °K, lowest possible ocean temperature
    var oceanBaseTemp = 250.0 // °K, increasing by 1°K ≈ 1°K increase in global ocean temperatures
    var oceanNowVsAnnualInsolationLerp = 0.6 // ∈[0,1], higher values -> less seasonality
    var oceanNowVsAnnualInsolationLerpPow = 0.75 // higher exp -> less insolation & more seasonality
    var oceanInsolationScale = 56.5 // °K, increasing by 1°K ≈ 1°K increase in tropical ocean temperatures

    var oceanWaterVsLandTemperatureLerp = 0.9 // higher values = more oceanic, lower values = more landlike temperature
    var oceanWaterVsLandTemperatureLerpScalar = 0.025
    var shoreWaterVsLandTemperatureLerpExp = 0.75 // higher exp -> peninsularity matters more for temperature moderation
    var shoreWaterVsLandTemperatureLerpMin = 0.1 // min lerp like above but for shorelines, modulated by peninsularity
    var shoreWaterVsLandTemperatureLerpMax = 0.4 // max lerp like above but for shorelines, modulated by peninsularity
    var inlandWaterVsLandTemperatureContinentialityScalar = 0.03 // lerp scalar for inland tiles based on continentiality
    var inlandWaterVsLandTemperatureContinentialityScalarMax = 2.0
    var inlandWaterVsLandTemperatureContinentialityBase = 0.85

    var dryLapseRate = -0.0098 // °C/meter
    var dryLapseRateScalar = 0.66 // linear scalar on dry lapse rate

    // the furthest away in tiles from the shore that ocean currents can affect temperature
    var maxOceanCurrentTemperatureContinentiality = 5.0
    var warmCurrentTemperatureStrength = 0.2 // °K, the max temperature increase from a warm current
    var warmCurrentTemperatureDistance = 5.0 // the furthest away that a warm current can affect temperature (in tiles)
    var warmCurrentTemperatureAverageInsolationExp = -1.5
    var warmCurrentTemperatureInsolationExp = -0.4
    var coolCurrentTemperatureStrength = -0.75 // °K, the max temperature decrease from a cool current
    var coolCurrentTemperatureDistance = 4.0 // the furthest away that a cool current can affect temperature (in tiles)
    var coolCurrentTemperatureAverageInsolationExp = 1.0
    var coolCurrentTemperatureInsolationExp = -0.2

    var moistureCoolingTargetTemperature = 273.15 // °K, the temperature that added moisture warms/cools towards
    var moistureCoolingExp = 1.5 // higher exp -> less moisture cooling & faster moisture cooling drop-off
    var maxMoistureCoolingLerp = 0.2 // max % that moisture can cool temperature towards target
    var maxMoistureForCooling = 1.25 // added moisture above this varue has no impact on cooling

    // MOISTURE

    var maxMoistureSteps = 50 // the maximum amount of steps the moisture simulator can run
    var moistureToMm = 120.0 // scalar to convert moisture to mm
    var startingMoistureMultiplier = 10.0 // multiply all starting moisture by this value
    var minStartingMoisture = 0.05 // minimum starting moisture
    var maxStartingMoisture = 3.0 // maximum starting moisture
    var oceanMoistureInsolationExp = 3.0 // higher exp -> less ocean moisture & most seasonality
    var oceanMoistureInsolationNowVsAnnualLerp = 1.35
    var averageInsolationMoistureCutoff = 0.6

    // awful hack to help moisture propagate into continental interiors
    var moisturePropagationMultiplier = 1.02 // VERY sensitive. can cause exponential turbo-rain

    // ITCZ = inter-tropical convergence zone
    var itczMoistureMaxDistance = 1.5 // max distance (in tiles) at which the ITCZ effects moisture
    var itczMoistureExp = 1.0 // higher exp -> less ITCZ effect & faster effect decay with distance
    var itczMoistureScalar = 1.25 // how much is moisture multiplied by directly under the ITCZ

    var equatorMoistureEffectMaxContinentiality = 9.0 // maximum continentiality for equator moisture effect
    var equatorMoistureEffectScalar = 5.0 // maximum amount that equator can effect moisture
    var equatorMoistureEffectInsolationExp = 2.0 // higher exp -> lower moisture effect at equator & faster drop-off
    var equatorMoistureEffectMaxDistance = 1.0 // max distance that equatorial updraft effects moisture, in °latitude

    var ferrelMoistureEffectMaxContinentiality = 15.0 // maximum continentiality for ferrel moisture effect
    var ferrelMoistureEffectScalar = 0.15 // exponential falloff rate for ferrel moisture effect
    var ferrelMoistureEffectMax = 0.4 // maximum amount that ferrel cell updraft can effect moisture
    var ferrelMoistureEffectInsolationExp = 0.75 // higher exp -> lower ferrel moisture effect & faster drop-off
    var ferrelMoistureEffectMaxDistance = 15.0 // max distance that ferrel cell updraft effects moisture
    var ferrelMoistureEffectLatitude = 60.0 // °latitude at which ferrel moisture effect is centered

    var hadleyMoistureEffectScalar = -3.5 // exponential falloff rate for hadley moisture effect
    var hadleyMoistureEffectMax = 0.4 // maximum amount that hadley cell downdraft can effect moisture
    var hadleyMoistureEffectInsolationExp = 0.75 // higher exp -> lower hadley moisture effect & faster drop-off
    var hadleyMoistureEffectMaxDistance = 10.0 // max distance that hadley cell updraft effects moisture
    var hadleyMoistureEffectLatitude = 30.0 // °latitude at which hadley moisture effect is centered

    var landPrecipitationScalar = 1.0 // scalar for all land precipitation
    var oceanPrecipitationScalar = 0.5 // scalar for all oceanic precipitation

    // the furthest away in tiles from the shore that ocean currents can affect moisture
    var maxOceanCurrentMoistureContinentiality = 5.0
    var warmCurrentMoistureStrength = 40.0 // °K, the max moisture increase from a warm current
    var warmCurrentMoistureDistance = 7.5 // the furthest away that a warm current can affect moisture
    var warmCurrentMoistureAverageInsolationExp = 5.0
    var coolCurrentMoistureStrength = -50.0 // °K, the max moisture decrease from a cool current
    var coolCurrentMoistureDistance = 4.0 // the furthest away that a cool current can affect moisture
    var coolCurrentMoistureAverageInsolationExp = 4.0

    var minPrecipitation = 0.01 // min % of moisture to precipitate per tile a cloud moves
    var upslopeOfMinMoisture = 500.0 // at what slope does moisture propagation drop to the % below
    var upslopePrecipitationFactor = 0.5
    var minUpslopeMoisture = 0.1 // proportional, must be ≤ 1.0
    var upslopeMoistureExp = 5.0 // higher exp -> less moisture & faster moisture loss upslope
    var saturationThreshold = 1.0 // simulator tries to push water away from tiles with moisture higher than this varue
}
