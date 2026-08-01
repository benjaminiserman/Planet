package dev.biserman.planet.planet.ecology

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
    val minimumActiveTemperatureC: Double,
    val frozenDormantSurvival: Double,
    val seasonalColdToleranceC: Double,
    val seasonalColdTriggerInsolation: Double,
    val thermalStrategy: ThermalStrategy?,
    val aquaticSalinityTolerance: AquaticSalinityTolerance,
    val underwaterBreathing: Boolean,
    val prolongedBreathHolding: Boolean,
    val minimumWater: Double,
    val optimalMaximumWater: Double,
    val maximumWater: Double,
    val optimalMaximumWaterDepthM: Double,
    val absoluteMaximumWaterDepthM: Double,
    val elevationToleranceShiftM: Double,
    val snowHydration: Boolean,
    val insolationOptimum: Double,
    val canopyLightEfficiency: Double,
    val denseCanopyForagingPenalty: Double,
    val reserveCapacity: Double,
    val nicheCompetitionSensitivity: Double,
    val dormancyKind: DormancyKind,
    val dormantSurvival: Double,
    val dormantEntryBiomassRetention: Double,
    val dormantReactivationMultiplier: Double,
    val dispersalKind: DispersalKind,
    val captureAbility: Double,
    val pursuitSpeed: Double,
    val defense: Double,
    val aposematicColoration: Boolean,
    val dangerousWarningModel: Boolean,
    val reefUse: Double,
    val reefBuilding: Double,
    val fruitProduction: Double,
    val flowering: Boolean,
    val nectarProduction: Double,
    val pollinationEfficiency: Double,
    val wasteFertilization: Double,
    val producerCompetitionLayer: ProducerCompetitionLayer,
    val pelagicAerialResident: Boolean,
    val darkWaterAdapted: Boolean,
    val requiresAdjacentLand: Boolean,
    val photosyntheticColor: BiologicalColor?,
    val camouflageColor: BiologicalColor?,
    val habitatSupport: DoubleArray,
    val strategySupport: DoubleArray,
    val camouflage: DoubleArray,
    val nicheFit: DoubleArray,
    val ancestorSpeciesId: String?,
)

/**
 * Internal spatial partitioning for photosynthetic competitors. Suspended
 * producers occupy the water column; attached producers occupy substrate.
 * This is compiled from descriptive traits rather than authored by species.
 */
enum class ProducerCompetitionLayer {
    NONE,
    SUSPENDED,
    ATTACHED,
}

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

        val context = SpeciesCompilationContext(
            speciesDisplayName = definition.displayName,
            sizeTemperatureTolerance = sizeTemperatureTolerance(definition.sizeClass),
        )
        definition.traits.forEach(context::apply)

        val habitatSupport = context.habitatSupport
        val strategySupport = context.strategySupport
        val camouflage = context.camouflage
        val temperatureShift = context.temperatureShift
        val colderTolerance = context.colderTolerance
        val hotterTolerance = context.hotterTolerance
        val colderOptimalTolerance = context.colderOptimalTolerance
        val hotterOptimalTolerance = context.hotterOptimalTolerance
        val minimumActiveTemperatureC = context.minimumActiveTemperatureC
        val frozenDormantSurvival = context.frozenDormantSurvival
        val seasonalColdTolerance = context.seasonalColdTolerance
        val seasonalColdTrigger = context.seasonalColdTrigger
        val thermalStrategy = context.thermalStrategy
        val waterRequirement = context.waterRequirement
        val optimalMaximumWater = context.optimalMaximumWater
        val maximumWater = context.maximumWater
        val optimalMaximumWaterDepthM = context.optimalMaximumWaterDepthM
        val absoluteMaximumWaterDepthM = context.absoluteMaximumWaterDepthM
        val elevationToleranceShiftM = context.elevationToleranceShiftM
        val snowHydration = context.snowHydration
        val insolationOptimum = context.insolationOptimum
        val canopyLightEfficiency = context.canopyLightEfficiency
        val denseCanopyForagingPenalty = context.denseCanopyForagingPenalty
        val reserveCapacity = context.reserveCapacity
        val nicheCompetitionSensitivity = context.nicheCompetitionSensitivity
        val dormancyKind = context.dormancyKind
        val dormantSurvival = context.dormantSurvival
        val dormantEntryBiomassRetention = context.dormantEntryBiomassRetention
        val dormantReactivationMultiplier = context.dormantReactivationMultiplier
        val dispersalKind = context.dispersalKind
        val reproductionMultiplier = context.reproductionMultiplier
        val metabolicDemandMultiplier = context.metabolicDemandMultiplier
        val maintenanceCost = context.maintenanceCost
        val captureAbility = context.captureAbility
        val pursuitSpeed = context.pursuitSpeed
        val defense = context.defense
        val aposematicColoration = context.aposematicColoration
        val reefUse = context.reefUse
        val reefBuilding = context.reefBuilding
        val fruitProduction = context.fruitProduction
        val flowering = context.flowering
        val nectarProduction = context.nectarProduction
        val pollinationEfficiency = context.pollinationEfficiency
        val wasteFertilization = context.wasteFertilization
        val pelagicAerialResident = context.pelagicAerialResident
        val darkWaterAdapted = context.darkWaterAdapted
        val freshwaterAdapted = context.freshwaterAdapted
        val broadSalinityTolerance = context.broadSalinityTolerance
        val underwaterBreathing = context.underwaterBreathing
        val prolongedBreathHolding = context.prolongedBreathHolding
        val obligateResidentHabitat = context.obligateResidentHabitat
        val requiresAdjacentLand = context.requiresAdjacentLand

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
        // A river occupies only a small fraction of a 40,000 km² land tile and
        // cannot support populations of the two largest aquatic body classes.
        // Keep this in compilation so suitability, niche selection,
        // interactions, and loaded communities all see the same restriction.
        if (definition.sizeClass.ordinal >= SizeClass.HUGE.ordinal) {
            habitatSupport[Habitat.FRESHWATER.ordinal] = 0.0
        }
        if (!underwaterBreathing && !prolongedBreathHolding) {
            // Coastal and river habitats allow routine access to air or the
            // waterline. Living in the open-ocean water column requires an
            // explicit way to obtain oxygen while submerged.
            habitatSupport[Habitat.SUNLIT_WATER.ordinal] = 0.0
            habitatSupport[Habitat.DARK_WATER.ordinal] = 0.0
        }
        obligateResidentHabitat?.let { requiredHabitat ->
            habitatSupport.indices.forEach { habitatIndex ->
                if (habitatIndex != requiredHabitat.ordinal) {
                    habitatSupport[habitatIndex] = 0.0
                }
            }
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
        if (
            CommonTrait.NEST_PROBING_TONGUE in commonTraits &&
            CommonTrait.DIGGING_CLAWS in commonTraits
        ) {
            // The tongue reaches prey within galleries after the claws breach
            // the nest; neither adaptation alone defines colony raiding.
            strategySupport[EcoStrategy.COLONY_RAIDING.ordinal] = 0.86
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
                metabolicDemandMultiplier *
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
            temperatureOuterLow = 5.0 + temperatureShift - colderTolerance,
            temperatureOptimalLow = 15.0 + temperatureShift - colderOptimalTolerance,
            temperatureOptimalHigh = 25.0 + temperatureShift + hotterOptimalTolerance,
            temperatureOuterHigh = 30.0 + temperatureShift + hotterTolerance,
            minimumActiveTemperatureC = minimumActiveTemperatureC,
            frozenDormantSurvival = frozenDormantSurvival.coerceIn(0.0, 1.0),
            seasonalColdToleranceC = seasonalColdTolerance,
            seasonalColdTriggerInsolation = seasonalColdTrigger,
            thermalStrategy = thermalStrategy,
            aquaticSalinityTolerance = aquaticSalinityTolerance,
            underwaterBreathing = underwaterBreathing,
            prolongedBreathHolding = prolongedBreathHolding,
            minimumWater = compiledMinimumWater,
            optimalMaximumWater = compiledOptimalMaximumWater,
            maximumWater = compiledMaximumWater,
            optimalMaximumWaterDepthM = optimalMaximumWaterDepthM,
            absoluteMaximumWaterDepthM = absoluteMaximumWaterDepthM,
            elevationToleranceShiftM = elevationToleranceShiftM,
            snowHydration = snowHydration,
            insolationOptimum = insolationOptimum.coerceIn(0.05, 1.0),
            canopyLightEfficiency = canopyLightEfficiency.coerceIn(0.0, 0.8),
            denseCanopyForagingPenalty = denseCanopyForagingPenalty.coerceIn(0.0, 1.0),
            reserveCapacity = reserveCapacity.coerceIn(0.0, 1.5),
            nicheCompetitionSensitivity = nicheCompetitionSensitivity.coerceIn(0.0, 1.0),
            dormancyKind = dormancyKind,
            dormantSurvival = dormantSurvival,
            dormantEntryBiomassRetention =
                dormantEntryBiomassRetention.coerceIn(0.0, 1.0),
            dormantReactivationMultiplier = dormantReactivationMultiplier,
            dispersalKind = dispersalKind,
            captureAbility = captureAbility.coerceIn(0.05, 1.5),
            pursuitSpeed = pursuitSpeed.coerceIn(0.0, 1.0),
            defense = defense.coerceIn(0.0, 1.5),
            aposematicColoration = aposematicColoration,
            dangerousWarningModel =
                CommonTrait.VENOMOUS_STINGER in commonTraits ||
                    CommonTrait.TOXIC_SKIN in commonTraits,
            reefUse = reefUse.coerceIn(0.0, 1.0),
            reefBuilding = reefBuilding.coerceIn(0.0, 0.25),
            fruitProduction = fruitProduction.coerceIn(0.0, 0.10),
            flowering = flowering,
            nectarProduction = nectarProduction.coerceIn(0.0, 0.10),
            pollinationEfficiency = pollinationEfficiency.coerceIn(0.0, 1.0),
            wasteFertilization = wasteFertilization.coerceIn(0.0, 1.0),
            producerCompetitionLayer = when {
                strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] <= 0.0 ->
                    ProducerCompetitionLayer.NONE
                CommonTrait.ROOTED_BODY in commonTraits ||
                    CommonTrait.SUBSTRATE_HOLDFAST in commonTraits ->
                    ProducerCompetitionLayer.ATTACHED
                else -> ProducerCompetitionLayer.SUSPENDED
            },
            pelagicAerialResident = pelagicAerialResident,
            darkWaterAdapted = darkWaterAdapted,
            requiresAdjacentLand = requiresAdjacentLand,
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
        SizeClass.COLOSSAL -> 4.0
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
        val obligateFoodConsumers = BooleanArray(count) { consumerIndex ->
            definitions[consumerIndex].traits
                .flatMap { it.relationships }
                .any { it is RelationshipEffect.ObligateFood }
        }

        fun sharesFeedingHabitat(consumer: CompiledSpecies, target: CompiledSpecies): Boolean {
            val directlyShared = Habitat.entries.any {
                consumer.habitatSupport[it.ordinal] > 0.0 &&
                    target.habitatSupport[it.ordinal] > 0.0
            }
            val seaIceMarineInterface =
                (
                    consumer.habitatSupport[Habitat.SEA_ICE.ordinal] > 0.0 &&
                        EcologyFitness.aquaticHabitats.any {
                            target.habitatSupport[it.ordinal] > 0.0
                        }
                    ) ||
                    (
                        target.habitatSupport[Habitat.SEA_ICE.ordinal] > 0.0 &&
                            EcologyFitness.aquaticHabitats.any {
                                consumer.habitatSupport[it.ordinal] > 0.0
                            }
                        )
            return directlyShared || seaIceMarineInterface
        }

        fun sizeCompatiblePredation(consumer: CompiledSpecies, target: CompiledSpecies): Boolean {
            val sizeRatio = consumer.massKg / target.massKg
            val sharedAquaticHabitat = EcologyFitness.aquaticHabitats.any {
                consumer.habitatSupport[it.ordinal] > 0.0 &&
                    target.habitatSupport[it.ordinal] > 0.0
            } || (
                consumer.habitatSupport[Habitat.SEA_ICE.ordinal] > 0.0 &&
                    EcologyFitness.aquaticHabitats.any { target.habitatSupport[it.ordinal] > 0.0 }
                ) || (
                target.habitatSupport[Habitat.SEA_ICE.ordinal] > 0.0 &&
                    EcologyFitness.aquaticHabitats.any { consumer.habitatSupport[it.ordinal] > 0.0 }
                )
            val aquaticTinyPreyCompatible =
                sharedAquaticHabitat &&
                    consumer.sizeClass == SizeClass.MEDIUM &&
                    target.sizeClass == SizeClass.TINY &&
                    sizeRatio <= 1_000_000.0
            return sizeRatio in 0.25..1_000.0 ||
                (target.sizeClass == SizeClass.SMALL && sizeRatio <= 10_000.0) ||
                aquaticTinyPreyCompatible
        }

        // Diet overlap is a compiled property of the potential food web. It
        // distinguishes genuine intraguild competitors from separated trophic
        // tiers such as orca -> seal -> fish.
        val potentialPredation = Array(count) { BooleanArray(count) }
        for (consumer in species) {
            val predatorSupport = max(
                consumer.strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal],
                consumer.strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal],
            )
            if (predatorSupport <= 0.0 || obligateFoodConsumers[consumer.index]) continue
            for (target in species) {
                potentialPredation[consumer.index][target.index] =
                    consumer.index != target.index && target.motile &&
                        sharesFeedingHabitat(consumer, target) &&
                        sizeCompatiblePredation(consumer, target)
            }
        }
        fun dietsOverlap(firstIndex: Int, secondIndex: Int): Boolean =
            species.indices.any { preyIndex ->
                preyIndex != firstIndex && preyIndex != secondIndex &&
                    potentialPredation[firstIndex][preyIndex] &&
                    potentialPredation[secondIndex][preyIndex]
            }

        for (consumer in species) {
            for (target in species) {
                if (consumer.index == target.index) continue
                val directlySharedHabitat = Habitat.entries.any {
                    consumer.habitatSupport[it.ordinal] > 0.0 &&
                        target.habitatSupport[it.ordinal] > 0.0
                }
                val seaIceMarineInterface =
                    (
                        consumer.habitatSupport[Habitat.SEA_ICE.ordinal] > 0.0 &&
                            EcologyFitness.aquaticHabitats.any {
                                target.habitatSupport[it.ordinal] > 0.0
                            }
                        ) ||
                        (
                            target.habitatSupport[Habitat.SEA_ICE.ordinal] > 0.0 &&
                                EcologyFitness.aquaticHabitats.any {
                                    consumer.habitatSupport[it.ordinal] > 0.0
                                }
                            )
                val sharedHabitat = directlySharedHabitat || seaIceMarineInterface
                if (obligateFoodConsumers[consumer.index]) continue
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
                            consumer.sizeClass.ordinal >= SizeClass.HUGE.ordinal &&
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
                val colonyRaidingSupport =
                    consumer.strategySupport[EcoStrategy.COLONY_RAIDING.ordinal]
                val sharedColonyRaidingHabitat =
                    EcoStrategy.COLONY_RAIDING.supportedHabitats.any {
                        consumer.habitatSupport[it.ordinal] > 0.0 &&
                            target.habitatSupport[it.ordinal] > 0.0
                    }
                val targetColonial =
                    CommonTrait.COLONY_LIVING in definitions[target.index].traits
                if (
                    target.motile &&
                    target.sizeClass == SizeClass.MINUSCULE &&
                    targetColonial &&
                    sharedColonyRaidingHabitat &&
                    colonyRaidingSupport > 0.0
                ) {
                    val attack =
                        (
                            0.10 *
                                colonyRaidingSupport *
                                consumer.captureAbility /
                                max(0.25, target.defense)
                            ).coerceIn(0.0, 0.30)
                    kinds[offset] = InteractionKind.PREDATION.ordinal.toByte()
                    targetLoss[offset] = attack
                    consumerGain[offset] = attack * 1.20
                    continue
                }
                if (!target.motile) continue
                val ambushSupport =
                    consumer.strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal]
                val pursuitSupport =
                    consumer.strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal]
                val predatorSupport = max(ambushSupport, pursuitSupport)
                val pursuitInteraction = pursuitSupport > ambushSupport
                val targetPredatorSupport = max(
                    target.strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal],
                    target.strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal],
                )
                val sizeRatio = consumer.massKg / target.massKg
                val meaningfulIntraguildPredation =
                    targetPredatorSupport <= 0.0 || sizeRatio >= 1.5
                val sizeCompatible = sizeCompatiblePredation(consumer, target)
                if (
                    sharedHabitat &&
                    predatorSupport > 0.0 &&
                    meaningfulIntraguildPredation &&
                    sizeCompatible
                ) {
                    val intraguildAttackMultiplier =
                        if (
                            targetPredatorSupport > 0.0 &&
                            sizeRatio < 4.0 &&
                            dietsOverlap(consumer.index, target.index)
                        ) {
                            0.50
                        } else {
                            1.0
                        }
                    val effectiveCapture =
                        consumer.captureAbility +
                            if (pursuitInteraction) consumer.pursuitSpeed else 0.0
                    val effectiveDefense =
                        target.defense +
                            if (pursuitInteraction) target.pursuitSpeed else 0.0
                    val attack = (
                        0.07 *
                            intraguildAttackMultiplier *
                            predatorSupport *
                            effectiveCapture /
                            max(0.25, effectiveDefense)
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
                    is RelationshipEffect.ObligateFood -> {
                        resolveTargets(relationship.target, definitions, idToIndex).forEach { targetIndex ->
                            val target = species[targetIndex]
                            require(
                                !target.motile &&
                                    target.strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] > 0.0,
                            ) {
                                "${definition.displayName} has non-producer obligate food ${target.displayName}"
                            }
                            val offset = consumerIndex * count + targetIndex
                            kinds[offset] = InteractionKind.GRAZING.ordinal.toByte()
                            targetLoss[offset] = relationship.attackRate
                            consumerGain[offset] =
                                relationship.attackRate * relationship.assimilationEfficiency
                            requiredTargets[offset] = 1
                        }
                    }
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
