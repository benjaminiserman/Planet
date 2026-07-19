package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.biserman.planet.planet.BiotaDistributionMethod.Companion.available
import dev.biserman.planet.planet.tectonics.TectonicGlobals.biotaDistributionClearChance
import dev.biserman.planet.planet.tectonics.TectonicGlobals.biotaDistributionTerrestrialMaxSlope
import dev.biserman.planet.planet.ecology.EcologyRuntime
import dev.biserman.planet.planet.ecology.TaxonomicOrder
import kotlin.math.absoluteValue
import kotlin.random.Random

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = BiotaDistributionTerrestrial::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = BiotaDistributionTerrestrial::class, name = "terrestrial"),
    JsonSubTypes.Type(value = BiotaDistributionAquatic::class, name = "aquatic")
)
interface BiotaDistributionMethod {
    fun isValid(tile: PlanetTile): Boolean
    fun canSpread(from: PlanetTile, to: PlanetTile): Boolean = isValid(to)
    fun neighborsFor(tile: PlanetTile): List<PlanetTile> = tile.neighbors.filter { canSpread(tile, it) }

    companion object {
        val available = listOf(BiotaDistributionTerrestrial, BiotaDistributionAquatic)
    }
}

object BiotaDistributionTerrestrial : BiotaDistributionMethod {
    override fun isValid(tile: PlanetTile) = tile.isAboveWater
    override fun canSpread(from: PlanetTile, to: PlanetTile) =
        super.canSpread(from, to) &&
                (to.elevation - from.elevation).absoluteValue < biotaDistributionTerrestrialMaxSlope
}

object BiotaDistributionAquatic : BiotaDistributionMethod {
    override fun isValid(tile: PlanetTile) = !tile.isAboveWater
}


class BiotaDistribution(
    val method: BiotaDistributionMethod,
    val region: PlanetRegion,
    // The default keeps saves written before ecology productionization loadable.
    var taxonomicOrder: TaxonomicOrder = EcologyRuntime.ordersCompatibleWith(method).first(),
) {
    fun spread() {
        val planet = region.planet
        val currentTiles = HashSet<PlanetTile>(region.tiles.size)

        for (tile in region.tiles) {
            val currentTile = planet.getTile(tile.tile)
            if (method.isValid(currentTile)) currentTiles.add(currentTile)
        }

        if (currentTiles.isEmpty()) {
            currentTiles.add(randomValidTile(planet, method))
        } else {
            val addedTiles = HashSet<PlanetTile>()
            for (tile in currentTiles) {
                for (neighbor in tile.tile.tiles) {
                    val neighborTile = planet.getTile(neighbor)
                    if (neighborTile !in currentTiles && method.canSpread(tile, neighborTile)) {
                        addedTiles.add(neighborTile)
                    }
                }
            }
            currentTiles.addAll(addedTiles)
        }

        region.tiles.clear()
        region.tiles.addAll(currentTiles)
    }

    companion object {
        private fun randomValidTile(planet: Planet, method: BiotaDistributionMethod): PlanetTile {
            var selectedTile: PlanetTile? = null
            var count = 0
            for (tile in planet.planetTiles.values) {
                if (method.isValid(tile)) {
                    count += 1
                    if (planet.random.nextInt(count) == 0) selectedTile = tile
                }
            }
            return selectedTile ?: error("No valid tile found for biota distribution")
        }

        internal fun randomAvailableOrder(
            method: BiotaDistributionMethod,
            excludedOrders: Set<TaxonomicOrder>,
            random: Random,
        ): TaxonomicOrder = EcologyRuntime.ordersCompatibleWith(method)
            .filterNot { it in excludedOrders }
            .randomOrNull(random)
            ?: error("No unused taxonomic order remains for ${method::class.simpleName}")

        fun random(
            planet: Planet,
            excludedOrders: Set<TaxonomicOrder> = emptySet(),
        ): BiotaDistribution {
            val candidatesByMethod = available.mapNotNull { method ->
                EcologyRuntime.ordersCompatibleWith(method)
                    .filterNot { it in excludedOrders }
                    .takeIf { it.isNotEmpty() }
                    ?.let { method to it }
            }
            val (method, candidateOrders) = candidatesByMethod.randomOrNull(planet.random)
                ?: error("No unused taxonomic order remains for a new biota distribution")
            val order = candidateOrders.random(planet.random)
            return BiotaDistribution(
                method,
                PlanetRegion(planet, mutableSetOf(randomValidTile(planet, method))),
                order,
            )
        }

        fun ensureUniqueOrders(planet: Planet) {
            val usedOrders = mutableSetOf<TaxonomicOrder>()
            planet.biotaDistributions.forEach { distribution ->
                val compatibleOrders = EcologyRuntime.ordersCompatibleWith(distribution.method)
                if (distribution.taxonomicOrder !in compatibleOrders ||
                    !usedOrders.add(distribution.taxonomicOrder)
                ) {
                    distribution.taxonomicOrder = randomAvailableOrder(
                        distribution.method,
                        usedOrders,
                        planet.random,
                    )
                    usedOrders += distribution.taxonomicOrder
                }
            }
        }

        fun updatePlanet(planet: Planet) {
            ensureUniqueOrders(planet)
            val shouldReplace = planet.biotaDistributions.associateWith {
                planet.random.nextDouble() <= biotaDistributionClearChance
            }
            val usedOrders = planet.biotaDistributions.asSequence()
                .filterNot { shouldReplace.getValue(it) }
                .mapTo(mutableSetOf()) { it.taxonomicOrder }

            planet.biotaDistributions = planet.biotaDistributions.map { distribution ->
                if (shouldReplace.getValue(distribution)) {
                    random(planet, usedOrders).also { replacement ->
                        usedOrders += replacement.taxonomicOrder
                    }
                } else {
                    distribution.also { it.spread() }
                }
            }
        }
    }
}
