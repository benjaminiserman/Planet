package dev.biserman.planet.planet

import dev.biserman.planet.planet.tectonics.StonePlacement
import dev.biserman.planet.planet.tectonics.StonePlacementType
import dev.biserman.planet.things.Stone

class WorldKinds {
    var stonePlacements = mapOf<StonePlacementType, StonePlacement>()
    lateinit var defaultSurfaceStone: Stone
    lateinit var defaultDeepStone: Stone
}