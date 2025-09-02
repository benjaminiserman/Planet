package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.toRTree
import dev.biserman.planet.geometry.torque
import dev.biserman.planet.geometry.weightedAverageInverse
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.PlanetTile.Companion.floodFillGroupBy
import dev.biserman.planet.planet.TectonicGlobals.edgeForceStrength
import dev.biserman.planet.planet.TectonicGlobals.mantleConvectionStrength
import dev.biserman.planet.planet.TectonicGlobals.maxElevation
import dev.biserman.planet.planet.TectonicGlobals.minElevation
import dev.biserman.planet.planet.TectonicGlobals.minPlateSize
import dev.biserman.planet.planet.TectonicGlobals.plateMergeCutoff
import dev.biserman.planet.planet.TectonicGlobals.plateTorqueScalar
import dev.biserman.planet.planet.TectonicGlobals.riftCutoff
import dev.biserman.planet.planet.TectonicGlobals.tectonicErosion
import dev.biserman.planet.planet.TectonicGlobals.tryHotspotEruption
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.VectorWarpNoise
import godot.common.util.lerp
import kotlin.math.max
import kotlin.math.min

object Tectonics {
    val random by lazy { Main.random }

    fun seedPlates(planet: Planet, plateCount: Int): MutableList<TectonicPlate> {
        val plates = (1..plateCount).map { TectonicPlate(planet) }
        val remainingTiles = planet.planetTiles.values.shuffled(random).toMutableList()

        for (plate in plates) {
            val selectedTile = remainingTiles.first()
            remainingTiles.remove(selectedTile)
            selectedTile.tectonicPlate = plate
        }

        return plates.toMutableList()
    }

    fun voronoiPlates(planet: Planet) {
        val remainingTiles =
            planet.planetTiles.values.filter { it.tectonicPlate == null }.shuffled(random).toMutableList()

        val warpNoise = VectorWarpNoise(random.nextInt(), 0.75f)

        val centerTiles = planet.tectonicPlates.associateBy { it.tiles.first() }
        for (planetTile in remainingTiles) {
            val warpedPosition = warpNoise.warp(planetTile.tile.position, 0.5)
            val closestCenterTile =
                centerTiles.keys.minBy { warpedPosition.distanceTo(warpNoise.warp(it.tile.position, 0.5)) }
            planetTile.tectonicPlate = closestCenterTile.tectonicPlate
        }

        deleteOrphanedPlates(planet)
        patchAllHoles(planet)
    }

    fun deleteOrphanedPlates(planet: Planet) {
        val remainingTiles = planet.planetTiles.values.toMutableSet()
        val visitedTiles = mutableSetOf<PlanetTile>()
        val tileQueue = ArrayDeque(planet.tectonicPlates.map { it.tiles.first() })
        var i = 0
        while (tileQueue.isNotEmpty()) {
            i += 1
            if (i > planet.planetTiles.size * 10) {
                throw IllegalStateException("Something went wrong, too many iterations")
            }

            val currentTile = tileQueue.removeFirst()
            if (visitedTiles.contains(currentTile)) {
                continue
            }
            visitedTiles.add(currentTile)
            remainingTiles.remove(currentTile)

            val currentPlate = currentTile.tectonicPlate!!
            for (neighbor in currentTile.tile.tiles) {
                val neighborTile = planet.planetTiles[neighbor]!!
                if (visitedTiles.contains(neighborTile)) {
                    continue
                }
                if (neighborTile.tectonicPlate == currentPlate) {
                    tileQueue.addLast(neighborTile)
                }
            }
        }

        for (tile in remainingTiles) {
            tile.tectonicPlate = null
        }
    }

    fun patchAllHoles(planet: Planet) {
        val tiles = planet.planetTiles.values.shuffled(random).filter { it.tectonicPlate == null }.toMutableList()
        var i = 0
        while (tiles.any { it.tectonicPlate == null }) {
            i += 1
            if (i > planet.planetTiles.size * 10) {
                throw IllegalStateException("Something went wrong, too many iterations $i with ${tiles.size} tiles remaining to assign")
            }

            for (planetTile in tiles.toList()) {
                val neighborCounts =
                    planetTile.tile.tiles.groupingBy { planet.planetTiles[it]!!.tectonicPlate }.eachCount()

                val bestNeighbor = neighborCounts.filter { it.key != null }.maxByOrNull { it.value }
                if (bestNeighbor != null) {
                    planetTile.tectonicPlate = bestNeighbor.key
                    tiles.remove(planetTile)
                }
            }
        }
    }

    fun assignDensities(planet: Planet) {
        planet.tectonicPlates.withIndex().forEach { (index, plate) ->
            val averageDensity = (plate.tiles.sumOf { it.density } / plate.tiles.size)
            val adjustedDensity = lerp(averageDensity, random.nextDouble(-1.0, 0.5), 0.75)
            plate.density = adjustedDensity

            plate.tiles.forEach {
                it.elevation = lerp(it.elevation, adjustedDensity * 1000, 0.33)
            }
        }
    }

    fun stepTectonicPlateForces(planet: Planet) {
        planet.tectonicPlates.forEach { plate ->
            val oldTorqueWithDrag = plate.torque * 0.9
            val mantleConvectionTorque = torque(plate.tiles.map { tile ->
                Pair(
                    tile.tile.position, planet.noise.mantleConvection.sample4d(
                        tile.tile.position, planet.tectonicAge.toDouble()
                    ) * mantleConvectionStrength
                )
            })
            val slabPull = torque(planet.subductionZones.filter { (_, zone) -> plate in zone.subductingPlates }
                .flatMap { (_, zone) -> zone.slabPull })
            val ridgePush = torque(planet.divergenceZones.filter { (_, zone) -> plate in zone.divergingPlates }
                .flatMap { (_, zone) -> zone.ridgePush })
            val estimatedEdgeForces = torque(plate.tiles.map { tile ->
                Pair(
                    tile.tile.position, tile.plateBoundaryForces * tile.tile.area * edgeForceStrength
                )
            })

//            GD.print("including: ${mantleConvectionTorque.length()} ${slabPull.length()} ${ridgePush.length()}")

            plate.torque =
                oldTorqueWithDrag + (mantleConvectionTorque + slabPull + ridgePush + estimatedEdgeForces) * plateTorqueScalar
        }

        planet.planetTiles.values.forEach {
            it.updateMovement()
        }
    }

    fun movePlanetTiles(planet: Planet) {
        val movedTiles =
            planet.planetTiles.values.filter { it.tectonicPlate != null }.sortedByDescending { it.elevation }
                .associateWith { tile -> tile.tile.position + tile.movement }

        val subductionZones = mutableMapOf<Tile, SubductionZone>()
        val newTileMap = planet.planetTiles.mapValues { null as PlanetTile? }.toMutableMap()

        val movedTilesRTree = movedTiles.entries.map {
            object {
                val tile = it.key
                val newPosition = it.value
            }
        }.toRTree { movedTiles[it.tile]!!.toPoint() to it }
        val divergenceZones = mutableMapOf<Tile, DivergenceZone>()
        for ((tile, assignedPlanetTile) in newTileMap) {
            if (assignedPlanetTile != null) {
                assignedPlanetTile.tile = tile
            } else {
                val searchRadius = planet.topology.averageRadius
                val nearestMovedTiles = movedTilesRTree.nearest(tile.position.toPoint(), searchRadius, 3).toList()
                if (nearestMovedTiles.isNotEmpty()) {
                    val nearestMovedTile = nearestMovedTiles.first().value().tile
                    newTileMap[tile] = nearestMovedTile.copy().apply {
                        val groups = nearestMovedTiles.map { it.value() }.groupBy { it.tile.tectonicPlate }

                        val overridingPlate = groups.maxBy { (_, plateTiles) -> plateTiles.maxOf { it.tile.elevation } }

                        this.elevation = overridingPlate.value.map { it.tile.elevation }.average()
                        this.tile = tile
                        this.movement += (tile.position - nearestMovedTiles.first().value().newPosition)

                        // subduction
                        if (groups.size > 1) {
                            val subductionStrength = groups.filter { it != overridingPlate }.flatMap { (_, tiles) ->
                                tiles.map { entry ->
                                    val pulledTile = entry.tile
                                    val speedScale = entry.tile.movement.length() / searchRadius
                                    entry.tile.tile.position to speedScale
                                }
                            }.weightedAverageInverse(tile.position, searchRadius)

                            subductionZones[tile] = SubductionZone(
                                tile,
                                subductionStrength,
                                overridingPlate.key!!,
                                groups.filter { it != overridingPlate }.mapNotNull { it.key })
                        }
                    }
                } else {
                    // divergence & gap filling
                    val (newPlanetTile, divergenceZone) = DivergenceZone.divergeTileOrFillGap(planet, tile)
                    newTileMap[tile] = newPlanetTile
                    if (divergenceZone != null) {
                        divergenceZones[tile] = divergenceZone
                    }
                }
            }
        }

        val plateRegions =
            newTileMap.values.filterNotNull().floodFillGroupBy(planetTileFn = { newTileMap[it]!! }) { it.tectonicPlate }
                .toMutableMap().mapValues { (_, value) ->
                    value.map { PlanetRegion(planet, it.toMutableSet()) }.sortedByDescending { it.tiles.size }
                }

        for ((plate, regions) in plateRegions) {
            val regionsToRemap = regions.filter {
                plate == null || it.tiles.size < regions.first().tiles.size || it.tiles.size <= minPlateSize
            }
            for (region in regionsToRemap) {
                val neighbors = region.calculateNeighborLengths(planetTileFn = { newTileMap[it]!! }) {
                    val tectonicPlate = it.tectonicPlate
                    if (tectonicPlate == null || it in plateRegions[tectonicPlate]!!.first().tiles) {
                        tectonicPlate
                    } else {
                        null
                    }
                }

                val edgeLength = neighbors.values.sum()
                val maxNeighbor = neighbors.maxByOrNull { it.value }
                if (maxNeighbor == null || maxNeighbor.value >= edgeLength * plateMergeCutoff || region.tiles.size <= minPlateSize) {
                    region.tiles.forEach {
                        it.tectonicPlate = maxNeighbor?.key
                    }
                } else {
                    region.tiles.forEach {
                        it.tectonicPlate = null
                    }
                }
            }
        }

        val newPlates = newTileMap.values.filterNotNull()
            .floodFillGroupBy(planetTileFn = { newTileMap[it]!! }) { it.tectonicPlate == null }[true] ?: listOf()

        for (plate in newPlates) {
            val newPlate = TectonicPlate(planet, planet.tectonicAge)
            planet.tectonicPlates.add(newPlate)
            plate.forEach { it.tectonicPlate = newPlate }
        }

        val subductionZonesRTree =
            subductionZones.entries.toRTree { (tile, zone) -> tile.position.toPoint() to zone }

        // erosion elevation & subduction pulldown & hotspots
        newTileMap.values.forEach {
            if (it != null) {
                it.elevation -= tectonicErosion(it)
                it.elevation += SubductionZone.adjustElevation(it, subductionZonesRTree)
                it.elevation = tryHotspotEruption(it)
                it.elevation = it.elevation.coerceIn(minElevation..maxElevation)
            }
        }

        planet.subductionZones = subductionZones
        planet.divergenceZones = divergenceZones
        planet.planetTiles = newTileMap.mapValues { it.value!! }.toMap()
        planet.tectonicAge += 1
        planet.tectonicPlates.forEach { it.clean() }
        planet.tectonicPlates.removeIf { it.tiles.isEmpty() }

        val oversizedPlate = planet.tectonicPlates.firstOrNull { it.tiles.size > planet.planetTiles.size * riftCutoff }
        oversizedPlate?.rift()
    }

    fun stepTectonicsSimulation(planet: Planet) {
        movePlanetTiles(planet)
        stepTectonicPlateForces(planet)
        Gui.instance.updateInfobox()
    }
}