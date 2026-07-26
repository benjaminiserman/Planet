package dev.biserman.planet.planet.ecology.v2

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
                require(species.dispersalKind.rangeClass >= DispersalKind.SHORT_MIGRATION.rangeClass) {
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
                val destination = when (species.dispersalKind) {
                    DispersalKind.NONE -> -1
                    DispersalKind.NEIGHBOR -> {
                        val adjacent = neighbors[originTile]
                        if (adjacent.isEmpty()) -1
                        else adjacent[Math.floorMod(speciesIndex + seasonIndex, adjacent.size)]
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
                val transferFraction = when (species.dispersalKind) {
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
}
