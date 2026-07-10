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
    fun `best date line chooses the center of the widest ocean corridor`() {
        val coverage = intArrayOf(1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1)

        assertEquals(
            120.0,
            bestOceanCorridorDateLineDegrees(coverage, listOf(-60.0 to 1.0))
        )
    }

    @Test
    fun `best date line centers land when ocean corridors are equally wide`() {
        val coverage = intArrayOf(0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1)

        assertEquals(
            180.0,
            bestOceanCorridorDateLineDegrees(coverage, listOf(0.0 to 1.0))
        )
    }
}
