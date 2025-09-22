package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.planet.TectonicGlobals.divergedCrustHeight
import dev.biserman.planet.planet.TectonicGlobals.divergedCrustLerp
import dev.biserman.planet.planet.TectonicGlobals.divergenceCutoff
import dev.biserman.planet.planet.TectonicGlobals.divergencePatchUplift
import dev.biserman.planet.planet.TectonicGlobals.divergenceSearchRadius
import dev.biserman.planet.planet.TectonicGlobals.searchMaxResults
import dev.biserman.planet.planet.TectonicGlobals.tectonicElevationVariogram
import dev.biserman.planet.topology.Tile
import godot.common.util.lerp
import godot.core.Vector3
import kotlin.math.pow

@JsonIdentityInfo(
    generator = ObjectIdGenerators.IntSequenceGenerator::class,
    scope = DivergenceZone::class,
    property = "id"
)
class DivergenceZone(
    val planet: Planet,
    val tileId: Int,
    val strength: Double,
    val divergingPlates: List<TectonicPlate>
) {
    @get:JsonIgnore
    val tile get() = planet.topology.tiles[tileId]

    @get:JsonIgnore
    val ridgePush
        get() = divergingPlates.map { divergingPlate ->
            PointForce(
                tile.position,
                (divergingPlate.region.center - tile.position).normalized() * tile.area * TectonicGlobals.ridgePushStrength
            )
        }

    companion object {
        fun divergeTileOrFillGap(
            planet: Planet,
            tile: Tile,
            newTileMap: MutableMap<Tile, PlanetTile?>,
            movedTiles: Map<PlanetTile, Vector3>
        ): Pair<PlanetTile, DivergenceZone?> {
            // divergence & gap filling
            val newPlanetTile = PlanetTile(planet, tile.id)
            val searchDistance = planet.topology.averageRadius * divergenceSearchRadius
            val nearestOldTiles =
                planet.topology.rTree.nearest(tile.position.toPoint(), searchDistance, searchMaxResults)
                    .map { planet.getTile(it.value()) }

            val neighbors = tile.tiles
                .map { planet.getTile(it) }
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
                krigingElevation + (divergencePatchUplift / planet.topology.averageRadius) * nearestTileDistance,
                divergedCrustHeight,
                divergenceStrength * divergedCrustLerp
            )
            newPlanetTile.springDisplacement = planet.getTile(tile).springDisplacement

            return Pair(
                newPlanetTile,
                if (divergenceStrength >= divergenceCutoff) {
                    DivergenceZone(planet, tile.id, divergenceStrength, nearestOldTiles.mapNotNull { it.tectonicPlate })
                } else {
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