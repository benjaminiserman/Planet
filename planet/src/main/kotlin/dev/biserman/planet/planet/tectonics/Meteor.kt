package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.tectonics.Geology.getLayerFor
import dev.biserman.planet.planet.tectonics.TectonicGlobals.meteorImpactChance
import dev.biserman.planet.planet.tectonics.TectonicGlobals.minMeteorElevationChange
import dev.biserman.planet.planet.tectonics.TectonicGlobals.maxMeteorElevationChange
import dev.biserman.planet.things.StonePlacementType
import dev.biserman.planet.things.StoneType
import godot.global.GD
import kotlin.math.min

object Meteor {
    fun impactMeteor(planet: Planet) {
        while (planet.random.nextDouble() < meteorImpactChance) {
            val impactTile = planet.planetTiles.values.random(planet.random)

            impactTile.stoneColumn.surface = getLayerFor(impactTile, StonePlacementType.Meteoric)

            val initialElevationChange =
                planet.random.nextDouble(maxMeteorElevationChange - minMeteorElevationChange) + minMeteorElevationChange
            val metersUnderWater = planet.seaLevel - impactTile.elevation
            val elevationChange =
                if (metersUnderWater > 0.0) initialElevationChange - metersUnderWater else initialElevationChange
            if (elevationChange > 0.0) {
                impactTile.elevation -= elevationChange
            }

            val shockMetamorphic = impactTile.stoneColumn.middle.stoneComponent.placementType.metamorphicForm
            if (shockMetamorphic != null) {
                impactTile.stoneColumn.middle = getLayerFor(impactTile, shockMetamorphic)
            }

            planet.lastMeteorImpact = planet.tectonicAge + 1
        }
    }
}