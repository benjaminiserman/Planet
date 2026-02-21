package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.things.Concept
import dev.biserman.planet.things.Stone
import dev.biserman.planet.things.StonePlacementCondition
import dev.biserman.planet.things.StonePlacementType
import dev.biserman.planet.utils.weightedBagOf
import kotlin.math.absoluteValue
import kotlin.random.Random

data class StonePlacement(
    val type: StonePlacementType,
    val specialOptions: List<SpecialOption>,
    val defaultOption: Stone
) {
    data class SpecialOption(val condition: StonePlacementCondition, val stone: Stone)
}


data class StoneColumn(var surface: Stone, var middle: Stone, var deep: Stone) {
    fun getLayer(planetTile: PlanetTile, stonePlacementType: StonePlacementType): Stone {
        val stonePlacement = planetTile.planet.worldKinds.stonePlacements[stonePlacementType]
            ?: return planetTile.planet.worldKinds.defaultSurfaceStone
        val firstSpecial = stonePlacement.specialOptions.firstOrNull { it.condition.canPlace(planetTile) }
        return firstSpecial?.stone ?: stonePlacement.defaultOption
    }

    fun accreteLayer(planetTile: PlanetTile, stonePlacementType: StonePlacementType) {
        val layer = getLayer(planetTile, stonePlacementType)
        deep = middle
        middle = surface
        surface = layer
    }

    fun erodeLayer(planetTile: PlanetTile) {
        surface = middle
        middle = deep
    }

    fun divergeColumn(planetTile: PlanetTile) {
        val divergenceLayer = getLayer(planetTile, StonePlacementType.MantleVolcanic)
        surface = divergenceLayer
        middle = divergenceLayer
        deep = divergenceLayer
    }

    fun tryTransmuteDeep(planetTile: PlanetTile) {
        val metamorphicForm = deep.stoneComponent.placementType.metamorphicForm ?: return
        deep = getLayer(planetTile, metamorphicForm)
    }

    fun igneousIntrude(planetTile: PlanetTile) {
        val contactMetamorphicForm = middle.stoneComponent.placementType.metamorphicForm
        if (contactMetamorphicForm != null) {
            middle = getLayer(planetTile, contactMetamorphicForm)
        }
        deep = getLayer(planetTile, StonePlacementType.MantleVolcanic)
    }

    companion object {
        fun default(planet: Planet) = StoneColumn(
            planet.worldKinds.defaultSurfaceStone,
            planet.worldKinds.defaultSurfaceStone,
            planet.worldKinds.defaultDeepStone
        )
    }
}

object Geology {
    fun simulateGeology(planet: Planet) {
        // hotspot accretion is done in tryHotspotEruption
        // divergence volcanism is done on tile creation
        for (tile in planet.planetTiles.values) {
            // alluvial & oceanic deposition
            if (tile.accruedDeposit > 200.0) {
                val layer =
                    if (tile.isAboveWater) StonePlacementType.AlluvialDeposition
                    else StonePlacementType.OceanicDeposition
                tile.stoneColumn.accreteLayer(tile, layer)
            } else if (tile.accruedDeposit < -2000.0) {
                tile.stoneColumn.erodeLayer(tile)
            }

            // orogenic metamorphosis
            if ((tile.planet.convergenceZones[tile.tileId]
                    ?.subductionStrengths[tile.tileId]
                    ?.absoluteValue ?: 0.0) > 0.1
            ) {
                tile.stoneColumn.tryTransmuteDeep(tile)
            }

            // tectonic volcanism
            if ((tile.planet.convergenceZones[tile.tileId]
                    ?.subductionStrengths[tile.tileId] ?: 0.0) > 0.1
            ) {
                tile.stoneColumn.accreteLayer(tile, StonePlacementType.SubductionVolcanic)
            }
        }
    }
}