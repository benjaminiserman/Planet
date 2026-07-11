package dev.biserman.planet.planet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RiverNetworkTest {
    @Test
    fun `upstream segment counts accumulate tributaries downstream`() {
        val firstTributary = "a" to "confluence"
        val secondTributary = "b" to "confluence"
        val joinedRiver = "confluence" to "downstream"
        val lowerRiver = "downstream" to "mouth"

        val counts = upstreamSegmentCounts(
            listOf(firstTributary, secondTributary, joinedRiver, lowerRiver)
        )

        assertEquals(0, counts[firstTributary])
        assertEquals(0, counts[secondTributary])
        assertEquals(2, counts[joinedRiver])
        assertEquals(3, counts[lowerRiver])
    }
}
