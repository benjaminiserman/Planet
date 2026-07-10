package dev.biserman.planet.geometry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeometryTest {
    @Test
    fun `longitude distance wraps across the date line`() {
        assertEquals(0.0, longitudeDistanceDegrees(-10.0, 350.0))
        assertEquals(2.0, longitudeDistanceDegrees(-179.0, 179.0))
    }
}
