package dev.biserman.planet.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HistoryCalendarTest {
    @Test
    fun `four seasonal turns advance one year`() {
        assertEquals(0L, HistoryCalendar.year(0))
        assertEquals(0L, HistoryCalendar.year(3))
        assertEquals(1L, HistoryCalendar.year(4))
        assertEquals(2L, HistoryCalendar.year(8))
    }

    @Test
    fun `northern seasons begin with spring and repeat each year`() {
        assertEquals(Season.SPRING, HistoryCalendar.season(0, Hemisphere.NORTHERN))
        assertEquals(Season.SUMMER, HistoryCalendar.season(1, Hemisphere.NORTHERN))
        assertEquals(Season.AUTUMN, HistoryCalendar.season(2, Hemisphere.NORTHERN))
        assertEquals(Season.WINTER, HistoryCalendar.season(3, Hemisphere.NORTHERN))
        assertEquals(Season.SPRING, HistoryCalendar.season(4, Hemisphere.NORTHERN))
    }

    @Test
    fun `southern seasons are opposite northern seasons`() {
        assertEquals(Season.AUTUMN, HistoryCalendar.season(0, Hemisphere.SOUTHERN))
        assertEquals(Season.WINTER, HistoryCalendar.season(1, Hemisphere.SOUTHERN))
        assertEquals(Season.SPRING, HistoryCalendar.season(2, Hemisphere.SOUTHERN))
        assertEquals(Season.SUMMER, HistoryCalendar.season(3, Hemisphere.SOUTHERN))
    }
}
