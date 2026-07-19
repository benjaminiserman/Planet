package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.ClimateDatumSample

fun climateSampleAtElevation(
    climate: ClimateDatumSample,
    elevationMeters: Double,
): ClimateDatumSample {
    require(elevationMeters in -500.0..9_000.0)
    return climate.copy(
        averageTemperature = climate.averageTemperature - 0.0065 * elevationMeters,
    )
}

fun speciesSuitabilityIssues(
    species: SpeciesDefinition,
    climate: ClimateDatumSample,
    elevationMeters: Double,
): List<String> {
    val local = climateSampleAtElevation(climate, elevationMeters)
    val niche = species.climateNiche
    return buildList {
        if (local.averageTemperature < niche.minOptimalTemperatureC) add("too cold")
        if (local.averageTemperature > niche.maxOptimalTemperatureC) add("too hot")
        if (local.precipitation < niche.minOptimalMoistureMm) add("too dry")
        if (niche.maxOptimalMoistureMm != null && local.precipitation > niche.maxOptimalMoistureMm) add("too wet")
        if (local.insolation < niche.minOptimalInsolationWm2) add("too dark")
        if (local.insolation > niche.maxOptimalInsolationWm2) add("too bright")
        if (frostStressMortalityRate(species, local) > 0.0) add("direct frost exposure")
        if (insulationHeatStressMortalityRate(species, local) > 0.0) add("insulation heat stress")
        if (altitudeStressMortalityRate(species, elevationMeters) > 0.0) add("altitude hypoxia")
    }.distinct()
}

fun isSpeciesSuitedTo(
    species: SpeciesDefinition,
    climate: ClimateDatumSample,
    elevationMeters: Double,
): Boolean = speciesSuitabilityIssues(species, climate, elevationMeters).isEmpty()

/** Total annualized mortality pressure used to judge establishment in one monthly sample. */
fun speciesClimateStressRate(
    species: SpeciesDefinition,
    climate: ClimateDatumSample,
    elevationMeters: Double,
): Double {
    val local = climateSampleAtElevation(climate, elevationMeters)
    return climateStressMortalityRate(species.climateNiche, local) +
        frostStressMortalityRate(species, local) +
        insulationHeatStressMortalityRate(species, local) +
        altitudeStressMortalityRate(species, elevationMeters)
}

/** Allows survivable seasonal stress while rejecting severe average or peak conditions. */
fun isSpeciesSuitedTo(
    species: SpeciesDefinition,
    climate: ClimateDatum,
    elevationMeters: Double,
): Boolean {
    val stressRates = climate.months.map { sample ->
        speciesClimateStressRate(species, sample, elevationMeters)
    }
    return stressRates.average() <= EcologyConfig.maximumAverageEstablishmentStress &&
        stressRates.max() <= EcologyConfig.maximumPeakEstablishmentStress
}

/** Simple fallback ranking used only when no producer has a perfect year-round match. */
fun speciesSuitabilityIssueCount(
    species: SpeciesDefinition,
    climate: ClimateDatum,
    elevationMeters: Double,
): Int = climate.months.sumOf { sample ->
    speciesSuitabilityIssues(species, sample, elevationMeters).size
}
