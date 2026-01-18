package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.things.Concept
import dev.biserman.planet.things.Stone

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

enum class StoneType {
    Sedimentary,
    Metamorphic,
    Igneous,
    Meteoric
}

data class StonePlacement(var stone: Stone, var height: Double)
class StratigraphicColumn(val column: MutableList<StonePlacement>) {
    fun addTop(stone: Stone, height: Double) {
        if (stone == column.first().stone) {
            column[0].height += height
        } else {
            column.add(0, StonePlacement(stone, height))
        }
    }

    fun addBottom(stone: Stone, height: Double) {
        if (stone == column.last().stone) {
            column[column.size - 1].height += height
        } else {
            column.add(StonePlacement(stone, height))
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
                column.add(i + 1, StonePlacement(mutationFn(layer.stone), withinHeight))

                cumulativeHeight += aboveHeight
                i++
            }
            // Case 3: Layer spans across lower bound
            else if (regionTop in upperBound..<lowerBound && regionBottom > lowerBound) {
                val withinHeight = lowerBound - regionTop
                val belowHeight = regionBottom - lowerBound

                layer.height = withinHeight
                layer.stone = mutationFn(layer.stone)
                column.add(i + 1, StonePlacement(layer.stone, belowHeight))

                cumulativeHeight += withinHeight
                i++
            }
            // Case 4: Layer spans across both bounds
            else if (regionTop < upperBound && regionBottom > lowerBound) {
                val aboveHeight = upperBound - regionTop
                val withinHeight = lowerBound - upperBound
                val belowHeight = regionBottom - lowerBound

                layer.height = aboveHeight
                column.add(i + 1, StonePlacement(mutationFn(layer.stone), withinHeight))
                column.add(i + 2, StonePlacement(layer.stone, belowHeight))

                cumulativeHeight += aboveHeight
                i++
            }
        }
    }
}