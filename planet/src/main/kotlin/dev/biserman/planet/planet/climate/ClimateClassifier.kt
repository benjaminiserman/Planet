package dev.biserman.planet.planet.climate

import dev.biserman.planet.planet.Planet
import godot.core.Color

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
}

data class ClimateClassification(val id: String, val name: String, val color: Color, val terrainColor: Color)
interface ClimateClassifier {
    fun classify(planet: Planet, datum: ClimateDatum): ClimateClassification
}