package dev.biserman.planet.planet.climate

import godot.core.Color

// average colors courtesy of u/paculino: https://www.reddit.com/r/MapPorn/comments/q4pv42/the_average_color_of_each_k%C3%B6ppen_climate/
object Koppen : ClimateClassifier {
    override fun classify(datum: ClimateDatum): Classification {
        if (datum.months.all { it.averageTemperature < 10.0 }) {
            if (datum.months.any { it.averageTemperature >= 0.0 }) {
                return Classification("ET", "tundra", Color.html("#b2b2b2"), Color.html("#6d6d5e"))
            } else {
                return Classification("EF", "ice_cap", Color.html("#666666"), Color.html("#e9ecee"))
            }
        }
        if (datum.months.all { it.averageTemperature >= 18.0 }) {

        }
    }
}