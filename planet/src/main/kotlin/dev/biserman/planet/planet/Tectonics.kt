package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.toRTree
import dev.biserman.planet.geometry.torque
import dev.biserman.planet.planet.PlanetTile.Companion.floodFillGroupBy
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.toWeightedBag
import godot.common.util.lerp
import godot.global.GD
import kotlin.math.max

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

    fun floodFillPlates(planet: Planet) {
        val plateEdges = planet.tectonicPlates.associateWith { mutableListOf<PlanetTile>() }.toMutableMap()
        val plateBag = planet.tectonicPlates.toWeightedBag(random) { random.nextInt(10, 30) }

        val remainingTiles = planet.planetTiles.values.shuffled(random).toMutableList()

        fun (TectonicPlate).addTile(selectedTile: PlanetTile) {
            selectedTile.tectonicPlate = this
            remainingTiles.remove(selectedTile)
            plateEdges[this]!!.add(selectedTile)
        }

        for (plate in planet.tectonicPlates) {
            val seededTile = plate.tiles.first()
            remainingTiles.remove(seededTile)
            plateEdges[plate]!!.add(seededTile)
        }

        var i = 0
        while (remainingTiles.isNotEmpty()) {
            i += 1
            if (i > planet.planetTiles.size * 10) {
                throw IllegalStateException("Something went wrong, too many iterations")
            }

            val chosenPlate = plateBag.grab() ?: break
            if (plateEdges[chosenPlate].isNullOrEmpty()) {
                continue
            }

            val chosenEdgeTile = plateEdges[chosenPlate]!!.random(random)
            val validNeighbors = chosenEdgeTile.tile.tiles.filter { planet.planetTiles[it]!!.tectonicPlate == null }
            if (validNeighbors.isEmpty()) {
                plateEdges[chosenPlate]!!.remove(chosenEdgeTile)
                continue
            }

            val chosenNeighbor = validNeighbors.random(random)
            chosenPlate.addTile(planet.planetTiles[chosenNeighbor]!!)
        }
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

    fun patchInteriorHoles(planet: Planet) {
        val visited = mutableSetOf<PlanetTile>()

        for (tile in planet.planetTiles.values.filter { it.tectonicPlate == null }) {
            if (visited.contains(tile)) {
                continue
            }

            val region = PlanetRegion(planet, tile.floodFill { it.tectonicPlate == null }.toMutableSet())
            visited.addAll(region.tiles)
            val neighbors = region.border.groupBy { planet.planetTiles[it.oppositeTile(tile.tile)]?.tectonicPlate }
            if (neighbors.count { it.key != null } == 1) {
                val chosenNeighbor = neighbors.entries.first { it.key != null }.key!!
                region.tiles.forEach { tile ->
                    tile.tectonicPlate = chosenNeighbor
                    tile.elevation = tile.tile.tiles.mapNotNull {
                        val planetTile = planet.planetTiles[it]
                        if (planetTile?.tectonicPlate == null) {
                            null
                        } else {
                            planetTile.elevation
                        }
                    }.average().toFloat()
                }
            }
        }
    }

    fun assignDensities(planet: Planet) {
        planet.tectonicPlates.withIndex().forEach { (index, plate) ->
            val averageDensity = (plate.tiles.sumOf { it.density.toDouble() } / plate.tiles.size).toFloat()
            val adjustedDensity = lerp(averageDensity, random.nextDouble(-1.0, 0.5).toFloat(), 0.75f)

            plate.tiles.forEach {
                it.elevation = lerp(it.elevation, adjustedDensity * 1000, 0.33f)
            }
        }
    }

    fun stepTectonicPlateForces(planet: Planet) {
        planet.tectonicPlates.forEach { plate ->
            val oldTorqueWithDrag = plate.torque * plate.basalDrag
            val mantleConvectionTorque = torque(plate.tiles.map { tile ->
                Pair(
                    tile.tile.position,
                    planet.noise.mantleConvection.sample4d(tile.tile.position, planet.tectonicAge.toDouble()) * 0.0003
                )
            })
            val slabPull = torque(planet.subductionZones.filter { (_, subductingPlate) -> subductingPlate == plate }
                .map { (tile, subductingPlate) ->
                    Pair(
                        tile.position,
                        (tile.position - subductingPlate.region.center).normalized() * tile.area * 0.02
                    )
                })
            val ridgePush = torque(planet.divergenceZones.filter { (_, divergingPlate) -> divergingPlate == plate }
                .map { (tile, divergingPlate) ->
                    Pair(
                        tile.position,
                        (divergingPlate.region.center - tile.position).normalized() * tile.area * 0.005
                    )
                })

            GD.print("including: ${mantleConvectionTorque.length()} ${slabPull.length()} ${ridgePush.length()}")

            plate.torque = oldTorqueWithDrag + mantleConvectionTorque + slabPull + ridgePush
        }

        planet.planetTiles.values.forEach {
            it.updateMovement()
        }
    }

    fun movePlanetTiles(planet: Planet) {
        val movedTiles = planet.planetTiles.values
            .filter { it.tectonicPlate != null }
            .sortedByDescending { it.elevation }
            .associate { tile ->
                tile.tile to tile.tile.position + tile.movement
            }

        val subductedTiles = mutableMapOf<Tile, TectonicPlate>()
        val newTileMap = planet.planetTiles.mapValues { null as PlanetTile? }.toMutableMap()
        for ((tile, newPosition) in movedTiles) {
            val planetTile = planet.planetTiles[tile]!!
            val moveDelta = newPosition - tile.position
            val closestTiles =
                planet.topology.rTree.nearest(
                    newPosition.toPoint(),
                    max(2 * planet.topology.averageRadius, moveDelta.length()),
                    4
                ).map {
                    it.value()
                }
            val nearestOther =
                closestTiles.firstOrNull { newTileMap[it]?.tectonicPlate != planetTile.tectonicPlate }
            if (nearestOther != null) {
                if (newTileMap[nearestOther] != null) {
                    if (planetTile.tectonicPlate != null) {
                        subductedTiles[tile] = planetTile.tectonicPlate!!
                    }
                    newTileMap[nearestOther]!!.elevation += max(25f, planetTile.elevation * 0.1f)
                    planet.planetTiles[tile]?.tectonicPlate = null
                } else {
                    newTileMap[nearestOther] = planetTile.copy()
                }
            } else {
                planetTile.tectonicPlate = null
            }
        }

        val movedTilesRTree = movedTiles.keys.toRTree { movedTiles[it]!!.toPoint() }
        val divergedTiles = mutableMapOf<Tile, TectonicPlate>()
        for ((tile, assignedPlanetTile) in newTileMap) {
            if (assignedPlanetTile != null) {
                assignedPlanetTile.tile = tile
            } else {
                val nearestMovedTiles =
                    movedTilesRTree.nearest(tile.position.toPoint(), planet.topology.averageRadius, 1).toList()
                if (nearestMovedTiles.isNotEmpty()) {
                    newTileMap[tile] = planet.planetTiles[tile]!!.copy()
                } else {
                    val planetTile = PlanetTile(planet, tile)
                    planetTile.elevation = -500f
                    newTileMap[tile] = planetTile
                    if (planetTile.tectonicPlate != null) {
                        divergedTiles[tile] = planetTile.tectonicPlate!!
                    }
                }
            }
        }

        val plateRegions = newTileMap.values
            .filterNotNull()
            .floodFillGroupBy(planetTileFn = { newTileMap[it]!! }) { it.tectonicPlate }
            .toMutableMap()
            .mapValues { (_, value) ->
                value.map { PlanetRegion(planet, it.toMutableSet()) }
                    .sortedByDescending { it.tiles.size }
            }

        for ((plate, regions) in plateRegions) {
            val regionsToRemap = if (plate == null) regions else regions.drop(1)
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
                if (edgeLength == 0.0) {
                    continue
                }
                val maxNeighbor = neighbors.maxByOrNull { it.value }
                if (maxNeighbor == null || maxNeighbor.value >= edgeLength * 0.5 || region.tiles.size <= 5) {
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

        planet.subductionZones = subductedTiles
        planet.divergenceZones = divergedTiles
        planet.planetTiles = newTileMap.mapValues { it.value!! }.toMap()
        planet.tectonicAge += 1

        planet.tectonicPlates.forEach { it.clean() }
    }

    fun stepTectonicsSimulation(planet: Planet) {
        movePlanetTiles(planet)
        patchInteriorHoles(planet)
        stepTectonicPlateForces(planet)
    }
}