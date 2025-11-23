package dev.biserman.planet.planet.climate

import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.planet.Planet
import godot.core.Color
import kotlin.math.max

// average colors courtesy of https://github.com/syntax3rr/
object Koppen : ClimateClassifier {
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
            return ClimateClassification("O", "ocean", Color.html("#000000"), Color.html("#030b21"))
        }

        // Group E: Polar
        if (warmMonthCount == 0) {
            if (datum.months.any { it.averageTemperature >= 0.0 }) {
                return ClimateClassification("ET", "tundra", Color.html("#b2b2b2"), Color.html("#b0afa8"))
            } else {
                return ClimateClassification("EF", "ice_cap", Color.html("#686868"), Color.html("#b1afab"))
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
                ClimateClassification("BWh", "hot_desert", Color.html("#ff0000"), Color.html("#cdaf80"))
            else
                ClimateClassification("BWk", "cold_desert", Color.html("#ff9695"), Color.html("#ab966d"))
        } else if (aridityFactor <= 1.0) {
            return if (datum.averageTemperature > 18.0)
                ClimateClassification("BSh", "hot_semiarid", Color.html("#f5a301"), Color.html("#746b3e"))
            else
                ClimateClassification("BSk", "cold_semiarid", Color.html("#ffdb63"), Color.html("#6e6a3e"))
        }

        // Group A: Tropical
        if (datum.months.all { it.averageTemperature >= 18.0 }) {
            val tropicalPrecipitationThreshold = 100 - (datum.annualPrecipitation / 25)
            val driestMonth = datum.months.minBy { it.precipitation }

            return when {
                datum.months.all { it.precipitation >= 60 } ->
                    ClimateClassification("Af", "tropical_rainforest", Color.html("#0000ff"), Color.html("#293f11"))
                driestMonth.precipitation >= tropicalPrecipitationThreshold ->
                    ClimateClassification("Am", "tropical_monsoon", Color.html("#0077ff"), Color.html("#344914"))
                driestMonth in summer -> ClimateClassification(
                    "As",
                    "tropical_savanna",
                    Color.html("#79baec"),
                    Color.html("#4a5521")
                )
                else -> ClimateClassification("Aw", "tropical_savanna", Color.html("#46a9fa"), Color.html("#4a5521"))
            }
        }

        val winterDriest = winter.minBy { it.precipitation }
        val winterWettest = winter.maxBy { it.precipitation }
        val summerDriest = summer.minBy { it.precipitation }
        val summerWettest = summer.maxBy { it.precipitation }
        val hasHotMonth = datum.months.any { it.averageTemperature > 22.0 }

        // Group C: Temperate
        if (datum.months.all { it.averageTemperature >= 0.0 }) {
            val isHighland = planetTile.elevation >= 1500

            // Mediterranean
            if (summerDriest.precipitation < 40.0 && winterWettest.precipitation >= summerDriest.precipitation * 3) {
                return when {
                    hasHotMonth -> ClimateClassification(
                        "Csa",
                        "hot_summer_mediterranean",
                        Color.html("#ffff00"),
                        Color.html("#51532a")
                    )
                    warmMonthCount >= 4 -> ClimateClassification(
                        "Csb",
                        "warm_summer_mediterranean",
                        Color.html("#c6c700"),
                        Color.html("#424a29")
                    )
                    else -> ClimateClassification(
                        "Csc",
                        "cool_summer_mediterranean",
                        Color.html("#969600"),
                        Color.html("#656954")
                    )
                }
            }

            // Monsoon
            if (summerWettest.precipitation >= winterDriest.precipitation * 10) {
                return when {
                    hasHotMonth -> ClimateClassification(
                        "Cwa",
                        "humid_subtropical_monsoon",
                        Color.html("#96ff96"),
                        Color.html("#42531c")
                    )
                    warmMonthCount >= 4 -> if (isHighland) ClimateClassification(
                        "Cwb",
                        "subtropical_highland_monsoon",
                        Color.html("#63c764"),
                        Color.html("#50502a")
                    ) else ClimateClassification(
                        "Cwb",
                        "temperate_oceanic_monsoon",
                        Color.html("#63c764"),
                        Color.html("#50502a")
                    )
                    else -> ClimateClassification(
                        "Cwc",
                        "subpolar_oceanic_monsoon",
                        Color.html("#329633"),
                        Color.html("#987b50")
                    )
                }
            }

            // Other
            return when {
                hasHotMonth -> ClimateClassification(
                    "Cfa",
                    "humid_subtropical",
                    Color.html("#c6ff4e"),
                    Color.html("#3c4d19")
                )
                warmMonthCount >= 4 -> if (isHighland) ClimateClassification(
                    "Cfb",
                    "subtropical_highland",
                    Color.html("#66ff33"),
                    Color.html("#30401a")
                ) else ClimateClassification(
                    "Cfb",
                    "temperate_oceanic",
                    Color.html("#66ff33"),
                    Color.html("#30401a")
                )
                else -> ClimateClassification(
                    "Cfc",
                    "subpolar_oceanic",
                    Color.html("#33c701"),
                    Color.html("#565d4e")
                )
            }
        }

        // Group D: Continental
        // Mediterranean
        if (summerDriest.precipitation < 30.0 && winterWettest.precipitation >= summerDriest.precipitation * 3) {
            return when {
                warmMonthCount >= 4 -> if (hasHotMonth) ClimateClassification(
                    "Dsa",
                    "mediterranean_hot_summer_humid_continental",
                    Color.html("#ff00ff"),
                    Color.html("#58592b")
                ) else ClimateClassification(
                    "Dsb",
                    "mediterranean_warm_summer_humid_continental",
                    Color.html("#c600c7"),
                    Color.html("#39421c")
                )
                datum.months.any { it.averageTemperature < -38.0 } -> ClimateClassification(
                    "Dsd",
                    "mediterranean_extremely_cold_subartic",
                    Color.html("#966495"),
                    Color.html("#4e4d27")
                )
                else -> ClimateClassification(
                    "Dsc",
                    "mediterranean_subartic",
                    Color.html("#963295"),
                    Color.html("#4e4d27")
                )
            }
        }

        // Monsoon
        if (summerWettest.precipitation >= winterDriest.precipitation * 10) {
            return when {
                warmMonthCount >= 4 -> if (hasHotMonth) ClimateClassification(
                    "Dwa",
                    "hot_summer_humid_continental_monsoon",
                    Color.html("#abb1ff"),
                    Color.html("#3d5019")
                ) else ClimateClassification(
                    "Dwb",
                    "warm_summer_humid_continental_monsoon",
                    Color.html("#5a77db"),
                    Color.html("#374717")
                )
                datum.months.any { it.averageTemperature < -38.0 } -> ClimateClassification(
                    "Dwd",
                    "extremely_cold_subartic_monsoon",
                    Color.html("#320087"),
                    Color.html("#44461e")
                )
                else -> ClimateClassification(
                    "Dwc",
                    "subarctic_monsoon",
                    Color.html("#4c51b5"),
                    Color.html("#545629")
                )
            }
        }

        return when {
            warmMonthCount >= 4 -> if (hasHotMonth) ClimateClassification(
                "Dfa",
                "hot_summer_humid_continental",
                Color.html("#00ffff"),
                Color.html("#2f4211")
            ) else ClimateClassification(
                "Dfb",
                "warm_summer_humid_continental",
                Color.html("#38c7ff"),
                Color.html("#2d3a11")
            )
            datum.months.any { it.averageTemperature < -38.0 } -> ClimateClassification(
                "Dfd",
                "extremely_cold_subartic",
                Color.html("#00455e"),
                Color.html("#0c0c35")
            )
            else -> ClimateClassification(
                "Dfc",
                "subarctic",
                Color.html("#007e7d"),
                Color.html("#464a22")
            )
        }
    }
}