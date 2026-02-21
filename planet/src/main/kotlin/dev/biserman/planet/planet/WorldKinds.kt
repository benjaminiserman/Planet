package dev.biserman.planet.planet

import dev.biserman.planet.planet.tectonics.StonePlacement
import dev.biserman.planet.things.MutableComponentSet.Companion.mutableComponentSetOf
import dev.biserman.planet.things.Resource
import dev.biserman.planet.things.Stone
import dev.biserman.planet.things.StoneComponent
import dev.biserman.planet.things.StonePlacementCondition
import dev.biserman.planet.things.StonePlacementType
import dev.biserman.planet.things.StoneType
import godot.core.Color
import godot.global.GD

class WorldKinds {
    var resources = mapOf<Int, Resource>()

    lateinit var stonePlacements: Map<StonePlacementType, StonePlacement>
    lateinit var defaultSurfaceStone: Stone
    lateinit var defaultDeepStone: Stone

    fun generateStoneTypes(planet: Planet) {
        val workingStonePlacements = mutableMapOf<StonePlacementType, StonePlacement>()
        for (placementType in StonePlacementType.entries) {
            val defaultOption = generateStone(planet, placementType, 1)
            val variations = planet.random.nextInt(3)
            val specialOptions = (1..variations).map { generateStone(planet, placementType, it + 1) }
            workingStonePlacements[placementType] = StonePlacement(
                placementType,
                specialOptions.map {
                    StonePlacement.SpecialOption(
                        StonePlacementCondition.conditionBag.grab(planet.random).invoke(planet.random),
                        it
                    )
                },
                defaultOption
            )
        }
        stonePlacements = workingStonePlacements
        defaultSurfaceStone = stonePlacements[StonePlacementType.Primordial]!!.defaultOption
        defaultDeepStone = stonePlacements[StonePlacementType.MetamorphicPrimordial]!!.defaultOption
        GD.print(stonePlacements.values.flatMap {
            listOf(
                it.defaultOption,
                *it.specialOptions.map { specialOption -> specialOption.stone }.toTypedArray()
            )
        })
    }

    fun generateStone(planet: Planet, placementType: StonePlacementType, ordinal: Int): Stone {
        return Stone(
            StoneComponent(
                placementType.name + " $ordinal",
                planet.random.nextDouble(-1.0, 1.0),
                planet.random.nextDouble(-1.0, 1.0) + if (placementType.stoneType == StoneType.Igneous) 0.5 else 0.0,
                planet.random.nextDouble(0.8, 1.2) + if (placementType.stoneType == StoneType.Sedimentary) 0.2 else 0.0,
                placementType
            ),
            mutableComponentSetOf(),
            listOf(
                Color.fromHsv(
                    planet.random.nextDouble(0.0, 1.0),
                    planet.random.nextDouble(0.05, 0.25),
                    planet.random.nextDouble(0.1, 0.9),
                    1.0
                )
            ),
            placementType.concepts,
        )
    }
}
