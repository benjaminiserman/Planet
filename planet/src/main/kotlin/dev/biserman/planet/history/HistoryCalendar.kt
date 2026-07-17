package dev.biserman.planet.history

enum class Hemisphere {
    NORTHERN,
    SOUTHERN,
}

enum class Season(val displayName: String) {
    SPRING("Spring"),
    SUMMER("Summer"),
    AUTUMN("Autumn"),
    WINTER("Winter"),
}

object HistoryCalendar {
    const val TURNS_PER_YEAR = 4L

    fun year(turn: Long): Long = turn.coerceAtLeast(0L) / TURNS_PER_YEAR

    fun season(turn: Long, hemisphere: Hemisphere): Season {
        val northernSeason = Math.floorMod(turn, TURNS_PER_YEAR).toInt()
        val hemisphereOffset = if (hemisphere == Hemisphere.NORTHERN) 0 else 2
        return Season.entries[(northernSeason + hemisphereOffset) % Season.entries.size]
    }
}
