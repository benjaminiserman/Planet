import dev.biserman.planet.planet.ecology.BiologicalColor
import dev.biserman.planet.planet.ecology.StarLight

object LightColorModel {
    data class PhotosyntheticCompatibility(
        val byPigment: Map<BiologicalColor, Double>,
    ) {
        init {
            require(byPigment.values.all { it in 0.0..1.0 })
        }
    }

    val authoredCompatibility: Map<StarLight, PhotosyntheticCompatibility> = mapOf(
        StarLight.BLUE_WHITE to compatibility(0.84, 0.64, 0.76, 0.94, 0.58, 0.82, 0.38),
        StarLight.WHITE to compatibility(0.90, 0.70, 0.88, 0.90, 0.72, 0.86, 0.42),
        StarLight.YELLOW to compatibility(0.90, 0.72, 1.00, 0.86, 0.78, 0.84, 0.42),
        StarLight.ORANGE to compatibility(0.88, 0.76, 0.90, 0.74, 0.94, 0.82, 0.40),
        StarLight.RED to compatibility(0.82, 0.74, 0.66, 0.54, 1.00, 0.88, 0.36),
    )

    private val compiledCompatibility: Array<DoubleArray> by lazy {
        Array(StarLight.entries.size) { starIndex ->
            val starLight = StarLight.entries[starIndex]
            val compatibility = requireNotNull(authoredCompatibility[starLight]) {
                "Missing photosynthetic compatibility for $starLight"
            }
            DoubleArray(BiologicalColor.entries.size) { colorIndex ->
                val color = BiologicalColor.entries[colorIndex]
                requireNotNull(compatibility.byPigment[color]) {
                    "Missing $color photosynthetic compatibility for $starLight"
                }
            }
        }
    }

    fun photosyntheticMatch(starLight: StarLight, pigment: BiologicalColor): Double =
        compiledCompatibility[starLight.ordinal][pigment.ordinal]

    private fun compatibility(
        black: Double,
        brown: Double,
        green: Double,
        blueGreen: Double,
        red: Double,
        purple: Double,
        pale: Double,
        white: Double = pale,
        countershade: Double = pale,
    ) = PhotosyntheticCompatibility(
        mapOf(
            BiologicalColor.BLACK to black,
            BiologicalColor.BROWN to brown,
            BiologicalColor.GREEN to green,
            BiologicalColor.BLUE to blueGreen,
            BiologicalColor.RED to red,
            BiologicalColor.PURPLE to purple,
            BiologicalColor.PALE to pale,
            BiologicalColor.WHITE to white,
            BiologicalColor.COUNTERSHADE to countershade,
        ).let { it.plus(BiologicalColor.ADAPTIVE to it.values.max()) },
    )
}
