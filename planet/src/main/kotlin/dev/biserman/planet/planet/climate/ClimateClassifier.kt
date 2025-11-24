package dev.biserman.planet.planet.climate

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import godot.core.Color
import godot.global.GD

enum class MonthIndex {
    JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC;
}

fun <T> (List<T>).monthRange(from: MonthIndex, through: MonthIndex): List<T> {
    val fromIndex = from.ordinal
    val throughIndex = through.ordinal
    if (fromIndex > throughIndex) {
        return this.subList(fromIndex, 12).plus(this.subList(0, throughIndex + 1))
    } else {
        return this.subList(fromIndex, throughIndex + 1)
    }
}

data class ClimateDatumMonth(
    val averageTemperature: Double, // avgT, °C
    val insolation: Double, // W/m²
    val precipitation: Double, // mm
    val minTemperature: Double? = null, // minT, °C
    val maxTemperature: Double? = null, // maxT, °C
)

class ClimateDatum(val tileId: Int, val months: List<ClimateDatumMonth>) {
    val averageTemperature = months.map { it.averageTemperature }.average()
    val annualPrecipitation = months.map { it.precipitation }.sum()
    val temperatureRange = months.maxOf { it.averageTemperature } - months.minOf { it.averageTemperature }
    val precipitationRange = months.maxOf { it.precipitation } - months.minOf { it.precipitation }
}

data class ClimateClassification(val id: String, val name: String, val color: Color, val terrainColor: Color)
interface ClimateClassifier {
    fun classify(planet: Planet, datum: ClimateDatum): ClimateClassification

    companion object {
        fun printCachedStats(planet: Planet) {
            val koppen = planet.planetTiles.values.groupBy { it.koppen.orElse(null) }
            val hersfeldt = planet.planetTiles.values.groupBy { it.hersfeldt.orElse(null) }

            GD.print("==============================")
            GD.print("Koppen Climate Prevalences")
            GD.print("==============================")

            koppen.entries.sortedByDescending { it.value.size }.forEach { (key, values) ->
                GD.print(" - ${values.size} (${(values.size * 100.0 / planet.planetTiles.size).formatDigits(1)}%) - ${key.id} (${key.name})")
            }

            GD.print("==============================")
            GD.print("Hersfeldt Climate Prevalences")
            GD.print("==============================")

            hersfeldt.entries.sortedByDescending { it.value.size }.forEach { (key, values) ->
                GD.print(" - ${values.size} (${(values.size * 100.0 / planet.planetTiles.size).formatDigits(1)}%) - ${key.id} (${key.name})")
            }
        }
    }
}

val UNKNOWN_CLIMATE = ClimateClassification("UNKNOWN", "unknown", Color.black, Color.black)