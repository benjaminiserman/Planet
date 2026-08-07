package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.ClimateDatumSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HersfeldtClimatePresetsTest {
    @Test
    fun `catalog contains the notebook climates and five ocean climates`() {
        assertEquals(7, HersfeldtClimatePresets.LAND.size)
        assertEquals(5, HersfeldtClimatePresets.OCEAN.size)
        assertEquals(12, HersfeldtClimatePresets.ALL.size)
        assertEquals(
            setOf(
                "oceanic-temperate",
                "desert",
                "savanna",
                "jungle",
                "boreal",
                "tundra",
                "ice-cap",
            ),
            HersfeldtClimatePresets.LAND.mapTo(hashSetOf()) { it.id },
        )
        assertTrue(HersfeldtClimatePresets.LAND.none { it.ocean })
        assertTrue(HersfeldtClimatePresets.OCEAN.all { it.ocean })
        assertEquals(HersfeldtClimatePresets.ALL.size, HersfeldtClimatePresets.ALL.map { it.id }.distinct().size)
    }

    @Test
    fun `notebook desert profile is preserved month for month`() {
        val desert = HersfeldtClimatePresets.DESERT
        assertFalse(desert.ocean)
        assertEquals(
            listOf(18.0, 20.0, 24.0, 29.0, 33.0, 36.0, 38.0, 37.0, 34.0, 29.0, 23.0, 19.0),
            desert.months.map { it.averageTemperature },
        )
        assertEquals(
            listOf(220.0, 245.0, 280.0, 310.0, 330.0, 345.0, 340.0, 325.0, 300.0, 270.0, 235.0, 215.0),
            desert.months.map { it.insolation },
        )
        assertEquals(
            listOf(5.0, 4.0, 4.0, 2.0, 1.0, 0.5, 1.0, 2.0, 3.0, 5.0, 6.0, 6.0),
            desert.months.map { it.precipitation },
        )
    }

    @Test
    fun `marine presets cover illuminated and permanently dark water`() {
        assertTrue(HersfeldtClimatePresets.TROPICAL_REEF.months.all { it.insolation > 0.0 })
        assertTrue(HersfeldtClimatePresets.DEEP_OCEAN.months.all { it.insolation == 0.0 })
        assertTrue(HersfeldtClimatePresets.POLAR_SEA.months.any { it.averageTemperature < 0.0 })
        assertTrue(HersfeldtClimatePresets.PERMANENT_SEA_ICE.months.all { it.averageTemperature < 0.0 })
        assertTrue(HersfeldtClimatePresets.TEMPERATE_SHELF.months.any { it.averageTemperature >= 18.0 })
    }

    @Test
    fun `sea ice requires a freezing majority and no warm summer`() {
        assertTrue(
            PlanetEcologyEnvironment.supportsSeaIceHabitat(
                climate(listOf(-8.0, -7.0, -5.0, -2.0, 0.0, 0.0, 0.0, 4.0, 8.0, 10.0, 3.0, 1.0)),
            ),
        )
        assertFalse(
            PlanetEcologyEnvironment.supportsSeaIceHabitat(
                climate(listOf(-8.0, -7.0, -5.0, -2.0, 0.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0)),
            ),
            "Exactly half the year at or below freezing is insufficient",
        )
        assertFalse(
            PlanetEcologyEnvironment.supportsSeaIceHabitat(
                climate(listOf(-8.0, -7.0, -5.0, -2.0, 0.0, 0.0, 0.0, 2.0, 4.0, 8.0, 10.1, 1.0)),
            ),
            "A month above 10 C excludes the sea-ice habitat",
        )
    }

    private fun climate(temperatures: List<Double>): ClimateDatum =
        ClimateDatum(
            1,
            temperatures.map { temperature ->
                ClimateDatumSample(
                    averageTemperature = temperature,
                    insolation = 100.0,
                    precipitation = 20.0,
                )
            },
        )
}