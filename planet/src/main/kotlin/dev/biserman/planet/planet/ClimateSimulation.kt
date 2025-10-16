package dev.biserman.planet.planet

import dev.biserman.planet.gui.Gui
import kotlin.math.floor

object ClimateSimulation {
    data class Band(val latitude: Double, val pressureDelta: Double)

    @Suppress("UnusedUnaryOperator")
    val bands = listOf(
        +90 to +5,
        +60 to -10,
        +30 to +10,
        0 to +5,
        -30 to +10,
        -60 to -10,
        -90 to +5,
    ).map { Band(it.first.toDouble(), it.second.toDouble()) }
    val basePressure = 1010.0

    fun estimateMonth(planet: Planet, daysPassed: Number): String =
        when (((daysPassed.toDouble() % Insolation.yearLength) / (365.242 / 12.0)).toInt() + 1) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Unknown"
        }

    fun stepClimateSimulation(planet: Planet) {
        planet.daysPassed += 10
        Gui.instance.daysPassedLabel.setText("${planet.daysPassed} — ${estimateMonth(planet, planet.daysPassed)}")
        Gui.instance.updateInfobox()
//        Gui.instance.statsGraph.update(planet)
    }
}