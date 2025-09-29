package dev.biserman.planet.planet

import kotlin.math.*

@Suppress("MayBeConstant")
object Insolation {

    val solarConstant = 1.0 // 1361.0 // W/m^2
    val orbitEccentricity = 0.016718
    val axialTiltDeg = 23.45
    val northSpringEquinox = 79.0
    val yearLength = 365.0
    val opticalDepthConstant = -0.14

    // Earth–Sun distance correction
    fun eccentricityFactor(dayOfYear: Int): Double {
        return 1.0 + orbitEccentricity * 2 * cos(2.0 * Math.PI * dayOfYear / 365.0)
    }

    // Solar declination (radians, Cooper’s formula)
    fun solarDeclination(dayOfYear: Int): Double {
        return (axialTiltDeg * PI / 180.0) *
                sin(2.0 * PI * (northSpringEquinox + dayOfYear) / yearLength)
    }

    // Cosine of solar zenith at noon (hour angle = 0)
    fun cosZenith(latitude: Double, dayOfYear: Int): Double {
        val dec = solarDeclination(dayOfYear)
        return sin(latitude) * sin(dec) + cos(latitude) * cos(dec)
    }

    // Air mass (simple, assumes sea level)
    fun airMass(cosZenith: Double): Double {
        return 1.0 / cosZenith
    }

    // Direct horizontal irradiance at noon
    fun directHorizontal(dayOfYear: Int, latitude: Double): Double {
        val e0 = eccentricityFactor(dayOfYear)
        val cosZ = cosZenith(latitude, dayOfYear)
        val m = airMass(cosZ)
        val transmittance = exp(opticalDepthConstant * m)
        return solarConstant * e0 * cosZ * transmittance
    }
}

fun main() {
    val lat = 40.0 * PI / 180.0  // latitude in radians
    val day = 172                // ~June 21
    val ghi = Insolation.directHorizontal(day, lat)
    println("Noon insolation: ${"%.1f".format(ghi)} W/m²")
}