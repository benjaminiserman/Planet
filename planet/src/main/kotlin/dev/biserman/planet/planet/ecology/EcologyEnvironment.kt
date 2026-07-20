package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateClassification
import dev.biserman.planet.planet.climate.ClimateDatum
import kotlin.math.exp
import kotlin.math.max

/** Weighted environmental character of one tile, derived from climate and Hersfeldt bioclimate. */
data class EnvironmentProfile(
    val weights: Map<EnvironmentTag, Double>,
) {
    operator fun get(tag: EnvironmentTag): Double = weights[tag] ?: 0.0
}

/**
 * Converts a Hersfeldt classification and its underlying continuous climate into placement tags.
 * Classification names provide likely vegetation structure while measurements soften zone edges.
 */
fun hersfeldtEnvironmentProfile(
    classification: ClimateClassification,
    climate: ClimateDatum,
): EnvironmentProfile {
    val name = classification.name.lowercase()
    val weights = mutableMapOf<EnvironmentTag, Double>()
    fun raise(tag: EnvironmentTag, value: Double) {
        weights[tag] = max(weights[tag] ?: 0.0, value.coerceIn(0.0, 1.0))
    }

    val meanTemperature = climate.averageTemperature
    val temperatureRange = climate.temperatureRange
    val annualPrecipitation = climate.annualPrecipitation
    val meanInsolation = climate.months.map { it.insolation }.average()

    raise(EnvironmentTag.COLD, ((8.0 - meanTemperature) / 25.0).coerceIn(0.0, 1.0))
    raise(EnvironmentTag.HOT, ((meanTemperature - 18.0) / 17.0).coerceIn(0.0, 1.0))
    raise(EnvironmentTag.DRY, ((700.0 - annualPrecipitation) / 650.0).coerceIn(0.0, 1.0))
    raise(EnvironmentTag.WET, ((annualPrecipitation - 600.0) / 1_800.0).coerceIn(0.0, 1.0))
    raise(EnvironmentTag.SEASONAL, (temperatureRange / 30.0).coerceIn(0.0, 1.0))
    raise(EnvironmentTag.HYPERSEASONAL, ((temperatureRange - 25.0) / 25.0).coerceIn(0.0, 1.0))
    raise(EnvironmentTag.ASEASONAL, (1.0 - temperatureRange / 20.0).coerceIn(0.0, 1.0))
    raise(EnvironmentTag.HIGH_INSOLATION, ((meanInsolation - 220.0) / 120.0).coerceIn(0.0, 1.0))
    raise(EnvironmentTag.LOW_LIGHT, ((150.0 - meanInsolation) / 120.0).coerceIn(0.0, 1.0))

    if (name.contains("forest") || name.contains("rainforest") || name.contains("boreal")) {
        raise(EnvironmentTag.FOREST, if (name.contains("rainforest")) 1.0 else 0.8)
    }
    if (name.contains("savanna") || name.contains("steppe") || name.contains("pulse")) {
        raise(EnvironmentTag.GRASSLAND, 1.0)
    }
    if (name.contains("desert") || name.contains("semidesert") ||
        name.contains("parch") || name.contains("barren")
    ) {
        raise(EnvironmentTag.DESERT, 1.0)
        raise(EnvironmentTag.DRY, 0.9)
    }
    if (name.contains("rainforest") || name.contains("hyperpluvial") ||
        name.contains("monsoon") || name.contains("moist")
    ) {
        raise(EnvironmentTag.WET, 0.8)
        raise(EnvironmentTag.WETLAND, if (name.contains("hyperpluvial")) 0.9 else 0.55)
    }
    if (name.contains("tundra") || name.contains("ice") || name.contains("frozen") ||
        name.contains("frigid") || name.contains("boreal")
    ) {
        raise(EnvironmentTag.COLD, 0.8)
        raise(EnvironmentTag.SNOWY, if (name.contains("ice") || name.contains("frozen")) 1.0 else 0.7)
    }
    if (name.contains("superseasonal")) raise(EnvironmentTag.SEASONAL, 0.85)
    if (name.contains("hyperseasonal") || name.contains("extraseasonal")) {
        raise(EnvironmentTag.HYPERSEASONAL, 1.0)
    }
    if (name.contains("tropical") || name.contains("supertropical") ||
        name.contains("hot_") || name.contains("torrid") || name.contains("boiling")
    ) {
        raise(EnvironmentTag.HOT, 0.75)
    }
    if (name.contains("dark") || name.contains("twilight") || name.contains("permanent_night")) {
        raise(EnvironmentTag.LOW_LIGHT, 1.0)
    }

    return EnvironmentProfile(weights)
}

/** Sum of all concrete-trait affinities toward this tile's environmental profile. */
fun SpeciesDefinition.environmentAffinity(profile: EnvironmentProfile): Double = effects
    .filterIsInstance<EnvironmentAffinityEffect>()
    .sumOf { effect -> effect.affinity * profile[effect.tag] }

/** Smooth positive placement weight combining tag affinity with continuous climate stress. */
fun speciesEstablishmentWeight(
    species: SpeciesDefinition,
    profile: EnvironmentProfile,
    averageClimateStress: Double,
): Double = exp(
    (
        EcologyConfig.environmentAffinitySelectionStrength * species.environmentAffinity(profile) -
            EcologyConfig.climateStressSelectionStrength * averageClimateStress
        ).coerceIn(-20.0, 20.0)
)
