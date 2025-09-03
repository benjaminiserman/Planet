package dev.biserman.planet.planet

import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverageInverse
import dev.biserman.planet.planet.TectonicGlobals.tectonicElevationVariogram
import dev.biserman.planet.topology.Tile
import godot.common.util.lerp
import godot.core.Vector3
import kotlin.math.pow

class DivergenceZone(val tile: Tile, val strength: Double, val divergingPlates: List<TectonicPlate>) {
    val ridgePush
        get() = divergingPlates.map { divergingPlate ->
            Pair(
                tile.position,
                (divergingPlate.region.center - tile.position).normalized() * tile.area * TectonicGlobals.ridgePushStrength
            )
        }

    @Suppress("MayBeConstant")
    companion object {
        val divergenceStrength = 1.0
        val divergenceCutoff = 0.5
        val divergencePatchUplift = 100.0
        val oceanicRidgeHeight = -2000.0
        fun divergeTileOrFillGap(
            planet: Planet,
            tile: Tile,
            movedTiles: Map<PlanetTile, Vector3>
        ): Pair<PlanetTile, DivergenceZone?> {
            // divergence & gap filling
            val newPlanetTile = PlanetTile(planet, tile)
            val searchDistance = planet.topology.averageRadius * 1.5
            val nearestOldTiles = planet.topology.rTree.nearest(tile.position.toPoint(), searchDistance, 10)
                .map { planet.planetTiles[it.value()]!! }
            val divergenceStrength = nearestOldTiles.map {
                val strength = if (it.isTectonicBoundary) {
                    val averageBorder = it.tectonicBoundaries.map { border -> border.midpoint }.average()
                    val delta = movedTiles[it]!!
                    val borderDelta = tile.position - averageBorder
                    (delta.normalized().dot(borderDelta.normalized()) + 1) / 2.0
                } else 0.0

                Pair(
                    it.tile.position, strength
                )
            }.weightedAverageInverse(tile.position, searchDistance).pow(1 / 2.0) * divergenceStrength

            val krigingElevation = Kriging.interpolate(
                nearestOldTiles.map { it.tile.position to it.elevation },
                tile.position,
                tectonicElevationVariogram
            )

            // divergence elevation
            newPlanetTile.elevation = lerp(
                krigingElevation + divergencePatchUplift, oceanicRidgeHeight, divergenceStrength
            )
            newPlanetTile.springDisplacement = planet.planetTiles[tile]!!.springDisplacement

            return Pair(
                newPlanetTile,
                if (divergenceStrength > divergenceCutoff) {
                    DivergenceZone(tile, divergenceStrength, nearestOldTiles.mapNotNull { it.tectonicPlate })
                } else null
            )
        }
    }
}