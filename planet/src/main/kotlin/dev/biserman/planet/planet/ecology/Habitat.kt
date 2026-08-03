package dev.biserman.planet.planet.ecology

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
        canopyCover: Double,
        reefCover: Double,
    ): Double {
        if (color == null) return 0.0
        if (color == BiologicalColor.ADAPTIVE) return 0.35
        if (snowOrIce && color == BiologicalColor.WHITE) return 0.35
        if (aquatic && reefCover > 0.45) {
            return when (color) {
                BiologicalColor.BROWN, BiologicalColor.GREEN, BiologicalColor.PURPLE, BiologicalColor.BLUE, BiologicalColor.RED -> 0.24
                else -> 0.08
            }
        }
        return when (this) {
            CANOPY ->
                when (color) {
                    BiologicalColor.GREEN, BiologicalColor.BROWN -> 0.28
                    else -> 0.05
                }

            LAND_SURFACE -> when (color) {
                BiologicalColor.BROWN -> 0.24
                BiologicalColor.PALE -> if (canopyCover < 0.35) 0.24 else 0.15
                BiologicalColor.GREEN -> if (canopyCover > 0.2) 0.2 else 0.15
                BiologicalColor.WHITE -> if (canopyCover < 0.35) 0.1 else 0.0
                else -> 0.05
            }

            SEA_ICE -> when {
                color == BiologicalColor.WHITE -> 0.20
                else -> 0.05
            }

            FRESHWATER, COASTAL, SUNLIT_WATER ->
                when (color) {
                    BiologicalColor.COUNTERSHADE -> 0.30
                    BiologicalColor.BLUE -> 0.20
                    else -> 0.05
                }

            DARK_WATER ->
                when (color) {
                    BiologicalColor.BLACK -> 0.1
                    BiologicalColor.BLUE -> 0.075
                    BiologicalColor.BROWN -> 0.075
                    else -> 0.05
                }

            AERIAL ->
                when (color) {
                    BiologicalColor.WHITE -> 0.24
                    BiologicalColor.BLUE -> 0.18
                    else -> 0.04
                }
        }
    }
}