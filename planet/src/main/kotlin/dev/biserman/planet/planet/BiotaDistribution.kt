package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.biserman.planet.planet.BiotaDistributionMethod.Companion.available
import dev.biserman.planet.planet.tectonics.TectonicGlobals.biotaDistributionClearChance
import dev.biserman.planet.planet.tectonics.TectonicGlobals.biotaDistributionTerrestrialMaxSlope
import godot.global.GD
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.measureTime

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


class BiotaDistribution(val method: BiotaDistributionMethod, val region: PlanetRegion) {
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
        private inline fun <T> profileStep(
            timings: MutableList<Pair<String, Duration>>,
            name: String,
            block: () -> T
        ): T {
            var result: T? = null
            timings += name to measureTime { result = block() }
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

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

        fun random(planet: Planet): BiotaDistribution {
            val method = available.random(planet.random)
            return BiotaDistribution(method, PlanetRegion(planet, mutableSetOf(randomValidTile(planet, method))))
        }

        fun updatePlanet(planet: Planet) {
            val timings = mutableListOf<Pair<String, Duration>>()
            var resetCount = 0
            var spreadCount = 0

            planet.biotaDistributions = planet.biotaDistributions.map { distribution ->
                if (planet.random.nextDouble() <= biotaDistributionClearChance) {
                    resetCount += 1
                    profileStep(timings, "reset") { random(planet) }
                } else {
                    spreadCount += 1
                    profileStep(timings, "spread") { distribution.also { it.spread() } }
                }
            }

            GD.print("biota update breakdown: reset=$resetCount spread=$spreadCount")
            timings.groupBy { it.first }
                .mapValues { (_, values) -> values.sumOf { it.second.inWholeMilliseconds } }
                .forEach { (name, time) ->
                    GD.print(" - biota.$name: ${time}ms")
                }
        }
    }
}
