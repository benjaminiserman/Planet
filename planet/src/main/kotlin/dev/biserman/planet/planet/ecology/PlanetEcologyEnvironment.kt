package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.topology.Border
import kotlin.math.max
import kotlin.math.pow

data class PlanetEcologyEnvironmentContext(
    val maximumInsolationWm2: Double,
    val majorRiverBorderIds: Set<Int>,
)

object PlanetEcologyEnvironment {
    private const val MAJOR_RIVER_UPSTREAM_SEGMENTS = 24

    /**
     * Coarse annual proxy for an ocean that retains a meaningful sea-ice
     * compartment: most monthly means freeze, and summer never becomes warm.
     */
    fun supportsSeaIceHabitat(climate: ClimateDatum): Boolean =
        climate.months.count { it.averageTemperature <= 0.0 } >
            climate.months.size / 2 &&
            climate.months.none { it.averageTemperature > 10.0 }

    fun context(planet: Planet): PlanetEcologyEnvironmentContext {
        val maximumInsolation = planet.climateMap.values
            .asSequence()
            .flatMap { it.months.asSequence() }
            .maxOfOrNull { it.insolation }
            ?.coerceAtLeast(1.0)
            ?: 340.0
        val majorRiverBorderIds = planet.riverUpstreamSegmentCounts
            .asSequence()
            .filter { (_, upstreamSegments) -> upstreamSegments >= MAJOR_RIVER_UPSTREAM_SEGMENTS }
            .mapNotNull { (segment, _) ->
                segment.first.borders
                    .firstOrNull { border -> segment.second in border.corners }
                    ?.id
            }
            .toSet()
        return PlanetEcologyEnvironmentContext(maximumInsolation, majorRiverBorderIds)
    }

    fun areaKm2(tile: PlanetTile): Double =
        max(1.0, tile.tile.area * tile.planet.radiusMeters.pow(2) / 1_000_000.0)

    internal fun signedElevationM(tileElevationM: Double): Double = tileElevationM

    fun environment(
        tile: PlanetTile,
        climate: ClimateDatum,
        year: Double,
        context: PlanetEcologyEnvironmentContext,
        resources: FunctionalResources = tile.ecosystem.resources,
    ): SeasonalCellEnvironment {
        val sample = climate.sampleAt(year)
        val anomaly = EcologyClimateVariability.anomaly(tile.tileId, year)
        val isLand = tile.isAboveWater
        val adjacentToOcean = if (isLand) {
            sharedEdgeFraction(tile) { border ->
                !tile.planet.getTile(border.oppositeTile(tile.tile)).isAboveWater
            }
        } else {
            0.0
        }
        val adjacentToLand = if (!isLand) {
            sharedEdgeFraction(tile) { border ->
                tile.planet.getTile(border.oppositeTile(tile.tile)).isAboveWater
            }
        } else {
            0.0
        }
        val adjacentToMajorRiver = if (isLand) {
            sharedEdgeFraction(tile) { it.id in context.majorRiverBorderIds }
        } else {
            0.0
        }
        val waterDepthM = if (isLand) 0.0 else (tile.planet.seaLevel - tile.elevation).coerceAtLeast(0.0)
        val stone = tile.stoneColumn.surface.stoneComponent
        return SeasonalCellEnvironment.create(
            areaKm2 = areaKm2(tile),
            temperatureC = sample.averageTemperature + anomaly.temperatureC,
            annualAverageTemperatureC = climate.averageTemperature,
            insolation = (sample.insolation / context.maximumInsolationWm2).coerceIn(0.0, 1.0),
            precipitationMm =
            (sample.precipitation * anomaly.precipitationMultiplier).coerceAtLeast(0.0),
            surfaceFertilityModifier = stone.fertilityModifier,
            surfaceMoistureCapacityMultiplier =
            stone.moistureCapacityMultiplier.coerceAtLeast(0.05),
            surfaceAcidityModifier = stone.acidityModifier,
            isLand = isLand,
            adjacentToOcean = adjacentToOcean,
            adjacentToLand = adjacentToLand,
            adjacentToMajorRiver = adjacentToMajorRiver,
            elevationM = signedElevationM(tile.elevation),
            waterDepthM = waterDepthM,
            // Depth decides whether a dark compartment exists; illumination
            // decides whether the surface compartment exists. A deep lit ocean
            // therefore exposes both, while a dark hemisphere may expose only
            // dark water.
            usefulSunlightReachesWater =
            !isLand &&
                sample.insolation / context.maximumInsolationWm2 > 0.02,
            permanentSeaIce =
            !isLand &&
                supportsSeaIceHabitat(climate),
            canopyCover = estimatedCanopyCover(climate, isLand),
            reefCover = tile.ecosystem.reefCover.coerceIn(0.0, 1.0),
            starLight = EcologyGlobals.starLight,
            resources = resources,
        )
    }

    fun annualEnvironments(
        tile: PlanetTile,
        climate: ClimateDatum,
        context: PlanetEcologyEnvironmentContext,
    ): List<SeasonalCellEnvironment> =
        (0 until 12).map { month ->
            environment(
                tile = tile,
                climate = climate,
                year = month / 12.0,
                context = context,
                resources = tile.ecosystem.resources,
            )
        }

    private fun estimatedCanopyCover(climate: ClimateDatum, isLand: Boolean): Double {
        if (!isLand) return 0.0
        val moisture = ((climate.annualPrecipitation - 350.0) / 1_900.0).coerceIn(0.0, 1.0)
        val warmth = (1.0 - ((climate.averageTemperature - 18.0) / 42.0).pow(2)).coerceIn(0.0, 1.0)
        return (moisture * warmth * 0.92).coerceIn(0.0, 0.92)
    }

    private fun sharedEdgeFraction(
        tile: PlanetTile,
        matches: (Border) -> Boolean,
    ): Double {
        val perimeter = tile.tile.borders.sumOf { it.length }
        if (perimeter <= 0.0) return 0.0
        return (tile.tile.borders.filter(matches).sumOf { it.length } / perimeter)
            .coerceIn(0.0, 1.0)
    }
}