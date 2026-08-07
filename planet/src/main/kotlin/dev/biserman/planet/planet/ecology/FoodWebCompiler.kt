package dev.biserman.planet.planet.ecology

import kotlin.math.max

class RelationshipCompilationContext internal constructor(
    private val consumer: SpeciesDefinition,
    private val compiledSpecies: List<CompiledSpecies>,
    private val resolve: (SpeciesSelector) -> List<Int>,
    private val writer: RelationshipInteractionWriter,
) {
    fun forEachTarget(selector: SpeciesSelector, action: (Int) -> Unit) =
        resolve(selector).forEach(action)

    fun requireProducerTarget(targetIndex: Int) {
        val target = compiledSpecies[targetIndex]
        require(
            !target.motile &&
                target.niche.supportFor(EcoStrategy.PHOTOSYNTHESIS) > 0.0,
        ) {
            "${consumer.displayName} has non-producer obligate food ${target.displayName}"
        }
    }

    fun setInteraction(
        targetIndex: Int,
        kind: InteractionKind,
        targetLossRate: Double,
        consumerGainRate: Double,
        required: Boolean = false,
    ) {
        writer.setInteraction(
            targetIndex,
            kind,
            targetLossRate,
            consumerGainRate,
            required,
        )
    }

    fun addTargetBenefit(targetIndex: Int, benefitRate: Double) {
        writer.addTargetBenefit(targetIndex, benefitRate)
    }

    fun requireTarget(targetIndex: Int) {
        writer.requireTarget(targetIndex)
    }
}

internal interface RelationshipInteractionWriter {
    fun setInteraction(
        targetIndex: Int,
        kind: InteractionKind,
        targetLossRate: Double,
        consumerGainRate: Double,
        required: Boolean,
    )

    fun addTargetBenefit(targetIndex: Int, benefitRate: Double)

    fun requireTarget(targetIndex: Int)
}

private class InteractionMatrixBuilder(
    private val speciesCount: Int,
) {
    private val kinds = ByteArray(speciesCount * speciesCount)
    private val consumerGains = DoubleArray(speciesCount * speciesCount)
    private val targetLosses = DoubleArray(speciesCount * speciesCount)
    private val targetBenefits = DoubleArray(speciesCount * speciesCount)
    private val requiredTargets = ByteArray(speciesCount * speciesCount)

    fun set(
        consumerIndex: Int,
        targetIndex: Int,
        interaction: CompiledInteraction,
    ) {
        val offset = offset(consumerIndex, targetIndex)
        kinds[offset] = interaction.kind.ordinal.toByte()
        consumerGains[offset] = interaction.consumerGainRate
        targetLosses[offset] = interaction.targetLossRate
        targetBenefits[offset] = interaction.targetBenefitRate
        requiredTargets[offset] = if (interaction.targetRequired) 1 else 0
    }

    fun relationshipWriter(consumerIndex: Int): RelationshipInteractionWriter =
        object : RelationshipInteractionWriter {
            override fun setInteraction(
                targetIndex: Int,
                kind: InteractionKind,
                targetLossRate: Double,
                consumerGainRate: Double,
                required: Boolean,
            ) {
                val offset = offset(consumerIndex, targetIndex)
                kinds[offset] = kind.ordinal.toByte()
                consumerGains[offset] = consumerGainRate
                targetLosses[offset] = targetLossRate
                // Authored effects compose: setting a feeding edge must not
                // erase a benefit or requirement authored by another effect.
                if (required) requiredTargets[offset] = 1
            }

            override fun addTargetBenefit(targetIndex: Int, benefitRate: Double) {
                targetBenefits[offset(consumerIndex, targetIndex)] += benefitRate
            }

            override fun requireTarget(targetIndex: Int) {
                requiredTargets[offset(consumerIndex, targetIndex)] = 1
            }
        }

    fun build() = InteractionMatrix(
        speciesCount,
        kinds,
        consumerGains,
        targetLosses,
        targetBenefits,
        requiredTargets,
    )

    private fun offset(consumerIndex: Int, targetIndex: Int): Int =
        consumerIndex * speciesCount + targetIndex
}

private data class SpeciesPair(
    val consumer: CompiledSpecies,
    val target: CompiledSpecies,
    val targetDefinition: SpeciesDefinition,
) {
    val sizeRatio: Double = consumer.physiology.massKg / target.physiology.massKg

    val sharedFeedingHabitat: Boolean =
        directlySharesAnyHabitat() || sharesSeaIceMarineInterface()

    val sharedAquaticHabitat: Boolean =
        directlySharesAny(EcologyFitness.aquaticHabitats) || sharesSeaIceMarineInterface()

    fun sharesHabitatFor(strategy: EcoStrategy): Boolean =
        directlySharesAny(strategy.supportedHabitats)

    private fun directlySharesAnyHabitat(): Boolean =
        directlySharesAny(Habitat.entries)

    private fun directlySharesAny(habitats: Iterable<Habitat>): Boolean =
        habitats.any { habitat ->
            consumer.supports(habitat) && target.supports(habitat)
        }

    private fun sharesSeaIceMarineInterface(): Boolean =
        (
            consumer.supports(Habitat.SEA_ICE) &&
                EcologyFitness.aquaticHabitats.any(target::supports)
            ) ||
            (
                target.supports(Habitat.SEA_ICE) &&
                    EcologyFitness.aquaticHabitats.any(consumer::supports)
                )
}

private class PredationGraph(
    private val potentialPredation: Array<BooleanArray>,
) {
    fun dietsOverlap(firstIndex: Int, secondIndex: Int): Boolean =
        potentialPredation.indices.any { preyIndex ->
            preyIndex != firstIndex &&
                preyIndex != secondIndex &&
                potentialPredation[firstIndex][preyIndex] &&
                potentialPredation[secondIndex][preyIndex]
        }
}

internal object FoodWebCompiler {
    fun compile(
        definitions: List<SpeciesDefinition>,
        species: List<CompiledSpecies>,
    ): InteractionMatrix {
        require(definitions.size == species.size)
        val builder = InteractionMatrixBuilder(species.size)
        val obligateFoodConsumers = obligateFoodConsumers(definitions)
        val predationGraph = buildPredationGraph(
            definitions,
            species,
            obligateFoodConsumers,
        )

        for (consumer in species) {
            if (obligateFoodConsumers[consumer.index]) continue
            for (target in species) {
                if (consumer.index == target.index) continue
                val pair = SpeciesPair(consumer, target, definitions[target.index])
                val interaction =
                    filterFeedingInteraction(pair)
                        ?: grazingInteraction(pair)
                        ?: colonyRaidingInteraction(pair)
                        ?: predationInteraction(pair, predationGraph)
                if (interaction != null) {
                    builder.set(consumer.index, target.index, interaction)
                }
            }
        }

        compileAuthoredRelationships(definitions, species, builder)
        return builder.build()
    }

    private fun filterFeedingInteraction(pair: SpeciesPair): CompiledInteraction? {
        val support = pair.consumer.supportFor(EcoStrategy.FILTER_FEEDING)
        val sizeMatches =
            pair.target.sizeClass == SizeClass.MINUSCULE ||
                (
                    pair.consumer.sizeClass.ordinal >= SizeClass.HUGE.ordinal &&
                        pair.target.sizeClass == SizeClass.TINY
                    )
        if (
            !pair.target.motile ||
            !pair.sharesHabitatFor(EcoStrategy.FILTER_FEEDING) ||
            support <= 0.0 ||
            !sizeMatches
        ) {
            return null
        }

        val attack = (
            0.08 * support * pair.consumer.interactions.captureAbility /
                max(0.25, pair.target.interactions.defense)
            ).coerceIn(0.0, 0.30)
        return CompiledInteraction(
            kind = InteractionKind.FILTER_FEEDING,
            consumerGainRate =
            attack * EcologyBiomass.filterFeedingEfficiency(pair.consumer.sizeClass),
            targetLossRate = attack,
        )
    }

    private fun grazingInteraction(pair: SpeciesPair): CompiledInteraction? {
        val support = pair.consumer.supportFor(EcoStrategy.GRAZING)
        val targetPhotosynthetic =
            pair.target.supportFor(EcoStrategy.PHOTOSYNTHESIS) > 0.0
        if (
            pair.target.motile ||
            !targetPhotosynthetic ||
            !pair.sharesHabitatFor(EcoStrategy.GRAZING) ||
            support <= 0.0
        ) {
            return null
        }

        val attack = (
            0.025 *
                EcologyBiomass.grazingAccessibility(pair.target) *
                support *
                pair.consumer.interactions.captureAbility /
                max(0.25, pair.target.interactions.defense)
            ).coerceIn(0.0, 0.24)
        return CompiledInteraction(
            kind = InteractionKind.GRAZING,
            consumerGainRate = attack * 0.65,
            targetLossRate = attack,
        )
    }

    private fun colonyRaidingInteraction(pair: SpeciesPair): CompiledInteraction? {
        val support = pair.consumer.supportFor(EcoStrategy.COLONY_RAIDING)
        val targetColonial = CommonTrait.COLONY_LIVING in pair.targetDefinition.traits
        if (
            !pair.target.motile ||
            pair.target.sizeClass != SizeClass.MINUSCULE ||
            !targetColonial ||
            !pair.sharesHabitatFor(EcoStrategy.COLONY_RAIDING) ||
            support <= 0.0
        ) {
            return null
        }

        val attack = (
            0.10 * support * pair.consumer.interactions.captureAbility /
                max(0.25, pair.target.interactions.defense)
            ).coerceIn(0.0, 0.30)
        return CompiledInteraction(
            kind = InteractionKind.PREDATION,
            consumerGainRate = attack * 1.20,
            targetLossRate = attack,
        )
    }

    private fun predationInteraction(
        pair: SpeciesPair,
        predationGraph: PredationGraph,
    ): CompiledInteraction? {
        if (!pair.target.motile) return null
        val ambushSupport = pair.consumer.supportFor(EcoStrategy.AMBUSH_PREDATION)
        val pursuitSupport = pair.consumer.supportFor(EcoStrategy.PURSUIT_PREDATION)
        val predatorSupport = max(ambushSupport, pursuitSupport)
        val targetPredatorSupport = max(
            pair.target.supportFor(EcoStrategy.AMBUSH_PREDATION),
            pair.target.supportFor(EcoStrategy.PURSUIT_PREDATION),
        )
        val meaningfulIntraguildPredation =
            targetPredatorSupport <= 0.0 || pair.sizeRatio >= 1.5
        if (
            !pair.sharedFeedingHabitat ||
            predatorSupport <= 0.0 ||
            !meaningfulIntraguildPredation ||
            !sizeCompatiblePredation(pair)
        ) {
            return null
        }

        val intraguildAttackMultiplier =
            if (
                targetPredatorSupport > 0.0 &&
                pair.sizeRatio < 4.0 &&
                predationGraph.dietsOverlap(pair.consumer.index, pair.target.index)
            ) {
                0.50
            } else {
                1.0
            }
        val pursuitInteraction = pursuitSupport > ambushSupport
        val effectiveCapture =
            pair.consumer.interactions.captureAbility +
                if (pursuitInteraction) pair.consumer.interactions.pursuitSpeed else 0.0
        val effectiveDefense =
            pair.target.interactions.defense +
                if (pursuitInteraction) pair.target.interactions.pursuitSpeed else 0.0
        val attack = (
            0.07 *
                intraguildAttackMultiplier *
                predatorSupport *
                effectiveCapture /
                max(0.25, effectiveDefense)
            ).coerceIn(0.0, 0.25)
        return CompiledInteraction(
            kind = InteractionKind.PREDATION,
            // Gross usable intake before maintenance, not net trophic biomass production.
            consumerGainRate = attack * 1.30,
            targetLossRate = attack,
        )
    }

    private fun buildPredationGraph(
        definitions: List<SpeciesDefinition>,
        species: List<CompiledSpecies>,
        obligateFoodConsumers: BooleanArray,
    ): PredationGraph {
        val potentialPredation = Array(species.size) { BooleanArray(species.size) }
        for (consumer in species) {
            val predatorSupport = max(
                consumer.supportFor(EcoStrategy.AMBUSH_PREDATION),
                consumer.supportFor(EcoStrategy.PURSUIT_PREDATION),
            )
            if (predatorSupport <= 0.0 || obligateFoodConsumers[consumer.index]) continue
            for (target in species) {
                val pair = SpeciesPair(consumer, target, definitions[target.index])
                potentialPredation[consumer.index][target.index] =
                    consumer.index != target.index &&
                    target.motile &&
                    pair.sharedFeedingHabitat &&
                    sizeCompatiblePredation(pair)
            }
        }
        return PredationGraph(potentialPredation)
    }

    private fun sizeCompatiblePredation(pair: SpeciesPair): Boolean {
        val aquaticTinyPreyCompatible =
            pair.sharedAquaticHabitat &&
                pair.consumer.sizeClass == SizeClass.MEDIUM &&
                pair.target.sizeClass == SizeClass.TINY &&
                pair.sizeRatio <= 1_000_000.0
        return pair.sizeRatio in 0.25..1_000.0 ||
            (pair.target.sizeClass == SizeClass.SMALL && pair.sizeRatio <= 10_000.0) ||
            aquaticTinyPreyCompatible
    }

    private fun compileAuthoredRelationships(
        definitions: List<SpeciesDefinition>,
        species: List<CompiledSpecies>,
        builder: InteractionMatrixBuilder,
    ) {
        val idToIndex = definitions
            .mapIndexed { index, definition -> definition.id to index }
            .toMap()
        definitions.forEachIndexed { consumerIndex, definition ->
            val writer = builder.relationshipWriter(consumerIndex)
            definition.traits.flatMap { it.relationships }.forEach { relationship ->
                relationship.compile(
                    RelationshipCompilationContext(
                        consumer = definition,
                        compiledSpecies = species,
                        resolve = { selector ->
                            resolveTargets(selector, definitions, idToIndex)
                        },
                        writer = writer,
                    ),
                )
            }
        }
    }

    private fun obligateFoodConsumers(definitions: List<SpeciesDefinition>): BooleanArray =
        BooleanArray(definitions.size) { consumerIndex ->
            definitions[consumerIndex].traits
                .flatMap { it.relationships }
                .any { it is RelationshipEffect.ObligateFood }
        }

    private fun resolveTargets(
        selector: SpeciesSelector,
        definitions: List<SpeciesDefinition>,
        idToIndex: Map<String, Int>,
    ): List<Int> = when (selector) {
        is SpeciesSelector.ExactSpecies ->
            listOf(
                requireNotNull(idToIndex[selector.speciesId]) {
                    "Unknown targeted species: ${selector.speciesId}"
                }
            )
        is SpeciesSelector.DescendantsOf -> {
            require(selector.ancestorSpeciesId in idToIndex) {
                "Unknown targeted ancestor: ${selector.ancestorSpeciesId}"
            }
            definitions.indices.filter { index ->
                isDescendantOf(
                    definitions[index],
                    selector.ancestorSpeciesId,
                    definitions,
                    idToIndex,
                )
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

private fun CompiledSpecies.supports(habitat: Habitat): Boolean =
    niche.supports(habitat)

private fun CompiledSpecies.supportFor(strategy: EcoStrategy): Double =
    niche.supportFor(strategy)