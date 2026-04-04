package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.tectonics.TectonicGlobals.meteorImpactChance
import dev.biserman.planet.planet.tectonics.TectonicGlobals.minMeteorElevationChange
import dev.biserman.planet.planet.tectonics.TectonicGlobals.maxMeteorElevationChange
import dev.biserman.planet.things.StonePlacementType
import dev.biserman.planet.things.StoneType
import godot.global.GD
import kotlin.math.min

object Meteor {
    fun impactMeteor(planet: Planet) {
        if (planet.random.nextDouble() < meteorImpactChance) return

        val impactTile = planet.planetTiles.values.random()

        impactTile.stoneColumn.surface = impactTile.stoneColumn.getLayer(impactTile, StonePlacementType.Meteoric)

        val initialElevationChange = planet.random.nextDouble(maxMeteorElevationChange - minMeteorElevationChange) + minMeteorElevationChange
        val metersUnderWater = planet.seaLevel - impactTile.elevation
        val elevationChange = if (metersUnderWater > 0.0) initialElevationChange - metersUnderWater else initialElevationChange
        if (elevationChange > 0.0) {
            impactTile.elevation -= elevationChange
        }

        val shockMetamorphic = impactTile.stoneColumn.middle.stoneComponent.placementType.metamorphicForm
        if (shockMetamorphic != null) {
            impactTile.stoneColumn.middle = impactTile.stoneColumn.getLayer(impactTile, shockMetamorphic)
        }

        planet.lastMeteorImpact = planet.tectonicAge + 1
    }
}