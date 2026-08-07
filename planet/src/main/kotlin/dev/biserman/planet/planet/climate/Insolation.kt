package dev.biserman.planet.planet.climate

import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.northSpringEquinox
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.opticalDepthConstant
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.solarConstant
import dev.biserman.planet.planet.climate.ClimateSimulationGlobals.yearLength
import dev.biserman.planet.utils.UtilityExtensions.degToRad
import kotlin.math.*

@Suppress("MayBeConstant")
object Insolation {
    // Earth–Sun distance correction
    fun eccentricityFactor(dayOfYear: Double): Double {
        val eccentricity = ClimateRuntimeConfig.orbitalEccentricity.coerceIn(0.0, 0.999999)
        val meanAnomaly = 2.0 * PI * (dayOfYear + ClimateRuntimeConfig.periapsis) / yearLength
        val eccentricAnomaly = solveKeplersEquation(meanAnomaly, eccentricity)
        val trueAnomaly = 2.0 * atan2(
            sqrt(1.0 + eccentricity) * sin(eccentricAnomaly / 2.0),
            sqrt(1.0 - eccentricity) * cos(eccentricAnomaly / 2.0)
        )
        val distanceFactor = (1.0 + eccentricity * cos(trueAnomaly)) /
            (1.0 - eccentricity * eccentricity)
        return distanceFactor * distanceFactor
    }

    /** Solves M = E - e sin(E), preserving the periapsis phase convention. */
    private fun solveKeplersEquation(meanAnomaly: Double, eccentricity: Double): Double {
        var eccentricAnomaly = meanAnomaly
        repeat(12) {
            eccentricAnomaly -= (eccentricAnomaly - eccentricity * sin(eccentricAnomaly) - meanAnomaly) /
                (1.0 - eccentricity * cos(eccentricAnomaly))
        }
        return eccentricAnomaly
    }

    // Solar declination (radians, Cooper’s formula)
    fun solarDeclination(dayOfYear: Double): Double = ClimateRuntimeConfig.axialTiltDegrees.degToRad() *
        sin(2.0 * PI * (northSpringEquinox + dayOfYear) / yearLength)

    // Cosine of solar zenith at noon (hour angle = 0)
    fun cosZenith(latitude: Double, dayOfYear: Double): Double {
        val dec = solarDeclination(dayOfYear)
        return sin(latitude) * sin(dec) + cos(latitude) * cos(dec)
    }

    // Kasten–Young (1989) airmass
    fun airMass(cosZenith: Double): Double {
        val theta = acos(cosZenith)
        val thetaDeg = theta * 180.0 / PI
        if (thetaDeg >= 90.0) return Double.POSITIVE_INFINITY
        return 1.0 / (cosZenith + 0.50572 * (96.07995 - thetaDeg).pow(-1.6364))
    }

    // Direct horizontal irradiance at noon
    fun directHorizontal(dayOfYear: Double, latitude: Double): Double {
        val e0 = eccentricityFactor(dayOfYear)
        val cosZ = cosZenith(latitude, dayOfYear)
        val m = airMass(cosZ)
        val transmittance = exp(opticalDepthConstant * m)
        return (solarConstant * e0 * cosZ * transmittance).coerceIn(0.0, 1.0)
    }
}