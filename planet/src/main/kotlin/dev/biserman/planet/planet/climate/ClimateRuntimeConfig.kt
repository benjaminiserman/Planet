package dev.biserman.planet.planet.climate

object ClimateRuntimeConfig {
    const val DEFAULT_AXIAL_TILT_DEGREES = 23.5
    const val DEFAULT_ORBITAL_ECCENTRICITY = 0.017

    var revision = 0
        private set

    var axialTiltDegrees = DEFAULT_AXIAL_TILT_DEGREES
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var orbitalEccentricity = DEFAULT_ORBITAL_ECCENTRICITY
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var distanceToStar = 0.0
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var greenhouseEffect = 0.0
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var moisture = 0.0
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var oceanCurrentStrength = 0.0
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var monsoonStrength = 0.0
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }

    var hotHeavens = false
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var clockworkWinds = false
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var coldSun = false
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var dimSun = false
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }

    // Positive distance means farther away, so it reduces insolation-driven heating.
    val insolationTemperatureOffset get() = -distanceToStar * 5.0
    val greenhouseTemperatureOffset get() = greenhouseEffect * 5.0
    val moistureScale get() = 1.0 + moisture / 10.0
    val oceanCurrentScale get() = 1.0 + oceanCurrentStrength / 10.0
    val oceanCurrentDistanceScale get() = 1.0 + oceanCurrentStrength / 20.0
    val monsoonScale get() = 1.0 + monsoonStrength / 10.0
    val monsoonDistanceScale get() = 1.0 + monsoonStrength / 20.0
    val insolationTemperatureSign get() = if (coldSun) -1.0 else 1.0
    val lapseRateSign get() = if (hotHeavens) -1.0 else 1.0
    val backwardsWind get() = if (clockworkWinds) 0.0 else ClimateSimulationGlobals.backwardsWind
    val brightnessScale get() = if (dimSun) 0.25 else 1.0

    fun resetToDefaults() {
        axialTiltDegrees = DEFAULT_AXIAL_TILT_DEGREES
        orbitalEccentricity = DEFAULT_ORBITAL_ECCENTRICITY
        distanceToStar = 0.0
        greenhouseEffect = 0.0
        moisture = 0.0
        oceanCurrentStrength = 0.0
        monsoonStrength = 0.0
        hotHeavens = false
        clockworkWinds = false
        coldSun = false
        dimSun = false
    }
}
