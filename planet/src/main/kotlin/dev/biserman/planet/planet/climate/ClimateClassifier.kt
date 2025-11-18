package dev.biserman.planet.planet.climate

import godot.core.Color

data class Classification(val id: String, val name: String, val color: Color, val terrainColor: Color)
interface ClimateClassifier {
    fun classify(datum: ClimateDatum): Classification
}