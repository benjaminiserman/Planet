package dev.biserman.planet.planet.ecology.v2

/**
 * Author-facing ecology vocabulary. Runtime code compiles these values into
 * primitive arrays and never walks trait objects during a seasonal turn.
 */
enum class Habitat(
    val displayName: String,
    val aquatic: Boolean,
) {
    LAND_SURFACE("land-surface", false),
    CANOPY("canopy", false),
    FRESHWATER("freshwater", true),
    COASTAL("coastal", true),
    SUNLIT_WATER("sunlit-water", true),
    DARK_WATER("dark-water", true),
    SEA_ICE("sea-ice", false),
    AERIAL("aerial", false);

    fun availableLight(insolation: Double, canopyCover: Double): Double = when (this) {
        CANOPY, SEA_ICE, AERIAL -> insolation
        LAND_SURFACE -> insolation * (1.0 - canopyCover * 0.72)
        COASTAL, FRESHWATER, SUNLIT_WATER -> insolation * (1.0 - canopyCover * 0.15)
        DARK_WATER -> 0.0
    }.coerceIn(0.0, 1.0)

    fun camouflageMatch(
        color: BiologicalColor?,
        snowOrIce: Boolean,
        reefCover: Double,
    ): Double {
        if (color == null) return 0.0
        if (snowOrIce && color == BiologicalColor.PALE) return 0.35
        if (aquatic && reefCover > 0.45) {
            return when (color) {
                BiologicalColor.BROWN, BiologicalColor.GREEN, BiologicalColor.PURPLE -> 0.24
                else -> 0.08
            }
        }
        return when (this) {
            CANOPY ->
                if (color == BiologicalColor.GREEN || color == BiologicalColor.BROWN) 0.28 else 0.05

            LAND_SURFACE, SEA_ICE ->
                if (color == BiologicalColor.BROWN || color == BiologicalColor.PALE) 0.24 else 0.05

            FRESHWATER, COASTAL, SUNLIT_WATER, DARK_WATER ->
                if (color == BiologicalColor.BLUE_GREEN || color == BiologicalColor.BLACK) 0.22 else 0.05

            AERIAL ->
                if (color == BiologicalColor.PALE || color == BiologicalColor.BLUE_GREEN) 0.18 else 0.04
        }
    }
}
