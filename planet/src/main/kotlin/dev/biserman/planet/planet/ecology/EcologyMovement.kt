package dev.biserman.planet.planet.ecology

import java.util.Arrays

data class SeasonalRouteDefinition(
    val speciesId: String,
    /**
     * One origin-to-destination array per season. A value of -1 means that the
     * cached route has no destination from that origin in that season.
     */
    val destinationsBySeason: List<IntArray>,
)

class CompiledMovementPlan private constructor(
    val tileCount: Int,
    val seasonCount: Int,
    private val destinations: IntArray,
) {
    fun destination(speciesIndex: Int, seasonIndex: Int, originTile: Int): Int {
        require(originTile in 0 until tileCount)
        val season = Math.floorMod(seasonIndex, seasonCount)
        return destinations[(speciesIndex * seasonCount + season) * tileCount + originTile]
    }

    companion object {
        fun compile(
            ecology: CompiledEcology,
            tileCount: Int,
            seasonCount: Int = 4,
            routes: List<SeasonalRouteDefinition>,
        ): CompiledMovementPlan {
            require(tileCount > 0)
            require(seasonCount > 0)
            val destinations = IntArray(ecology.species.size * seasonCount * tileCount) { -1 }
            routes.forEach { route ->
                val speciesIndex = ecology.speciesIndex(route.speciesId)
                val species = ecology.species[speciesIndex]
                require(species.lifeHistory.dispersalKind.rangeClass >= DispersalKind.SHORT_MIGRATION.rangeClass) {
                    "${species.displayName} has a cached migration route but no migration trait"
                }
                require(route.destinationsBySeason.size == seasonCount)
                route.destinationsBySeason.forEachIndexed { season, seasonDestinations ->
                    require(seasonDestinations.size == tileCount)
                    seasonDestinations.forEachIndexed { origin, destination ->
                        require(destination == -1 || destination in 0 until tileCount)
                        destinations[(speciesIndex * seasonCount + season) * tileCount + origin] = destination
                    }
                }
            }
            return CompiledMovementPlan(tileCount, seasonCount, destinations)
        }
    }
}

/**
 * Reusable primitive transfer storage. Set [maximumTransfers] to at least the
 * maximum number of populations expected to move in one season.
 */
class MovementScratch(maximumTransfers: Int, nicheCount: Int) {
    internal val capacity: Int = maximumTransfers
    internal val originTiles = IntArray(maximumTransfers)
    internal val destinationTiles = IntArray(maximumTransfers)
    internal val speciesIndices = IntArray(maximumTransfers)
    internal val nicheIndices = IntArray(maximumTransfers)
    internal val activeBiomass = DoubleArray(maximumTransfers)
    internal val reserves = DoubleArray(maximumTransfers)
    internal val competitionByNiche = DoubleArray(nicheCount)
    internal var size: Int = 0

    internal fun clear() {
        size = 0
    }
}

object EcologyMovement {
    /**
     * Low-frequency neighboring colonization for ordinary range expansion.
     *
     * This is deliberately separate from authored seasonal migration routes.
     * Transfers are planned from the pre-transfer world and applied together,
     * so tile traversal order cannot create multi-tile movement in one season.
     */
    fun applyRadiation(
        ecology: CompiledEcology,
        communities: Array<TileCommunity>,
        environments: Array<SeasonalCellEnvironment>,
        neighbors: Array<IntArray>,
        seasonIndex: Long,
        planetSeed: Int,
        scratch: MovementScratch,
        config: EcologyRuntimeConfig = EcologyRuntimeConfig(),
        canEstablish: (
            speciesIndex: Int,
            destinationTile: Int,
            nicheIndex: Int,
        ) -> Boolean = { _, _, _ -> true },
    ) {
        require(communities.size == environments.size)
        require(communities.size == neighbors.size)
        scratch.clear()

        for (originTile in communities.indices) {
            val origin = communities[originTile]
            for (populationIndex in 0 until origin.size) {
                val speciesIndex = origin.speciesIndices[populationIndex]
                val species = ecology.species[speciesIndex]
                if (species.kind == SpeciesKind.INVARIANT) continue
                val chance = when (species.lifeHistory.dispersalKind) {
                    DispersalKind.NEIGHBOR ->
                        config.neighborRadiationChancePerSeason
                    DispersalKind.SHORT_MIGRATION,
                    DispersalKind.REGIONAL_MIGRATION,
                    DispersalKind.LONG_MIGRATION ->
                        config.migrationRadiationChancePerSeason
                    DispersalKind.NONE ->
                        config.unassistedRadiationChancePerSeason
                }
                val hash = radiationHash(planetSeed, seasonIndex, originTile, speciesIndex)
                if (hashToUnit(hash) >= chance) continue

                val adjacent = neighbors[originTile]
                if (adjacent.isEmpty()) continue
                val start = Math.floorMod(hash, adjacent.size)
                var destination = -1
                var nicheIndex = -1
                for (offset in adjacent.indices) {
                    val candidate = adjacent[(start + offset) % adjacent.size]
                    val target = communities[candidate]
                    if (target.find(speciesIndex) < 0 && target.size >= target.capacity) continue
                    val candidateNiche = NicheSelection.choose(
                        species,
                        ecology,
                        environments[candidate],
                        scratch.competitionByNiche,
                        minimumRelativeIntrinsicFit =
                        config.minimumRelativeRadiationNicheFit,
                        competitionAffectsSelection = false,
                    )
                    if (candidateNiche < 0) continue
                    if (!canEstablish(speciesIndex, candidate, candidateNiche)) continue
                    val candidateHabitat = ecology.niches[candidateNiche].habitat
                    if (
                        EcologyFitness.combined(
                            species,
                            environments[candidate],
                            candidateHabitat,
                        ) < EcologySuitability.MINIMUM_ACTIVE_FITNESS
                    ) {
                        continue
                    }
                    destination = candidate
                    nicheIndex = candidateNiche
                    break
                }
                if (destination < 0) continue

                val active = origin.activeBiomass[populationIndex]
                val founderFloor = species.physiology.massKg * 4.0
                if (active < founderFloor * 4.0) continue
                val transferFraction = if (species.motile) 0.010 else 0.005
                val transfer = maxOf(active * transferFraction, founderFloor)
                if (transfer >= active * 0.25) continue
                val reserveFraction = transfer / active
                val reserveTransfer = origin.reserves[populationIndex] * reserveFraction

                require(scratch.size < scratch.originTiles.size) {
                    "Movement scratch capacity exceeded"
                }
                val transferIndex = scratch.size++
                scratch.originTiles[transferIndex] = originTile
                scratch.destinationTiles[transferIndex] = destination
                scratch.speciesIndices[transferIndex] = speciesIndex
                scratch.nicheIndices[transferIndex] = nicheIndex
                scratch.activeBiomass[transferIndex] = transfer
                scratch.reserves[transferIndex] = reserveTransfer
            }
        }

        // Remove every founder group before adding any of them. Planning above
        // therefore observes one immutable seasonal snapshot, independent of
        // tile traversal order, and a population cannot radiate through several
        // tiles during one season.
        for (transferIndex in 0 until scratch.size) {
            val origin = communities[scratch.originTiles[transferIndex]]
            val populationIndex = origin.find(scratch.speciesIndices[transferIndex])
            check(populationIndex >= 0)
            origin.activeBiomass[populationIndex] -= scratch.activeBiomass[transferIndex]
            origin.reserves[populationIndex] -= scratch.reserves[transferIndex]
        }

        for (transferIndex in 0 until scratch.size) {
            val destination = communities[scratch.destinationTiles[transferIndex]]
            val speciesIndex = scratch.speciesIndices[transferIndex]
            val existing = destination.find(speciesIndex)
            if (existing >= 0) {
                destination.activeBiomass[existing] += scratch.activeBiomass[transferIndex]
                destination.reserves[existing] += scratch.reserves[transferIndex]
            } else {
                destination.add(
                    speciesIndex = speciesIndex,
                    nicheIndex = scratch.nicheIndices[transferIndex],
                    activeBiomass = scratch.activeBiomass[transferIndex],
                    reserves = scratch.reserves[transferIndex],
                )
            }
        }
    }

    fun applySeason(
        ecology: CompiledEcology,
        runtime: EcologyRuntime,
        communities: Array<TileCommunity>,
        environments: Array<SeasonalCellEnvironment>,
        neighbors: Array<IntArray>,
        seasonIndex: Int,
        movementPlan: CompiledMovementPlan,
        scratch: MovementScratch,
    ) {
        require(communities.size == environments.size)
        require(communities.size == neighbors.size)
        require(communities.size == movementPlan.tileCount)
        scratch.clear()

        for (originTile in communities.indices) {
            val origin = communities[originTile]
            for (populationIndex in 0 until origin.size) {
                val speciesIndex = origin.speciesIndices[populationIndex]
                val species = ecology.species[speciesIndex]
                val destination = when (species.lifeHistory.dispersalKind) {
                    DispersalKind.NONE -> -1
                    DispersalKind.NEIGHBOR -> {
                        val adjacent = neighbors[originTile]
                        if (adjacent.isEmpty()) {
                            -1
                        } else {
                            adjacent[Math.floorMod(speciesIndex + seasonIndex, adjacent.size)]
                        }
                    }
                    else -> movementPlan.destination(speciesIndex, seasonIndex, originTile)
                }
                if (destination < 0 || destination == originTile) continue
                val target = communities[destination]
                if (target.find(speciesIndex) < 0 && target.size >= target.capacity) continue

                Arrays.fill(scratch.competitionByNiche, 0.0)
                for (targetPopulation in 0 until target.size) {
                    scratch.competitionByNiche[target.nicheIndices[targetPopulation]] +=
                        target.activeBiomass[targetPopulation]
                }
                val nicheIndex = NicheSelection.choose(
                    species,
                    ecology,
                    environments[destination],
                    scratch.competitionByNiche,
                )
                if (nicheIndex < 0) continue

                require(scratch.size < scratch.originTiles.size) {
                    "Movement scratch capacity exceeded"
                }
                val transferFraction = when (species.lifeHistory.dispersalKind) {
                    DispersalKind.NEIGHBOR -> 0.025
                    DispersalKind.SHORT_MIGRATION -> 0.08
                    DispersalKind.REGIONAL_MIGRATION -> 0.12
                    DispersalKind.LONG_MIGRATION -> 0.16
                    DispersalKind.NONE -> 0.0
                }
                val transfer = origin.activeBiomass[populationIndex] * transferFraction
                if (transfer <= 0.0) continue
                val reserveTransfer = origin.reserves[populationIndex] * transferFraction
                origin.activeBiomass[populationIndex] -= transfer
                origin.reserves[populationIndex] -= reserveTransfer

                val transferIndex = scratch.size++
                scratch.originTiles[transferIndex] = originTile
                scratch.destinationTiles[transferIndex] = destination
                scratch.speciesIndices[transferIndex] = speciesIndex
                scratch.nicheIndices[transferIndex] = nicheIndex
                scratch.activeBiomass[transferIndex] = transfer
                scratch.reserves[transferIndex] = reserveTransfer
            }
        }

        for (transferIndex in 0 until scratch.size) {
            val destination = communities[scratch.destinationTiles[transferIndex]]
            val speciesIndex = scratch.speciesIndices[transferIndex]
            val existing = destination.find(speciesIndex)
            if (existing >= 0) {
                destination.activeBiomass[existing] += scratch.activeBiomass[transferIndex]
                destination.reserves[existing] += scratch.reserves[transferIndex]
            } else {
                destination.add(
                    speciesIndex = speciesIndex,
                    nicheIndex = scratch.nicheIndices[transferIndex],
                    activeBiomass = scratch.activeBiomass[transferIndex],
                    reserves = scratch.reserves[transferIndex],
                )
            }
        }

        communities.forEach(runtime::finalizeLocalExtinctions)
    }

    private fun radiationHash(
        planetSeed: Int,
        seasonIndex: Long,
        originTile: Int,
        speciesIndex: Int,
    ): Int {
        var value = planetSeed
        value = value * 31 + seasonIndex.hashCode()
        value = value * 31 + originTile
        value = value * 31 + speciesIndex
        value = (value xor (value ushr 16)) * 0x45D9F3B
        value = (value xor (value ushr 16)) * 0x45D9F3B
        return value xor (value ushr 16)
    }

    private fun hashToUnit(hash: Int): Double =
        hash.toUInt().toDouble() / UInt.MAX_VALUE.toDouble()
}