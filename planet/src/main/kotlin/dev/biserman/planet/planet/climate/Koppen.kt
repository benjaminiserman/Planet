package dev.biserman.planet.planet.climate

import dev.biserman.planet.geometry.toGeoPoint
import godot.core.Color

// average colors courtesy of u/paculino: https://www.reddit.com/r/MapPorn/comments/q4pv42/the_average_color_of_each_k%C3%B6ppen_climate/
object Koppen : ClimateClassifier {
    fun getSummerAndWinter(datum: ClimateDatum): Pair<Set<ClimateDatumMonth>, Set<ClimateDatumMonth>> {
        val groupA = MonthIndex.values().toList().subList(MonthIndex.APR.ordinal, MonthIndex.SEP.ordinal)
            .map { datum.months[it.ordinal] }
        val groupB = MonthIndex.values().toList().subList(MonthIndex.OCT.ordinal, MonthIndex.MAR.ordinal)
            .map { datum.months[it.ordinal] }

        return if (groupA.map { it.averageTemperature }.average() >=
            groupB.map { it.averageTemperature }.average()
        ) {
            groupA.toSet() to groupB.toSet()
        } else {
            groupB.toSet() to groupA.toSet()
        }
    }

    override fun classify(datum: ClimateDatum): Classification {
        val (summer, winter) = getSummerAndWinter(datum)
        val geoPoint = datum.tile.tile.position.toGeoPoint()
        val warmMonthCount = datum.months.count { it.averageTemperature >= 10.0 }

        // Group E: Polar
        if (warmMonthCount == 0) {
            if (datum.months.any { it.averageTemperature >= 0.0 }) {
                return Classification("ET", "tundra", Color.html("#b2b2b2"), Color.html("insert_here"))
            } else {
                return Classification("EF", "ice_cap", Color.html("#686868"), Color.html("insert_here"))
            }
        }

        // Group B: Desert and semi-arid
        val springSummerPrecipitation = when {
            geoPoint.latitude >= 0.0 -> datum.months
                .subList(MonthIndex.APR, MonthIndex.SEP)
                .sumOf { it.precipitation }
            else -> datum.months
                .subList(MonthIndex.OCT, MonthIndex.MAR)
                .sumOf { it.precipitation }
        }
        val springSummerPrecipitationRatio = springSummerPrecipitation / datum.annualPrecipitation
        val aridPrecipitationThreshold = datum.averageTemperature * 20 + when {
            springSummerPrecipitationRatio >= 0.7 -> 280.0
            springSummerPrecipitationRatio >= 0.3 -> 140.0
            else -> 0.0
        }
        val aridityFactor = datum.annualPrecipitation / aridPrecipitationThreshold
        if (aridityFactor <= 0.5) {
            return if (datum.averageTemperature > 18.0)
                Classification("BWh", "hot_desert", Color.html("#ff0000"), Color.html("insert_here"))
            else
                Classification("BWk", "cold_desert", Color.html("#ff9695"), Color.html("insert_here"))
        } else if (aridityFactor <= 1.0) {
            return if (datum.averageTemperature > 18.0)
                Classification("BSh", "hot_semiarid", Color.html("#f5a301"), Color.html("insert_here"))
            else
                Classification("BSk", "cold_semiarid", Color.html("#ffdb63"), Color.html("insert_here"))
        }

        // Group A: Tropical
        if (datum.months.all { it.averageTemperature >= 18.0 }) {
            val tropicalPrecipitationThreshold = 100 - (datum.annualPrecipitation / 25)
            val driestMonth = datum.months.minBy { it.precipitation }

            return when {
                datum.months.all { it.precipitation >= 60 } ->
                    Classification("Af", "tropical_rainforest", Color.html("#0000ff"), Color.html("insert_here"))
                driestMonth.precipitation >= tropicalPrecipitationThreshold ->
                    Classification("Am", "tropical_monsoon", Color.html("#0077ff"), Color.html("insert_here"))
                driestMonth in summer -> Classification(
                    "As",
                    "tropical_savanna",
                    Color.html("#79baec"),
                    Color.html("insert_here")
                )
                else -> Classification("Aw", "tropical_savanna", Color.html("#46a9fa"), Color.html("insert_here"))
            }
        }

        val winterDriest = winter.minBy { it.precipitation }
        val winterWettest = winter.maxBy { it.precipitation }
        val summerDriest = summer.minBy { it.precipitation }
        val summerWettest = summer.maxBy { it.precipitation }
        val hasHotMonth = datum.months.any { it.averageTemperature > 22.0 }

        // Group C: Temperate
        if (datum.months.all { it.averageTemperature >= 0.0 }) {
            val isHighland = datum.tile.elevation >= 1500

            // Mediterranean
            if (summerDriest.precipitation < 40.0 && winterWettest.precipitation >= summerDriest.precipitation * 3) {
                return when {
                    hasHotMonth -> Classification(
                        "Csa",
                        "hot_summer_mediterranean",
                        Color.html("#ffff00"),
                        Color.html("insert_here")
                    )
                    warmMonthCount >= 4 -> Classification(
                        "Csb",
                        "warm_summer_mediterranean",
                        Color.html("#c6c700"),
                        Color.html("insert_here")
                    )
                    else -> Classification(
                        "Csc",
                        "cool_summer_mediterranean",
                        Color.html("#969600"),
                        Color.html("insert_here")
                    )
                }
            }

            // Monsoon
            if (summerWettest.precipitation >= winterDriest.precipitation * 10) {
                return when {
                    hasHotMonth -> Classification(
                        "Cwa",
                        "humid_subtropical_monsoon",
                        Color.html("#96ff96"),
                        Color.html("insert_here")
                    )
                    warmMonthCount >= 4 -> if (isHighland) Classification(
                        "Cwb",
                        "subtropical_highland",
                        Color.html("#63c764"),
                        Color.html("insert_here")
                    ) else Classification(
                        "Cwb",
                        "temperate_oceanic_monsoon",
                        Color.html("#63c764"),
                        Color.html("insert_here")
                    )
                    else -> if (isHighland) Classification(
                        "Cwc",
                        "cold_subtropical_highland",
                        Color.html("#329633"),
                        Color.html("insert_here")
                    ) else Classification(
                        "Cwc",
                        "subpolar_oceanic_monsoon",
                        Color.html("#329633"),
                        Color.html("insert_here")
                    )
                }
            }

            // Other
            return when {
                hasHotMonth -> Classification(
                    "Cfa",
                    "humid_subtropical",
                    Color.html("#c6ff4e"),
                    Color.html("insert_here")
                )
                warmMonthCount >= 4 -> if (isHighland) Classification(
                    "Cfb",
                    "subtropical_highland",
                    Color.html("#66ff33"),
                    Color.html("insert_here")
                ) else Classification(
                    "Cfb",
                    "temperate_oceanic",
                    Color.html("#66ff33"),
                    Color.html("insert_here")
                )
                else -> Classification(
                    "Cfc",
                    "subpolar_oceanic",
                    Color.html("#33c701"),
                    Color.html("insert_here")
                )
            }
        }

        // Group D: Continental
        // Mediterranean
        if (summerDriest.precipitation < 30.0 && winterWettest.precipitation >= summerDriest.precipitation * 3) {
            return when {
                warmMonthCount >= 4 -> if (hasHotMonth) Classification(
                    "Dsa",
                    "mediterranean_hot_summer_humid_continental",
                    Color.html("#ff00ff"),
                    Color.html("insert_here")
                ) else Classification(
                    "Dsb",
                    "mediterranean_warm_summer_humid_continental",
                    Color.html("#c600c7"),
                    Color.html("insert_here")
                )
                datum.months.any { it.averageTemperature < -38.0 } -> Classification(
                    "Dsd",
                    "mediterranean_extremely_cold_subartic",
                    Color.html("#966495"),
                    Color.html("insert_here")
                )
                else -> Classification(
                    "Dsc",
                    "mediterranean_subartic",
                    Color.html("#963295"),
                    Color.html("insert_here")
                )
            }
        }

        // Monsoon
        if (summerWettest.precipitation >= winterDriest.precipitation * 10) {
            return when {
                warmMonthCount >= 4 -> if (hasHotMonth) Classification(
                    "Dwa",
                    "hot_summer_humid_continental_monsoon",
                    Color.html("#abb1ff"),
                    Color.html("insert_here")
                ) else Classification(
                    "Dwb",
                    "warm_summer_humid_continental_monsoon",
                    Color.html("#5a77db"),
                    Color.html("insert_here")
                )
                datum.months.any { it.averageTemperature < -38.0 } -> Classification(
                    "Dwd",
                    "extremely_cold_subartic_monsoon",
                    Color.html("#320087"),
                    Color.html("insert_here")
                )
                else -> Classification(
                    "Dwc",
                    "subarctic_monsoon",
                    Color.html("#4c51b5"),
                    Color.html("insert_here")
                )
            }
        }

        return when {
            warmMonthCount >= 4 -> if (hasHotMonth) Classification(
                "Dfa",
                "hot_summer_humid_continental",
                Color.html("#00ffff"),
                Color.html("insert_here")
            ) else Classification(
                "Dfb",
                "warm_summer_humid_continental",
                Color.html("#38c7ff"),
                Color.html("insert_here")
            )
            datum.months.any { it.averageTemperature < -38.0 } -> Classification(
                "Dfd",
                "extremely_cold_subartic",
                Color.html("#00455e"),
                Color.html("insert_here")
            )
            else -> Classification(
                "Dfc",
                "subarctic",
                Color.html("#007e7d"),
                Color.html("insert_here")
            )
        }
    }
}