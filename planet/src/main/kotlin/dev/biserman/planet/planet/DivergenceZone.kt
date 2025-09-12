package dev.biserman.planet.planet

import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.average
import dev.biserman.planet.geometry.scaleAndCoerce01
import dev.biserman.planet.geometry.sigmoid
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverageInverse
import dev.biserman.planet.planet.TectonicGlobals.divergedCrustHeight
import dev.biserman.planet.planet.TectonicGlobals.divergedCrustLerp
import dev.biserman.planet.planet.TectonicGlobals.divergenceCutoff
import dev.biserman.planet.planet.TectonicGlobals.divergenceSearchRadius
import dev.biserman.planet.planet.TectonicGlobals.searchMaxResults
import dev.biserman.planet.planet.TectonicGlobals.tectonicElevationVariogram
import dev.biserman.planet.topology.Tile
import godot.common.util.lerp
import godot.core.Vector3
import godot.global.GD
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
        fun divergeTileOrFillGap(
            planet: Planet,
            tile: Tile,
            newTileMap: MutableMap<Tile, PlanetTile?>,
            movedTiles: Map<PlanetTile, Vector3>
        ): Pair<PlanetTile, DivergenceZone?> {
            // divergence & gap filling
            val divergencePatchUplift = -1000 / planet.topology.averageRadius
            val newPlanetTile = PlanetTile(planet, tile)
            val searchDistance = planet.topology.averageRadius * divergenceSearchRadius
            val nearestOldTiles =
                planet.topology.rTree.nearest(tile.position.toPoint(), searchDistance, searchMaxResults)
                    .map { planet.planetTiles[it.value()]!! }

            val neighbors = tile.tiles
                .mapNotNull { planet.planetTiles[it] }
            val mostOverlap =
                if (neighbors.all { it.tectonicPlate == neighbors.first().tectonicPlate }) 1.0
                else neighbors
                    .filter { it.isTectonicBoundary }
                    .maxOfOrNull { neighbor ->
                        neighbor.movement
                            .dot((tile.position - neighbor.tile.position).normalized()) / planet.topology.averageRadius
                    } ?: 1.0

            val divergenceStrength = (1 - mostOverlap).coerceIn(0.0, 1.0).pow(0.33)

            val krigingElevation = Kriging.interpolate(
                nearestOldTiles.map { it.tile.position to it.elevation },
                tile.position,
                tectonicElevationVariogram
            )

            val nearestTileDistance =
                nearestOldTiles.firstOrNull()?.tile?.position?.distanceTo(tile.position) ?: searchDistance
            // divergence elevation
            newPlanetTile.elevation = lerp(
                krigingElevation + divergencePatchUplift * nearestTileDistance,
                divergedCrustHeight,
                divergenceStrength * divergedCrustLerp
            )
            newPlanetTile.springDisplacement = planet.planetTiles[tile]!!.springDisplacement

            return Pair(
                newPlanetTile,
                if (divergenceStrength >= divergenceCutoff) {
                    DivergenceZone(tile, divergenceStrength, nearestOldTiles.mapNotNull { it.tectonicPlate })
                } else {
                    @Suppress("SimplifiableCallChain")
                    newPlanetTile.formationTime =
                        tile.tiles.map { newTileMap[it]?.formationTime }
                            .groupBy { it }
                            .maxByOrNull { it.value.size }?.key ?: planet.tectonicAge
                    null
                }
            )
        }
    }
}