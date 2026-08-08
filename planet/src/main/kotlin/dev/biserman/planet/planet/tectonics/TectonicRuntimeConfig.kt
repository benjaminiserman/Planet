package dev.biserman.planet.planet.tectonics

/** Runtime tectonic controls used by the in-game calibration panel. */
object TectonicRuntimeConfig {
    var revision = 0
        private set

    /** Multiplier applied to mantle-convection forces. */
    var geothermalActivity = 1.0
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var riftCutoff = TectonicGlobals.riftCutoff
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var desiredLandPercent = TectonicGlobals.desiredLandPercent
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }

    fun resetToDefaults() {
        geothermalActivity = 1.0
        riftCutoff = TectonicGlobals.riftCutoff
        desiredLandPercent = TectonicGlobals.desiredLandPercent
    }
}