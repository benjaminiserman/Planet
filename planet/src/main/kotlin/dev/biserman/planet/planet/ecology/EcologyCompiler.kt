package dev.biserman.planet.planet.ecology

import kotlin.math.abs
import kotlin.math.pow

fun numericValue(
    blueprint: SpeciesBlueprint,
    target: NumericTarget,
    baseValue: Double,
): Double {
    val multiplier = blueprint.effects
        .filterIsInstance<NumericMultiplier>()
        .filter { it.target == target }
        .fold(1.0) { product, effect -> product * effect.multiplier }
    val offset = blueprint.effects
        .filterIsInstance<NumericOffset>()
        .filter { it.target == target }
        .sumOf { it.offset }
    val upperBound = blueprint.effects
        .filterIsInstance<NumericUpperBound>()
        .filter { it.target == target }
        .minOfOrNull { it.maximum } ?: Double.POSITIVE_INFINITY
    return ((baseValue + offset) * multiplier).coerceAtMost(upperBound)
}

fun validateBlueprints(blueprints: List<SpeciesBlueprint>) {
    val ids = blueprints.map { it.id }
    require(ids.size == ids.toSet().size) { "Species ids must be unique" }

    for (blueprint in blueprints) {
        require(
            blueprint.hasTag(EffectTag.AUTOTROPHY) ||
                blueprint.hasTag(EffectTag.FEEDING)
        ) {
            blueprint.id + " needs an autotrophy or feeding effect"
        }

        for (requirement in blueprint.effects.filterIsInstance<RequiresTag>()) {
            require(blueprint.hasTag(requirement.tag)) {
                blueprint.id + " requires effect tag " + requirement.tag
            }
        }
        for (conflict in blueprint.effects.filterIsInstance<ConflictsWithTag>()) {
            require(!blueprint.hasTag(conflict.tag)) {
                blueprint.id + " conflicts with effect tag " + conflict.tag
            }
        }
        for (group in ExclusiveGroup.entries) {
            val count = blueprint.effects
                .filterIsInstance<ExclusiveEffect>()
                .count { it.group == group }
            require(count <= 1) {
                blueprint.id + " has multiple effects in exclusive group " + group
            }
        }
    }
}

fun deriveAutotrophy(blueprint: SpeciesBlueprint): AutotrophyParameters? {
    if (!blueprint.hasTag(EffectTag.AUTOTROPHY)) return null

    return AutotrophyParameters(
        growthRate = numericValue(
            blueprint,
            NumericTarget.AUTOTROPHY_GROWTH_RATE,
            baseValue = 1.40,
        ),
        baseCarryingCapacity = numericValue(
            blueprint,
            NumericTarget.CARRYING_CAPACITY,
            baseValue = 0.55,
        ),
        seasonalAmplitude = numericValue(
            blueprint,
            NumericTarget.SEASONAL_AMPLITUDE,
            baseValue = 0.18,
        ),
        seasonalPhase = 0.14 + 0.025 * blueprint.sizeClass.rank,
        photosynthesisHalfSaturationWm2 = numericValue(
            blueprint,
            NumericTarget.PHOTOSYNTHESIS_HALF_SATURATION_W_M2,
            baseValue = 100.0,
        ),
    )
}

fun deriveFeeding(blueprint: SpeciesBlueprint): FeedingParameters? {
    if (!blueprint.hasTag(EffectTag.FEEDING)) return null

    return FeedingParameters(
        maxConsumptionRate = numericValue(
            blueprint,
            NumericTarget.MAX_CONSUMPTION_RATE,
            baseValue = 2.50 * blueprint.sizeClass.feedingAllometry,
        ),
        assimilationEfficiency = numericValue(
            blueprint,
            NumericTarget.ASSIMILATION_EFFICIENCY,
            baseValue = 0.18,
        ),
        mortalityRate = numericValue(
            blueprint,
            NumericTarget.MORTALITY_RATE,
            baseValue = 0.06 * blueprint.sizeClass.feedingAllometry,
        ),
        metabolicDemandRate = numericValue(
            blueprint,
            NumericTarget.METABOLIC_DEMAND_RATE,
            baseValue = 0.08 * blueprint.sizeClass.feedingAllometry,
        ),
        starvationMortalityRate = numericValue(
            blueprint,
            NumericTarget.STARVATION_MORTALITY_RATE,
            baseValue = 2.20 * blueprint.sizeClass.feedingAllometry,
        ),
        densityDependence = numericValue(
            blueprint,
            NumericTarget.DENSITY_DEPENDENCE,
            baseValue = 0.90 / (1.0 + 0.18 * blueprint.sizeClass.rank),
        ),
        interference = numericValue(
            blueprint,
            NumericTarget.INTERFERENCE,
            baseValue = 0.60,
        ),
        responseExponent = numericValue(
            blueprint,
            NumericTarget.RESPONSE_EXPONENT,
            baseValue = 2.0,
        ),
    )
}

fun deriveClimateNiche(blueprint: SpeciesBlueprint): ClimateNiche {
    val bergmannShift = if (blueprint.hasTag(EffectTag.MOTILE)) {
        blueprint.sizeClass.bergmannTemperatureShiftC
    } else 0.0
    val minTemperature = numericValue(
        blueprint,
        NumericTarget.MIN_OPTIMUM_TEMPERATURE_C,
        baseValue = 5.0 + bergmannShift,
    )
    val maxTemperature = numericValue(
        blueprint,
        NumericTarget.MAX_OPTIMUM_TEMPERATURE_C,
        baseValue = 25.0 + bergmannShift,
    )
    val maximumMoisture = numericValue(
        blueprint,
        NumericTarget.MAX_OPTIMUM_MOISTURE_MM,
        baseValue = Double.POSITIVE_INFINITY,
    )
    return ClimateNiche(
        minOptimalTemperatureC = minTemperature,
        maxOptimalTemperatureC = maxTemperature,
        minOptimalMoistureMm = numericValue(
            blueprint,
            NumericTarget.MIN_OPTIMUM_MOISTURE_MM,
            baseValue = 40.0,
        ),
        maxOptimalMoistureMm = maximumMoisture.takeIf { it.isFinite() },
        minOptimalInsolationWm2 = numericValue(
            blueprint,
            NumericTarget.MIN_OPTIMUM_INSOLATION_W_M2,
            baseValue = if (blueprint.hasTag(EffectTag.MOTILE)) 40.0 else 0.0,
        ),
        maxOptimalInsolationWm2 = numericValue(
            blueprint,
            NumericTarget.MAX_OPTIMUM_INSOLATION_W_M2,
            baseValue = if (blueprint.hasTag(EffectTag.MOTILE)) 350.0 else 500.0,
        ),
        stressSensitivity = numericValue(
            blueprint,
            NumericTarget.CLIMATE_STRESS_SENSITIVITY,
            baseValue = 1.0,
        ),
    )
}

fun relativeSizePreference(
    feedingEffect: FeedsOn,
    feeder: SpeciesBlueprint,
    prey: SpeciesBlueprint,
): Double {
    val minimum = feedingEffect.minimumRelativeSize ?: return 1.0
    val maximumBonus = feeder.effects
        .filterIsInstance<RelativeSizeModifier>()
        .sumOf { it.maximumRelativeSizeBonus }
    val maximum = (feedingEffect.maximumRelativeSize ?: 0) + maximumBonus
    val gap = prey.sizeClass.rank - feeder.sizeClass.rank
    if (gap < minimum || gap > maximum) return 0.0

    return when {
        gap > 0 -> 0.80
        gap == 0 -> 1.00
        gap == -1 -> 0.85
        else -> 0.55
    }
}

fun defenseMultiplier(
    defense: DefenseEffect,
    feeder: SpeciesBlueprint,
    prey: SpeciesBlueprint,
): Double {
    val sizeAdvantage = feeder.sizeClass.rank - prey.sizeClass.rank
    if (
        defense.ignoredWhenFeederSizeAdvantageAtLeast != null &&
        sizeAdvantage >= defense.ignoredWhenFeederSizeAdvantageAtLeast
    ) {
        return 1.0
    }

    return defense.counterMultipliers
        .filterKeys { feeder.hasTag(it) }
        .values
        .maxOrNull()
        ?: defense.defaultPreferenceMultiplier
}

fun deriveDietEntries(
    feeder: SpeciesBlueprint,
    prey: SpeciesBlueprint,
): Map<FoodSource, DietEntry> {
    if (feeder.id == prey.id) return emptyMap()

    val feedingEffects = feeder.effects.filterIsInstance<FeedsOn>()
    val edibleEffects = prey.effects.filterIsInstance<EdibleAs>()

    return edibleEffects.mapNotNull { edibleEffect ->
        var preference = 0.0
        for (feedingEffect in feedingEffects) {
            if (feedingEffect.resource != edibleEffect.resource) continue
            if (
                feedingEffect.maximumPreySizeRank != null &&
                prey.sizeClass.rank > feedingEffect.maximumPreySizeRank
            ) continue
            val sizePreference = relativeSizePreference(feedingEffect, feeder, prey)
            if (sizePreference <= 0.0) continue
            val candidate = feedingEffect.preference *
                edibleEffect.preferenceMultiplier *
                sizePreference
            preference = maxOf(preference, candidate)
        }
        if (preference <= 0.0) return@mapNotNull null
        for (defense in prey.effects.filterIsInstance<DefenseEffect>()) {
            preference *= defenseMultiplier(defense, feeder, prey)
        }
        val source = FoodSource(prey.id, edibleEffect.resource)
        source to DietEntry(
            preference = preference.coerceAtLeast(0.01),
            halfSaturation = 0.06 * 1.45.pow(prey.sizeClass.rank),
            preyLossFraction = edibleEffect.preyLossFraction,
            renewableProductionRate = edibleEffect.renewableProductionRate,
        )
    }.toMap()
}

fun deriveMaintenanceRate(blueprint: SpeciesBlueprint): Double =
    0.005 + blueprint.effects
        .filterIsInstance<MaintenanceCost>()
        .sumOf { it.biomassLossRatePerYear }

fun deriveSpeciesCatalog(
    blueprints: List<SpeciesBlueprint>,
): List<SpeciesDefinition> {
    validateBlueprints(blueprints)
    val definitions = blueprints.map { blueprint ->
        val feeding = deriveFeeding(blueprint)
        val diet = if (feeding == null) {
            emptyMap()
        } else {
            blueprints.flatMap { prey ->
                deriveDietEntries(blueprint, prey).entries
            }.associate { it.key to it.value }
        }
        SpeciesDefinition(
            blueprint = blueprint,
            autotrophy = deriveAutotrophy(blueprint),
            feeding = feeding,
            climateNiche = deriveClimateNiche(blueprint),
            maintenanceRate = deriveMaintenanceRate(blueprint),
            diet = diet,
        )
    }
    validateSpeciesCatalog(definitions)
    return definitions
}

fun validateSpeciesCatalog(catalog: List<SpeciesDefinition>) {
    val ids = catalog.map { it.id }
    require(ids.size == ids.toSet().size) { "Species ids must be unique" }
    val idSet = ids.toSet()

    for (definition in catalog) {
        require(definition.maintenanceRate >= 0.0)
        definition.autotrophy?.let {
            require(it.growthRate > 0.0)
            require(it.baseCarryingCapacity > 0.0)
            require(it.seasonalAmplitude in 0.0..<1.0)
            require(it.photosynthesisHalfSaturationWm2 > 0.0)
        }
        definition.feeding?.let {
            require(it.maxConsumptionRate > 0.0)
            require(it.assimilationEfficiency in 0.0..1.0)
            require(it.mortalityRate >= 0.0)
            require(it.metabolicDemandRate > 0.0)
            require(it.starvationMortalityRate > 0.0)
            require(it.densityDependence > 0.0)
            require(it.interference >= 0.0)
            require(it.responseExponent > 1.0)
        }
        require(definition.diet.keys.all { it.preyId in idSet })
    }
}

fun validateFoodWeb(definitions: List<SpeciesDefinition>) {
    val ids = definitions.map { it.id }
    require(ids.size == ids.toSet().size) { "Species ids must be unique" }
}

