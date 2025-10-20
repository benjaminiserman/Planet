package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.*
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.TectonicGlobals.continentSpringDamping
import dev.biserman.planet.planet.TectonicGlobals.continentSpringSearchRadius
import dev.biserman.planet.planet.TectonicGlobals.continentSpringStiffness
import dev.biserman.planet.planet.TectonicGlobals.convergenceSearchRadius
import dev.biserman.planet.planet.TectonicGlobals.depositLoss
import dev.biserman.planet.planet.TectonicGlobals.depositStrength
import dev.biserman.planet.planet.TectonicGlobals.depositionStartHeight
import dev.biserman.planet.planet.TectonicGlobals.edgeInteractionStrength
import dev.biserman.planet.planet.TectonicGlobals.elevationErosion
import dev.biserman.planet.planet.TectonicGlobals.prominenceErosion
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
import dev.biserman.planet.planet.TectonicGlobals.tectonicElevationVariogram
import dev.biserman.planet.planet.TectonicGlobals.tryHotspotEruption
import dev.biserman.planet.planet.TectonicGlobals.waterErosion
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.sum
import godot.common.util.lerp
import godot.core.Vector3
import godot.global.GD
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.measureTime

object Tectonics {
    fun seedPlates(planet: Planet, plateCount: Int): MutableList<TectonicPlate> {
        val plates = (1..plateCount).map { TectonicPlate(planet) }
        val remainingTiles = planet.planetTiles.values.shuffled(planet.random).toMutableList()

        for (plate in plates) {
            val selectedTile = remainingTiles.first()
            remainingTiles.remove(selectedTile)
            selectedTile.tectonicPlate = plate
        }

        return plates.toMutableList()
    }

    fun voronoiPlates(planet: Planet) {
        val remainingTiles =
            planet.planetTiles.values.filter { it.tectonicPlate == null }.shuffled(planet.random).toMutableList()

        val warpNoise = VectorWarpNoise(planet.random.nextInt(), 0.75f)

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
                val neighborTile = planet.getTile(neighbor)
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
        val tiles =
            planet.planetTiles.values.shuffled(planet.random).filter { it.tectonicPlate == null }.toMutableList()
        var i = 0
        while (tiles.any { it.tectonicPlate == null }) {
            i += 1
            if (i > planet.planetTiles.size * 10) {
                throw IllegalStateException("Something went wrong, too many iterations $i with ${tiles.size} tiles remaining to assign")
            }

            for (planetTile in tiles.toList()) {
                val neighborCounts =
                    planetTile.tile.tiles.groupingBy { planet.getTile(it).tectonicPlate }.eachCount()

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
            val adjustedDensity = lerp(averageDensity, planet.random.nextDouble(-0.5, 1.0), 0.75)
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
                PointForce(
                    tile.tile.position, planet.noise.mantleConvection.sample4d(
                        tile.tile.position, planet.tectonicAge.toDouble()
                    ) * mantleConvectionStrength
                )
            })
            val slabPull = torque(
                planet.convergenceZones
                    .filter { (_, zone) -> plate.id in zone.subductingPlates }
                    .flatMap { (_, zone) -> zone.slabPull[plate.id] ?: listOf() }
            )
            val convergencePush = torque(
                planet.convergenceZones
                    .filter { (_, zone) -> plate.id in zone.subductingPlates }
                    .flatMap { (_, zone) -> zone.convergencePush[plate.id] ?: listOf() }
            )
            val ridgePush = torque(
                planet.divergenceZones
                    .filter { (_, zone) -> plate in zone.divergingPlates }
                    .flatMap { (_, zone) -> zone.ridgePush }
            )
            val springForces = torque(plate.tiles.map { tile ->
                PointForce(
                    tile.tile.position, tile.springDisplacement * springPlateContributionStrength
                )
            })
            val edgeInteractionForces = torque(plate.tiles.map { tile ->
                PointForce(
                    tile.tile.position, tile.getEdgeForces().sum() * edgeInteractionStrength
                )
            })

            plate.torque =
                oldTorqueWithDrag + (mantleConvectionTorque + slabPull + convergencePush + ridgePush + springForces + edgeInteractionForces) * plateTorqueScalar
        }

        planet.planetTiles.values.forEach {
            it.updateMovement()
        }
    }

    fun springAndDamp(tiles: Map<PlanetTile, Vector3>): Map<PlanetTile, Vector3> {
        val continentalTilesRTree = tiles.filter { it.key.isContinentalCrust }.entries.toRTree { it.value.toPoint() to it }
        val searchRadius = tiles.entries.first().key.planet.topology.averageRadius * continentSpringSearchRadius

        return tiles.mapValues { entry ->
            val (planetTile, newPosition) = entry
            val nearbyContinentalCrust = if (planetTile.isContinentalCrust) {
                continentalTilesRTree.nearest(planetTile.tile.position.toPoint(), searchRadius, 6)
                    .toList()
                    .filter { (entry, _) -> entry.key != planetTile }
                    .map { (entry, _) -> entry.key.tile to continentSpringStiffness }
            } else listOf()

            val displacement = nearbyContinentalCrust.fold(Vector3.ZERO) { sum, (otherTile, stiffness) ->
                val restLength = (otherTile.position - planetTile.tile.position).length()
                val currentDelta = (newPosition - tiles[planetTile.planet.getTile(otherTile)]!!)
                sum + currentDelta * (currentDelta.length() - restLength) * -stiffness
            }

            newPosition.lerp(newPosition + displacement, 1 - continentSpringDamping)
        }
    }

    fun springAndDamp(tiles: Map<PlanetTile, Vector3>, steps: Int) =
        (1..steps).fold(tiles) { acc, _ -> springAndDamp(acc) }

    data class MovedTile(val tile: PlanetTile, val newPosition: Vector3)

    fun movePlanetTiles(planet: Planet) {
        val originalMovedTiles = planet.planetTiles.values.filter { it.tectonicPlate != null }
            .sortedByDescending { it.elevation }
            .associateWith { tile -> tile.tile.position + tile.movement }
        val movedTiles = springAndDamp(originalMovedTiles)
        planet.planetTiles.forEach { (_, planetTile) ->
            planetTile.springDisplacement = movedTiles[planetTile]!! - originalMovedTiles[planetTile]!!
        }

        val convergenceZones = mutableMapOf<Tile, ConvergenceZone>()
        val newTileMap = planet.planetTiles
            .mapKeys { planet.topology.tiles[it.key] }
            .mapValues { null as PlanetTile? }
            .toMutableMap()

        val movedTilesRTree =
            movedTiles.entries.map { MovedTile(it.key, it.value) }.toRTree { movedTiles[it.tile]!!.toPoint() to it }
        val possibleDivergenceZones = mutableListOf<Tile>()
        for ((tile, _) in newTileMap) {
            val searchRadius = planet.topology.averageRadius
            val nearestMovedTiles = movedTilesRTree.nearest(
                tile.position.toPoint(), searchRadius * convergenceSearchRadius, searchMaxResults
            ).toList()
            val overlappingTiles =
                nearestMovedTiles.filter { it.value().newPosition.distanceTo(tile.position) < searchRadius }
            if (overlappingTiles.isNotEmpty()) {
                val groups = overlappingTiles.map { it.value() }.groupBy { it.tile.tectonicPlate!! }
                val overridingPlate = groups.maxBy { (_, plateTiles) -> plateTiles.maxOf { it.tile.elevation } }
                val nearestMovedTile =
                    overlappingTiles.first { it.value().tile.tectonicPlate == overridingPlate.key }.value().tile
                newTileMap[tile] = nearestMovedTile.copy().apply {
                    val goalElevation = Kriging.interpolate(
                        nearestMovedTiles
                            .filter { it.value().tile.tectonicPlate == overridingPlate.key }
                            .map { it.value().newPosition to it.value().tile.elevation },
                        tile.position,
                        tectonicElevationVariogram
                    )
                    val closestElevation = nearestMovedTiles
                        .filter { it.value().tile.tectonicPlate == overridingPlate.key }
                        .minBy { (it.value().tile.elevation - goalElevation).absoluteValue }
                        .value().tile.elevation
                    this.elevation = lerp(goalElevation, closestElevation, 0.5)

                    this.tile = tile
                    this.movement += (tile.position - nearestMovedTiles.first().value().newPosition)

                    // subduction
                    if (groups.size > 1) {
                        val subductionSpeed = groups.flatMap { (_, tiles) ->
                            tiles.map { entry ->
                                entry.tile.movement.dot(tile.position - entry.tile.tile.position) / searchRadius
                            }
                        }.average()

                        convergenceZones[tile] = ConvergenceZone.make(
                            planet,
                            tile,
                            subductionSpeed,
                            ConvergenceInteraction(overridingPlate),
                            groups.filter { it != overridingPlate }
                                .mapNotNull { ConvergenceInteraction(it) }
                                .associateBy { it.plate },
                            groups
                        )
                    }
                }
            } else {
                possibleDivergenceZones.add(tile)
            }
        }

        val divergenceZones = mutableMapOf<Tile, DivergenceZone>()
        possibleDivergenceZones.forEach { tile ->
            // divergence & gap filling
            val (newPlanetTile, divergenceZone) = DivergenceZone.divergeTileOrFillGap(
                planet, tile, newTileMap, movedTiles
            )
            newTileMap[tile] = newPlanetTile
            if (divergenceZone != null) {
                divergenceZones[tile] = divergenceZone
            }
        }

        newTileMap.forEach { (tile, newPlanetTile) ->
            newPlanetTile?.tile = tile
        }

        val plateRegions = PlanetRegion(planet, newTileMap.values.filterNotNull())
            .floodFillGroupBy(planetTileFn = { newTileMap[it]!! }) { it.tectonicPlate }
            .toMutableMap()
            .mapValues { (_, value) ->
                value.sortedByDescending { it.tiles.size }
            }

        for ((plate, regions) in plateRegions) {
            val regionsToRemap = regions.filter {
                plate == null || it.tiles.size < regions.first().tiles.size || it.tiles.size < minPlateSize
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
                if (maxNeighbor == null || maxNeighbor.value >= edgeLength * plateMergeCutoff || region.tiles.size < minPlateSize) {
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

        val newPlates = PlanetRegion(planet, newTileMap.values.filterNotNull())
            .floodFillGroupBy(planetTileFn = { newTileMap[it]!! }) { it.tectonicPlate == null }[true] ?: listOf()

        for (plate in newPlates) {
            val newPlate = if (plate.tiles.size >= minPlateSize) {
                TectonicPlate(planet)
            } else {
                TectonicPlate(planet, name = "Complex")
            }
            GD.print("creating plate of size ${plate.tiles.size}")
            planet.tectonicPlates.add(newPlate)
            plate.tiles.forEach { it.tectonicPlate = newPlate }
        }

        val subductionZonesRTree = convergenceZones.entries.toRTree { (tile, zone) -> tile.position.toPoint() to zone }

        // erosion elevation & subduction pulldown & hotspots
        newTileMap.values.forEach {
            if (it != null) {
                it.elevation -= oceanicSubsidence(it.elevation)
                it.elevation += ConvergenceZone.adjustElevation(it, subductionZonesRTree)
                it.elevation = tryHotspotEruption(it)
                it.elevation = it.elevation.coerceIn(minElevation..maxElevation)
            }
        }

        val oversizedPlate = planet.tectonicPlates.firstOrNull { it.tiles.size > planet.planetTiles.size * riftCutoff }
        oversizedPlate?.rift()

        val internalPlates = planet.tectonicPlates.associateWith { it.calculateNeighborLengths() }
            .filter { (_, neighbors) -> neighbors.size == 1 }

        internalPlates.forEach { (plate, neighbors) ->
            plate.mergeInto(neighbors.keys.first())
        }

        planet.convergenceZones = convergenceZones.mapKeys { it.key.id }.toMutableMap()
        planet.divergenceZones = divergenceZones.mapKeys { it.key.id }.toMutableMap()
        planet.planetTiles = newTileMap.mapKeys { it.key.id }.mapValues { it.value!! }.toMap()
        planet.tectonicPlates.forEach { it.clean() }
        planet.tectonicPlates.removeIf { it.tiles.isEmpty() }
    }

    fun performErosion(planet: Planet) {
        val deposits = planet.planetTiles.values.associateWith { 0.0 }.toMutableMap()
        val waterFlow = planet.planetTiles.values.associateWith { 1.0 }.toMutableMap()
        for (planetTile in planet.planetTiles.values.sortedByDescending { it.elevation }) {
            val originalElevation = planetTile.elevation
            val prominenceScale = planetTile.prominence.scaleAndCoerceIn(0.0..1000.0, 0.0..1.0)
            val deposit = deposits[planetTile]!!
            val water = waterFlow[planetTile]!!
            val depositTaken =
                if (planetTile.elevation <= depositionStartHeight)
                    deposit * depositStrength * (1 - prominenceScale).pow(3)
                else 0.0
            planetTile.elevation += depositTaken * (1 - depositLoss)

            val depositeeTiles = planetTile.neighbors
                .filter { it.elevation < planetTile.elevation }
            val sumDecline = depositeeTiles.sumOf { planetTile.elevation - it.elevation }
            val erosion = max(
                0.0, min(
                    planetTile.prominence, min(
                        planetTile.elevation + 1.0,
                        planetTile.prominence.pow(0.5) * prominenceErosion +
                                planetTile.elevation.pow(2) * elevationErosion +
                                water * waterErosion
                    )
                )
            )
            val totalDepositAvailable = erosion + deposit - depositTaken

            planetTile.elevation -= erosion

            // deposit water
            if (depositeeTiles.isNotEmpty() && planetTile.isAboveWater) {
                depositeeTiles.forEach { depositeeTile ->
                    val waterSent = water * (planetTile.elevation - depositeeTile.elevation) / sumDecline
                    waterFlow[depositeeTile] = (waterFlow[depositeeTile] ?: 0.0) + waterSent
                }
            }

            // deposit sediment
            if (depositeeTiles.isNotEmpty() && totalDepositAvailable >= 0.1) {
                depositeeTiles.forEach { depositeeTile ->
                    val depositSent = totalDepositAvailable *
                            (planetTile.elevation - depositeeTile.elevation) / sumDecline
                    deposits[depositeeTile] = (deposits[depositeeTile] ?: 0.0) + depositSent
                }
            } else {
                planetTile.elevation += totalDepositAvailable
            }

            planetTile.erosionDelta = planetTile.elevation - originalElevation
            planetTile.depositFlow = deposits[planetTile]!!
            planetTile.waterFlow = waterFlow[planetTile]!!
        }
    }

    fun stepTectonicsSimulation(planet: Planet) {
        val movePlanetTilesTime = measureTime { movePlanetTiles(planet) }
        val tectonicPlateForcesTime = measureTime { stepTectonicPlateForces(planet) }
        val performErosionTime = measureTime { performErosion(planet) }
        val runGuardrailsTime = measureTime {
            planet.planetTiles.values.forEach {
                it.elevation = it.elevation.coerceIn(minElevation..maxElevation)
            }
            runGuardrails(planet)
        }

        val timeTaken = movePlanetTilesTime +
                tectonicPlateForcesTime +
                performErosionTime +
                runGuardrailsTime

        planet.tectonicAge += 1

        OceanCurrents.viaEarthlikeHeuristic(planet, 7)

        Gui.instance.tectonicAgeLabel.setText("${planet.tectonicAge} My")
        Gui.instance.updateInfobox()
        Gui.instance.statsGraph.update(planet)

        val percentContinental =
            planet.planetTiles.values.filter { it.isAboveWater }.size / planet.planetTiles.size.toFloat()
        GD.print("completed step ${planet.tectonicAge} in ${timeTaken.inWholeMilliseconds}ms")
        GD.print(" - movePlanetTiles: ${movePlanetTilesTime.inWholeMilliseconds}ms")
        GD.print(" - tectonicPlateForces: ${tectonicPlateForcesTime.inWholeMilliseconds}ms")
        GD.print(" - performErosion: ${performErosionTime.inWholeMilliseconds}ms")
        GD.print(" - runGuardrails: ${runGuardrailsTime.inWholeMilliseconds}ms")
        GD.print("continental crust: ${(percentContinental * 100).toInt()}%, ${planet.tectonicPlates.size} plates")
        GD.print("average movement: ${planet.planetTiles.values.sumOf { it.movement.length() } / planet.planetTiles.size}")

        // hacky way to stop simulation from running forever
        if (planet.tectonicAge % 10000 == 0) {
            Main.instance.timerActive = "none"
        }
    }

    fun runGuardrails(planet: Planet) {
        val averageContinentalHeight = planet.planetTiles.values
            .filter { it.isContinentalCrust }
            .map { it.elevation }
            .average()

        val percentContinental =
            planet.planetTiles.values.filter { it.isAboveWater }.size / planet.planetTiles.size.toFloat()

        if (averageContinentalHeight <= 750 && percentContinental <= 0.15) {
            GD.print("raising elevation — ${averageContinentalHeight}m & ${(percentContinental * 100).formatDigits()}%")
            planet.planetTiles.values.forEach { it.elevation += 100 }
        }

        if (averageContinentalHeight >= 1250 && percentContinental >= 0.55) {
            GD.print("lowering elevation — ${averageContinentalHeight}m & ${(percentContinental * 100).formatDigits()}%")
            planet.planetTiles.values.forEach { it.elevation -= 100 }
        }
    }
}