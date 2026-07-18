package dev.biserman.planet.planet.climate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClimateDatumTest {
    private val climate = ClimateDatum(
        tileId = 7,
        months = (0 until 12).map { month ->
            ClimateDatumSample(
                averageTemperature = month.toDouble(),
                insolation = month * 10.0,
                precipitation = month * 100.0,
            )
        },
    )

    @Test
    fun `sampleAt returns monthly samples at month boundaries`() {
        assertEquals(climate.months[0], climate.sampleAt(0.0))
        assertEquals(climate.months[6], climate.sampleAt(0.5))
        assertEquals(climate.months[0], climate.sampleAt(1.0))
    }

    @Test
    fun `sampleAt interpolates every climate field and wraps the year`() {
        assertEquals(
            ClimateDatumSample(0.5, 5.0, 50.0),
            climate.sampleAt(1.0 / 24.0),
        )
        assertEquals(
            ClimateDatumSample(5.5, 55.0, 550.0),
            climate.sampleAt(23.0 / 24.0),
        )
    }
}
