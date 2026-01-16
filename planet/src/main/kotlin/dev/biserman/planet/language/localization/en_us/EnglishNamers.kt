package dev.biserman.planet.language.localization.en_us

import dev.biserman.planet.language.KindNamer
import dev.biserman.planet.language.NamerProvider
import dev.biserman.planet.planet.tectonics.StonePlacementType.AlluvialDeposition
import dev.biserman.planet.planet.tectonics.StonePlacementType.DeepPressureIgneous
import dev.biserman.planet.planet.tectonics.StonePlacementType.DeepPressureSedimentary
import dev.biserman.planet.planet.tectonics.StonePlacementType.Hotspot
import dev.biserman.planet.planet.tectonics.StonePlacementType.Meteoric
import dev.biserman.planet.planet.tectonics.StonePlacementType.OceanicDeposition
import dev.biserman.planet.planet.tectonics.StonePlacementType.OrogenicFoldingIgneous
import dev.biserman.planet.planet.tectonics.StonePlacementType.OrogenicFoldingSedimentary
import dev.biserman.planet.planet.tectonics.StonePlacementType.Primordial
import dev.biserman.planet.planet.tectonics.StonePlacementType.Rifting
import dev.biserman.planet.planet.tectonics.StoneType
import dev.biserman.planet.things.Concept
import dev.biserman.planet.utils.UtilityExtensions.cartesianProduct

object EnglishNamerProvider : NamerProvider {
    val genericStoneNames = listOf("stone", "rock")
    val stoneNamesByType = mapOf(
        AlluvialDeposition to listOf("shale", "varve", "wacke"),
        OceanicDeposition to listOf("chert", "flint", "marl"),
        OrogenicFoldingSedimentary to listOf("schist", "skarn", "slate"),
        OrogenicFoldingIgneous to listOf("schist"),
        DeepPressureSedimentary to listOf("marble", "gneiss"),
        DeepPressureIgneous to listOf("gneiss", "horn"),
        Rifting to listOf("gabbro"),
        Hotspot to listOf("tuff", "sidian", "scoria", "cinder", "trap"),
        Primordial to listOf("craton"),
        Meteoric to listOf()
    )

    val stoneNamer = KindNamer(Concept.STONE) {
    }

    override val kindNamers: Map<Concept, KindNamer>
        get() = TODO("Not yet implemented")

}