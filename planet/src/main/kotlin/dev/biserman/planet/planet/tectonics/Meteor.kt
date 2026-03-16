package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.tectonics.TectonicGlobals.meteorImpactChance
import dev.biserman.planet.planet.tectonics.TectonicGlobals.minMeteorElevationChange
import dev.biserman.planet.planet.tectonics.TectonicGlobals.maxMeteorElevationChange
import dev.biserman.planet.things.StonePlacementType
import dev.biserman.planet.things.StoneType
import kotlin.math.min

object Meteor {
    fun impactMeteor(planet: Planet) {
        /*
            intake planet
            roll dice
            add impact
                replace surface layer with meteor
                mid-layer metamorphism
                lower total elevation
         */
        if(planet.random.nextDouble() < meteorImpactChance) return

        val epicenter = planet.planetTiles.values.random()
        epicenter.stoneColumn.surface = epicenter.stoneColumn.getLayer(epicenter, StonePlacementType.Meteoric)

        var elevationChange = planet.random.nextDouble(maxMeteorElevationChange - minMeteorElevationChange) + minMeteorElevationChange
        val metersUnderWater = planet.seaLevel - epicenter.elevation
        if(metersUnderWater > 0.0) {
            elevationChange -= metersUnderWater
        }
        if(elevationChange > 0.0) {
            epicenter.elevation -= elevationChange
        }
    }
}