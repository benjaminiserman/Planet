package dev.biserman.planet.planet.ecology.v2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcologySeasonalDynamicsTest {
    @Test
    fun `climate anomalies are deterministic and remain within authored bounds`() {
        val first = EcologyClimateVariability.anomaly(tileId = 42, year = 17.25)
        val repeated = EcologyClimateVariability.anomaly(tileId = 42, year = 17.25)

        assertEquals(first, repeated)
        repeat(1_000) { quarter ->
            val anomaly = EcologyClimateVariability.anomaly(tileId = 42, year = quarter / 4.0)
            assertTrue(anomaly.temperatureC in -2.0..2.0)
            assertTrue(anomaly.precipitationMultiplier in 0.75..1.25)
        }
    }

    @Test
    fun `detritus accessibility prevents complete alternating depletion`() {
        var level = 0.30
        repeat(20) {
            level = OrganicPoolDynamics.update(
                previousLevel = level,
                producedBiomassKg = 8_000.0,
                consumedBiomassKg = 1_000_000.0,
                biomassKgPerLevel = 480_000.0,
                seasonalRetention = 0.68,
                maximumAccessibleFraction = 0.75,
            )
            assertTrue(level > 0.0)
        }
    }
}
