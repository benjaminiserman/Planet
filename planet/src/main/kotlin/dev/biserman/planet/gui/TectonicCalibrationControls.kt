package dev.biserman.planet.gui

import dev.biserman.planet.planet.tectonics.TectonicRuntimeConfig
import godot.api.Button
import godot.api.Control
import godot.api.HSlider
import godot.api.Label
import godot.core.connect
import kotlin.math.roundToInt

/** Runtime tectonic controls used by [Gui]. */
class TectonicCalibrationControls(private val gui: Gui) {
    private val showButton by lazy { gui.findChild("ShowTectonicConfigButton") as Button }
    private val panel by lazy { gui.findChild("TectonicConfigPanel") as Control }
    private val resetButton by lazy { gui.findChild("ResetTectonicConfigButton") as Button }

    private fun slider(name: String) = gui.findChild(name) as HSlider
    private fun label(name: String) = gui.findChild(name) as Label

    fun initialize() {
        showButton.pressed.connect { panel.visible = showButton.buttonPressed }

        bindSlider("GeothermalActivitySlider", "GeothermalActivityValue", TectonicRuntimeConfig.geothermalActivity) {
            TectonicRuntimeConfig.geothermalActivity = it
            "${(it * 100).roundToInt()}%"
        }
        bindSlider("RiftCutoffSlider", "RiftCutoffValue", TectonicRuntimeConfig.riftCutoff) {
            TectonicRuntimeConfig.riftCutoff = it
            "${(it * 100).roundToInt()}%"
        }
        bindSlider("DesiredLandSlider", "DesiredLandValue", TectonicRuntimeConfig.desiredLandPercent) {
            TectonicRuntimeConfig.desiredLandPercent = it
            "${(it * 100).roundToInt()}%"
        }
        resetButton.pressed.connect {
            TectonicRuntimeConfig.resetToDefaults()
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

    private fun syncControlsToConfig() {
        slider("GeothermalActivitySlider").value = TectonicRuntimeConfig.geothermalActivity
        slider("RiftCutoffSlider").value = TectonicRuntimeConfig.riftCutoff
        slider("DesiredLandSlider").value = TectonicRuntimeConfig.desiredLandPercent
    }
}