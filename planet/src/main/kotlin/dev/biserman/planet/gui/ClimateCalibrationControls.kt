package dev.biserman.planet.gui

import dev.biserman.planet.planet.climate.ClimateRuntimeConfig
import godot.api.Button
import godot.api.CheckButton
import godot.api.Control
import godot.api.HSlider
import godot.api.Label
import godot.core.connect
import kotlin.math.roundToInt

/** Runtime climate controls used by [Gui]. */
class ClimateCalibrationControls(private val gui: Gui) {
    private val showButton by lazy { gui.findChild("ShowClimateConfigButton") as Button }
    private val panel by lazy { gui.findChild("ClimateConfigPanel") as Control }
    private val resetButton by lazy { gui.findChild("ResetClimateConfigButton") as Button }

    private fun slider(name: String) = gui.findChild(name) as HSlider
    private fun label(name: String) = gui.findChild(name) as Label
    private fun modifier(name: String) = gui.findChild(name) as CheckButton

    fun initialize() {
        showButton.pressed.connect { panel.visible = showButton.buttonPressed }

        bindSlider("AxialTiltSlider", "AxialTiltValue", ClimateRuntimeConfig.axialTiltDegrees) {
            ClimateRuntimeConfig.axialTiltDegrees = it
            "${formatHalfDegree(it)} deg"
        }
        bindSlider("EccentricitySlider", "EccentricityValue", ClimateRuntimeConfig.orbitalEccentricity) {
            ClimateRuntimeConfig.orbitalEccentricity = it
            String.format("%.3f", it)
        }
        bindSlider("PeriapsisSlider", "PeriapsisValue", ClimateRuntimeConfig.periapsis) {
            ClimateRuntimeConfig.periapsis = it
            "${formatSignedDay(it)} (${seasonForPeriapsis(it)})"
        }
        bindSignedSlider("DistanceSlider", "DistanceValue", ClimateRuntimeConfig.distanceToStar) {
            ClimateRuntimeConfig.distanceToStar = it
        }
        bindSignedSlider("GreenhouseSlider", "GreenhouseValue", ClimateRuntimeConfig.greenhouseEffect) {
            ClimateRuntimeConfig.greenhouseEffect = it
        }
        bindSignedSlider("MoistureSlider", "MoistureValue", ClimateRuntimeConfig.moisture) {
            ClimateRuntimeConfig.moisture = it
        }
        bindSignedSlider("OceanCurrentSlider", "OceanCurrentValue", ClimateRuntimeConfig.oceanCurrentStrength) {
            ClimateRuntimeConfig.oceanCurrentStrength = it
        }
        bindSignedSlider("MonsoonSlider", "MonsoonValue", ClimateRuntimeConfig.monsoonStrength) {
            ClimateRuntimeConfig.monsoonStrength = it
        }

        bindModifier("HotHeavensModifier", ClimateRuntimeConfig.hotHeavens) { ClimateRuntimeConfig.hotHeavens = it }
        bindModifier("ClockworkWindsModifier", ClimateRuntimeConfig.clockworkWinds) {
            ClimateRuntimeConfig.clockworkWinds = it
        }
        bindModifier("ColdSunModifier", ClimateRuntimeConfig.coldSun) { ClimateRuntimeConfig.coldSun = it }
        bindModifier("DimSunModifier", ClimateRuntimeConfig.dimSun) { ClimateRuntimeConfig.dimSun = it }
        bindModifier("HotspotHeatingModifier", ClimateRuntimeConfig.hotspotHeating) {
            ClimateRuntimeConfig.hotspotHeating = it
        }
        resetButton.pressed.connect {
            ClimateRuntimeConfig.resetToDefaults()
            syncControlsToConfig()
        }
    }

    private fun bindSlider(
        sliderName: String,
        labelName: String,
        initialValue: Double,
        update: (Double) -> String,
    ) {
        val slider = slider(sliderName)
        val valueLabel = label(labelName)
        slider.value = initialValue
        valueLabel.text = update(initialValue)
        slider.valueChanged.connect { value -> valueLabel.text = update(value) }
    }

    private fun bindSignedSlider(
        sliderName: String,
        labelName: String,
        initialValue: Double,
        update: (Double) -> Unit,
    ) = bindSlider(sliderName, labelName, initialValue) {
        update(it)
        if (it > 0) "+${it.roundToInt()}" else it.roundToInt().toString()
    }

    private fun bindModifier(name: String, initialValue: Boolean, update: (Boolean) -> Unit) {
        val button = modifier(name)
        button.buttonPressed = initialValue
        button.toggled.connect { enabled -> update(enabled) }
    }

    private fun syncControlsToConfig() {
        slider("AxialTiltSlider").value = ClimateRuntimeConfig.axialTiltDegrees
        slider("EccentricitySlider").value = ClimateRuntimeConfig.orbitalEccentricity
        slider("PeriapsisSlider").value = ClimateRuntimeConfig.periapsis
        slider("DistanceSlider").value = ClimateRuntimeConfig.distanceToStar
        slider("GreenhouseSlider").value = ClimateRuntimeConfig.greenhouseEffect
        slider("MoistureSlider").value = ClimateRuntimeConfig.moisture
        slider("OceanCurrentSlider").value = ClimateRuntimeConfig.oceanCurrentStrength
        slider("MonsoonSlider").value = ClimateRuntimeConfig.monsoonStrength
        modifier("HotHeavensModifier").buttonPressed = ClimateRuntimeConfig.hotHeavens
        modifier("ClockworkWindsModifier").buttonPressed = ClimateRuntimeConfig.clockworkWinds
        modifier("ColdSunModifier").buttonPressed = ClimateRuntimeConfig.coldSun
        modifier("DimSunModifier").buttonPressed = ClimateRuntimeConfig.dimSun
        modifier("HotspotHeatingModifier").buttonPressed = ClimateRuntimeConfig.hotspotHeating
    }

    private fun formatHalfDegree(value: Double): String =
        if (value % 1.0 == 0.0) value.roundToInt().toString() else String.format("%.1f", value)

    private fun formatSignedDay(value: Double): String =
        if (value >= 0) "+${value.roundToInt()} d" else "${value.roundToInt()} d"

    private fun seasonForPeriapsis(offset: Double): String {
        // Insolation reaches its orbital maximum when cos(...) == 1:
        // dayOfYear + periapsis == 0 (mod yearLength).
        val day = ((-offset) % 365.242 + 365.242) % 365.242
        return when (day) {
            in 80.0..<172.0 -> "spring"
            in 172.0..<266.0 -> "summer"
            in 266.0..<355.0 -> "autumn"
            else -> "winter"
        }
    }
}