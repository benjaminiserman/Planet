package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.things.Concept
import dev.biserman.planet.things.Stone

enum class StonePlacementType(val stoneType: StoneType, val concepts: List<Concept> = emptyList()) {
    AlluvialDeposition(StoneType.Sedimentary, listOf(Concept.RIVER)),
    OceanicDeposition(StoneType.Sedimentary, listOf(Concept.OCEAN)),
    OrogenicFoldingSedimentary(StoneType.Metamorphic, listOf(Concept.MOUNTAIN)),
    OrogenicFoldingIgneous(StoneType.Metamorphic, listOf(Concept.MOUNTAIN)),
    DeepPressureSedimentary(StoneType.Metamorphic, listOf(Concept.ABYSSAL)),
    DeepPressureIgneous(StoneType.Metamorphic, listOf(Concept.ABYSSAL)),
    Rifting(StoneType.Igneous, listOf(Concept.FIRE)),
    Hotspot(StoneType.Igneous, listOf(Concept.FIRE)),
    Primordial(StoneType.Igneous, listOf(Concept.ANCIENT)),
    Meteoric(StoneType.Meteoric, listOf(Concept.COMET));
}

enum class StoneType {
    Sedimentary,
    Metamorphic,
    Igneous,
    Meteoric
}

class StratigraphicColumn(column: List<Stone>)