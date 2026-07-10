package dev.biserman.planet.geometry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeometryTest {
    @Test
    fun `longitude distance wraps across the date line`() {
        assertEquals(0.0, longitudeDistanceDegrees(-10.0, 350.0))
        assertEquals(2.0, longitudeDistanceDegrees(-179.0, 179.0))
    }

    @Test
    fun `best date line centers land after avoiding the edge`() {
        assertEquals(180, bestDateLineDegrees(listOf(0.0 to 1.0)))
        assertEquals(170, bestDateLineDegrees(listOf(-10.0 to 1.0)))
    }
}
