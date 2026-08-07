package dev.biserman.planet.planet.ecology

/**
 * Completes a proposed community with locally viable obligate dependencies.
 * If none of a consumer's required targets can use the tile, the consumer is
 * removed instead of being initialized into an ecosystem where it cannot feed.
 */
object EcologyAssembly {
    fun completeRequiredTargets(
        ecology: CompiledEcology,
        selected: List<CompiledSpecies>,
        availableTargets: List<CompiledSpecies>,
        targetScore: (CompiledSpecies) -> Double = { 0.0 },
    ): List<CompiledSpecies> {
        val result = LinkedHashMap<Int, CompiledSpecies>()
        selected.forEach { result[it.index] = it }
        val availableByIndex = availableTargets.associateBy { it.index }

        var changed: Boolean
        do {
            changed = false
            result.values.toList().forEach { consumer ->
                val required = requiredTargetIndices(ecology, consumer.index)
                if (required.isEmpty() || required.any(result::containsKey)) {
                    return@forEach
                }
                val target = required
                    .asSequence()
                    .mapNotNull(availableByIndex::get)
                    .maxByOrNull(targetScore)
                if (target == null) {
                    result.remove(consumer.index)
                } else {
                    result[target.index] = target
                }
                changed = true
            }
        } while (changed)

        return result.values.toList()
    }

    fun requiredTargetPresent(
        ecology: CompiledEcology,
        consumerSpeciesIndex: Int,
        community: TileCommunity,
    ): Boolean {
        val required = requiredTargetIndices(ecology, consumerSpeciesIndex)
        return required.isEmpty() ||
            required.any { targetIndex ->
                val populationIndex = community.find(targetIndex)
                populationIndex >= 0 &&
                    community.activeBiomass[populationIndex] +
                    community.dormantBiomass[populationIndex] > 0.0
            }
    }

    private fun requiredTargetIndices(
        ecology: CompiledEcology,
        consumerSpeciesIndex: Int,
    ): List<Int> {
        val offset = consumerSpeciesIndex * ecology.species.size
        return ecology.species.indices.filter { targetIndex ->
            ecology.interactions.targetRequiredAt(offset + targetIndex)
        }
    }
}