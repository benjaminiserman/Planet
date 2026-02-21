package dev.biserman.planet.language.localization.en_us

import dev.biserman.planet.language.KindNamer
import dev.biserman.planet.language.Name
import dev.biserman.planet.language.NamerProvider
import dev.biserman.planet.planet.tectonics.StonePlacement
import dev.biserman.planet.things.Concept
import dev.biserman.planet.things.Kind
import dev.biserman.planet.things.Resource
import dev.biserman.planet.things.ResourceComponent
import dev.biserman.planet.things.Stone
import dev.biserman.planet.things.StonePlacementType
import godot.core.Color
import kotlin.math.roundToInt
import kotlin.random.Random

object EnglishNamerProvider : NamerProvider {
    val random = Random(0)
    val genericStoneNames = listOf("stone", "rock")
    val stoneNamesByType = mapOf(
        StonePlacementType.AlluvialDeposition to listOf("shale", "varve", "wacke"),
        StonePlacementType.OceanicDeposition to listOf("chert", "flint", "marl"),
        StonePlacementType.MetamorphicAlluvial to listOf("schist", "skarn", "slate"),
        StonePlacementType.MetamorphicOceanic to listOf("marble", "gneiss"),
        StonePlacementType.MetamorphicMantle to listOf("gneiss", "horn"),
        StonePlacementType.MetamorphicSubduction to listOf("schist"),
        StonePlacementType.MetamorphicPrimordial to listOf("schist"),
        StonePlacementType.SubductionVolcanic to listOf("gabbro", "tuff", "sidian", "scoria", "cinder", "trap"),
        StonePlacementType.MantleVolcanic to listOf("gabbro", "tuff", "sidian", "scoria", "cinder", "trap"),
        StonePlacementType.Primordial to listOf("core"),
        StonePlacementType.Meteoric to listOf("comet")
    )

    val stoneNamer = KindNamer(Concept.STONE) {
        val stone = (this as? Resource)?.components?.get<Stone>() ?: throw error("Stone component not found")
        val color = hueNames.keys.first { it.contains(colors.first().h.roundToInt()) }
        val hueName = hueNames[color]!!.random()
        val end = (genericStoneNames + stoneNamesByType[stone.placementType]).random()

        Name(hueName + end)
    }

    override val kindNamers: Map<Concept, KindNamer>
        get() = mapOf(Concept.STONE to stoneNamer)

    val redHueNames = listOf("red", "crim", "ruby", "scar", "rouge")
    val hueNames: Map<IntRange, List<String>> = mapOf(
        0..5 to redHueNames,
        5..45 to listOf("orange", "amber", "gold", "fire"),
        45..65 to listOf("yellow", "sun", "jaun"),
        65..160 to listOf("verdi", "emer", "gras"),
        160..255 to listOf("blu", "azur", "moon", "sea"),
        255..300 to listOf("purp", "vio", "indi"),
        300..355 to listOf("pink", "rose", "heart"),
        355..360 to redHueNames
    )
}