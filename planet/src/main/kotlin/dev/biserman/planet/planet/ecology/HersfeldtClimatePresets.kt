package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.ClimateDatumSample

/**
 * Readable climate fixtures for ecology experiments. The seven land profiles
 * are copied month-for-month from ecology.ipynb; the ocean profiles extend the
 * same authoring format to representative marine surface and deep-water zones.
 */
data class HersfeldtClimatePreset(
    val id: String,
    val displayName: String,
    val ocean: Boolean,
    val months: List<ClimateDatumSample>,
) {
    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(months.size == 12)
    }

    fun climateDatum(tileId: Int): ClimateDatum = ClimateDatum(tileId, months)
}

object HersfeldtClimatePresets {
    val OCEANIC_TEMPERATE = land(
        "oceanic-temperate",
        "Oceanic temperate",
        temperatures = listOf(6.0, 6.5, 8.0, 10.0, 13.0, 16.0, 18.0, 18.0, 15.5, 12.0, 9.0, 7.0),
        insolations = listOf(60.0, 90.0, 130.0, 180.0, 220.0, 250.0, 260.0, 220.0, 160.0, 110.0, 70.0, 50.0),
        precipitation = listOf(115.0, 100.0, 90.0, 80.0, 75.0, 70.0, 70.0, 75.0, 85.0, 100.0, 115.0, 120.0),
    )
    val DESERT = land(
        "desert",
        "Hot subtropical desert",
        temperatures = listOf(18.0, 20.0, 24.0, 29.0, 33.0, 36.0, 38.0, 37.0, 34.0, 29.0, 23.0, 19.0),
        insolations = listOf(220.0, 245.0, 280.0, 310.0, 330.0, 345.0, 340.0, 325.0, 300.0, 270.0, 235.0, 215.0),
        precipitation = listOf(5.0, 4.0, 4.0, 2.0, 1.0, 0.5, 1.0, 2.0, 3.0, 5.0, 6.0, 6.0),
    )
    val SAVANNA = land(
        "savanna",
        "Tropical savanna",
        temperatures = listOf(24.0, 25.0, 27.0, 29.0, 29.0, 28.0, 27.0, 27.0, 28.0, 28.0, 26.0, 24.0),
        insolations = listOf(235.0, 250.0, 270.0, 280.0, 265.0, 235.0, 220.0, 225.0, 245.0, 265.0, 255.0, 235.0),
        precipitation = listOf(8.0, 12.0, 25.0, 65.0, 130.0, 190.0, 220.0, 185.0, 110.0, 45.0, 15.0, 8.0),
    )
    val JUNGLE = land(
        "jungle",
        "Equatorial rainforest",
        temperatures = listOf(26.0, 26.0, 26.5, 26.5, 26.5, 26.0, 25.5, 26.0, 26.5, 26.5, 26.0, 26.0),
        insolations = listOf(205.0, 215.0, 220.0, 215.0, 200.0, 185.0, 190.0, 205.0, 220.0, 225.0, 215.0, 205.0),
        precipitation = listOf(240.0, 220.0, 260.0, 285.0, 300.0, 245.0, 210.0, 205.0, 225.0, 275.0, 290.0, 260.0),
    )
    val BOREAL = land(
        "boreal",
        "Continental boreal forest",
        temperatures = listOf(-18.0, -15.0, -8.0, 1.0, 9.0, 15.0, 18.0, 15.0, 8.0, 0.0, -9.0, -16.0),
        insolations = listOf(25.0, 55.0, 110.0, 175.0, 225.0, 255.0, 245.0, 190.0, 125.0, 65.0, 30.0, 15.0),
        precipitation = listOf(25.0, 20.0, 22.0, 28.0, 40.0, 60.0, 75.0, 65.0, 50.0, 38.0, 30.0, 25.0),
    )
    val TUNDRA = land(
        "tundra",
        "Arctic tundra",
        temperatures = listOf(-26.0, -25.0, -20.0, -11.0, -3.0, 4.0, 8.0, 6.0, 1.0, -8.0, -18.0, -24.0),
        insolations = listOf(2.0, 18.0, 75.0, 145.0, 205.0, 235.0, 215.0, 160.0, 90.0, 32.0, 5.0, 0.5),
        precipitation = listOf(10.0, 8.0, 8.0, 9.0, 12.0, 18.0, 25.0, 24.0, 18.0, 14.0, 11.0, 10.0),
    )
    val ICE_CAP = land(
        "ice-cap",
        "Permanent ice cap",
        temperatures = listOf(-38.0, -40.0, -38.0, -31.0, -23.0, -17.0, -14.0, -17.0, -24.0, -31.0, -35.0, -37.0),
        insolations = listOf(0.0, 2.0, 35.0, 100.0, 165.0, 205.0, 190.0, 125.0, 55.0, 10.0, 0.0, 0.0),
        precipitation = listOf(5.0, 4.0, 4.0, 4.0, 5.0, 7.0, 9.0, 8.0, 7.0, 6.0, 5.0, 5.0),
    )

    val TROPICAL_REEF = ocean(
        "tropical-reef",
        "Tropical reef sea",
        temperatures = listOf(27.0, 27.0, 27.5, 28.0, 28.5, 29.0, 29.0, 29.0, 28.5, 28.0, 27.5, 27.0),
        insolations = listOf(240.0, 255.0, 275.0, 290.0, 285.0, 270.0, 260.0, 265.0, 280.0, 285.0, 260.0, 240.0),
        precipitation = listOf(120.0, 95.0, 85.0, 90.0, 120.0, 175.0, 210.0, 225.0, 205.0, 190.0, 165.0, 140.0),
    )
    val TEMPERATE_SHELF = ocean(
        "temperate-shelf",
        "Temperate continental shelf",
        temperatures = listOf(8.0, 7.0, 8.0, 10.0, 13.0, 16.0, 18.0, 19.0, 17.0, 14.0, 11.0, 9.0),
        insolations = listOf(65.0, 90.0, 135.0, 185.0, 230.0, 260.0, 270.0, 235.0, 175.0, 120.0, 80.0, 55.0),
        precipitation = listOf(95.0, 85.0, 75.0, 65.0, 60.0, 55.0, 50.0, 55.0, 70.0, 85.0, 100.0, 105.0),
    )
    val POLAR_SEA = ocean(
        "polar-sea",
        "Seasonal polar sea",
        temperatures = listOf(-1.8, -1.8, -1.5, -0.8, 0.5, 2.0, 3.5, 3.0, 1.5, 0.0, -1.0, -1.7),
        insolations = listOf(0.0, 8.0, 55.0, 125.0, 195.0, 245.0, 235.0, 175.0, 95.0, 30.0, 2.0, 0.0),
        precipitation = listOf(18.0, 15.0, 15.0, 18.0, 22.0, 28.0, 32.0, 30.0, 25.0, 22.0, 20.0, 18.0),
    )
    val PERMANENT_SEA_ICE = ocean(
        "permanent-sea-ice",
        "Permanent coastal sea ice",
        temperatures = listOf(-9.0, -10.0, -9.0, -7.0, -4.0, -2.0, -1.0, -1.5, -3.5, -6.0, -8.0, -9.0),
        insolations = listOf(0.0, 4.0, 40.0, 105.0, 175.0, 220.0, 210.0, 150.0, 75.0, 18.0, 0.0, 0.0),
        precipitation = listOf(14.0, 12.0, 12.0, 14.0, 18.0, 24.0, 28.0, 27.0, 22.0, 18.0, 16.0, 14.0),
    )
    val DEEP_OCEAN = ocean(
        "deep-ocean",
        "Dark deep ocean",
        temperatures = listOf(3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0),
        insolations = List(12) { 0.0 },
        precipitation = List(12) { 80.0 },
    )

    val LAND: List<HersfeldtClimatePreset> =
        listOf(OCEANIC_TEMPERATE, DESERT, SAVANNA, JUNGLE, BOREAL, TUNDRA, ICE_CAP)
    val OCEAN: List<HersfeldtClimatePreset> =
        listOf(TROPICAL_REEF, TEMPERATE_SHELF, POLAR_SEA, PERMANENT_SEA_ICE, DEEP_OCEAN)
    val ALL: List<HersfeldtClimatePreset> = LAND + OCEAN

    private fun land(
        id: String,
        displayName: String,
        temperatures: List<Double>,
        insolations: List<Double>,
        precipitation: List<Double>,
    ) = preset(id, displayName, ocean = false, temperatures, insolations, precipitation)

    private fun ocean(
        id: String,
        displayName: String,
        temperatures: List<Double>,
        insolations: List<Double>,
        precipitation: List<Double>,
    ) = preset(id, displayName, ocean = true, temperatures, insolations, precipitation)

    private fun preset(
        id: String,
        displayName: String,
        ocean: Boolean,
        temperatures: List<Double>,
        insolations: List<Double>,
        precipitation: List<Double>,
    ): HersfeldtClimatePreset {
        require(temperatures.size == 12 && insolations.size == 12 && precipitation.size == 12)
        return HersfeldtClimatePreset(
            id,
            displayName,
            ocean,
            temperatures.indices.map { month ->
                ClimateDatumSample(
                    temperatures[month],
                    insolations[month],
                    precipitation[month],
                )
            },
        )
    }
}
