package dev.biserman.planet.planet.climate

import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.planet.Planet
import godot.core.Color
import kotlin.math.max

// average colors courtesy of https://github.com/syntax3rr/
object Koppen : ClimateClassifier {
    // Climate classifications
    val OCEAN = ClimateClassification("O", "ocean", Color.html("#000000"), Color.html("#030b21"))
    val TUNDRA = ClimateClassification("ET", "tundra", Color.html("#b2b2b2"), Color.html("#b0afa8"))
    val ICE_CAP = ClimateClassification("EF", "ice_cap", Color.html("#686868"), Color.html("#b1afab"))

    val HOT_DESERT = ClimateClassification("BWh", "hot_desert", Color.html("#ff0000"), Color.html("#cdaf80"))
    val COLD_DESERT = ClimateClassification("BWk", "cold_desert", Color.html("#ff9695"), Color.html("#ab966d"))
    val HOT_SEMIARID =
        ClimateClassification("BSh", "hot_semiarid", Color.html("#f5a301"), Color.html("#746b3e"))
    val COLD_SEMIARID =
        ClimateClassification("BSk", "cold_semiarid", Color.html("#ffdb63"), Color.html("#6e6a3e"))

    val TROPICAL_RAINFOREST =
        ClimateClassification("Af", "tropical_rainforest", Color.html("#0000ff"), Color.html("#293f11"))
    val TROPICAL_MONSOON =
        ClimateClassification("Am", "tropical_monsoon", Color.html("#0077ff"), Color.html("#344914"))
    val TROPICAL_SAVANNA_SUMMER =
        ClimateClassification("As", "tropical_savanna", Color.html("#79baec"), Color.html("#4a5521"))
    val TROPICAL_SAVANNA_WINTER =
        ClimateClassification("Aw", "tropical_savanna", Color.html("#46a9fa"), Color.html("#4a5521"))

    val HOT_SUMMER_MEDITERRANEAN =
        ClimateClassification("Csa", "hot_summer_mediterranean", Color.html("#ffff00"), Color.html("#51532a"))
    val WARM_SUMMER_MEDITERRANEAN =
        ClimateClassification("Csb", "warm_summer_mediterranean", Color.html("#c6c700"), Color.html("#424a29"))
    val COOL_SUMMER_MEDITERRANEAN =
        ClimateClassification("Csc", "cool_summer_mediterranean", Color.html("#969600"), Color.html("#656954"))

    val HUMID_SUBTROPICAL_MONSOON =
        ClimateClassification("Cwa", "humid_subtropical_monsoon", Color.html("#96ff96"), Color.html("#42531c"))
    val SUBTROPICAL_HIGHLAND_MONSOON =
        ClimateClassification("Cwb", "subtropical_highland_monsoon", Color.html("#63c764"), Color.html("#50502a"))
    val TEMPERATE_OCEANIC_MONSOON =
        ClimateClassification("Cwb", "temperate_oceanic_monsoon", Color.html("#63c764"), Color.html("#50502a"))
    val SUBPOLAR_OCEANIC_MONSOON =
        ClimateClassification("Cwc", "subpolar_oceanic_monsoon", Color.html("#329633"), Color.html("#987b50"))

    val HUMID_SUBTROPICAL =
        ClimateClassification("Cfa", "humid_subtropical", Color.html("#c6ff4e"), Color.html("#3c4d19"))
    val SUBTROPICAL_HIGHLAND =
        ClimateClassification("Cfb", "subtropical_highland", Color.html("#66ff33"), Color.html("#30401a"))
    val TEMPERATE_OCEANIC =
        ClimateClassification("Cfb", "temperate_oceanic", Color.html("#66ff33"), Color.html("#30401a"))
    val SUBPOLAR_OCEANIC =
        ClimateClassification("Cfc", "subpolar_oceanic", Color.html("#33c701"), Color.html("#565d4e"))

    val MEDITERRANEAN_HOT_SUMMER_HUMID_CONTINENTAL = ClimateClassification(
        "Dsa",
        "mediterranean_hot_summer_humid_continental",
        Color.html("#ff00ff"),
        Color.html("#58592b")
    )
    val MEDITERRANEAN_WARM_SUMMER_HUMID_CONTINENTAL = ClimateClassification(
        "Dsb",
        "mediterranean_warm_summer_humid_continental",
        Color.html("#c600c7"),
        Color.html("#39421c")
    )
    val MEDITERRANEAN_SUBARCTIC =
        ClimateClassification("Dsc", "mediterranean_subarctic", Color.html("#963295"), Color.html("#4e4d27"))
    val MEDITERRANEAN_EXTREMELY_COLD_SUBARCTIC = ClimateClassification(
        "Dsd",
        "mediterranean_extremely_cold_subarctic",
        Color.html("#966495"),
        Color.html("#4e4d27")
    )

    val HOT_SUMMER_HUMID_CONTINENTAL_MONSOON = ClimateClassification(
        "Dwa",
        "hot_summer_humid_continental_monsoon",
        Color.html("#abb1ff"),
        Color.html("#3d5019")
    )
    val WARM_SUMMER_HUMID_CONTINENTAL_MONSOON = ClimateClassification(
        "Dwb",
        "warm_summer_humid_continental_monsoon",
        Color.html("#5a77db"),
        Color.html("#374717")
    )
    val SUBARCTIC_MONSOON =
        ClimateClassification("Dwc", "subarctic_monsoon", Color.html("#4c51b5"), Color.html("#545629"))
    val EXTREMELY_COLD_SUBARCTIC_MONSOON =
        ClimateClassification("Dwd", "extremely_cold_subarctic_monsoon", Color.html("#320087"), Color.html("#44461e"))

    val HOT_SUMMER_HUMID_CONTINENTAL =
        ClimateClassification("Dfa", "hot_summer_humid_continental", Color.html("#00ffff"), Color.html("#2f4211"))
    val WARM_SUMMER_HUMID_CONTINENTAL =
        ClimateClassification("Dfb", "warm_summer_humid_continental", Color.html("#38c7ff"), Color.html("#2d3a11"))
    val SUBARCTIC = ClimateClassification("Dfc", "subarctic", Color.html("#007e7d"), Color.html("#464a22"))
    val EXTREMELY_COLD_SUBARCTIC =
        ClimateClassification("Dfd", "extremely_cold_subarctic", Color.html("#00455e"), Color.html("#0c0c35"))

    val highlandElevationThreshold = 1500

    fun getSummerAndWinter(datum: ClimateDatum): Pair<Set<ClimateDatumMonth>, Set<ClimateDatumMonth>> {
        val groupA = MonthIndex.values().toList().monthRange(MonthIndex.APR, MonthIndex.SEP)
            .map { datum.months[it.ordinal] }
        val groupB = MonthIndex.values().toList().monthRange(MonthIndex.OCT, MonthIndex.MAR)
            .map { datum.months[it.ordinal] }

        return if (groupA.map { it.averageTemperature }.average() >=
            groupB.map { it.averageTemperature }.average()
        ) {
            groupA.toSet() to groupB.toSet()
        } else {
            groupB.toSet() to groupA.toSet()
        }
    }


    override fun classify(planet: Planet, datum: ClimateDatum): ClimateClassification {
        val (summer, winter) = getSummerAndWinter(datum)
        val planetTile = planet.planetTiles[datum.tileId]!!
        val geoPoint = planetTile.tile.position.toGeoPoint()
        val warmMonthCount = datum.months.count { it.averageTemperature >= 10.0 }

        if (!planetTile.isAboveWater) {
            return OCEAN
        }

        // Group E: Polar
        if (warmMonthCount == 0) {
            if (datum.months.any { it.averageTemperature >= 0.0 }) {
                return TUNDRA
            } else {
                return ICE_CAP
            }
        }

        // Group B: Desert and semi-arid
        val springSummerPrecipitation = when {
            geoPoint.latitude >= 0.0 -> datum.months
                .monthRange(MonthIndex.APR, MonthIndex.SEP)
                .sumOf { it.precipitation }
            else -> datum.months
                .monthRange(MonthIndex.OCT, MonthIndex.MAR)
                .sumOf { it.precipitation }
        }
        val springSummerPrecipitationRatio = springSummerPrecipitation / datum.annualPrecipitation
        val aridPrecipitationThreshold = max(
            1.0, datum.averageTemperature * 20 + when {
                springSummerPrecipitationRatio >= 0.7 -> 280.0
                springSummerPrecipitationRatio >= 0.3 -> 140.0
                else -> 0.0
            }
        )
        val aridityFactor = datum.annualPrecipitation / aridPrecipitationThreshold
        if (aridityFactor <= 0.5) {
            return if (datum.averageTemperature > 18.0)
                HOT_DESERT
            else
                COLD_DESERT
        } else if (aridityFactor <= 1.0) {
            return if (datum.averageTemperature > 18.0)
                HOT_SEMIARID
            else
                COLD_SEMIARID
        }

        // Group A: Tropical
        if (datum.months.all { it.averageTemperature >= 18.0 }) {
            val tropicalPrecipitationThreshold = 100 - (datum.annualPrecipitation / 25)
            val driestMonth = datum.months.minBy { it.precipitation }

            return when {
                datum.months.all { it.precipitation >= 60 } ->
                    TROPICAL_RAINFOREST
                driestMonth.precipitation >= tropicalPrecipitationThreshold ->
                    TROPICAL_MONSOON
                driestMonth in summer -> TROPICAL_SAVANNA_SUMMER
                else -> TROPICAL_SAVANNA_WINTER
            }
        }

        val winterDriest = winter.minBy { it.precipitation }
        val winterWettest = winter.maxBy { it.precipitation }
        val summerDriest = summer.minBy { it.precipitation }
        val summerWettest = summer.maxBy { it.precipitation }
        val hasHotMonth = datum.months.any { it.averageTemperature > 22.0 }

        // Group C: Temperate
        if (datum.months.all { it.averageTemperature >= 0.0 }) {
            val isHighland = planetTile.elevation >= highlandElevationThreshold

            // Mediterranean
            if (summerDriest.precipitation < 40.0 && winterWettest.precipitation >= summerDriest.precipitation * 3) {
                return when {
                    hasHotMonth -> HOT_SUMMER_MEDITERRANEAN
                    warmMonthCount >= 4 -> WARM_SUMMER_MEDITERRANEAN
                    else -> COOL_SUMMER_MEDITERRANEAN
                }
            }

            // Monsoon
            if (summerWettest.precipitation >= winterDriest.precipitation * 10) {
                return when {
                    hasHotMonth -> HUMID_SUBTROPICAL_MONSOON
                    warmMonthCount >= 4 -> if (isHighland) SUBTROPICAL_HIGHLAND_MONSOON
                    else TEMPERATE_OCEANIC_MONSOON
                    else -> SUBPOLAR_OCEANIC_MONSOON
                }
            }

            // Other
            return when {
                hasHotMonth -> HUMID_SUBTROPICAL
                warmMonthCount >= 4 -> if (isHighland) SUBTROPICAL_HIGHLAND
                else TEMPERATE_OCEANIC
                else -> SUBPOLAR_OCEANIC
            }
        }

        // Group D: Continental
        // Mediterranean
        if (summerDriest.precipitation < 30.0 && winterWettest.precipitation >= summerDriest.precipitation * 3) {
            return when {
                warmMonthCount >= 4 -> if (hasHotMonth) MEDITERRANEAN_HOT_SUMMER_HUMID_CONTINENTAL
                else MEDITERRANEAN_WARM_SUMMER_HUMID_CONTINENTAL
                datum.months.any { it.averageTemperature < -38.0 } -> MEDITERRANEAN_EXTREMELY_COLD_SUBARCTIC
                else -> MEDITERRANEAN_SUBARCTIC
            }
        }

        // Monsoon
        if (summerWettest.precipitation >= winterDriest.precipitation * 10) {
            return when {
                warmMonthCount >= 4 -> if (hasHotMonth) HOT_SUMMER_HUMID_CONTINENTAL_MONSOON
                else WARM_SUMMER_HUMID_CONTINENTAL_MONSOON
                datum.months.any { it.averageTemperature < -38.0 } -> EXTREMELY_COLD_SUBARCTIC_MONSOON
                else -> SUBARCTIC_MONSOON
            }
        }

        return when {
            warmMonthCount >= 4 -> if (hasHotMonth) HOT_SUMMER_HUMID_CONTINENTAL
            else WARM_SUMMER_HUMID_CONTINENTAL
            datum.months.any { it.averageTemperature < -38.0 } -> EXTREMELY_COLD_SUBARCTIC
            else -> SUBARCTIC
        }
    }
}