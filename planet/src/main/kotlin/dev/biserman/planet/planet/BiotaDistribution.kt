package dev.biserman.planet.planet

import dev.biserman.planet.planet.BiotaDistributionMethod.Companion.available
import dev.biserman.planet.planet.tectonics.TectonicGlobals.biotaDistributionClearChance
import dev.biserman.planet.planet.tectonics.TectonicGlobals.biotaDistributionTerrestrialMaxSlope
import kotlin.math.absoluteValue

interface BiotaDistributionMethod {
    fun isValid(tile: PlanetTile): Boolean
    fun neighborsFor(tile: PlanetTile): List<PlanetTile> = tile.neighbors.filter(::isValid)

    companion object {
        val available = listOf(BiotaDistributionTerrestrial, BiotaDistributionAquatic)
    }
}

object BiotaDistributionTerrestrial : BiotaDistributionMethod {
    override fun isValid(tile: PlanetTile) = tile.isAboveWater
    override fun neighborsFor(tile: PlanetTile): List<PlanetTile> =
        super.neighborsFor(tile)
            .filter { (it.elevation - tile.elevation).absoluteValue < biotaDistributionTerrestrialMaxSlope }
}

object BiotaDistributionAquatic : BiotaDistributionMethod {
    override fun isValid(tile: PlanetTile) = !tile.isAboveWater
}

class BiotaDistribution(val method: BiotaDistributionMethod, val region: PlanetRegion) {
    fun spread() {
        region.tiles.removeAll { !method.isValid(it) }
        if (region.tiles.isEmpty()) {
            region.tiles.add(region.planet.planetTiles.values.filter { method.isValid(it) }.random())
        } else {
            region.tiles.addAll(region.tiles.flatMap { method.neighborsFor(it) })
        }
    }

    companion object {
        fun random(planet: Planet): BiotaDistribution {
            val method = available.random()
            val seedTile = planet.planetTiles.values.filter { method.isValid(it) }.random()
            return BiotaDistribution(method, PlanetRegion(planet, mutableSetOf(seedTile)))
        }

        fun updatePlanet(planet: Planet) {
            planet.biotaDistributions = planet.biotaDistributions.map { distribution ->
                if (planet.random.nextDouble() <= biotaDistributionClearChance) {
                    random(planet)
                } else {
                    distribution.also { it.spread() }
                }
            }
        }
    }
}