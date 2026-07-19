package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatumSample
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

fun feedingFluxes(
    state: Biomass,
    model: EcosystemModel,
    year: Double = 0.0,
    habitatValues: Map<HabitatAxis, Double> = habitatAxisValues(year, state, model),
): List<FeedingFlux> {
    val profile = EcologyProfiler.current
    val profileStart = if (profile != null) System.nanoTime() else 0L
    val opportunities = mutableListOf<FeedingOpportunity>()
    val targetIngestionByFeeder = mutableMapOf<String, Double>()
    val totalSignalByFeeder = mutableMapOf<String, Double>()

    for (definition in model.species) {
        val feeding = definition.feeding ?: continue
        val feederBiomass = state.getValue(definition.id)
        if (feederBiomass <= 0.0) continue
        val predatorVisualMultiplier = visualInteractionMultiplier(
            definition, true, year, state, model, habitatValues,
        )

        val foodSignals = definition.diet.mapValues { (source, diet) ->
            val preyBiomass = state[source.preyId]?.coerceAtLeast(0.0) ?: 0.0
            val preyDensity = preyBiomass / model.landArea
            val availableFoodDensity = preyDensity *
                (diet.renewableProductionRate ?: 1.0)
            val refugeMultiplier = if (source.resource == FoodResource.MOTILE_PREY) {
                model.speciesById[source.preyId]?.let { preyDefinition ->
                    habitatRefugePreferenceMultiplier(
                        preyDefinition,
                        habitatValues,
                    )
                } ?: 1.0
            } else 1.0
            val visualMultiplier = if (source.resource == FoodResource.MOTILE_PREY) {
                val preyDefinition = model.speciesById[source.preyId]
                predatorVisualMultiplier * if (preyDefinition != null) {
                    visualInteractionMultiplier(
                        preyDefinition, false, year, state, model, habitatValues,
                    )
                } else 1.0
            } else 1.0
            diet.preference * refugeMultiplier * visualMultiplier *
                (availableFoodDensity / diet.halfSaturation)
                    .pow(feeding.responseExponent)
        }.filterValues { it > 0.0 }
        val totalFoodSignal = foodSignals.values.sum()
        if (totalFoodSignal <= 0.0) continue

        val overlappingConsumerDensity = model.consumerNicheOverlaps
            .getValue(definition.id)
            .entries
            .sumOf { (competitorId, overlap) ->
                overlap * state.getValue(competitorId).coerceAtLeast(0.0)
            } / model.landArea
        val interferenceLoad = feeding.interference * overlappingConsumerDensity
        val maximumIngestion = feeding.maxConsumptionRate * feederBiomass
        val targetIngestion = maximumIngestion * totalFoodSignal /
            (1.0 + totalFoodSignal + interferenceLoad)
        targetIngestionByFeeder[definition.id] = targetIngestion
        totalSignalByFeeder[definition.id] = totalFoodSignal

        for ((source, signal) in foodSignals) {
            val diet = definition.diet.getValue(source)
            opportunities += FeedingOpportunity(
                feeder = definition.id,
                source = source,
                signal = signal,
                pathwayCapacity = maximumIngestion * signal /
                    (1.0 + signal + interferenceLoad),
                preyLossFraction = diet.preyLossFraction,
                assimilationEfficiency = feeding.assimilationEfficiency,
                renewableProductionRate = diet.renewableProductionRate,
            )
        }
    }

    val allocated = opportunities.associateWith { opportunity ->
        (targetIngestionByFeeder.getValue(opportunity.feeder) *
            opportunity.signal / totalSignalByFeeder.getValue(opportunity.feeder))
            .coerceAtMost(opportunity.pathwayCapacity)
    }.toMutableMap()

    fun renewableCapacity(opportunity: FeedingOpportunity): Double? =
        opportunity.renewableProductionRate?.let { rate ->
            state.getValue(opportunity.source.preyId).coerceAtLeast(0.0) * rate
        }

    for ((_, sourceOpportunities) in opportunities
        .filter { it.renewableProductionRate != null }
        .groupBy { it.source }) {
        val capacity = renewableCapacity(sourceOpportunities.first())!!
        val requested = sourceOpportunities.sumOf { allocated.getValue(it) }
        if (requested > capacity && requested > 0.0) {
            val scale = capacity / requested
            sourceOpportunities.forEach { opportunity ->
                allocated[opportunity] = allocated.getValue(opportunity) * scale
            }
        }
    }

    val allocationTolerance = 1e-12
    var allocationIteration = 0
    var allocationChanged = true
    while (allocationChanged && allocationIteration < opportunities.size * 2 + 8) {
        allocationIteration += 1
        val allocatedByFeeder = opportunities
            .groupBy { it.feeder }
            .mapValues { (_, feederOpportunities) ->
                feederOpportunities.sumOf { allocated.getValue(it) }
            }
        val renewableUsedBySource = opportunities
            .filter { it.renewableProductionRate != null }
            .groupBy { it.source }
            .mapValues { (_, sourceOpportunities) ->
                sourceOpportunities.sumOf { allocated.getValue(it) }
            }
        val eligible = opportunities.filter { opportunity ->
            val feederUnmet = targetIngestionByFeeder.getValue(opportunity.feeder) -
                (allocatedByFeeder[opportunity.feeder] ?: 0.0)
            val pathwayHeadroom = opportunity.pathwayCapacity -
                allocated.getValue(opportunity)
            val renewableHeadroom = renewableCapacity(opportunity)?.let { capacity ->
                capacity - (renewableUsedBySource[opportunity.source] ?: 0.0)
            } ?: Double.POSITIVE_INFINITY
            feederUnmet > allocationTolerance &&
                pathwayHeadroom > allocationTolerance &&
                renewableHeadroom > allocationTolerance
        }
        val eligibleSignalByFeeder = eligible
            .groupBy { it.feeder }
            .mapValues { (_, feederOpportunities) -> feederOpportunities.sumOf { it.signal } }
        val proposed = eligible.associateWith { opportunity ->
            val unmet = targetIngestionByFeeder.getValue(opportunity.feeder) -
                (allocatedByFeeder[opportunity.feeder] ?: 0.0)
            val weightedShare = unmet * opportunity.signal /
                eligibleSignalByFeeder.getValue(opportunity.feeder)
            minOf(
                weightedShare,
                opportunity.pathwayCapacity - allocated.getValue(opportunity),
            ).coerceAtLeast(0.0)
        }
        val renewableProposedBySource = proposed.entries
            .filter { it.key.renewableProductionRate != null }
            .groupBy { it.key.source }
            .mapValues { (_, entries) -> entries.sumOf { it.value } }

        var acceptedTotal = 0.0
        for ((opportunity, proposal) in proposed) {
            val sourceScale = renewableCapacity(opportunity)?.let { capacity ->
                val remaining = (capacity -
                    (renewableUsedBySource[opportunity.source] ?: 0.0))
                    .coerceAtLeast(0.0)
                val totalProposed = renewableProposedBySource
                    .getValue(opportunity.source)
                if (totalProposed > 0.0)
                    (remaining / totalProposed).coerceAtMost(1.0)
                else 0.0
            } ?: 1.0
            val accepted = proposal * sourceScale
            allocated[opportunity] = allocated.getValue(opportunity) + accepted
            acceptedTotal += accepted
        }
        allocationChanged = acceptedTotal > allocationTolerance
    }

    val result = opportunities.mapNotNull { opportunity ->
        val ingested = allocated.getValue(opportunity)
        if (ingested <= 0.0) return@mapNotNull null
        FeedingFlux(
            feeder = opportunity.feeder,
            source = opportunity.source,
            ingested = ingested,
            preyLoss = ingested * opportunity.preyLossFraction,
            assimilated = ingested * opportunity.assimilationEfficiency,
            renewableProductionRate = opportunity.renewableProductionRate,
        )
    }
    if (profile != null) {
        profile.recordFeeding(
            elapsedNanos = System.nanoTime() - profileStart,
            opportunityCount = opportunities.size,
            fluxCount = result.size,
            iterations = allocationIteration,
        )
    }
    return result
}

fun carryingCapacity(
    parameters: AutotrophyParameters,
    baseCarryingCapacity: Double,
    year: Double,
    seasonsEnabled: Boolean,
): Double {
    val seasonalOffset = if (seasonsEnabled) {
        parameters.seasonalAmplitude * sin(2.0 * PI * (year - parameters.seasonalPhase))
    } else {
        0.0
    }
    return baseCarryingCapacity * (1.0 + seasonalOffset)
}

/** Extra fractional mortality per year when conditions lie outside a species' optimum. */
fun climateStressMortalityRate(
    niche: ClimateNiche,
    climate: ClimateDatumSample,
): Double {
    val temperatureDistanceC = when {
        climate.averageTemperature < niche.minOptimalTemperatureC ->
            niche.minOptimalTemperatureC - climate.averageTemperature
        climate.averageTemperature > niche.maxOptimalTemperatureC ->
            climate.averageTemperature - niche.maxOptimalTemperatureC
        else -> 0.0
    }
    val dryFraction = if (climate.precipitation < niche.minOptimalMoistureMm) {
        (niche.minOptimalMoistureMm - climate.precipitation) /
            niche.minOptimalMoistureMm.coerceAtLeast(1.0)
    } else 0.0
    val wetFraction = niche.maxOptimalMoistureMm?.let { maximum ->
        if (climate.precipitation > maximum) {
            (climate.precipitation - maximum) / maximum.coerceAtLeast(1.0)
        } else 0.0
    } ?: 0.0
    val lowLightFraction = if (climate.insolation < niche.minOptimalInsolationWm2) {
        (niche.minOptimalInsolationWm2 - climate.insolation) /
            niche.minOptimalInsolationWm2.coerceAtLeast(1.0)
    } else 0.0
    val brightLightFraction = if (climate.insolation > niche.maxOptimalInsolationWm2) {
        (climate.insolation - niche.maxOptimalInsolationWm2) /
            niche.maxOptimalInsolationWm2.coerceAtLeast(1.0)
    } else 0.0
    return niche.stressSensitivity * (
        0.06 * temperatureDistanceC +
            1.25 * dryFraction +
            0.75 * wetFraction +
            0.75 * lowLightFraction +
            0.75 * brightLightFraction
    )
}

/** Direct freeze injury not already represented by the broad climate niche. */
fun frostStressMortalityRate(
    definition: SpeciesDefinition,
    climate: ClimateDatumSample,
): Double {
    val degreesBelowFreezing = (-climate.averageTemperature).coerceAtLeast(0.0)
    val unprotectedPlantMortality = if (definition.isSessileProducer() &&
        !definition.blueprint.hasTag(EffectTag.WINTER_SURVIVAL)
    ) 0.20 * degreesBelowFreezing else 0.0
    val exposedAnimalMortality = definition.effects.filterIsInstance<ColdExposureEffect>()
        .sumOf { it.mortalityPerDegreeBelowFreezing } * degreesBelowFreezing
    return unprotectedPlantMortality + exposedAnimalMortality
}

/** Direct heat load retained by fur, blubber, and other heavy insulation. */
fun insulationHeatStressMortalityRate(
    definition: SpeciesDefinition,
    climate: ClimateDatumSample,
): Double = definition.effects.filterIsInstance<HeatRetentionEffect>().sumOf { effect ->
    (climate.averageTemperature - effect.thresholdC).coerceAtLeast(0.0) *
        effect.mortalityPerDegreeAboveThreshold
}

/** Mortality from hypoxia above the species' trait-derived optimal elevation. */
fun altitudeStressMortalityRate(definition: SpeciesDefinition, altitudeMeters: Double): Double {
    val adaptations = definition.effects.filterIsInstance<AltitudeToleranceEffect>()
    val maximumOptimalAltitude = adaptations.maxOfOrNull { it.maximumOptimalAltitudeMeters } ?: 2_500.0
    val mortalityPerKilometer = adaptations.minOfOrNull { it.mortalityPerKilometerAbove } ?: 0.45
    return ((altitudeMeters - maximumOptimalAltitude) / 1_000.0).coerceAtLeast(0.0) * mortalityPerKilometer
}

fun derivatives(year: Double, state: Biomass, model: EcosystemModel): Biomass {
    val profile = EcologyProfiler.current
    val profileStart = if (profile != null) System.nanoTime() else 0L
    val changes = model.species.associate { it.id to 0.0 }.toMutableMap()
    val climate = model.localClimateAt(year)
    val habitatStart = if (profile != null) System.nanoTime() else 0L
    val habitatValues = habitatAxisValues(year, state, model)
    if (profile != null) profile.habitatNanos.add(System.nanoTime() - habitatStart)
    val fluxes = feedingFluxes(state, model, year, habitatValues)
    val assimilatedByFeeder = fluxes
        .groupBy { it.feeder }
        .mapValues { (_, feederFluxes) -> feederFluxes.sumOf { it.assimilated } }
    val sessileGroupBiomass = model.sessileProducerGroups.mapValues { (_, definitions) ->
        definitions.sumOf { state.getValue(it.id).coerceAtLeast(0.0) }
    }
    for (definition in model.species) {
        val biomass = state.getValue(definition.id).coerceAtLeast(0.0)
        changes[definition.id] = changes.getValue(definition.id) -
            definition.maintenanceRate * biomass
        var autotrophicEnergyGain = 0.0
        definition.autotrophy?.let { autotrophy ->
            val sessile = definition.isSessileProducer()
            val capacityDensity = if (sessile) {
                model.sessileCarryingCapacityDensity(definition.sizeClass, year)
            } else {
                carryingCapacity(
                    autotrophy,
                    autotrophy.baseCarryingCapacity,
                    year,
                    model.seasonsEnabled,
                )
            }
            val biomassUsingCapacity = if (sessile) {
                sessileGroupBiomass.getValue(definition.sizeClass)
            } else biomass
            val totalCapacity = capacityDensity * model.landArea
            val canopyLightFraction = if (sessile) {
                availableLightFraction(definition, year, state, model)
            } else 1.0
            val availableInsolation =
                (climate.insolation * canopyLightFraction).coerceAtLeast(0.0)
            val lightResponse = availableInsolation /
                (availableInsolation + autotrophy.photosynthesisHalfSaturationWm2)
            val minimumMoisture = definition.climateNiche.minOptimalMoistureMm
            val moistureResponse = if (minimumMoisture > 0.0) {
                (climate.precipitation / minimumMoisture).coerceIn(0.0, 1.0)
            } else 1.0
            val autotrophicChange = autotrophy.growthRate * lightResponse *
                moistureResponse * biomass *
                (1.0 - biomassUsingCapacity / totalCapacity)
            autotrophicEnergyGain = autotrophicChange.coerceAtLeast(0.0)
            changes[definition.id] = changes.getValue(definition.id) + autotrophicChange
        }
        definition.feeding?.let { feeding ->
            val selfDensity = biomass / model.landArea
            changes[definition.id] = changes.getValue(definition.id) -
                feeding.mortalityRate * biomass -
                feeding.densityDependence * biomass * selfDensity
            val requiredAssimilation = feeding.metabolicDemandRate * biomass
            changes[definition.id] = changes.getValue(definition.id) - requiredAssimilation
            val assimilationCoverage = if (requiredAssimilation > 0.0) {
                ((autotrophicEnergyGain +
                    (assimilatedByFeeder[definition.id] ?: 0.0)) / requiredAssimilation)
                    .coerceIn(0.0, 1.0)
            } else 1.0
            val starvationShortfall = 1.0 - assimilationCoverage
            changes[definition.id] = changes.getValue(definition.id) -
                feeding.starvationMortalityRate * starvationShortfall.pow(2.0) * biomass
        }
        val climateMortalityRate = climateStressMortalityRate(
            definition.climateNiche,
            climate,
        )
        changes[definition.id] = changes.getValue(definition.id) -
            climateMortalityRate * biomass
        val frostMortalityRate = frostStressMortalityRate(definition, climate)
        val heatRetentionMortalityRate = insulationHeatStressMortalityRate(definition, climate)
        val altitudeMortalityRate = altitudeStressMortalityRate(definition, model.altitudeMeters)
        changes[definition.id] = changes.getValue(definition.id) -
            (frostMortalityRate + heatRetentionMortalityRate + altitudeMortalityRate) * biomass
        val habitatMortalityRate = habitatStressMortalityRate(
            definition,
            habitatValues,
        )
        changes[definition.id] = changes.getValue(definition.id) -
            habitatMortalityRate * biomass
    }

    for (flux in fluxes) {
        changes[flux.prey] = changes.getValue(flux.prey) - flux.preyLoss
        changes[flux.feeder] = changes.getValue(flux.feeder) + flux.assimilated
    }

    if (profile != null) {
        profile.derivativeCalls.increment()
        profile.derivativeNanos.add(System.nanoTime() - profileStart)
    }
    return changes
}
