package dev.biserman.planet.planet.ecology.v2

import kotlin.math.max

class CompiledSpecies(
    val index: Int,
    val id: String,
    val displayName: String,
    val sizeClass: SizeClass,
    val motile: Boolean,
    val kind: SpeciesKind,
    val massKg: Double,
    val maintenanceDemand: Double,
    val seasonalReproduction: Double,
    val temperatureOuterLow: Double,
    val temperatureOptimalLow: Double,
    val temperatureOptimalHigh: Double,
    val temperatureOuterHigh: Double,
    val seasonalColdToleranceC: Double,
    val seasonalColdTriggerInsolation: Double,
    val thermalStrategy: ThermalStrategy?,
    val aquaticSalinityTolerance: AquaticSalinityTolerance,
    val minimumWater: Double,
    val optimalMaximumWater: Double,
    val maximumWater: Double,
    val insolationOptimum: Double,
    val canopyLightEfficiency: Double,
    val reserveCapacity: Double,
    val nicheCompetitionSensitivity: Double,
    val dormancyKind: DormancyKind,
    val dormantSurvival: Double,
    val dispersalKind: DispersalKind,
    val captureAbility: Double,
    val defense: Double,
    val reefUse: Double,
    val reefBuilding: Double,
    val wasteFertilization: Double,
    val pelagicAerialResident: Boolean,
    val darkWaterAdapted: Boolean,
    val photosyntheticColor: BiologicalColor?,
    val camouflageColor: BiologicalColor?,
    val habitatSupport: DoubleArray,
    val strategySupport: DoubleArray,
    val camouflage: DoubleArray,
    val nicheFit: DoubleArray,
    val ancestorSpeciesId: String?,
)

enum class InteractionKind {
    NONE,
    PREDATION,
    FILTER_FEEDING,
    GRAZING,
    SUPPLEMENTAL_FEEDING,
    PARASITISM,
}

data class CompiledInteraction(
    val kind: InteractionKind,
    val consumerGainRate: Double,
    val targetLossRate: Double,
    val targetBenefitRate: Double = 0.0,
    val targetRequired: Boolean = false,
) {
    companion object {
        val NONE = CompiledInteraction(InteractionKind.NONE, 0.0, 0.0)
    }
}

class InteractionMatrix internal constructor(
    val speciesCount: Int,
    private val kinds: ByteArray,
    private val consumerGainRates: DoubleArray,
    private val targetLossRates: DoubleArray,
    private val targetBenefitRates: DoubleArray,
    private val requiredTargets: ByteArray,
) {
    fun get(consumerIndex: Int, targetIndex: Int): CompiledInteraction {
        val offset = consumerIndex * speciesCount + targetIndex
        return CompiledInteraction(
            kind = InteractionKind.entries[kinds[offset].toInt()],
            consumerGainRate = consumerGainRates[offset],
            targetLossRate = targetLossRates[offset],
            targetBenefitRate = targetBenefitRates[offset],
            targetRequired = requiredTargets[offset].toInt() != 0,
        )
    }

    internal fun kindAt(offset: Int): Int = kinds[offset].toInt()
    internal fun consumerGainAt(offset: Int): Double = consumerGainRates[offset]
    internal fun targetLossAt(offset: Int): Double = targetLossRates[offset]
    internal fun targetBenefitAt(offset: Int): Double = targetBenefitRates[offset]
    internal fun targetRequiredAt(offset: Int): Boolean = requiredTargets[offset].toInt() != 0
}

data class CompiledEcology(
    val species: List<CompiledSpecies>,
    val niches: List<NicheDefinition>,
    val interactions: InteractionMatrix,
) {
    private val indexById = species.associate { it.id to it.index }

    fun speciesIndex(id: String): Int =
        indexById[id] ?: error("Unknown species id: $id")
}

object EcologyCompiler {
    private val biochemistryTraits = setOf(
        CommonTrait.TEMPERATE_BIOCHEMISTRY,
        CommonTrait.FRIGID_BIOCHEMISTRY,
        CommonTrait.HOT_BIOCHEMISTRY,
    )
    fun compile(
        definitions: List<SpeciesDefinition>,
        niches: List<NicheDefinition> = EcologyNiches.defaults,
    ): CompiledEcology {
        require(definitions.isNotEmpty())
        require(definitions.map { it.id }.distinct().size == definitions.size) {
            "Species ids must be unique"
        }
        require(niches.distinct().size == niches.size) {
            "Niche definitions must be unique"
        }

        val compiledSpecies = definitions.mapIndexed { index, definition ->
            compileSpecies(index, definition, niches)
        }
        return CompiledEcology(
            species = compiledSpecies,
            niches = niches,
            interactions = compileInteractions(definitions, compiledSpecies),
        )
    }

    private fun compileSpecies(
        index: Int,
        definition: SpeciesDefinition,
        niches: List<NicheDefinition>,
    ): CompiledSpecies {
        val commonTraits = definition.traits.filterIsInstance<CommonTrait>().toSet()
        val allEffects = definition.traits.flatMap { it.effects }
        val thermalStrategies = allEffects.filterIsInstance<TraitEffect.ThermalRegulation>()
        require(
            definition.kind == SpeciesKind.INVARIANT ||
                definition.traits.none { it.invariantOnly },
        ) {
            "${definition.displayName} uses a trait reserved for invariant aggregate guilds"
        }
        require(
            definition.kind != SpeciesKind.INVARIANT ||
                CommonTrait.INVARIANT_RESISTANCE in commonTraits,
        ) {
            "${definition.displayName} is invariant and must have invariant guild resilience"
        }
        require(commonTraits.count { it in biochemistryTraits } == 1) {
            "${definition.displayName} must have exactly one biochemistry foundation"
        }
        require(!definition.motile || thermalStrategies.size == 1) {
            "${definition.displayName} is motile and must have exactly one thermal strategy"
        }
        require(definition.motile || thermalStrategies.isEmpty()) {
            "${definition.displayName} is not motile but has a motile thermal strategy"
        }
        require(!definition.motile || CommonTrait.ROOTED_BODY !in commonTraits) {
            "${definition.displayName} is motile and cannot have a rooted body; use a locomotion trait"
        }
        require(definition.motile || CommonTrait.TERRESTRIAL_LOCOMOTION !in commonTraits) {
            "${definition.displayName} is not motile and cannot have terrestrial locomotion"
        }
        definition.traits.filterNot { it.isFoundation }.forEach { trait ->
            val cost = trait.effects.filterIsInstance<TraitEffect.MaintenanceCost>().sumOf { it.fraction }
            require(cost > 0.0) {
                "Non-foundation trait '${trait.displayName}' must have an explicit maintenance/opportunity cost"
            }
            require(trait.effects.any { it !is TraitEffect.MaintenanceCost } || trait.relationships.isNotEmpty()) {
                "Non-foundation trait '${trait.displayName}' must provide an effect"
            }
        }

        val habitatSupport = DoubleArray(Habitat.entries.size)
        val strategySupport = DoubleArray(EcoStrategy.entries.size)
        val camouflage = DoubleArray(Habitat.entries.size)
        var temperatureShift = 0.0
        var colderTolerance = sizeTemperatureTolerance(definition.sizeClass)
        var hotterTolerance = sizeTemperatureTolerance(definition.sizeClass)
        var colderOptimalTolerance = 0.0
        var hotterOptimalTolerance = 0.0
        var seasonalColdTolerance = 0.0
        var seasonalColdTrigger = 0.0
        var thermalStrategy: ThermalStrategy? = null
        var waterRequirement = 0.25
        var optimalMaximumWater = 1.0
        var maximumWater = 1.0
        var insolationOptimum = 0.8
        var canopyLightEfficiency = 0.0
        var reserveCapacity = 0.25
        var nicheCompetitionSensitivity = 1.0
        var dormancyKind = DormancyKind.NONE
        var dormantSurvival = 0.0
        var dispersalKind = DispersalKind.NONE
        var reproductionMultiplier = 1.0
        val maintenanceCost = definition.traits.sumOf { trait ->
            val scale = if (trait.isFoundation) 1.0 else 0.35
            trait.effects.filterIsInstance<TraitEffect.MaintenanceCost>()
                .sumOf { it.fraction } * scale
        }
        var captureAbility = 0.5
        var defense = 0.25
        var reefUse = 0.0
        var reefBuilding = 0.0
        var wasteFertilization = 0.0
        var pelagicAerialResident = false
        var darkWaterAdapted = false
        var freshwaterAdapted = false
        var broadSalinityTolerance = false

        allEffects.forEach { effect ->
            when (effect) {
                is TraitEffect.HabitatSupport ->
                    habitatSupport[effect.habitat.ordinal] += effect.amount
                is TraitEffect.StrategySupport ->
                    strategySupport[effect.strategy.ordinal] += effect.amount
                is TraitEffect.TemperatureShift -> temperatureShift += effect.degreesC
                is TraitEffect.TemperatureTolerance -> {
                    colderTolerance += effect.colderC
                    hotterTolerance += effect.hotterC
                }
                is TraitEffect.TemperatureOptimalTolerance -> {
                    colderOptimalTolerance += effect.colderC
                    hotterOptimalTolerance += effect.hotterC
                }
                is TraitEffect.ThermalRegulation -> thermalStrategy = effect.strategy
                is TraitEffect.SeasonalColdTolerance -> {
                    seasonalColdTolerance += effect.maximumBonusC
                    seasonalColdTrigger = max(seasonalColdTrigger, effect.triggerInsolation)
                }
                is TraitEffect.WaterRequirement -> waterRequirement += effect.change
                is TraitEffect.MaximumWaterTolerance -> {
                    optimalMaximumWater += effect.optimalMaximumChange
                    maximumWater += effect.absoluteMaximumChange
                }
                is TraitEffect.InsolationOptimum -> insolationOptimum += effect.change
                is TraitEffect.CanopyLightEfficiency -> canopyLightEfficiency += effect.change
                is TraitEffect.CaptureAbility -> captureAbility += effect.change
                is TraitEffect.Defense -> defense += effect.change
                is TraitEffect.Camouflage -> camouflage[effect.habitat.ordinal] += effect.change
                is TraitEffect.ReefUse -> reefUse += effect.change
                is TraitEffect.ReefBuilding -> reefBuilding += effect.change
                is TraitEffect.WasteFertilization -> wasteFertilization += effect.change
                is TraitEffect.ReserveCapacity -> reserveCapacity += effect.change
                is TraitEffect.NicheCompetitionSensitivity ->
                    nicheCompetitionSensitivity *= effect.multiplier
                is TraitEffect.Dormancy -> {
                    require(dormancyKind == DormancyKind.NONE) {
                        "${definition.displayName} has multiple dormancy modes"
                    }
                    dormancyKind = effect.kind
                    dormantSurvival = effect.survivalPerSeason
                }
                is TraitEffect.Dispersal -> {
                    if (effect.kind.rangeClass > dispersalKind.rangeClass) {
                        dispersalKind = effect.kind
                    }
                }
                is TraitEffect.ReproductionMultiplier -> reproductionMultiplier *= effect.multiplier
                TraitEffect.FreshwaterOsmoregulation -> freshwaterAdapted = true
                TraitEffect.BroadSalinityTolerance -> broadSalinityTolerance = true
                TraitEffect.PelagicAerialResidency -> pelagicAerialResident = true
                TraitEffect.DarkWaterAdaptation -> darkWaterAdapted = true
                is TraitEffect.MaintenanceCost -> Unit
            }
        }

        val aquaticSalinityTolerance = when {
            broadSalinityTolerance -> AquaticSalinityTolerance.BROAD
            freshwaterAdapted -> AquaticSalinityTolerance.FRESHWATER_ONLY
            else -> AquaticSalinityTolerance.SALTWATER_ONLY
        }
        when (aquaticSalinityTolerance) {
            AquaticSalinityTolerance.SALTWATER_ONLY ->
                habitatSupport[Habitat.FRESHWATER.ordinal] = 0.0
            AquaticSalinityTolerance.FRESHWATER_ONLY -> {
                habitatSupport[Habitat.COASTAL.ordinal] = 0.0
                habitatSupport[Habitat.SUNLIT_WATER.ordinal] = 0.0
                habitatSupport[Habitat.DARK_WATER.ordinal] = 0.0
            }
            AquaticSalinityTolerance.BROAD -> Unit
        }

        val grazingSupport = strategySupport[EcoStrategy.GRAZING.ordinal]
        val predationSupport = max(
            strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal],
            strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal],
        )
        if (grazingSupport >= GENERALIST_MINIMUM_METHOD_SUPPORT &&
            predationSupport >= GENERALIST_MINIMUM_METHOD_SUPPORT
        ) {
            // Generalism is a derived niche, not a feeding mechanism. The
            // breadth bonus makes it the preferred competition bucket for a
            // genuine omnivore, while its two authored feeding adaptations
            // retain their ordinary costs and interaction behavior.
            strategySupport[EcoStrategy.GENERALIST_FORAGING.ordinal] =
                (max(grazingSupport, predationSupport) + GENERALIST_BREADTH_BONUS)
                    .coerceAtMost(1.0)
        }

        habitatSupport.indices.forEach { habitatSupport[it] = habitatSupport[it].coerceIn(0.0, 1.0) }
        strategySupport.indices.forEach { strategySupport[it] = strategySupport[it].coerceIn(0.0, 1.0) }

        val nicheFit = DoubleArray(niches.size) { nicheIndex ->
            val niche = niches[nicheIndex]
            val habitat = habitatSupport[niche.habitat.ordinal]
            val strategy = strategySupport[niche.strategy.ordinal]
            if (habitat <= 0.0 || strategy <= 0.0) 0.0 else habitat * strategy
        }
        val reproduction = definition.sizeClass.seasonalReproduction * reproductionMultiplier
        val sessilePhotosyntheticMaintenance =
            if (
                !definition.motile &&
                strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] > 0.0
            ) {
                0.30
            } else {
                1.0
            }
        val maintenance =
            definition.sizeClass.typicalMassKg *
                definition.sizeClass.maintenancePerKg *
                max(0.15, 1.0 + maintenanceCost) *
                sessilePhotosyntheticMaintenance
        val compiledMinimumWater = waterRequirement.coerceIn(0.0, 1.0)
        val compiledOptimalMaximumWater =
            optimalMaximumWater.coerceIn(compiledMinimumWater, 1.0)
        val compiledMaximumWater =
            maximumWater.coerceIn(compiledOptimalMaximumWater, 1.0)

        return CompiledSpecies(
            index = index,
            id = definition.id,
            displayName = definition.displayName,
            sizeClass = definition.sizeClass,
            motile = definition.motile,
            kind = definition.kind,
            massKg = definition.sizeClass.typicalMassKg,
            maintenanceDemand = maintenance,
            seasonalReproduction = reproduction,
            temperatureOuterLow = 0.0 + temperatureShift - colderTolerance,
            temperatureOptimalLow = 15.0 + temperatureShift - colderOptimalTolerance,
            temperatureOptimalHigh = 25.0 + temperatureShift + hotterOptimalTolerance,
            temperatureOuterHigh = 40.0 + temperatureShift + hotterTolerance,
            seasonalColdToleranceC = seasonalColdTolerance,
            seasonalColdTriggerInsolation = seasonalColdTrigger,
            thermalStrategy = thermalStrategy,
            aquaticSalinityTolerance = aquaticSalinityTolerance,
            minimumWater = compiledMinimumWater,
            optimalMaximumWater = compiledOptimalMaximumWater,
            maximumWater = compiledMaximumWater,
            insolationOptimum = insolationOptimum.coerceIn(0.05, 1.0),
            canopyLightEfficiency = canopyLightEfficiency.coerceIn(0.0, 0.8),
            reserveCapacity = reserveCapacity.coerceIn(0.0, 1.5),
            nicheCompetitionSensitivity = nicheCompetitionSensitivity.coerceIn(0.0, 1.0),
            dormancyKind = dormancyKind,
            dormantSurvival = dormantSurvival,
            dispersalKind = dispersalKind,
            captureAbility = captureAbility.coerceIn(0.05, 1.5),
            defense = defense.coerceIn(0.0, 1.5),
            reefUse = reefUse.coerceIn(0.0, 1.0),
            reefBuilding = reefBuilding.coerceIn(0.0, 0.25),
            wasteFertilization = wasteFertilization.coerceIn(0.0, 1.0),
            pelagicAerialResident = pelagicAerialResident,
            darkWaterAdapted = darkWaterAdapted,
            photosyntheticColor = definition.photosyntheticColor,
            camouflageColor = definition.camouflageColor,
            habitatSupport = habitatSupport,
            strategySupport = strategySupport,
            camouflage = camouflage,
            nicheFit = nicheFit,
            ancestorSpeciesId = definition.ancestorSpeciesId,
        )
    }

    private fun sizeTemperatureTolerance(sizeClass: SizeClass): Double = when (sizeClass) {
        SizeClass.MINUSCULE, SizeClass.TINY, SizeClass.SMALL -> 0.0
        SizeClass.MEDIUM -> 0.5
        SizeClass.LARGE -> 1.5
        SizeClass.HUGE -> 3.0
    }

    private const val GENERALIST_MINIMUM_METHOD_SUPPORT = 0.20
    private const val GENERALIST_BREADTH_BONUS = 0.05

    private fun compileInteractions(
        definitions: List<SpeciesDefinition>,
        species: List<CompiledSpecies>,
    ): InteractionMatrix {
        val count = species.size
        val kinds = ByteArray(count * count)
        val consumerGain = DoubleArray(count * count)
        val targetLoss = DoubleArray(count * count)
        val targetBenefit = DoubleArray(count * count)
        val requiredTargets = ByteArray(count * count)
        val idToIndex = definitions.mapIndexed { index, definition -> definition.id to index }.toMap()

        for (consumer in species) {
            for (target in species) {
                if (consumer.index == target.index) continue
                val sharedHabitat = Habitat.entries.any {
                    consumer.habitatSupport[it.ordinal] > 0.0 &&
                        target.habitatSupport[it.ordinal] > 0.0
                }
                val sharedFilterHabitat = EcoStrategy.FILTER_FEEDING.supportedHabitats.any {
                    consumer.habitatSupport[it.ordinal] > 0.0 &&
                        target.habitatSupport[it.ordinal] > 0.0
                }
                val sharedGrazingHabitat =
                    EcoStrategy.GRAZING.supportedHabitats.any {
                        consumer.habitatSupport[it.ordinal] > 0.0 &&
                            target.habitatSupport[it.ordinal] > 0.0
                    }
                val offset = consumer.index * count + target.index
                val filterSupport =
                    consumer.strategySupport[EcoStrategy.FILTER_FEEDING.ordinal]
                val filterSizeMatch =
                    target.sizeClass == SizeClass.MINUSCULE ||
                        (
                            consumer.sizeClass == SizeClass.HUGE &&
                                target.sizeClass == SizeClass.TINY
                            )
                if (
                    target.motile &&
                    sharedFilterHabitat &&
                    filterSupport > 0.0 &&
                    filterSizeMatch
                ) {
                    val attack =
                        (0.08 * filterSupport * consumer.captureAbility /
                            max(0.25, target.defense)).coerceIn(0.0, 0.30)
                    kinds[offset] = InteractionKind.FILTER_FEEDING.ordinal.toByte()
                    targetLoss[offset] = attack
                    consumerGain[offset] =
                        attack * EcologyBiomass.filterFeedingEfficiency(consumer.sizeClass)
                    continue
                }
                val grazingSupport =
                    consumer.strategySupport[EcoStrategy.GRAZING.ordinal]
                val targetPhotosynthetic =
                    target.strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] > 0.0
                if (
                    !target.motile &&
                    targetPhotosynthetic &&
                    sharedGrazingHabitat &&
                    grazingSupport > 0.0
                ) {
                    val attack =
                        (
                            0.025 *
                                EcologyBiomass.grazingAccessibility(target) *
                                grazingSupport *
                                consumer.captureAbility /
                                max(0.25, target.defense)
                            ).coerceIn(0.0, 0.24)
                    kinds[offset] = InteractionKind.GRAZING.ordinal.toByte()
                    targetLoss[offset] = attack
                    consumerGain[offset] = attack * 0.65
                    continue
                }
                if (!target.motile) continue
                val predatorSupport = max(
                    consumer.strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal],
                    consumer.strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal],
                )
                val targetPredatorSupport = max(
                    target.strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal],
                    target.strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal],
                )
                val sizeRatio = consumer.massKg / target.massKg
                val meaningfulIntraguildPredation =
                    targetPredatorSupport <= 0.0 || sizeRatio >= 4.0
                val sharedAquaticHabitat = EcologyFitness.aquaticHabitats.any {
                    consumer.habitatSupport[it.ordinal] > 0.0 &&
                        target.habitatSupport[it.ordinal] > 0.0
                }
                val aquaticTinyPreyCompatible =
                    sharedAquaticHabitat &&
                        consumer.sizeClass == SizeClass.MEDIUM &&
                        target.sizeClass == SizeClass.TINY &&
                        sizeRatio <= 1_000_000.0
                val sizeCompatible =
                    sizeRatio in 0.25..1_000.0 ||
                        (target.sizeClass == SizeClass.SMALL && sizeRatio <= 10_000.0) ||
                        aquaticTinyPreyCompatible
                if (
                    sharedHabitat &&
                    predatorSupport > 0.0 &&
                    meaningfulIntraguildPredation &&
                    sizeCompatible
                ) {
                    val intraguildAttackMultiplier =
                        if (targetPredatorSupport > 0.0) 0.10 else 1.0
                    val attack = (
                        0.07 *
                            intraguildAttackMultiplier *
                            predatorSupport *
                            consumer.captureAbility /
                            max(0.25, target.defense)
                        ).coerceIn(0.0, 0.25)
                    kinds[offset] = InteractionKind.PREDATION.ordinal.toByte()
                    targetLoss[offset] = attack
                    // This is gross usable intake before maintenance, not net
                    // trophic-level biomass production.
                    consumerGain[offset] = attack * 1.30
                }
            }
        }

        definitions.forEachIndexed { consumerIndex, definition ->
            definition.traits.flatMap { it.relationships }.forEach { relationship ->
                when (relationship) {
                    is RelationshipEffect.SupplementalFood -> {
                        resolveTargets(relationship.target, definitions, idToIndex).forEach { targetIndex ->
                            val offset = consumerIndex * count + targetIndex
                            kinds[offset] = InteractionKind.SUPPLEMENTAL_FEEDING.ordinal.toByte()
                            targetLoss[offset] = relationship.attackRate
                            consumerGain[offset] = relationship.attackRate * relationship.assimilationEfficiency
                        }
                    }
                    is RelationshipEffect.ParasiteOf -> {
                        resolveTargets(relationship.target, definitions, idToIndex).forEach { targetIndex ->
                            val offset = consumerIndex * count + targetIndex
                            kinds[offset] = InteractionKind.PARASITISM.ordinal.toByte()
                            targetLoss[offset] = relationship.drainRate
                            consumerGain[offset] = relationship.drainRate * 0.35
                        }
                    }
                    is RelationshipEffect.BenefitsTargetWhenFeeding -> {
                        resolveTargets(relationship.target, definitions, idToIndex).forEach { targetIndex ->
                            val offset = consumerIndex * count + targetIndex
                            targetBenefit[offset] += relationship.benefitRate
                        }
                    }
                    is RelationshipEffect.RequiresTarget -> {
                        resolveTargets(relationship.target, definitions, idToIndex).forEach { targetIndex ->
                            requiredTargets[consumerIndex * count + targetIndex] = 1
                        }
                    }
                }
            }
        }

        return InteractionMatrix(count, kinds, consumerGain, targetLoss, targetBenefit, requiredTargets)
    }

    private fun resolveTargets(
        selector: SpeciesSelector,
        definitions: List<SpeciesDefinition>,
        idToIndex: Map<String, Int>,
    ): List<Int> = when (selector) {
        is SpeciesSelector.ExactSpecies ->
            listOf(requireNotNull(idToIndex[selector.speciesId]) {
                "Unknown targeted species: ${selector.speciesId}"
            })
        is SpeciesSelector.DescendantsOf -> {
            require(selector.ancestorSpeciesId in idToIndex) {
                "Unknown targeted ancestor: ${selector.ancestorSpeciesId}"
            }
            definitions.indices.filter { index ->
                isDescendantOf(definitions[index], selector.ancestorSpeciesId, definitions, idToIndex)
            }
        }
    }

    private fun isDescendantOf(
        definition: SpeciesDefinition,
        ancestorId: String,
        definitions: List<SpeciesDefinition>,
        idToIndex: Map<String, Int>,
    ): Boolean {
        var cursor = definition.ancestorSpeciesId
        val visited = mutableSetOf<String>()
        while (cursor != null && visited.add(cursor)) {
            if (cursor == ancestorId) return true
            cursor = idToIndex[cursor]?.let { definitions[it].ancestorSpeciesId }
        }
        return false
    }
}
