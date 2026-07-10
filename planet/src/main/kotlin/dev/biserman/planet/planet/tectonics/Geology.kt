package dev.biserman.planet.planet.tectonics

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.tectonics.Geology.getLayerFor
import dev.biserman.planet.planet.tectonics.TectonicGlobals.accruedDepositThreshold
import dev.biserman.planet.planet.tectonics.TectonicGlobals.accruedErosionThreshold
import dev.biserman.planet.planet.tectonics.TectonicGlobals.depositionContinentialityThreshold
import dev.biserman.planet.planet.tectonics.TectonicGlobals.orogenicMetamorphosisThreshold
import dev.biserman.planet.planet.tectonics.TectonicGlobals.tectonicVolcanismThreshold
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

data class StoneColumn(
    @field:JsonIdentityReference(alwaysAsId = true) var surface: Stone,
    @field:JsonIdentityReference(alwaysAsId = true) var middle: Stone,
    @field:JsonIdentityReference(alwaysAsId = true) var deep: Stone
) {
    fun accreteLayer(planetTile: PlanetTile, stonePlacementType: StonePlacementType) {
        val layer = getLayerFor(planetTile, stonePlacementType)
        deep = middle
        middle = surface
        surface = layer
    }

    fun erodeLayer(planetTile: PlanetTile) {
        surface = middle
        middle = deep
    }

    fun divergeColumn(planetTile: PlanetTile) {
        val divergenceLayer = getLayerFor(planetTile, StonePlacementType.MantleVolcanic)
        surface = divergenceLayer
        middle = divergenceLayer
        deep = divergenceLayer
    }

    fun tryTransmuteDeep(planetTile: PlanetTile) {
        val metamorphicForm = deep.stoneComponent.placementType.metamorphicForm ?: return
        deep = getLayerFor(planetTile, metamorphicForm)
    }

    fun igneousIntrude(planetTile: PlanetTile) {
        val contactMetamorphicForm = middle.stoneComponent.placementType.metamorphicForm
        if (contactMetamorphicForm != null) {
            middle = getLayerFor(planetTile, contactMetamorphicForm)
        }
        deep = getLayerFor(planetTile, StonePlacementType.MantleVolcanic)
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
    fun getLayerFor(planetTile: PlanetTile, stonePlacementType: StonePlacementType): Stone {
        val stonePlacement = planetTile.planet.worldKinds.stonePlacements[stonePlacementType]
            ?: return planetTile.planet.worldKinds.defaultSurfaceStone
        val firstSpecial = stonePlacement.specialOptions.firstOrNull { it.condition.canPlace(planetTile) }
        return firstSpecial?.stone ?: stonePlacement.defaultOption
    }

    fun simulateGeology(planet: Planet) {
        // hotspot accretion is done in tryHotspotEruption
        // divergence volcanism is done on tile creation
        for (tile in planet.planetTiles.values) {
            // alluvial & oceanic deposition
            if (tile.accruedDeposit > accruedDepositThreshold) {
                val layer =
                    if (tile.neighbors.map { it.continentiality }
                            .plus(tile.continentiality)
                            .average() >= depositionContinentialityThreshold
                    ) StonePlacementType.AlluvialDeposition
                    else StonePlacementType.OceanicDeposition
                tile.stoneColumn.accreteLayer(tile, layer)
                tile.accruedDeposit = 0.0
            } else if (tile.accruedDeposit < accruedErosionThreshold) {
                tile.stoneColumn.erodeLayer(tile)
                tile.accruedDeposit = 0.0
            }

            // orogenic metamorphosis
            if ((tile.planet.convergenceZones[tile.tileId]
                    ?.subductionStrengths[tile.tectonicPlate?.id]
                    ?.absoluteValue ?: 0.0) > orogenicMetamorphosisThreshold
            ) {
                tile.stoneColumn.tryTransmuteDeep(tile)
            }

            // tectonic volcanism
            if ((tile.planet.convergenceZones[tile.tileId]
                    ?.subductionStrength ?: 0.0) > tectonicVolcanismThreshold
            ) {
                tile.stoneColumn.accreteLayer(tile, StonePlacementType.SubductionVolcanic)
            }
        }
    }
}
