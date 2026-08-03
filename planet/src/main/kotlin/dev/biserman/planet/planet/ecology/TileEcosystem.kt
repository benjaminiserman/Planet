package dev.biserman.planet.planet.ecology

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Stable, human-readable save representation for one local population.
 *
 * Species ids and strongly typed habitat/strategy pairs are saved instead of
 * compiled array indices, so compiled catalogs may be reordered after loading.
 */
data class EcosystemPopulation(
    var speciesId: String,
    var habitat: Habitat,
    var strategy: EcoStrategy,
    var activeBiomassKg: Double,
    var reservesKg: Double = 0.0,
    var dormantBiomassKg: Double = 0.0,
)

/**
 * Serialized ecology state owned by every PlanetTile.
 *
 * The primitive [TileCommunity] cache is deliberately excluded from saves. It
 * is reconstructed lazily and then reused by the global seasonal runtime.
 */
data class TileEcosystem(
    var populations: MutableList<EcosystemPopulation> = mutableListOf(),
    var resources: FunctionalResources = FunctionalResources(),
    var reefCover: Double = 0.0,
) {
    @field:JsonIgnore
    private var cachedCommunity: TileCommunity? = null

    val speciesCount: Int
        get() = populations.size

    val totalBiomassKg: Double
        get() = populations.sumOf { it.activeBiomassKg + it.dormantBiomassKg }

    fun clear() {
        populations.clear()
        resources = FunctionalResources()
        reefCover = 0.0
        cachedCommunity = null
    }

    internal fun community(ecology: CompiledEcology): TileCommunity {
        cachedCommunity?.let { return it }
        val community = TileCommunity(capacity = MAXIMUM_POPULATIONS)
        populations.forEach { population ->
            val speciesIndex = ecology.species.indexOfFirst { it.id == population.speciesId }
            val nicheIndex = ecology.niches.indexOfFirst {
                it.habitat == population.habitat &&
                    it.strategy == population.strategy
            }
            if (
                speciesIndex >= 0 &&
                nicheIndex >= 0 &&
                ecology.species[speciesIndex].niche.fitFor(nicheIndex) > 0.0 &&
                population.activeBiomassKg >= 0.0 &&
                population.reservesKg >= 0.0 &&
                population.dormantBiomassKg >= 0.0
            ) {
                community.add(
                    speciesIndex = speciesIndex,
                    nicheIndex = nicheIndex,
                    activeBiomass = population.activeBiomassKg,
                    reserves = population.reservesKg,
                    dormantBiomass = population.dormantBiomassKg,
                )
            }
        }
        return community.also { cachedCommunity = it }
    }

    internal fun replaceWith(
        community: TileCommunity,
        ecology: CompiledEcology,
    ) {
        repeat(community.size) { populationIndex ->
            val niche = ecology.niches[community.nicheIndices[populationIndex]]
            val population = populations.getOrNull(populationIndex)
                ?: EcosystemPopulation(
                    speciesId = "",
                    habitat = niche.habitat,
                    strategy = niche.strategy,
                    activeBiomassKg = 0.0,
                ).also(populations::add)
            population.speciesId =
                ecology.species[community.speciesIndices[populationIndex]].id
            population.habitat = niche.habitat
            population.strategy = niche.strategy
            population.activeBiomassKg = community.activeBiomass[populationIndex]
            population.reservesKg = community.reserves[populationIndex]
            population.dormantBiomassKg = community.dormantBiomass[populationIndex]
        }
        while (populations.size > community.size) {
            populations.removeAt(populations.lastIndex)
        }
        cachedCommunity = community
    }

    internal fun invalidateRuntimeCache() {
        cachedCommunity = null
    }

    companion object {
        const val MAXIMUM_POPULATIONS = 48
    }
}