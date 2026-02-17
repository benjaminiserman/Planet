package dev.biserman.planet.planet.climate

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import godot.core.Color
import godot.global.GD
import kotlin.jvm.optionals.getOrNull

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
//    val minTemperature: Double? = null, // minT, °C
//    val maxTemperature: Double? = null, // maxT, °C
) {

    companion object {
        fun Iterable<ClimateDatumMonth>.average(): ClimateDatumMonth {
            return ClimateDatumMonth(
                this.map { it.averageTemperature }.average(),
                this.map { it.insolation }.average(),
                this.map { it.precipitation }.average(),
            )
        }
    }
}

class ClimateDatum(val tileId: Int, val months: List<ClimateDatumMonth>) {
    val averageTemperature = months.map { it.averageTemperature }.average()
    val annualPrecipitation = months.sumOf { it.precipitation }
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

            GD.print("==============================")
            val arableBiomes = setOf("HAM", "HT", "HM", "TU", "TQ", "CM", "CAM", "CT", "CA", "CD", "EM", "ET")
            val totalLand = planet.planetTiles.values
                .filter { it.isAboveWater }
            val arableLand = totalLand
                .mapNotNull { it.hersfeldt.getOrNull() }
                .count { biome ->
                    arableBiomes.any { biome.id.startsWith(it) }
                }
            val percentArable = arableLand * 100.0 / totalLand.size

            GD.print("Estimated % of arable land: ${percentArable.formatDigits()}%")
        }
    }
}

val UNKNOWN_CLIMATE = ClimateClassification("UNKNOWN", "unknown", Color.black, Color.black)