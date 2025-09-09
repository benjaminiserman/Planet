package dev.biserman.planet.planet

import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.scaleAndCoerce01
import dev.biserman.planet.geometry.sigmoid
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverageInverse
import dev.biserman.planet.planet.TectonicGlobals.divergenceSearchRadius
import dev.biserman.planet.planet.TectonicGlobals.searchMaxResults
import dev.biserman.planet.planet.TectonicGlobals.tectonicElevationVariogram
import dev.biserman.planet.topology.Tile
import godot.common.util.lerp
import godot.core.Vector3
import kotlin.collections.average
import kotlin.math.max
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
        val divergenceCutoff = 0.3
        val divergencePatchUplift = -25.0
        val divergedCrustHeight = -2000.0
        val divergedCrustLerp = 0.99
        fun divergeTileOrFillGap(
            planet: Planet,
            tile: Tile,
            newTileMap: MutableMap<Tile, PlanetTile?>,
            movedTiles: Map<PlanetTile, Vector3>
        ): Pair<PlanetTile, DivergenceZone?> {
            // divergence & gap filling
            val newPlanetTile = PlanetTile(planet, tile)
            val searchDistance = planet.topology.averageRadius * divergenceSearchRadius
            val nearestOldTiles =
                planet.topology.rTree.nearest(tile.position.toPoint(), searchDistance, searchMaxResults)
                    .map { planet.planetTiles[it.value()]!! }

            val mostOverlap = tile.tiles.maxOf { neighbor ->
                val neighborPlanetTile = planet.planetTiles[neighbor]
                @Suppress("IfThenToElvis")
                if (neighborPlanetTile != null) {
                    neighborPlanetTile.movement
                        .dot((tile.position - neighborPlanetTile.tile.position).normalized()) / planet.topology.averageRadius
                } else {
                    0.0
                }
            }

            val divergenceStrength = (1 - mostOverlap).coerceIn(0.0, 1.0)

            val krigingElevation = Kriging.interpolate(
                nearestOldTiles.map { it.tile.position to it.elevation },
                tile.position,
                tectonicElevationVariogram
            )

            // divergence elevation
            newPlanetTile.elevation = lerp(
                krigingElevation + divergencePatchUplift, divergedCrustHeight, divergenceStrength * divergedCrustLerp
            )
            newPlanetTile.springDisplacement = planet.planetTiles[tile]!!.springDisplacement

            return Pair(
                newPlanetTile,
                if (divergenceStrength >= divergenceCutoff) {
                    DivergenceZone(tile, divergenceStrength, nearestOldTiles.mapNotNull { it.tectonicPlate })
                } else {
                    @Suppress("SimplifiableCallChain")
                    newPlanetTile.formationTime =
                        tile.tiles.map {
                            newTileMap[it]?.formationTime ?: planet.tectonicAge
                        }.random()
                    null
                }
            )
        }
    }
}