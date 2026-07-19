package dev.biserman.planet.planet.ecology

import com.fasterxml.jackson.annotation.JsonIgnore

data class TileEcosystem(
    var biomass: MutableMap<String, Double> = mutableMapOf(),
) {
    @get:JsonIgnore
    @set:JsonIgnore
    var cachedModel: EcosystemModel? = null

    val speciesIds: Set<String> get() = biomass.keys
    val speciesCount: Int get() = biomass.size
    val totalBiomass: Double get() = biomass.values.sum()

    fun clear() {
        biomass.clear()
        cachedModel = null
    }
}
