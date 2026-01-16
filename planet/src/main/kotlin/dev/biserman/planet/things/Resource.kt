package dev.biserman.planet.things

import dev.biserman.planet.planet.tectonics.StonePlacementType

class Resource(val components: ComponentSet<ResourceComponent>, val concepts: List<Concept>) : Kind() {

}

sealed interface ResourceComponent
class Stone(
    val acidityModifier: Double,
    val fertilityModifier: Double,
    val moistureCapacityMultiplier: Double,
    val placementType: StonePlacementType
) : ResourceComponent {
    val type get() = placementType.stoneType
}

