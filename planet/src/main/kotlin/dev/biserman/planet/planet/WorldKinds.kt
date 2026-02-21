package dev.biserman.planet.planet

import dev.biserman.planet.planet.tectonics.StonePlacement
import dev.biserman.planet.things.Resource
import dev.biserman.planet.things.Stone
import dev.biserman.planet.things.StonePlacementType

class WorldKinds {
    var resources = mapOf<Int, Resource>()

    var stonePlacements = mapOf<StonePlacementType, StonePlacement>()
    lateinit var defaultSurfaceStone: Stone
    lateinit var defaultDeepStone: Stone

    fun generateStoneTypes(planet: Planet) {
        for (placementType in StonePlacementType.entries) {
            val variations = planet.random.nextInt(3) + 1
            repeat(variations) {
                val stone = Resource(

                )
            }
        }
    }
}
