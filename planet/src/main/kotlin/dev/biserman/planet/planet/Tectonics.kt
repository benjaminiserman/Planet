package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.sigmoid
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.toRTree
import dev.biserman.planet.geometry.torque
import dev.biserman.planet.geometry.weightedAverageInverse
import dev.biserman.planet.planet.PlanetTile.Companion.floodFillGroupBy
import dev.biserman.planet.planet.Tectonics.movePlanetTiles
import dev.biserman.planet.planet.Tectonics.patchInteriorHoles
import dev.biserman.planet.planet.Tectonics.stepTectonicPlateForces
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.toWeightedBag
import godot.common.util.lerp
import godot.core.Vector2
import godot.core.plus
import godot.global.GD
import kotlin.math.E
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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
            plate.density = adjustedDensity

            plate.tiles.forEach {
                it.elevation = lerp(it.elevation, adjustedDensity * 1000, 0.33f)
            }
        }
    }

    fun stepTectonicPlateForces(planet: Planet) {
        planet.tectonicPlates.forEach { plate ->
            val oldTorqueWithDrag = plate.torque * 0.9
            val mantleConvectionTorque = torque(plate.tiles.map { tile ->
                Pair(
                    tile.tile.position,
                    planet.noise.mantleConvection.sample4d(tile.tile.position, planet.tectonicAge.toDouble()) * 0.0007
                )
            })
            val slabPull = torque(planet.subductionZones.filter { (_, subductingPlate) -> subductingPlate == plate }
                .flatMap { (tile, subductingPlates) ->
                    subductingPlates.map { subductingPlate ->
                        Pair(
                            tile.position,
                            (tile.position - subductingPlate.region.center).normalized() * tile.area * 0.02
                        )
                    }
                })
            val ridgePush = torque(planet.divergenceZones.filter { (_, divergingPlate) -> divergingPlate == plate }
                .flatMap { (tile, divergingPlates) ->
                    divergingPlates.map { divergingPlate ->
                        Pair(
                            tile.position,
                            (divergingPlate.region.center - tile.position).normalized() * tile.area * 0.005
                        )
                    }
                })
            val estimatedEdgeForces = torque(plate.tiles.map { tile ->
                Pair(
                    tile.tile.position,
                    tile.plateBoundaryForces * tile.tile.area * 800
                )
            })

//            GD.print("including: ${mantleConvectionTorque.length()} ${slabPull.length()} ${ridgePush.length()}")

            plate.torque =
                oldTorqueWithDrag + (mantleConvectionTorque + slabPull + ridgePush + estimatedEdgeForces) * 0.1
        }

        planet.planetTiles.values.forEach {
            it.updateMovement()
        }
    }

    fun movePlanetTiles(planet: Planet) {
        val movedTiles = planet.planetTiles.values
            .filter { it.tectonicPlate != null }
            .sortedByDescending { it.elevation }
            .associateWith { tile -> tile.tile.position + tile.movement }

        val subductedTiles = mutableMapOf<Tile, List<TectonicPlate>>()
        val subductPulldown = mutableMapOf<Tile, Double>()
        val newTileMap = planet.planetTiles.mapValues { null as PlanetTile? }.toMutableMap()
//        for ((tile, newPosition) in movedTiles) {
//            val moveDelta = newPosition - tile.tile.position
//            val closestTiles =
//                planet.topology.rTree.nearest(
//                    newPosition.toPoint(),
//                    max(2 * planet.topology.averageRadius, moveDelta.length()),
//                    4
//                ).map {
//                    it.value()
//                }
//            val nearestOther =
//                closestTiles.firstOrNull { newTileMap[it]?.tectonicPlate != tile.tectonicPlate }
//            if (nearestOther != null) {
//                if (newTileMap[nearestOther] != null) {
//                    if (tile.tectonicPlate != null) {
//                        subductedTiles[nearestOther] = tile.tectonicPlate!!
//                    }
//                    // subduction elevation
////                    newTileMap[nearestOther]!!.elevation += 5 * (moveDelta.length() / planet.topology.averageRadius).toFloat()
//                    tile.tectonicPlate = null
//                } else {
//                    newTileMap[nearestOther] = tile.copy().also { it.tile = nearestOther }
//                }
//            } else {
//                tile.tectonicPlate = null
//            }
//        }

        val movedTilesRTree = movedTiles.entries.toRTree { movedTiles[it.key]!!.toPoint() }
        val divergedTiles = mutableMapOf<Tile, List<TectonicPlate>>()
        for ((tile, assignedPlanetTile) in newTileMap) {
            if (assignedPlanetTile != null) {
                assignedPlanetTile.tile = tile
            } else {
                val searchRadius = planet.topology.averageRadius
                val nearestMovedTiles =
                    movedTilesRTree.nearest(tile.position.toPoint(), searchRadius, 3).toList()
                if (nearestMovedTiles.isNotEmpty()) {
                    val nearestMovedTile = nearestMovedTiles.first().value().key
                    val newPosition = nearestMovedTiles.first().value().value
                    val moveDelta = newPosition - nearestMovedTile.tile.position
                    newTileMap[tile] = nearestMovedTile.copy().apply {
                        val groups =
                            nearestMovedTiles
                                .groupBy { it.value().key.tectonicPlate }

                        val overridingPlate = groups
                            .maxBy { (_, plateTiles) -> plateTiles.maxOf { it.value().key.elevation } }

                        this.elevation =
                            overridingPlate
                                .value
                                .map { it.value().key.elevation }
                                .average()
                                .toFloat()
                        this.tile = tile
                        this.movement += (tile.position - nearestMovedTiles.first().value().value)

                        if (groups.size > 1) {
                            subductedTiles[tile] = groups.filter { it != overridingPlate }.keys.filterNotNull().toList()
                            // subduction elevation
                            val speedScale = (moveDelta.length() / planet.topology.averageRadius).toFloat()
                            val elevationDifference = groups
                                .filter { it != overridingPlate }
                                .entries
                                .flatMap { list ->
                                    list.value.map {
                                        it.value().key.tile.position to it.value().key.elevation.adjustRange(
                                            -5000f..5000f,
                                            0f..200f
                                        ).toDouble()
                                    }
                                }
                                .weightedAverageInverse(tile.position, searchRadius)
                                .toFloat()
                            this.elevation += elevationDifference * speedScale

                            // subduction pulldown (or up for convergence)
                            groups.filter { it != overridingPlate }.forEach { (plate, tiles) ->
                                tiles.forEach { entry ->
                                    val pulledTile = entry.value().key
                                    val densityScale =
                                        max(pulledTile.density, this.density).adjustRange(-1f..1f, 0.2f..1f)
                                    val distanceScale =
                                        pulledTile.tile.position.distanceTo(tile.position) / searchRadius
                                    val direction = min(pulledTile.density, this.density)

                                    val current = subductPulldown.computeIfAbsent(pulledTile.tile) { 0.0 }
                                    subductPulldown[pulledTile.tile] =
                                        current + densityScale * distanceScale * direction * 100
                                }
                            }
                        }
                    }
                } else {
                    // divergence
                    val newPlanetTile = PlanetTile(planet, tile)
                    val searchDistance = planet.topology.averageRadius * 1.5
                    val nearestOldTiles =
                        planet.topology.rTree.nearest(tile.position.toPoint(), searchDistance, 10)
                            .map { planet.planetTiles[it.value()]!! }
                    val divergenceStrength =
                        nearestOldTiles.map {
                            Pair(
                                it.tile.position,
                                if (it.isTectonicBoundary) 1.0 else 0.0
                            )
                        }.weightedAverageInverse(tile.position, searchDistance)
                            .pow(1 / 10.0)
                    val averageElevation =
                        nearestOldTiles.map {
                            Pair(
                                it.tile.position,
                                it.elevation.toDouble()
                            )
                        }.weightedAverageInverse(tile.position, searchDistance)
                    // divergence elevation
                    newPlanetTile.elevation =
                        lerp(averageElevation + 100, -2000.0, divergenceStrength).toFloat()
                    newTileMap[tile] = newPlanetTile
                    if (divergenceStrength > 0.25) {
                        divergedTiles[tile] = nearestOldTiles.mapNotNull { it.tectonicPlate }
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
            val regionsToRemap = regions.filter {
                plate == null || it.tiles.size < regions.first().tiles.size || it.tiles.size <= 5
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
                if (maxNeighbor == null || maxNeighbor.value >= edgeLength * 0.33 || region.tiles.size <= 5) {
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

        val newPlates =
            newTileMap.values
                .filterNotNull()
                .floodFillGroupBy(planetTileFn = { newTileMap[it]!! }) { it.tectonicPlate == null }[true]
                ?: listOf()

        for (plate in newPlates) {
            val newPlate = TectonicPlate(planet, planet.tectonicAge)
            planet.tectonicPlates.add(newPlate)
            plate.forEach { it.tectonicPlate = newPlate }
        }

        fun oceanicSubsidence(elevation: Float) =
            100 * (sigmoid(elevation, 0.003f, 5f) - sigmoid(elevation, 0.003f, 10f))

        // erosion elevation & subduction pulldown & hotspots
        newTileMap.values.forEach {
            if (it != null) {
                it.elevation -=
                    oceanicSubsidence(it.elevation) +
                            10 * max(0f, it.elevation * 0.0005f).pow(2)

                it.elevation += subductPulldown.getOrDefault(it.tile, 0.0).toFloat()

                if (random.nextFloat() >= 0.5f) {
                    val hotspot = planet.noise.hotspots.sample4d(it.tile.position, planet.tectonicAge.toDouble())
                        .toFloat() * 50000f
                    it.elevation += hotspot
                }

                it.elevation.coerceIn(-12000f..12000f)
            }
        }

        planet.subductionZones = subductedTiles
        planet.divergenceZones = divergedTiles
        planet.planetTiles = newTileMap.mapValues { it.value!! }.toMap()
        planet.tectonicAge += 1
        planet.tectonicPlates.forEach { it.clean() }
        planet.tectonicPlates.removeIf { it.tiles.isEmpty() }

        val oversizedPlate = planet.tectonicPlates.firstOrNull { it.tiles.size > planet.planetTiles.size * 0.35 }
        oversizedPlate?.rift()
    }

    fun stepTectonicsSimulation(planet: Planet) {
        movePlanetTiles(planet)
        stepTectonicPlateForces(planet)
    }
}