package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.PlanetTile
import kotlin.math.max

data class FunctionalResources(
    val carrion: Double = 0.0,
    val detritus: Double = 0.0,
    val waste: Double = 0.0,
    val marineSnow: Double = 0.0,
    val fruit: Double = 0.0,
)

/**
 * Standalone ecology input. A later adapter can construct this from PlanetTile,
 * ClimateDatumSample, surface StoneComponent, and river/ocean topology.
 */
class SeasonalCellEnvironment private constructor(
    val areaKm2: Double,
    val temperatureC: Double,
    val annualAverageTemperatureC: Double,
    val insolation: Double,
    val waterAvailability: Double,
    val fertility: Double,
    val acidity: Double,
    val canopyCover: Double,
    val reefCover: Double,
    val snowOrIce: Boolean,
    val starLight: StarLight,
    val isLand: Boolean,
    val adjacentToOcean: Double,
    val adjacentToLand: Double,
    val adjacentToMajorRiver: Double,
    val elevationM: Double,
    val waterDepthM: Double,
    val resources: FunctionalResources,
    private val habitatAvailability: DoubleArray,
) {
    init {
        require(areaKm2 > 0.0)
        require(habitatAvailability.size == Habitat.entries.size)
    }

    fun habitatAvailability(habitat: Habitat): Double = habitatAvailability[habitat.ordinal]

    fun resourceSupport(
        niche: NicheDefinition,
        consumerSize: SizeClass,
    ): Double = niche.strategy.resourceSupport(this, niche.habitat, consumerSize)

    fun lightAt(habitat: Habitat): Double = habitat.availableLight(insolation, canopyCover)

    fun withResources(resources: FunctionalResources): SeasonalCellEnvironment = SeasonalCellEnvironment(
        areaKm2 = areaKm2,
        temperatureC = temperatureC,
        annualAverageTemperatureC = annualAverageTemperatureC,
        insolation = insolation,
        waterAvailability = waterAvailability,
        fertility = fertility,
        acidity = acidity,
        canopyCover = canopyCover,
        reefCover = reefCover,
        snowOrIce = snowOrIce,
        starLight = starLight,
        isLand = isLand,
        adjacentToOcean = adjacentToOcean,
        adjacentToLand = adjacentToLand,
        adjacentToMajorRiver = adjacentToMajorRiver,
        elevationM = elevationM,
        waterDepthM = waterDepthM,
        resources = resources,
        habitatAvailability = habitatAvailability.copyOf(),
    )

    companion object {
        /**
         * Builds the current seasonal ecology input for [tile] from its
         * planet's climate, geology, topology, and stored ecosystem state.
         */
        fun from(
            tile: PlanetTile
        ): SeasonalCellEnvironment {
            val context = tile.planet.planetEcologyEnvironmentContext
            val climate = requireNotNull(tile.planet.climateMap[tile.tileId]) {
                "Climate must be calculated for tile ${tile.tileId}"
            }
            return PlanetEcologyEnvironment.environment(
                tile = tile,
                climate = climate,
                year = tile.planet.historyTurn / 4.0,
                context = context,
                resources = tile.ecosystem.resources,
            )
        }

        fun create(
            areaKm2: Double,
            temperatureC: Double,
            annualAverageTemperatureC: Double = temperatureC,
            insolation: Double,
            precipitationMm: Double,
            surfaceFertilityModifier: Double = 0.0,
            surfaceMoistureCapacityMultiplier: Double = 1.0,
            surfaceAcidityModifier: Double = 0.0,
            isLand: Boolean,
            adjacentToOcean: Double = 0.0,
            adjacentToLand: Double = 0.0,
            adjacentToMajorRiver: Double = 0.0,
            elevationM: Double = 0.0,
            waterDepthM: Double = 0.0,
            usefulSunlightReachesWater: Boolean = true,
            permanentSeaIce: Boolean = false,
            canopyCover: Double = 0.0,
            reefCover: Double = 0.0,
            starLight: StarLight = StarLight.YELLOW,
            resources: FunctionalResources = FunctionalResources(),
            habitatOverrides: Map<Habitat, Double> = emptyMap(),
        ): SeasonalCellEnvironment {
            require(insolation in 0.0..1.0)
            require(precipitationMm >= 0.0)
            require(surfaceMoistureCapacityMultiplier > 0.0)
            require(canopyCover in 0.0..1.0)
            require(reefCover in 0.0..1.0)
            require(adjacentToOcean in 0.0..1.0)
            require(adjacentToLand in 0.0..1.0)
            require(adjacentToMajorRiver in 0.0..1.0)
            // Exposed land can lie below mean sea level in enclosed basins.
            // Water depth remains the separate non-negative aquatic measure.
            require(elevationM.isFinite())
            require(waterDepthM >= 0.0)
            require(!permanentSeaIce || !isLand)

            val evaporationDemand = 45.0 + max(temperatureC, 0.0) * 3.2 + insolation * 95.0
            val retainedPrecipitation = precipitationMm * surfaceMoistureCapacityMultiplier
            var water = retainedPrecipitation / (retainedPrecipitation + evaporationDemand)
            water += adjacentToMajorRiver * 0.30
            if (isLand) water += adjacentToOcean * 0.06
            water = water.coerceIn(0.0, 1.0)

            val habitats = DoubleArray(Habitat.entries.size)
            if (isLand) {
                habitats[Habitat.LAND_SURFACE.ordinal] = 1.0
                habitats[Habitat.CANOPY.ordinal] = canopyCover
                habitats[Habitat.AERIAL.ordinal] = 0.45
                habitats[Habitat.FRESHWATER.ordinal] = adjacentToMajorRiver * 0.42
                habitats[Habitat.COASTAL.ordinal] = adjacentToOcean * 0.48
            } else {
                habitats[Habitat.AERIAL.ordinal] = 0.30
                habitats[Habitat.COASTAL.ordinal] = adjacentToLand
                if (permanentSeaIce) {
                    habitats[Habitat.SEA_ICE.ordinal] = 0.80
                }
                if (usefulSunlightReachesWater) {
                    habitats[Habitat.SUNLIT_WATER.ordinal] = 1.0
                }
                if (!usefulSunlightReachesWater || waterDepthM >= 180.0) {
                    habitats[Habitat.DARK_WATER.ordinal] = if (usefulSunlightReachesWater) 0.75 else 1.0
                }
            }
            habitatOverrides.forEach { (habitat, availability) ->
                habitats[habitat.ordinal] = availability.coerceIn(0.0, 1.0)
            }

            val snowOrIce = permanentSeaIce || (temperatureC < 0.0 && water > annualAverageTemperatureC / 100.0)

            return SeasonalCellEnvironment(
                areaKm2 = areaKm2,
                temperatureC = temperatureC,
                annualAverageTemperatureC = annualAverageTemperatureC,
                insolation = insolation,
                waterAvailability = water,
                fertility = (0.55 + surfaceFertilityModifier * 0.30).coerceIn(0.05, 1.0),
                acidity = surfaceAcidityModifier.coerceIn(-1.0, 1.0),
                canopyCover = canopyCover,
                reefCover = reefCover,
                snowOrIce = snowOrIce,
                starLight = starLight,
                isLand = isLand,
                adjacentToOcean = adjacentToOcean,
                adjacentToLand = adjacentToLand,
                adjacentToMajorRiver = adjacentToMajorRiver,
                elevationM = elevationM,
                waterDepthM = waterDepthM,
                resources = resources,
                habitatAvailability = habitats,
            )
        }
    }
}