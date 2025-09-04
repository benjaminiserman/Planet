package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.component1
import dev.biserman.planet.geometry.component2
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.toRTree
import dev.biserman.planet.geometry.torque
import dev.biserman.planet.geometry.weightedAverageInverse
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.PlanetTile.Companion.floodFillGroupBy
import dev.biserman.planet.planet.TectonicGlobals.continentSpringDamping
import dev.biserman.planet.planet.TectonicGlobals.continentSpringSearchRadius
import dev.biserman.planet.planet.TectonicGlobals.continentSpringStiffness
import dev.biserman.planet.planet.TectonicGlobals.depositStrength
import dev.biserman.planet.planet.TectonicGlobals.edgeForceStrength
import dev.biserman.planet.planet.TectonicGlobals.erosionStrength
import dev.biserman.planet.planet.TectonicGlobals.mantleConvectionStrength
import dev.biserman.planet.planet.TectonicGlobals.maxElevation
import dev.biserman.planet.planet.TectonicGlobals.minElevation
import dev.biserman.planet.planet.TectonicGlobals.minPlateSize
import dev.biserman.planet.planet.TectonicGlobals.oceanicSubsidence
import dev.biserman.planet.planet.TectonicGlobals.plateMergeCutoff
import dev.biserman.planet.planet.TectonicGlobals.plateTorqueScalar
import dev.biserman.planet.planet.TectonicGlobals.riftCutoff
import dev.biserman.planet.planet.TectonicGlobals.searchMaxResults
import dev.biserman.planet.planet.TectonicGlobals.springPlateContributionStrength
import dev.biserman.planet.planet.TectonicGlobals.subductionSearchRadius
import dev.biserman.planet.planet.TectonicGlobals.tectonicElevationVariogram
import dev.biserman.planet.planet.TectonicGlobals.tryHotspotEruption
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.VectorWarpNoise
import godot.common.util.lerp
import godot.core.Vector3
import godot.global.GD
import kotlin.time.measureTime

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
            val springForces = torque(plate.tiles.map { tile ->
                Pair(
                    tile.tile.position,
                    tile.springDisplacement * springPlateContributionStrength
                )
            })

//            GD.print("including: ${mantleConvectionTorque.length()} ${slabPull.length()} ${ridgePush.length()}")

            plate.torque = oldTorqueWithDrag + (
                    mantleConvectionTorque +
                            slabPull +
                            ridgePush +
                            estimatedEdgeForces +
                            springForces
                    ) * plateTorqueScalar
        }

        planet.planetTiles.values.forEach {
            it.updateMovement()
        }
    }

    fun springAndDamp(tiles: Map<PlanetTile, Vector3>): Map<PlanetTile, Vector3> {
        val continentalTilesRTree = tiles.filter { it.key.isContinental }.entries.toRTree { it.value.toPoint() to it }
        val searchRadius = tiles.entries.first().key.planet.topology.averageRadius * continentSpringSearchRadius

        return tiles.mapValues { entry ->
            val (planetTile, newPosition) = entry
            val nearbyContinentalCrust = if (planetTile.isContinental) {
                continentalTilesRTree.nearest(planetTile.tile.position.toPoint(), searchRadius, 6)
                    .toList()
                    .filter { (entry, _) -> entry.key != planetTile }
                    .map { (entry, _) -> entry.key.tile to continentSpringStiffness }
            } else listOf()

            val displacement = nearbyContinentalCrust
                .fold(Vector3.ZERO) { sum, (otherTile, stiffness) ->
                    val restLength = (otherTile.position - planetTile.tile.position).length()
                    val currentDelta = (newPosition - tiles[planetTile.planet.planetTiles[otherTile]!!]!!)
                    sum + currentDelta * (currentDelta.length() - restLength) * -stiffness
                }

            newPosition.lerp(newPosition + displacement, 1 - continentSpringDamping)
        }
    }

    fun springAndDamp(tiles: Map<PlanetTile, Vector3>, steps: Int) =
        (1..steps).fold(tiles) { acc, _ -> springAndDamp(acc) }

    data class MovedTile(val tile: PlanetTile, val newPosition: Vector3)

    fun movePlanetTiles(planet: Planet) {
        val originalMovedTiles = planet.planetTiles.values
            .filter { it.tectonicPlate != null }
            .sortedByDescending { it.elevation }
            .associateWith { tile -> tile.tile.position + tile.movement }
        val movedTiles = springAndDamp(originalMovedTiles, 2)
        planet.planetTiles.forEach { (_, planetTile) ->
            planetTile.springDisplacement = movedTiles[planetTile]!! - originalMovedTiles[planetTile]!!
        }

        val subductionZones = mutableMapOf<Tile, SubductionZone>()
        val newTileMap = planet.planetTiles.mapValues { null as PlanetTile? }.toMutableMap()

        val movedTilesRTree = movedTiles.entries
            .map { MovedTile(it.key, it.value) }
            .toRTree { movedTiles[it.tile]!!.toPoint() to it }
        val divergenceZones = mutableMapOf<Tile, DivergenceZone>()
        for ((tile, _) in newTileMap) {
            val searchRadius = planet.topology.averageRadius
            val nearestMovedTiles = movedTilesRTree.nearest(
                tile.position.toPoint(),
                searchRadius * subductionSearchRadius,
                searchMaxResults
            ).toList()
            val overlappingTiles =
                nearestMovedTiles.filter { it.value().newPosition.distanceTo(tile.position) < searchRadius }
            if (overlappingTiles.isNotEmpty()) {
                val groups = overlappingTiles.map { it.value() }.groupBy { it.tile.tectonicPlate!! }
                val overridingPlate = groups.maxBy { (_, plateTiles) -> plateTiles.maxOf { it.tile.elevation } }
                val nearestMovedTile =
                    overlappingTiles.first { it.value().tile.tectonicPlate == overridingPlate.key }.value().tile
                newTileMap[tile] = nearestMovedTile.copy().apply {
                    this.elevation = Kriging.interpolate(
                        nearestMovedTiles.filter { it.value().tile.tectonicPlate == overridingPlate.key }
                            .map { it.value().newPosition to it.value().tile.elevation },
                        tile.position,
                        tectonicElevationVariogram
                    )
                    this.tile = tile
                    this.movement += (tile.position - nearestMovedTiles.first().value().newPosition)

                    // subduction
                    if (groups.size > 1) {
                        val subductionStrength = groups.filter { it != overridingPlate }.flatMap { (_, tiles) ->
                            tiles.map { entry ->
//                                    val pulledTile = entry.tile
                                val speedScale = entry.tile.movement.length() / searchRadius
                                entry.tile.tile.position to speedScale
                            }
                        }.weightedAverageInverse(tile.position, searchRadius)

                        subductionZones[tile] = SubductionZone(
                            tile,
                            subductionStrength,
                            SubductionInteraction(overridingPlate),
                            groups.filter { it != overridingPlate }
                                .mapNotNull { SubductionInteraction(it) }
                                .associateBy { it.plate })
                    }
                }
            } else {
                // divergence & gap filling
                val (newPlanetTile, divergenceZone) = DivergenceZone.divergeTileOrFillGap(planet, tile, movedTiles)
                newTileMap[tile] = newPlanetTile
                if (divergenceZone != null) {
                    divergenceZones[tile] = divergenceZone
                }
            }
        }

        newTileMap.forEach { (tile, newPlanetTile) ->
            newPlanetTile?.tile = tile
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
//            if (regionsToRemap.isNotEmpty()) {
//                GD.print("remapping ${regionsToRemap.size} regions for plate $plate, total size: ${regionsToRemap.sumOf { it.tiles.size }}, max size: ${regionsToRemap.maxOfOrNull { it.tiles.size }}")
//            }
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
            GD.print("creating plate of size ${plate.size}")
            planet.tectonicPlates.add(newPlate)
            plate.forEach { it.tectonicPlate = newPlate }
        }

        val subductionZonesRTree =
            subductionZones.entries.toRTree { (tile, zone) -> tile.position.toPoint() to zone }

        // erosion elevation & subduction pulldown & hotspots
        newTileMap.values.forEach {
            if (it != null) {
                it.elevation -= oceanicSubsidence(it.elevation)
                it.elevation += SubductionZone.adjustElevation(it, subductionZonesRTree)
                it.elevation = tryHotspotEruption(it)
                it.elevation = it.elevation.coerceIn(minElevation..maxElevation)
            }
        }

        planet.subductionZones = subductionZones
        planet.divergenceZones = divergenceZones
        planet.planetTiles = newTileMap.mapValues { it.value!! }.toMap()
        planet.tectonicPlates.forEach { it.clean() }
        planet.tectonicPlates.removeIf { it.tiles.isEmpty() }
    }

    fun performErosion(planet: Planet) {
        val deposits = planet.planetTiles.values.associateWith { 0.0 }.toMutableMap()
        for (planetTile in planet.planetTiles.values.sortedByDescending { it.elevation }) {
            val originalElevation = planetTile.elevation
            val prominence = planetTile.prominence
            val slopeScale = prominence.scaleAndCoerceIn(0.0..1000.0, 0.0..1.0)
            val deposit = deposits[planetTile]!!
            val depositTaken =
                deposit * depositStrength * (1 - slopeScale)
            planetTile.elevation += depositTaken

            val depositeeTile = planetTile.neighbors.minBy { it.elevation }

            if (depositeeTile.elevation <= planetTile.elevation) {
                val depositProvided = if (planetTile.elevation > 0) planetTile.elevation * erosionStrength else 0.0
                planetTile.elevation -= depositProvided
                deposits[depositeeTile] = deposit + depositProvided - depositTaken
            }
            planetTile.erosionDelta = planetTile.elevation - originalElevation
        }
    }

    fun stepTectonicsSimulation(planet: Planet) {
        val timeTaken = measureTime {
            movePlanetTiles(planet)
            stepTectonicPlateForces(planet)
            performErosion(planet)

            val oversizedPlate =
                planet.tectonicPlates.firstOrNull { it.tiles.size > planet.planetTiles.size * riftCutoff }
            oversizedPlate?.rift()
        }

        planet.tectonicAge += 1
        Gui.instance.tectonicAgeLabel.setText("${planet.tectonicAge} My")
        Gui.instance.updateInfobox()

        val percentContinental =
            planet.planetTiles.values.filter { it.elevation >= planet.seaLevel }.size / planet.planetTiles.size.toFloat()
        GD.print("completed step ${planet.tectonicAge} in ${timeTaken.inWholeMilliseconds}ms")
        GD.print("continental crust: ${(percentContinental * 100).toInt()}%")
    }
}