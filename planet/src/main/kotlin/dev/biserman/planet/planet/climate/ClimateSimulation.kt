package dev.biserman.planet.planet.climate

import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.scaleAndCoerce01
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.backwardsWind
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxMoistureSteps
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.upslopeOfMinMoisture
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxWindBlocking
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.minPrecipitation
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.minStartingMoisture
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.saturationThreshold
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.startingMoistureMultiplier
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.windBlockingSlope
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetRegion
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureElevationFallStart
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureElevationFallStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSeasonalExpectedMin
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSeasonalAdjustmentScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSeasonalContinentialityExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSeasonalExpectedMax
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSeasonalInsolationCenter
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSeasonalInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSeasonalScalarMax
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSeasonalScalarMin
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.airPressureSolarDeclinationScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.baseTemperature
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.baseTemperatureInsolationScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentAirPressureContinentialityCenter
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentAirPressureMaxContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentAirPressureMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentAirPressureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentMoistureDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentMoistureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentTemperatureDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentTemperatureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.dryLapseRate
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.dryLapseRateScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.equatorMoistureEffectInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.equatorMoistureEffectMaxContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.equatorMoistureEffectMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.equatorMoistureEffectScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectLatitude
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectMaxContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.inlandWaterVsLandTemperatureContinentialityScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.itczAirPressureMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.itczAirPressureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.itczMoistureExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.itczMoistureMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.itczMoistureScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.itczPathfindingContinentialityWeight
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.itczPathfindingNowVsAnnualInsolationLerp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.landPrecipitationScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxMoistureCoolingLerp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxMoistureForCooling
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxOceanCurrentMoistureContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxOceanCurrentTemperatureContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxStartingMoisture
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.minUpslopeMoisture
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.moistureCoolingExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.moistureCoolingTargetTemperature
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.moisturePropagationMultiplier
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanBaseTemp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanInsolationScale
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanMinBaseTemp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanMoistureInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanNowVsAnnualInsolationLerp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanNowVsAnnualInsolationLerpPow
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanPrecipitationScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanWaterVsLandTemperatureLerp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.shoreWaterVsLandTemperatureLerpExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.shoreWaterVsLandTemperatureLerpMax
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.shoreWaterVsLandTemperatureLerpMin
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.upslopeMoistureExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentAirPressureContinentialityCenter
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentAirPressureMaxContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentAirPressureMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentAirPressureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentMoistureDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentMoistureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentTemperatureDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentTemperatureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.yearLength
import dev.biserman.planet.planet.climate.OceanCurrents.updateCurrentDistanceMap
import dev.biserman.planet.utils.AStar
import dev.biserman.planet.utils.Path
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import dev.biserman.planet.utils.UtilityExtensions.formatGeo
import dev.biserman.planet.utils.UtilityExtensions.radToDeg
import dev.biserman.planet.utils.UtilityExtensions.signPow
import dev.biserman.planet.utils.sum
import godot.common.util.lerp
import godot.core.Vector3
import godot.global.GD
import kotlin.math.absoluteValue
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin


object ClimateSimulation {
    data class Band(val latitude: Double, val pressureDelta: Double)

    @Suppress("UnusedUnaryOperator")
    val bands = listOf(
        +90 to +5.0,
        +60 to -7.5,
        +30 to +5.0,
        0 to +5.0,
        -30 to +5.0,
        -60 to -7.5,
        -90 to +5.0,
    ).map { Band(it.first.toDouble(), it.second.toDouble()) }
    val basePressure = 1010.0

    fun estimateMonth(planet: Planet, daysPassed: Number): String =
        when (((daysPassed.toDouble() % yearLength) / (365.242 / 12.0)).toInt() + 1) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Unknown"
        }

    fun stepClimateSimulation(planet: Planet) {
        planet.daysPassed += 45
        Gui.instance.daysPassedLabel.setText("${planet.daysPassed} — ${estimateMonth(planet, planet.daysPassed)}")
        Gui.instance.updateInfobox()

        updatePlanetClimate(planet)
    }

    fun updatePlanetClimate(planet: Planet) {
        planet.oceanCurrents = OceanCurrents.viaEarthlikeHeuristic(planet, 7)
            .distinctBy { it.planetTile }
            .associate { it.planetTile.tileId to it }
            .toMutableMap()
        planet.updateCurrentDistanceMap()

        val itcz = planet.calculateItcz().nodes.toSet()
        planet.itczDistanceMap = PlanetRegion(planet, planet.planetTiles.values.toMutableSet())
            .calculateEdgeDepthMap { it in itcz }
            .mapValues { it.value + if (it.key in itcz) -1 else 0 }
            .mapKeys { it.key.tileId }

        simulateMoisture(planet)
    }

    fun (PlanetTile).calculatePrevailingWind(): Vector3 {
        val pressureGradientForce = neighbors
            .filter { it.airPressure < airPressure }
            .map { (it.tile.position - tile.position).tangent(tile.position) * (airPressure - it.airPressure) * 0.1 }
            .sum()

        val latitudeRadians = asin(tile.position.y)
        val coriolisParameter = 2.0 * planet.rotationRate * -sin(latitudeRadians)

        // Deflection is perpendicular to both motion and local vertical
        val coriolisDeflection = tile.position.cross(pressureGradientForce).normalized() *
                coriolisParameter * pressureGradientForce.length()

        val direction = pressureGradientForce + coriolisDeflection
        return direction.normalized() * direction.length()
            .coerceIn(planet.topology.averageRadius * 0.25, planet.topology.averageRadius * 0.66)
    }

    fun (PlanetTile).calculateAirPressure(): Double {
        val latitude = tile.position.toGeoPoint().latitudeDegrees
        val adjustedLatitude = latitude +
                Insolation.solarDeclination(planet.daysPassed % yearLength)
                    .radToDeg() * airPressureSolarDeclinationScalar
        val nearestBandAbove = bands.lastOrNull { it.latitude >= adjustedLatitude } ?: bands.last()
        val nearestBandBelow = bands.firstOrNull { it.latitude <= adjustedLatitude } ?: bands.last()

        val oceanCurrentAdjustment = if (isAboveWater) 0.0 else {
            val warmContinentialityFactor =
                max(
                    0.0,
                    warmCurrentAirPressureMaxContinentiality -
                            (warmCurrentAirPressureContinentialityCenter - continentiality)
                ) * (1.0 / warmCurrentAirPressureMaxContinentiality)
            val coolContinentialityFactor =
                max(
                    0.0,
                    coolCurrentAirPressureMaxContinentiality -
                            (coolCurrentAirPressureContinentialityCenter - continentiality).absoluteValue
                ) * (1.0 / coolCurrentAirPressureMaxContinentiality)
            val warmCurrentStrength =
                (warmCurrentAirPressureMaxDistance - (planet.warmCurrentDistanceMap[tileId] ?: return 0.0))
                    .scaleAndCoerce01(0.0..warmCurrentAirPressureMaxDistance) * warmContinentialityFactor * insolation
            val coolCurrentStrength =
                (coolCurrentAirPressureMaxDistance - (planet.coolCurrentDistanceMap[tileId] ?: return 0.0))
                    .scaleAndCoerce01(0.0..coolCurrentAirPressureMaxDistance) * coolContinentialityFactor * (1 - insolation)
            warmCurrentStrength * warmCurrentAirPressureStrength + coolCurrentStrength * coolCurrentAirPressureStrength
        }

        val adjustedContinentiality = continentiality.toDouble()
        val seasonalAdjustment = -(2 / (1 + exp(
            -(insolation - airPressureSeasonalInsolationCenter).signPow(airPressureSeasonalInsolationExp) *
                    adjustedContinentiality.signPow(airPressureSeasonalContinentialityExp) *
                    airPressureSeasonalAdjustmentScalar
        )) - 1) * adjustedContinentiality.scaleAndCoerceIn(
            airPressureSeasonalExpectedMin..airPressureSeasonalExpectedMax,
            airPressureSeasonalScalarMin..airPressureSeasonalScalarMax
        )

        val elevationAdjustment =
            if (elevation < airPressureElevationFallStart) 0.0
            else (elevation - airPressureElevationFallStart) * airPressureElevationFallStrength

        val itczAdjustment = itczAirPressureStrength *
                ((itczAirPressureMaxDistance - (planet.itczDistanceMap[tileId] ?: return 0.0)) / itczAirPressureMaxDistance)
                    .coerceIn(0.0..1.0)

        return basePressure + lerp(
            nearestBandBelow.pressureDelta,
            nearestBandAbove.pressureDelta,
            (adjustedLatitude - nearestBandBelow.latitude) / (nearestBandAbove.latitude - nearestBandBelow.latitude)
        ) + seasonalAdjustment + elevationAdjustment + itczAdjustment + oceanCurrentAdjustment
    }

    fun simulateMoisture(planet: Planet) {
        var currentMoisture = planet.planetTiles.values.associateWith { tile ->
            val geoPoint = tile.tile.position.toGeoPoint()
            val equatorEffect = equatorMoistureEffectScalar *
                    tile.insolation.pow(equatorMoistureEffectInsolationExp) *
                    max(0.0, 1 - geoPoint.latitudeDegrees.absoluteValue / equatorMoistureEffectMaxDistance) *
                    max(0.0, 1 - (tile.continentiality / equatorMoistureEffectMaxContinentiality))
            val ferrelEffect = ferrelMoistureEffectScalar *
                    tile.insolation.pow(ferrelMoistureEffectInsolationExp) *
                    max(
                        0.0,
                        1 - ((geoPoint.latitudeDegrees.absoluteValue - ferrelMoistureEffectLatitude).absoluteValue) / ferrelMoistureEffectMaxDistance
                    ) *
                    max(0.0, 1 - (tile.continentiality / ferrelMoistureEffectMaxContinentiality))
            val oceanEffect = if (tile.isAboveWater) 0.0
            else {
                val oceanCurrentContinentialityScalar = 1 / maxOceanCurrentMoistureContinentiality
                val oceanCurrentContinentialityFactor = if (tile.continentiality >= 0) 1.0 else {
                    max(0.0, 1.0 + (tile.continentiality + 1) * oceanCurrentContinentialityScalar)
                }
                val warmCurrentEffect =
                    warmCurrentMoistureStrength * tile.insolation * oceanCurrentContinentialityFactor *
                            max(
                                warmCurrentMoistureDistance - (planet.warmCurrentDistanceMap[tile.tileId]?.toDouble()
                                    ?: warmCurrentMoistureDistance), 0.0
                            )
                val coolCurrentEffect =
                    coolCurrentMoistureStrength * tile.insolation * oceanCurrentContinentialityFactor *
                            max(
                                coolCurrentMoistureDistance - (planet.coolCurrentDistanceMap[tile.tileId]?.toDouble()
                                    ?: coolCurrentMoistureDistance), 0.0
                            )
                ((tile.insolation.pow(oceanMoistureInsolationExp) + warmCurrentEffect + coolCurrentEffect) * startingMoistureMultiplier)
                    .coerceIn(minStartingMoisture..maxStartingMoisture)
            }
            equatorEffect + ferrelEffect + oceanEffect
        }
        val finalMoisture = planet.planetTiles.values.associateWith { 0.0 }.toMutableMap()

        var steps = 0
        while (currentMoisture.values.any { it > 0.01 } && steps < maxMoistureSteps) {
            steps += 1
            val nextStep = planet.planetTiles.values.associateWith { 0.0 }.toMutableMap()

            currentMoisture.forEach { (tile, moisture) ->
                val neighborWeights = if (tile.prevailingWind == Vector3.ZERO) {
                    tile.neighbors.associateWith { 1.0 }
                } else {
                    tile.neighbors.associateWith { neighbor ->
                        val delta = (neighbor.tile.position - tile.tile.position).normalized()
                        (tile.prevailingWind.dot(delta) + backwardsWind)
                    }
                }.filter { (_, weight) -> weight > 0.0 }
                    .mapValues { (neighbor, weight) ->
                        weight *
                                (1 - (tile.slopeAboveWaterTo(neighbor) / windBlockingSlope))
                                    .coerceIn(1 - maxWindBlocking..1.0) *
                                tile.tile.borderFor(neighbor.tile).length.pow(0.1)
                    }

                val totalWeight = neighborWeights.values.sum()
                neighborWeights.forEach { (neighbor, weight) ->
                    val moistureProvided = if (totalWeight == 0.0) 0.0 else moisture * weight / totalWeight
                    val precipitation =
                        moistureProvided *
                                max(
                                    minPrecipitation,
                                    (tile.slopeAboveWaterTo(neighbor) / upslopeOfMinMoisture)
                                        .pow(upslopeMoistureExp)
                                        .coerceIn(minUpslopeMoisture..1.0) *
                                            (1 - ((finalMoisture[tile] ?: 0.0) / saturationThreshold).pow(2))
                                                .coerceIn(0.0..1.0)
                                )
                    finalMoisture[tile] = (finalMoisture[tile] ?: 0.0) + precipitation
                    nextStep[neighbor] =
                        (nextStep[neighbor] ?: 0.0) + moistureProvided * moisturePropagationMultiplier - precipitation
                }
            }

            currentMoisture = nextStep
        }

        finalMoisture.mapValuesTo(finalMoisture) { (tile, moisture) ->
            val itczEffect = max(0.0, 1 - planet.itczDistanceMap[tile.tileId]!! / itczMoistureMaxDistance)
                .pow(itczMoistureExp)
                .adjustRange(0.0..1.0, 1.0..itczMoistureScalar)
            moisture * itczEffect * if (tile.isAboveWater) landPrecipitationScalar else oceanPrecipitationScalar
        }
        planet.planetTiles.values.forEach { tile ->
            tile.moisture = tile.neighbors
                .plus(tile)
                .filter { it.isAboveWater == tile.isAboveWater }
                .map { neighbor -> finalMoisture[neighbor] ?: 0.0 }
                .average()
        }
    }

    val (PlanetTile).averageTemperature: Double
        get() {
            val geoPoint = tile.position.toGeoPoint()
            val localBaseTemperature =
                baseTemperature + insolation * baseTemperatureInsolationScalar

            val oceanCurrentContinentialityScalar = 1 / maxOceanCurrentTemperatureContinentiality
            val oceanCurrentContinentialityFactor = if (continentiality >= 0) 1.0 else {
                max(0.0, 1.0 + (continentiality + 1) * oceanCurrentContinentialityScalar)
            }
            val warmCurrentAdjustment =
                warmCurrentTemperatureStrength * max(
                    warmCurrentTemperatureDistance - (planet.warmCurrentDistanceMap[tileId] ?: return 0.0),
                    0.0
                ) * insolation * (1.0 - averageInsolation) * oceanCurrentContinentialityFactor
            val coolCurrentAdjustment =
                coolCurrentTemperatureStrength * max(
                    coolCurrentTemperatureDistance - (planet.coolCurrentDistanceMap[tileId] ?: return 0.0),
                    0.0
                ) * insolation * averageInsolation * oceanCurrentContinentialityFactor

            val elevationAdjustment = dryLapseRate * dryLapseRateScalar * max(0.0, elevation)

            val oceanTemperature = max(
                oceanMinBaseTemp,
                oceanBaseTemp + lerp(insolation, annualInsolation.average(), oceanNowVsAnnualInsolationLerp).pow(
                    oceanNowVsAnnualInsolationLerpPow
                ) * oceanInsolationScale
            ) + warmCurrentAdjustment + coolCurrentAdjustment + elevationAdjustment

            val moistureAdjustedTemperature =
                lerp(
                    moistureCoolingTargetTemperature,
                    localBaseTemperature,
                    max(0.0, 1 - (moisture / maxMoistureForCooling))
                        .pow(moistureCoolingExp)
                        .scaleAndCoerceIn(0.0..1.0, (1 - maxMoistureCoolingLerp)..1.0)
                )

            val adjustedTemperature =
                moistureAdjustedTemperature + warmCurrentAdjustment + coolCurrentAdjustment + elevationAdjustment

            val averageTemperature = if (continentiality < 0) {
                lerp(oceanTemperature, adjustedTemperature, oceanWaterVsLandTemperatureLerp)
            } else if (continentiality == 0) {
                lerp(
                    oceanTemperature,
                    adjustedTemperature,
                    (neighbors.filter { it.isAboveWater }.size / neighbors.size.toDouble())
                        .pow(shoreWaterVsLandTemperatureLerpExp)
                        .adjustRange(0.0..1.0, shoreWaterVsLandTemperatureLerpMin..shoreWaterVsLandTemperatureLerpMax)
                )
            } else {
                lerp(
                    oceanTemperature,
                    adjustedTemperature,
                    min(continentiality * inlandWaterVsLandTemperatureContinentialityScalar, 1.0)
                )
            }

            val celsius = averageTemperature - 273.15
            return celsius
        }

    fun (PlanetTile).calculateClimateDatumMonth(): ClimateDatumMonth {
        return ClimateDatumMonth(
            averageTemperature,
            insolation * 500.0,
            moisture * 120.0
        )
    }

    fun calculateClimate(planet: Planet): Map<PlanetTile, ClimateDatum> {
        val startDate = planet.daysPassed

        val monthToTileClimate = (0..<12).map { i ->
            GD.print("${MonthIndex.values()[i].name}...")
            planet.daysPassed = i * 30
            updatePlanetClimate(planet)
            planet.planetTiles.values.associateWith { it.calculateClimateDatumMonth() }
        }

        val climateData = planet.planetTiles.values.associateWith { tile ->
            ClimateDatum(tile.tileId, monthToTileClimate.map { it[tile]!! })
        }

//        planet.daysPassed = startDate
//        updatePlanetClimate(planet)

        GD.print("CLIMATE DATA")
        GD.print("==============================")

        val maxTempTile = climateData.values.maxBy { it.months.maxOf { month -> month.averageTemperature } }
        val maxTempMonth = maxTempTile.months.indexOf(maxTempTile.months.maxBy { month -> month.averageTemperature })
        GD.print("Max temp: ${planet.planetTiles[maxTempTile.tileId]!!.tile.position.formatGeo()}, ${MonthIndex.values()[maxTempMonth].name}: ${maxTempTile.months[maxTempMonth].averageTemperature}°C")
        val minTempTile = climateData.values.minBy { it.months.minOf { month -> month.averageTemperature } }
        val minTempMonth = minTempTile.months.indexOf(minTempTile.months.minBy { month -> month.averageTemperature })
        GD.print("Min temp: ${planet.planetTiles[minTempTile.tileId]!!.tile.position.formatGeo()}, ${MonthIndex.values()[minTempMonth].name}: ${minTempTile.months[minTempMonth].averageTemperature}°C")
        val maxAverageTempTile = climateData.values.maxBy { it.averageTemperature }
        GD.print("Max avg temp: ${planet.planetTiles[maxAverageTempTile.tileId]!!.tile.position.formatGeo()}: ${maxAverageTempTile.averageTemperature}°C")
        val minAverageTempTile = climateData.values.minBy { it.averageTemperature }
        GD.print("Min avg temp: ${planet.planetTiles[minAverageTempTile.tileId]!!.tile.position.formatGeo()}: ${minAverageTempTile.averageTemperature}°C")

        val maxPrecipTile = climateData.values.maxBy { it.months.maxOf { month -> month.precipitation } }
        val maxPrecipMonth = maxPrecipTile.months.indexOf(maxPrecipTile.months.maxBy { month -> month.precipitation })
        GD.print(
            "Max precipitation: ${planet.planetTiles[maxPrecipTile.tileId]!!.tile.position.formatGeo()}, ${MonthIndex.values()[maxPrecipMonth].name}: ${
                maxPrecipTile.months[maxPrecipMonth].precipitation.formatDigits(
                    1
                )
            }mm"
        )
        val minPrecipTile = climateData.values.minBy { it.months.minOf { month -> month.precipitation } }
        val minPrecipMonth = minPrecipTile.months.indexOf(minPrecipTile.months.minBy { month -> month.precipitation })
        GD.print(
            "Min precipitation: ${planet.planetTiles[minPrecipTile.tileId]!!.tile.position.formatGeo()}, ${MonthIndex.values()[minPrecipMonth].name}: ${
                minPrecipTile.months[minPrecipMonth].precipitation.formatDigits(
                    1
                )
            }mm"
        )
        val maxAveragePrecipTile = climateData.values.maxBy { it.annualPrecipitation }
        GD.print(
            "Max annual precipitation: ${planet.planetTiles[maxAveragePrecipTile.tileId]!!.tile.position.formatGeo()}: ${
                maxAveragePrecipTile.annualPrecipitation.formatDigits(
                    1
                )
            }mm"
        )
        val minAveragePrecipTile = climateData.values.minBy { it.annualPrecipitation }
        GD.print(
            "Min annual precipitation: ${planet.planetTiles[minAveragePrecipTile.tileId]!!.tile.position.formatGeo()}: ${
                minAveragePrecipTile.annualPrecipitation.formatDigits(
                    1
                )
            }mm"
        )

        val maxTempRangeTile = climateData.values.maxBy { it.temperatureRange }
        GD.print(
            "Max temperature range: ${planet.planetTiles[maxTempRangeTile.tileId]!!.tile.position.formatGeo()}: ${
                maxTempRangeTile.temperatureRange.formatDigits(
                    1
                )
            }°C"
        )
        val minTempRangeTile = climateData.values.minBy { it.temperatureRange }
        GD.print(
            "Min temperature range: ${planet.planetTiles[minTempRangeTile.tileId]!!.tile.position.formatGeo()}: ${
                minTempRangeTile.temperatureRange.formatDigits(
                    1
                )
            }°C"
        )

        val maxPrecipRangeTile = climateData.values.maxBy { it.precipitationRange }
        GD.print(
            "Max precipitation range: ${planet.planetTiles[maxPrecipRangeTile.tileId]!!.tile.position.formatGeo()}: ${
                maxPrecipRangeTile.precipitationRange.formatDigits(
                    1
                )
            }mm"
        )
        val minPrecipRangeTile = climateData.values.minBy { it.precipitationRange }
        GD.print(
            "Min precipitation range: ${planet.planetTiles[minPrecipRangeTile.tileId]!!.tile.position.formatGeo()}: ${
                minPrecipRangeTile.precipitationRange.formatDigits(
                    1
                )
            }mm"
        )

        val continentalRain =
            climateData.values.filter { planet.planetTiles[it.tileId]!!.isAboveWater }.sumOf { it.annualPrecipitation }
        val oceanicRain =
            climateData.values.filter { !planet.planetTiles[it.tileId]!!.isAboveWater }.sumOf { it.annualPrecipitation }
        val percentRainOnOcean = oceanicRain / (continentalRain + oceanicRain)
        GD.print(
            "Percent rain on ocean: ${percentRainOnOcean.formatDigits(1)}"
        )

        return climateData
    }

    // inter-tropical convergence zone
    fun (Planet).calculateItcz(): Path<PlanetTile> {
        fun costFn(_1: PlanetTile, tile: PlanetTile): Double =
            1 - lerp(tile.insolation, tile.averageInsolation, itczPathfindingNowVsAnnualInsolationLerp) -
                    max(0.0, tile.continentiality * itczPathfindingContinentialityWeight)

        fun neighborFn(tile: PlanetTile): List<PlanetTile> = tile.neighbors.filter {
            val tileToNeighbor = (it.tile.position - tile.tile.position).normalized()
            val crossProduct = tile.tile.position.cross(tileToNeighbor)
            val dotProduct = crossProduct.dot(Vector3.UP)
            dotProduct > 0
        }

        val startTile = planetTiles.values.minBy { costFn(it, it) }
        val validStartNeighbors = neighborFn(startTile)
        val goal = startTile.neighbors.filter { it !in validStartNeighbors }.maxBy { costFn(it, it) }

        fun goalFn(tile: PlanetTile): Boolean = tile == goal
        fun heuristic(tile: PlanetTile) = costFn(tile, tile)

        val path = AStar.path(startTile, ::goalFn, ::heuristic, ::costFn, ::neighborFn)
        if (path.nodes.size == 0) {
            throw Exception("No path found!")
        }
        return path
    }
}