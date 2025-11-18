package dev.biserman.planet.planet.climate

import dev.biserman.planet.geometry.GeoPoint
import dev.biserman.planet.planet.PlanetTile
import godot.core.Color

enum class MonthIndex {
    JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC;
}

fun (List<ClimateDatumMonth>).subList(from: MonthIndex, through: MonthIndex): List<ClimateDatumMonth> {
    val fromIndex = from.ordinal
    val throughIndex = through.ordinal
    if (fromIndex > throughIndex) {
        return this.subList(fromIndex, 12).plus(this.subList(0, throughIndex + 1))
    } else {
        return this.subList(fromIndex, throughIndex + 1)
    }
}

data class ClimateDatumMonth(
    val minTemperature: Double, // minT, °C
    val maxTemperature: Double, // maxT, °C
    val insolation: Double, // W/m²
    val precipitation: Double, // mm
) {
    val averageTemperature = (minTemperature + maxTemperature) * 0.5 // °C
}

class ClimateDatum(val tile: PlanetTile, val months: List<ClimateDatumMonth>) {
    val averageTemperature = months.map { it.averageTemperature }.average()
    val annualPrecipitation = months.map { it.precipitation }.sum()
}

data class Classification(val id: String, val name: String, val color: Color, val terrainColor: Color)
interface ClimateClassifier {
    fun classify(datum: ClimateDatum): Classification
}