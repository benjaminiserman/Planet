package dev.biserman.planet.planet.climate

import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.scaleAndCoerce01
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.gui.Gui
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
import dev.biserman.planet.planet.climate.ClimateDatumMonth.Companion.average
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
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.averageInsolationMoistureCutoff
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.baseTemperature
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.baseTemperatureInsolationScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.climateSimulationSamplesPerMonth
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentAirPressureContinentialityCenter
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentAirPressureMaxContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentAirPressureMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentAirPressureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentMoistureAverageInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentMoistureDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentMoistureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentTemperatureAverageInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentTemperatureDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentTemperatureInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.coolCurrentTemperatureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.dryLapseRate
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.dryLapseRateScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.equatorMoistureEffectInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.equatorMoistureEffectMaxContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.equatorMoistureEffectMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.equatorMoistureEffectScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectLatitude
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectMax
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectMaxContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.ferrelMoistureEffectScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.hadleyMoistureEffectInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.hadleyMoistureEffectLatitude
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.hadleyMoistureEffectMax
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.hadleyMoistureEffectMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.hadleyMoistureEffectScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.inlandWaterVsLandTemperatureContinentialityBase
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.inlandWaterVsLandTemperatureContinentialityScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.inlandWaterVsLandTemperatureContinentialityScalarMax
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.insolationToWm2
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
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.moistureToMm
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanBaseTemp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanInsolationScale
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanMinBaseTemp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanMoistureInsolationNowVsAnnualLerp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanMoistureInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanNowVsAnnualInsolationLerp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanNowVsAnnualInsolationLerpPow
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanPrecipitationScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanWaterVsLandTemperatureLerp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.oceanWaterVsLandTemperatureLerpScalar
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.shoreWaterVsLandTemperatureLerpExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.shoreWaterVsLandTemperatureLerpMax
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.shoreWaterVsLandTemperatureLerpMin
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.upslopeMoistureExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.upslopePrecipitationFactor
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentAirPressureContinentialityCenter
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentAirPressureMaxContinentiality
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentAirPressureMaxDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentAirPressureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentMoistureAverageInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentMoistureDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentMoistureStrength
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentTemperatureAverageInsolationExp
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentTemperatureDistance
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.warmCurrentTemperatureInsolationExp
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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.measureTime


object ClimateSimulation {
    data class Band(val latitude: Double, val pressureDelta: Double)
    private data class MoistureRoute(
        val neighborId: Int,
        val moistureFraction: Double,
        val upslopePrecipitation: Double
    )

    @Suppress("UnusedUnaryOperator")
    val bands = listOf(
        +180 to +5.0,
        +90 to +5.0,
        +60 to -7.5,
        +30 to +7.5,
        0 to +5.0,
        -30 to +7.5,
        -60 to -7.5,
        -90 to +5.0,
        -180 to +5.0,
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
        lateinit var oceanCurrents: List<OceanCurrent>
        val generateOceanCurrentsTime = measureTime {
            oceanCurrents = OceanCurrents.viaEarthlikeHeuristic(planet, 7)
        }
        val indexOceanCurrentsTime = measureTime {
            planet.oceanCurrents = oceanCurrents
                .distinctBy { it.planetTile }
                .associate { it.planetTile.tileId to it }
                .toMutableMap()
        }
        val currentDistanceMapsTime = measureTime {
            planet.updateCurrentDistanceMap()
        }

        lateinit var itcz: Set<PlanetTile>
        val calculateItczTime = measureTime {
            itcz = planet.calculateItcz().nodes.toSet()
        }
        val itczDistanceMapTime = measureTime {
            planet.itczDistanceMap = PlanetRegion(planet, planet.planetTiles.values.toMutableSet())
                .calculateEdgeDepthMap { it in itcz }
                .mapValues { it.value + if (it.key in itcz) -1 else 0 }
                .mapKeys { it.key.tileId }
        }

        val simulateMoistureTime = measureTime {
            simulateMoisture(planet)
        }

        GD.print("climate update breakdown:")
        GD.print(" - generateOceanCurrents: ${generateOceanCurrentsTime.inWholeMilliseconds}ms")
        GD.print(" - indexOceanCurrents: ${indexOceanCurrentsTime.inWholeMilliseconds}ms")
        GD.print(" - updateCurrentDistanceMaps: ${currentDistanceMapsTime.inWholeMilliseconds}ms")
        GD.print(" - calculateItcz: ${calculateItczTime.inWholeMilliseconds}ms")
        GD.print(" - calculateItczDistanceMap: ${itczDistanceMapTime.inWholeMilliseconds}ms")
        GD.print(" - simulateMoisture: ${simulateMoistureTime.inWholeMilliseconds}ms")
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
                            (warmCurrentAirPressureContinentialityCenter - continentiality).absoluteValue
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
                    .scaleAndCoerce01(0.0..coolCurrentAirPressureMaxDistance) * coolContinentialityFactor * insolation
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
                ((itczAirPressureMaxDistance - (planet.itczDistanceMap[tileId]
                    ?: return 0.0)) / itczAirPressureMaxDistance)
                    .coerceIn(0.0..1.0)

        return basePressure + lerp(
            nearestBandBelow.pressureDelta,
            nearestBandAbove.pressureDelta,
            (adjustedLatitude - nearestBandBelow.latitude) / (nearestBandAbove.latitude - nearestBandBelow.latitude)
        ) + seasonalAdjustment + elevationAdjustment + itczAdjustment + oceanCurrentAdjustment
    }

    fun simulateMoisture(planet: Planet) {
        val tiles = planet.planetTiles.values.toList()
        val effectiveWarmCurrentMoistureDistance =
            warmCurrentMoistureDistance * ClimateRuntimeConfig.oceanCurrentDistanceScale
        val effectiveCoolCurrentMoistureDistance =
            coolCurrentMoistureDistance * ClimateRuntimeConfig.oceanCurrentDistanceScale
        var currentMoisture = DoubleArray(planet.topology.tiles.size)
        val finalMoisture = DoubleArray(planet.topology.tiles.size)
        val initializeMoistureTime = measureTime {
            for (tile in tiles) {
                val geoPoint = tile.tile.position.toGeoPoint()
                val equatorEffect = equatorMoistureEffectScalar *
                        tile.insolation.pow(equatorMoistureEffectInsolationExp) *
                        max(0.0, 1 - geoPoint.latitudeDegrees.absoluteValue / equatorMoistureEffectMaxDistance) *
                        max(0.0, 1 - (tile.continentiality / equatorMoistureEffectMaxContinentiality))
                val ferrelEffect = ferrelMoistureEffectScalar *
                        tile.insolation.pow(ferrelMoistureEffectInsolationExp) *
                        (1 - (geoPoint.latitudeDegrees.absoluteValue - ferrelMoistureEffectLatitude).absoluteValue / ferrelMoistureEffectMaxDistance)
                            .coerceIn(0.0, ferrelMoistureEffectMax) *
                        max(0.0, 1 - (tile.continentiality / ferrelMoistureEffectMaxContinentiality))
                val hadleyEffect = hadleyMoistureEffectScalar *
                        tile.insolation.pow(hadleyMoistureEffectInsolationExp) *
                        (1 - (geoPoint.latitudeDegrees.absoluteValue - hadleyMoistureEffectLatitude).absoluteValue / hadleyMoistureEffectMaxDistance)
                            .coerceIn(0.0, hadleyMoistureEffectMax)
                val polarEffect = (tile.averageInsolation / averageInsolationMoistureCutoff).coerceIn(0.0..1.0)
                val oceanEffect = if (tile.isAboveWater) 0.0
                else {
                    val oceanCurrentContinentialityScalar = 1 / maxOceanCurrentMoistureContinentiality
                    val oceanCurrentContinentialityFactor = if (tile.continentiality >= 0) 1.0 else {
                        max(0.0, 1.0 + (tile.continentiality + 1) * oceanCurrentContinentialityScalar)
                    }
                    val oceanMoistureInsolation =
                        lerp(tile.insolation, tile.averageInsolation, oceanMoistureInsolationNowVsAnnualLerp).pow(
                            oceanMoistureInsolationExp
                        )
                    val warmCurrentEffect =
                        warmCurrentMoistureStrength * ClimateRuntimeConfig.oceanCurrentScale *
                                oceanMoistureInsolation * tile.averageInsolation.pow(
                            warmCurrentMoistureAverageInsolationExp
                        ) * oceanCurrentContinentialityFactor *
                                max(
                                    effectiveWarmCurrentMoistureDistance -
                                            (planet.warmCurrentDistanceMap[tile.tileId]?.toDouble()
                                                ?: effectiveWarmCurrentMoistureDistance), 0.0
                                )
                    val coolCurrentEffect =
                        coolCurrentMoistureStrength * ClimateRuntimeConfig.oceanCurrentScale *
                                oceanMoistureInsolation * tile.averageInsolation.pow(
                            coolCurrentMoistureAverageInsolationExp
                        ) * oceanCurrentContinentialityFactor *
                                max(
                                    effectiveCoolCurrentMoistureDistance -
                                            (planet.coolCurrentDistanceMap[tile.tileId]?.toDouble()
                                                ?: effectiveCoolCurrentMoistureDistance), 0.0
                                )

                    ((oceanMoistureInsolation + warmCurrentEffect + coolCurrentEffect) * polarEffect)
                }
                currentMoisture[tile.tileId] =
                    ((equatorEffect + ferrelEffect + hadleyEffect + oceanEffect) *
                            startingMoistureMultiplier * ClimateRuntimeConfig.moistureScale)
                    .coerceIn((minStartingMoisture * ClimateRuntimeConfig.moistureScale)..(maxStartingMoisture * ClimateRuntimeConfig.moistureScale))
            }
        }
        val startingMoistureSum = currentMoisture.sum()

        val moistureRoutes = Array<List<MoistureRoute>>(planet.topology.tiles.size) { emptyList() }
        val prepareRoutesTime = measureTime {
            for (tile in tiles) {
                val neighbors = tile.neighbors
                val weights = DoubleArray(neighbors.size)
                val slopes = DoubleArray(neighbors.size)
                val prevailingWind = tile.prevailingWind
                var totalWeight = 0.0

                for (index in neighbors.indices) {
                    val neighbor = neighbors[index]
                    val rawWeight = if (prevailingWind == Vector3.ZERO) {
                        1.0
                    } else {
                        val delta = (neighbor.tile.position - tile.tile.position).normalized()
                        prevailingWind.dot(delta) + ClimateRuntimeConfig.backwardsWind
                    }
                    if (rawWeight <= 0.0) continue

                    val slope = tile.slopeAboveWaterTo(neighbor)
                    val weight = rawWeight *
                            (1 - slope / windBlockingSlope).coerceIn(1 - maxWindBlocking..1.0) *
                            tile.tile.borderFor(neighbor.tile).length.pow(0.1)
                    weights[index] = weight
                    slopes[index] = slope
                    totalWeight += weight
                }

                val routes = ArrayList<MoistureRoute>(neighbors.size)
                for (index in neighbors.indices) {
                    if (weights[index] == 0.0) continue
                    routes.add(
                        MoistureRoute(
                            neighbors[index].tileId,
                            if (totalWeight == 0.0) 0.0 else weights[index] / totalWeight,
                            (slopes[index] / upslopeOfMinMoisture)
                                .pow(upslopeMoistureExp)
                                .coerceIn(minUpslopeMoisture..1.0)
                        )
                    )
                }
                moistureRoutes[tile.tileId] = routes
            }
        }

        var steps = 0
        val effectiveMoisturePropagationMultiplier =
            moisturePropagationMultiplier * ClimateRuntimeConfig.moisturePropagationScale
        val propagateMoistureTime = measureTime {
            var nextMoisture = DoubleArray(currentMoisture.size)
            while (currentMoisture.any { it > 0.01 } && steps < maxMoistureSteps) {
                steps += 1
                nextMoisture.fill(0.0)

                for (tile in tiles) {
                    val tileId = tile.tileId
                    val moisture = currentMoisture[tileId]
                    for (route in moistureRoutes[tileId]) {
                        val moistureProvided = moisture * route.moistureFraction
                        val slopePrecipitationFactor = route.upslopePrecipitation *
                                (1 - (finalMoisture[tileId] / saturationThreshold).pow(2))
                                    .coerceIn(0.0..1.0)
                        val precipitation = moistureProvided * max(minPrecipitation, slopePrecipitationFactor)
                        finalMoisture[tileId] += precipitation * (1 - upslopePrecipitationFactor)
                        finalMoisture[route.neighborId] += precipitation * upslopePrecipitationFactor
                        nextMoisture[route.neighborId] +=
                            moistureProvided * effectiveMoisturePropagationMultiplier - precipitation
                    }
                }

                val previousMoisture = currentMoisture
                currentMoisture = nextMoisture
                nextMoisture = previousMoisture
            }
        }

        val effectiveItczMoistureScalar =
            1.0 + (itczMoistureScalar - 1.0) * ClimateRuntimeConfig.monsoonScale
        val effectiveItczMoistureMaxDistance =
            itczMoistureMaxDistance * ClimateRuntimeConfig.monsoonDistanceScale
        val adjustMoistureTime = measureTime {
            for (tile in tiles) {
                val itczEffect = max(
                    0.0,
                    1 - planet.itczDistanceMap[tile.tileId]!! / effectiveItczMoistureMaxDistance
                )
                    .pow(itczMoistureExp)
                    .adjustRange(0.0..1.0, 1.0..effectiveItczMoistureScalar)
                finalMoisture[tile.tileId] *=
                    itczEffect * if (tile.isAboveWater) landPrecipitationScalar else oceanPrecipitationScalar
            }
        }
        val smoothMoistureTime = measureTime {
            for (tile in tiles) {
                var moisture = finalMoisture[tile.tileId]
                var sampleCount = 1
                for (neighbor in tile.neighbors) {
                    if (neighbor.isAboveWater == tile.isAboveWater) {
                        moisture += finalMoisture[neighbor.tileId]
                        sampleCount += 1
                    }
                }
                tile.moisture = moisture / sampleCount
            }
        }
        GD.print("Finished moisture simulation in $steps/$maxMoistureSteps steps. Remaining moisture: ${currentMoisture.sum()} / $startingMoistureSum")
        GD.print("moisture simulation breakdown:")
        GD.print(" - initializeMoisture: ${initializeMoistureTime.inWholeMilliseconds}ms")
        GD.print(" - prepareMoistureRoutes: ${prepareRoutesTime.inWholeMilliseconds}ms")
        GD.print(" - propagateMoisture ($steps steps): ${propagateMoistureTime.inWholeMilliseconds}ms")
        GD.print(" - adjustMoisture: ${adjustMoistureTime.inWholeMilliseconds}ms")
        GD.print(" - smoothMoisture: ${smoothMoistureTime.inWholeMilliseconds}ms")
    }

    val (PlanetTile).averageTemperature: Double
        get() {
            val geoPoint = tile.position.toGeoPoint()
            val greenhouseOffset = ClimateRuntimeConfig.greenhouseTemperatureOffset
            val insolationTemperatureSign = ClimateRuntimeConfig.insolationTemperatureSign
            val landInsolationScale =
                (baseTemperatureInsolationScalar + ClimateRuntimeConfig.insolationTemperatureOffset) *
                        insolationTemperatureSign
            val effectiveOceanInsolationScale =
                (oceanInsolationScale + ClimateRuntimeConfig.insolationTemperatureOffset) *
                        insolationTemperatureSign
            val effectiveWarmCurrentTemperatureDistance =
                warmCurrentTemperatureDistance * ClimateRuntimeConfig.oceanCurrentDistanceScale
            val effectiveCoolCurrentTemperatureDistance =
                coolCurrentTemperatureDistance * ClimateRuntimeConfig.oceanCurrentDistanceScale
            val localBaseTemperature =
                baseTemperature + greenhouseOffset + insolation * landInsolationScale

            val oceanCurrentContinentialityScalar = 1 / maxOceanCurrentTemperatureContinentiality
            val oceanCurrentContinentialityFactor = if (continentiality >= 0) 1.0 else {
                max(0.0, 1.0 + (continentiality + 1) * oceanCurrentContinentialityScalar)
            }
            val warmCurrentAdjustment =
                warmCurrentTemperatureStrength * ClimateRuntimeConfig.oceanCurrentScale * max(
                    effectiveWarmCurrentTemperatureDistance -
                            (planet.warmCurrentDistanceMap[tileId] ?: return 0.0),
                    0.0
                ) * max(insolation, 0.1).pow(warmCurrentTemperatureInsolationExp) * averageInsolation.pow(
                    warmCurrentTemperatureAverageInsolationExp
                ) * oceanCurrentContinentialityFactor
            val coolCurrentAdjustment =
                coolCurrentTemperatureStrength * ClimateRuntimeConfig.oceanCurrentScale * max(
                    effectiveCoolCurrentTemperatureDistance -
                            (planet.coolCurrentDistanceMap[tileId] ?: return 0.0),
                    0.0
                ) * max(insolation, 0.1).pow(coolCurrentTemperatureInsolationExp) * averageInsolation.pow(
                    coolCurrentTemperatureAverageInsolationExp
                ) * oceanCurrentContinentialityFactor

            val elevationAdjustment = dryLapseRate * ClimateRuntimeConfig.lapseRateSign *
                    dryLapseRateScalar * max(0.0, elevation)

            val oceanTemperature = max(
                oceanMinBaseTemp + greenhouseOffset,
                oceanBaseTemp + greenhouseOffset +
                        lerp(insolation, annualInsolation.average(), oceanNowVsAnnualInsolationLerp).pow(
                    oceanNowVsAnnualInsolationLerpPow
                ) * effectiveOceanInsolationScale
            ) + warmCurrentAdjustment + coolCurrentAdjustment + elevationAdjustment

            val moistureAdjustedTemperature =
                lerp(
                    moistureCoolingTargetTemperature,
                    localBaseTemperature,
                    max(0.0, 1 - (moisture / maxMoistureForCooling).pow(moistureCoolingExp))
                        .scaleAndCoerceIn(0.0..1.0, (1 - maxMoistureCoolingLerp)..1.0)
                )

            val adjustedTemperature =
                moistureAdjustedTemperature + warmCurrentAdjustment + coolCurrentAdjustment + elevationAdjustment

            val averageTemperature = if (continentiality < 0) {
                lerp(
                    adjustedTemperature,
                    oceanTemperature,
                    min(oceanWaterVsLandTemperatureLerp + -continentiality * oceanWaterVsLandTemperatureLerpScalar, 1.0)
                )
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
                    min(
                        (continentiality - 1) * inlandWaterVsLandTemperatureContinentialityScalar + inlandWaterVsLandTemperatureContinentialityBase,
                        inlandWaterVsLandTemperatureContinentialityScalarMax
                    )
                )
            }

            val hotspotTemperatureOffset =
                hotspot.scaleAndCoerceIn(0.0..0.1, 0.0..1.0) * ClimateRuntimeConfig.maxHotspotTemperatureOffset
            val celsius = averageTemperature - 273.15 + hotspotTemperatureOffset
            return celsius
        }

    fun (PlanetTile).calculateClimateDatumMonth(): ClimateDatumMonth {
        return ClimateDatumMonth(
            averageTemperature,
            lightLevel * insolationToWm2,
            moisture * moistureToMm
        )
    }

    fun calculateClimate(planet: Planet): Map<PlanetTile, ClimateDatum> {
        val startDate = planet.daysPassed
        val months = 12
        val totalSamples = months * climateSimulationSamplesPerMonth
        val monthToTileClimate = (0..<totalSamples).map { i ->
            if (i % climateSimulationSamplesPerMonth == 0) {
                GD.print("${MonthIndex.entries[i / climateSimulationSamplesPerMonth].name}...")
            }
            planet.daysPassed = (i * (yearLength / totalSamples)).roundToInt()
            updatePlanetClimate(planet)
            planet.planetTiles.values.associateWith { it.calculateClimateDatumMonth() }
        }.chunked(climateSimulationSamplesPerMonth).map { samples ->
            planet.planetTiles.values.associateWith { tile -> samples.map { it[tile]!! }.average() }
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
        GD.print("Max temp: ${planet.planetTiles[maxTempTile.tileId]!!.tile.position.formatGeo()}, ${MonthIndex.entries[maxTempMonth].name}: ${maxTempTile.months[maxTempMonth].averageTemperature}°C")
        val minTempTile = climateData.values.minBy { it.months.minOf { month -> month.averageTemperature } }
        val minTempMonth = minTempTile.months.indexOf(minTempTile.months.minBy { month -> month.averageTemperature })
        GD.print("Min temp: ${planet.planetTiles[minTempTile.tileId]!!.tile.position.formatGeo()}, ${MonthIndex.entries[minTempMonth].name}: ${minTempTile.months[minTempMonth].averageTemperature}°C")
        val maxAverageTempTile = climateData.values.maxBy { it.averageTemperature }
        GD.print("Max avg temp: ${planet.planetTiles[maxAverageTempTile.tileId]!!.tile.position.formatGeo()}: ${maxAverageTempTile.averageTemperature}°C")
        val minAverageTempTile = climateData.values.minBy { it.averageTemperature }
        GD.print("Min avg temp: ${planet.planetTiles[minAverageTempTile.tileId]!!.tile.position.formatGeo()}: ${minAverageTempTile.averageTemperature}°C")

        val maxPrecipTile = climateData.values.maxBy { it.months.maxOf { month -> month.precipitation } }
        val maxPrecipMonth = maxPrecipTile.months.indexOf(maxPrecipTile.months.maxBy { month -> month.precipitation })
        GD.print(
            "Max precipitation: ${planet.planetTiles[maxPrecipTile.tileId]!!.tile.position.formatGeo()}, ${MonthIndex.entries[maxPrecipMonth].name}: ${
                maxPrecipTile.months[maxPrecipMonth].precipitation.formatDigits(
                    1
                )
            }mm"
        )
        val minPrecipTile = climateData.values.minBy { it.months.minOf { month -> month.precipitation } }
        val minPrecipMonth = minPrecipTile.months.indexOf(minPrecipTile.months.minBy { month -> month.precipitation })
        GD.print(
            "Min precipitation: ${planet.planetTiles[minPrecipTile.tileId]!!.tile.position.formatGeo()}, ${MonthIndex.entries[minPrecipMonth].name}: ${
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

    fun toPrecipitation(moisture: Double) = moisture * 2500
}
