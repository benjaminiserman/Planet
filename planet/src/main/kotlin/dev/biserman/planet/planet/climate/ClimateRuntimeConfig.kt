package dev.biserman.planet.planet.climate

import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.defaultAxialTiltDegrees
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.defaultOrbitalEccentricity
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.maxMoisturePropagationScale

object ClimateRuntimeConfig {
    var revision = 0
        private set

    var axialTiltDegrees = defaultAxialTiltDegrees
        set(value) {
            if (field != value) {
                field = value
                revision++
            }
        }
    var orbitalEccentricity = defaultOrbitalEccentricity
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
    var hotspotHeating = false
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
    val moisturePropagationScale
        get() = 1.0 + (maxMoisturePropagationScale - 1.0) * moisture.coerceIn(0.0, 10.0) / 10.0
    val oceanCurrentScale get() = maxOf(0.0, 1.0 + oceanCurrentStrength / 5.0)
    val oceanCurrentDistanceScale get() = maxOf(0.0, 1.0 + oceanCurrentStrength / 5.0)
    val monsoonScale get() = maxOf(0.1, 1.0 + monsoonStrength / 5.0)
    val monsoonDistanceScale get() = maxOf(0.1, 1.0 + monsoonStrength / 5.0)
    val insolationTemperatureSign get() = if (coldSun) -1.0 else 1.0
    val lapseRateSign get() = if (hotHeavens) -1.0 else 1.0
    val backwardsWind get() = if (clockworkWinds) 0.0 else ClimateSimulationGlobals.backwardsWind
    val brightnessScale get() = if (dimSun) 0.25 else 1.0
    val maxHotspotTemperatureOffset get() = if (hotspotHeating) 120.0 else 0.0

    fun resetToDefaults() {
        axialTiltDegrees = defaultAxialTiltDegrees
        orbitalEccentricity = defaultOrbitalEccentricity
        distanceToStar = 0.0
        greenhouseEffect = 0.0
        moisture = 0.0
        oceanCurrentStrength = 0.0
        monsoonStrength = 0.0
        hotHeavens = false
        clockworkWinds = false
        coldSun = false
        dimSun = false
        hotspotHeating = false
    }
}
