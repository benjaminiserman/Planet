package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.planet.NoiseMaps
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.things.Concept
import dev.biserman.planet.things.Stone
import dev.biserman.planet.utils.WeightedBag
import dev.biserman.planet.utils.weightedBagOf
import kotlin.random.Random

enum class StonePlacementType(val stoneType: StoneType, val concepts: List<Concept> = emptyList()) {
    AlluvialDeposition(StoneType.Sedimentary, listOf(Concept.RIVER)),
    OceanicDeposition(StoneType.Sedimentary, listOf(Concept.OCEAN)),
    OrogenicFoldingSedimentary(StoneType.Metamorphic, listOf(Concept.MOUNTAIN)),
    OrogenicFoldingIgneous(StoneType.Metamorphic, listOf(Concept.MOUNTAIN)),
    DeepPressureSedimentary(StoneType.Metamorphic, listOf(Concept.DEEP)),
    DeepPressureIgneous(StoneType.Metamorphic, listOf(Concept.DEEP)),
    Rifting(StoneType.Igneous, listOf(Concept.FIRE)),
    Hotspot(StoneType.Igneous, listOf(Concept.MAGMA)),
    Primordial(StoneType.Igneous, listOf(Concept.ANCIENT)),
    Meteoric(StoneType.Meteoric, listOf(Concept.COMET));
}

data class StonePlacement(
    val type: StonePlacementType,
    val specialOptions: List<Pair<Stone, StonePlacementCondition>>,
    val defaultOption: Stone
)

enum class StoneType {
    Sedimentary,
    Metamorphic,
    Igneous,
    Meteoric
}

interface StonePlacementCondition {
    fun canPlace(planetTile: PlanetTile): Boolean

    class MantleConvectionMagnitudeAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) =
            planetTile.planet.noise.mantleConvection.sampleAt(planetTile).length() > threshold
    }

    class MantleConvectionMagnitudeBelow(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) =
            planetTile.planet.noise.mantleConvection.sampleAt(planetTile).length() < threshold
    }

    class EssenceAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) =
            planetTile.planet.noise.essence.sample3d(planetTile.tile.position) < threshold
    }

    class LocalHotspotActivityAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile): Boolean =
            planetTile.planet.noise.hotspots.sampleAt(planetTile) > threshold
    }

    class GlobalHotspotActivityAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile): Boolean = planetTile.planet.hotspotActivity > threshold
    }

    class GlobalHotspotActivityBelow(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile): Boolean = planetTile.planet.hotspotActivity < threshold
    }

    class WaterCoverageAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.planet.waterCoverage > threshold
    }

    class WaterCoverageBelow(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.planet.waterCoverage < threshold
    }

    class ContinentialityAbove(val threshold: Int) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.continentiality > threshold
    }

    class ContinentialityBelow(val threshold: Int) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.continentiality < threshold
    }

    class ContinentialityAround(val center: Int, val distance: Int): StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.continentiality in (center - distance)..(center + distance)
    }

    companion object {
        val conditionBag = weightedBagOf<(Random) -> StonePlacementCondition>(
            { random: Random -> MantleConvectionMagnitudeAbove(random.nextDouble()) } to 0
        )
    }
}


data class StoneLayer(var stone: Stone, var height: Double)
class StratigraphicColumn(val column: MutableList<StoneLayer>) {
    fun addTop(stone: Stone, height: Double) {
        if (stone == column.first().stone) {
            column[0].height += height
        } else {
            column.add(0, StoneLayer(stone, height))
        }
    }

    fun addBottom(stone: Stone, height: Double) {
        if (stone == column.last().stone) {
            column[column.size - 1].height += height
        } else {
            column.add(StoneLayer(stone, height))
        }
    }

    fun shrinkTo(maxHeight: Double) {
        var cumulativeHeight = 0.0
        for (i in column.indices) {
            cumulativeHeight += column[i].height
            if (cumulativeHeight >= maxHeight) {
                val overshootHeight = cumulativeHeight - maxHeight
                column[i].height -= overshootHeight
                val deleteFrom = if (column[i].height <= 0.0) i else i + 1
                while (deleteFrom <= column.size) {
                    column.removeAt(deleteFrom)
                }
            }
        }
    }

    fun transmute(upperBound: Double, lowerBound: Double, mutationFn: (Stone) -> Stone) {
        var i = 0
        var cumulativeHeight = 0.0
        while (i < column.size) {
            val regionTop = cumulativeHeight
            val regionBottom = cumulativeHeight + column[i].height

            // Check if this layer overlaps with the transmutation region
            if (regionBottom <= upperBound || regionTop >= lowerBound) {
                // Layer is completely outside bounds - skip it
                cumulativeHeight += column[i].height
                i++
                continue
            }

            val layer = column[i]

            // Case 1: Layer is completely within bounds
            if (regionTop >= upperBound && regionBottom <= lowerBound) {
                layer.stone = mutationFn(layer.stone)
                cumulativeHeight += layer.height
                i++
            }
            // Case 2: Layer spans across upper bound
            else if (regionTop < upperBound && regionBottom > upperBound && regionBottom <= lowerBound) {
                val aboveHeight = upperBound - regionTop
                val withinHeight = regionBottom - upperBound

                layer.height = aboveHeight
                column.add(i + 1, StoneLayer(mutationFn(layer.stone), withinHeight))

                cumulativeHeight += aboveHeight
                i++
            }
            // Case 3: Layer spans across lower bound
            else if (regionTop in upperBound..<lowerBound && regionBottom > lowerBound) {
                val withinHeight = lowerBound - regionTop
                val belowHeight = regionBottom - lowerBound

                layer.height = withinHeight
                layer.stone = mutationFn(layer.stone)
                column.add(i + 1, StoneLayer(layer.stone, belowHeight))

                cumulativeHeight += withinHeight
                i++
            }
            // Case 4: Layer spans across both bounds
            else if (regionTop < upperBound && regionBottom > lowerBound) {
                val aboveHeight = upperBound - regionTop
                val withinHeight = lowerBound - upperBound
                val belowHeight = regionBottom - lowerBound

                layer.height = aboveHeight
                column.add(i + 1, StoneLayer(mutationFn(layer.stone), withinHeight))
                column.add(i + 2, StoneLayer(layer.stone, belowHeight))

                cumulativeHeight += aboveHeight
                i++
            }
        }
    }
}