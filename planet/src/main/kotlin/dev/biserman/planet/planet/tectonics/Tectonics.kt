package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.*
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.BiotaDistribution
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetRegion
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.PointForce
import dev.biserman.planet.planet.tectonics.Meteor.impactMeteor
import dev.biserman.planet.planet.tectonics.TectonicGlobals.continentSpringDamping
import dev.biserman.planet.planet.tectonics.TectonicGlobals.continentSpringSearchRadius
import dev.biserman.planet.planet.tectonics.TectonicGlobals.continentSpringStiffness
import dev.biserman.planet.planet.tectonics.TectonicGlobals.convergenceSearchRadius
import dev.biserman.planet.planet.tectonics.TectonicGlobals.depositLoss
import dev.biserman.planet.planet.tectonics.TectonicGlobals.depositMultiplier
import dev.biserman.planet.planet.tectonics.TectonicGlobals.depositStrength
import dev.biserman.planet.planet.tectonics.TectonicGlobals.depositionStartHeight
import dev.biserman.planet.planet.tectonics.TectonicGlobals.desiredLandPercent
import dev.biserman.planet.planet.tectonics.TectonicGlobals.boundarySmoothingMinSamePlateNeighbors
import dev.biserman.planet.planet.tectonics.TectonicGlobals.boundarySmoothingPasses
import dev.biserman.planet.planet.tectonics.TectonicGlobals.divergenceContinuityStrength
import dev.biserman.planet.planet.tectonics.TectonicGlobals.edgeInteractionStrength
import dev.biserman.planet.planet.tectonics.TectonicGlobals.elevationErosion
import dev.biserman.planet.planet.tectonics.TectonicGlobals.guardrailStrictness
import dev.biserman.planet.planet.tectonics.TectonicGlobals.prominenceErosion
import dev.biserman.planet.planet.tectonics.TectonicGlobals.mantleConvectionStrength
import dev.biserman.planet.planet.tectonics.TectonicGlobals.maxAverageContinentalHeightGuardrail
import dev.biserman.planet.planet.tectonics.TectonicGlobals.maxElevation
import dev.biserman.planet.planet.tectonics.TectonicGlobals.maxErosionProportion
import dev.biserman.planet.planet.tectonics.TectonicGlobals.minAverageContinentalHeightGuardrail
import dev.biserman.planet.planet.tectonics.TectonicGlobals.minElevation
import dev.biserman.planet.planet.tectonics.TectonicGlobals.minPlateSize
import dev.biserman.planet.planet.tectonics.TectonicGlobals.oceanicSubsidence
import dev.biserman.planet.planet.tectonics.TectonicGlobals.plateMergeCutoff
import dev.biserman.planet.planet.tectonics.TectonicGlobals.plateTorqueScalar
import dev.biserman.planet.planet.tectonics.TectonicGlobals.riftCutoff
import dev.biserman.planet.planet.tectonics.TectonicGlobals.searchMaxResults
import dev.biserman.planet.planet.tectonics.TectonicGlobals.springPlateContributionStrength
import dev.biserman.planet.planet.tectonics.TectonicGlobals.tectonicElevationVariogram
import dev.biserman.planet.planet.tectonics.TectonicGlobals.tectonicSimulationStop
import dev.biserman.planet.planet.tectonics.TectonicGlobals.tryHotspotEruption
import dev.biserman.planet.planet.tectonics.TectonicGlobals.waterErosion
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.sum
import godot.common.util.lerp
import godot.core.Color
import godot.core.Vector3
import godot.global.GD
import kotlin.collections.associateWith
import kotlin.collections.filter
import kotlin.collections.iterator
import kotlin.collections.map
import kotlin.collections.mapValues
import kotlin.collections.withIndex
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
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
            fun finiteTorque(source: String, value: Vector3): Vector3 =
                if (value.isFinite()) {
                    value
                } else {
                    GD.print("Discarding non-finite $source torque for plate ${plate.id} (${plate.tiles.size} tiles)")
                    Vector3.ZERO
                }

            val oldTorqueWithDrag = finiteTorque("retained", plate.torque * 0.9)
            val mantleConvectionTorque = finiteTorque("mantle convection", torque(plate.tiles.map { tile ->
                PointForce(
                    tile.tile.position, planet.noise.mantleConvection.sample4d(
                        tile.tile.position, planet.tectonicAge.toDouble()
                    ) * mantleConvectionStrength
                )
            }))
            val slabPull = finiteTorque(
                "slab pull", torque(
                    planet.convergenceZones
                        .filter { (_, zone) -> plate.id in zone.subductingPlates }
                        .flatMap { (_, zone) -> zone.slabPull[plate.id] ?: listOf() }
                ))
            val convergencePush = finiteTorque(
                "convergence push", torque(
                    planet.convergenceZones
                        .flatMap { (_, zone) -> zone.convergencePush[plate.id] ?: listOf() }
                ))
            val ridgePush = finiteTorque(
                "ridge push", torque(
                    planet.divergenceZones
                        .filter { (_, zone) -> plate in zone.divergingPlates }
                        .flatMap { (_, zone) -> zone.ridgePush }
                ))
            val springForces = finiteTorque("spring", torque(plate.tiles.map { tile ->
                PointForce(
                    tile.tile.position, tile.springDisplacement * springPlateContributionStrength
                )
            }))
            val edgeInteractionForces = finiteTorque("edge interaction", torque(plate.tiles.map { tile ->
                PointForce(
                    tile.tile.position, tile.getEdgeForces().sum() * edgeInteractionStrength
                )
            }))

            val nextTorque =
                oldTorqueWithDrag + (mantleConvectionTorque + slabPull + convergencePush + ridgePush + springForces + edgeInteractionForces) * plateTorqueScalar
            plate.torque = if (nextTorque.isFinite()) {
                nextTorque
            } else {
                GD.print("Discarding non-finite torque for plate ${plate.id} (${plate.tiles.size} tiles)")
                Vector3.ZERO
            }
        }

        planet.planetTiles.values.forEach {
            it.updateMovement()
        }
    }

    data class MovedTile(val tile: PlanetTile, val newPosition: Vector3)

    private data class DivergentBorder(
        val border: Border,
        val plateIds: Pair<Int, Int>,
        val blockedByConvergence: Boolean,
        val signal: DivergenceSignal
    )

    private fun detectDivergenceSignals(
        planet: Planet,
        convergenceTiles: Set<Tile>
    ): Map<Tile, DivergenceSignal> {
        val rawBorders = planet.topology.borders.mapNotNull { border ->
            val tileA = planet.getTile(border.tiles[0])
            val tileB = planet.getTile(border.tiles[1])
            val plateA = tileA.tectonicPlate ?: return@mapNotNull null
            val plateB = tileB.tectonicPlate ?: return@mapNotNull null
            if (plateA == plateB) return@mapNotNull null

            val acrossBoundary = (tileB.tile.position - tileA.tile.position)
                .tangent(border.midpoint.normalized())
            if (acrossBoundary.lengthSquared() == 0.0) return@mapNotNull null

            val normal = acrossBoundary.normalized()
            val contactPoint = border.midpoint.normalized()
            val plateAMovement = plateA.eulerPole.cross(contactPoint)
            val plateBMovement = plateB.eulerPole.cross(contactPoint)
            val relativeMovement = plateBMovement - plateAMovement
            val separationSpeed = max(0.0, relativeMovement.dot(normal))
            val normalMotion = if (relativeMovement.lengthSquared() == 0.0) {
                0.0
            } else {
                separationSpeed / relativeMovement.length()
            }
            val normalizedSeparationSpeed = separationSpeed / planet.topology.averageRadius
            val nearConvergence = border.tiles.any { tile ->
                tile in convergenceTiles || tile.tiles.any { it in convergenceTiles }
            }
            val strength = if (
                !nearConvergence && normalMotion >= TectonicGlobals.divergenceMinNormalMotion
            ) {
                ((normalizedSeparationSpeed - TectonicGlobals.divergenceMinSeparationSpeed) /
                        max(
                            TectonicGlobals.divergenceFullSeparationSpeed -
                                    TectonicGlobals.divergenceMinSeparationSpeed,
                            1e-9
                        ))
                    .coerceIn(0.0, 1.0)
            } else {
                0.0
            }
            val plateIds = minOf(plateA.id, plateB.id) to maxOf(plateA.id, plateB.id)
            DivergentBorder(
                border,
                plateIds,
                nearConvergence,
                DivergenceSignal(strength, listOf(plateA, plateB))
            )
        }
        val rawByBorderId = rawBorders.associateBy { it.border.id }

        return rawBorders
            .map { divergentBorder ->
                val connectedDivergentStrengths = if (divergentBorder.blockedByConvergence) {
                    emptyList()
                } else {
                    divergentBorder.border.borders.asSequence()
                        .mapNotNull { rawByBorderId[it.id] }
                        .filter { it.plateIds == divergentBorder.plateIds && !it.blockedByConvergence }
                        .map { it.signal.strength }
                        .filter { it >= TectonicGlobals.divergenceCutoff }
                        .toList()
                }
                val hasBoundarySupport =
                    connectedDivergentStrengths.size >= TectonicGlobals.divergenceMinConnectedEdges
                val supportedStrength =
                    if (hasBoundarySupport) divergentBorder.signal.strength else 0.0
                val continuityStrength = if (hasBoundarySupport) {
                    connectedDivergentStrengths.average() * divergenceContinuityStrength
                } else {
                    0.0
                }
                divergentBorder.copy(
                    signal = divergentBorder.signal.copy(
                        strength = max(supportedStrength, continuityStrength)
                    )
                )
            }
            .filter { it.signal.strength >= TectonicGlobals.divergenceCutoff }
            .flatMap { divergentBorder ->
                divergentBorder.border.tiles.map { tile -> tile to divergentBorder.signal }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, signals) -> signals.maxBy { it.strength } }
    }

    private fun nearestMovedTiles(
        candidates: IntArray,
        movedTilesById: Array<MovedTile?>,
        position: Vector3,
        searchRadius: Double,
        maxResults: Int
    ): List<MovedTile> {
        val nearest = ArrayList<MovedTile>(maxResults)
        val distances = DoubleArray(maxResults)

        for (tileId in candidates) {
            val candidate = movedTilesById[tileId] ?: continue
            val distance = candidate.newPosition.distanceTo(position)
            if (!distance.isFinite() || distance > searchRadius) continue

            var insertionIndex = nearest.size
            while (insertionIndex > 0 && distance < distances[insertionIndex - 1]) {
                insertionIndex--
            }
            if (insertionIndex >= maxResults) continue

            if (nearest.size < maxResults) {
                nearest.add(candidate)
            }
            var index = nearest.lastIndex
            while (index > insertionIndex) {
                nearest[index] = nearest[index - 1]
                distances[index] = distances[index - 1]
                index--
            }
            nearest[insertionIndex] = candidate
            distances[insertionIndex] = distance
        }

        return nearest
    }

    private fun proximityListsFor(
        tiles: Map<PlanetTile, Vector3>,
        searchRadius: Double
    ): List<IntArray> {
        val maxDisplacement = tiles.maxOf { (tile, position) -> position.distanceTo(tile.tile.position) }
        return tiles.keys.first().planet.topology.proximityLists(searchRadius + maxDisplacement)
    }

    private fun springAndDamp(
        tiles: Map<PlanetTile, Vector3>,
        proximityLists: List<IntArray>
    ): Map<PlanetTile, Vector3> {
        val searchRadius = tiles.entries.first().key.planet.topology.averageRadius * continentSpringSearchRadius
        val movedTilesById = arrayOfNulls<MovedTile>(proximityLists.size)
        for ((tile, position) in tiles) {
            if (tile.isContinentalCrust) {
                movedTilesById[tile.tile.id] = MovedTile(tile, position)
            }
        }

        val result = HashMap<PlanetTile, Vector3>(tiles.size)
        for ((planetTile, newPosition) in tiles) {
            var displacement = Vector3.ZERO
            if (planetTile.isContinentalCrust) {
                val nearbyTiles = nearestMovedTiles(
                    proximityLists[planetTile.tile.id],
                    movedTilesById,
                    planetTile.tile.position,
                    searchRadius,
                    6
                )
                for (movedTile in nearbyTiles) {
                    val otherTile = movedTile.tile
                    if (otherTile != planetTile && otherTile.tectonicPlate == planetTile.tectonicPlate) {
                        val restLength = (otherTile.tile.position - planetTile.tile.position).length()
                        val currentDelta = newPosition - movedTile.newPosition
                        displacement += currentDelta * (currentDelta.length() - restLength) * -continentSpringStiffness
                    }
                }
            }

            result[planetTile] = newPosition.lerp(newPosition + displacement, 1 - continentSpringDamping)
        }
        return result
    }

    fun springAndDamp(tiles: Map<PlanetTile, Vector3>): Map<PlanetTile, Vector3> {
        val searchRadius = tiles.entries.first().key.planet.topology.averageRadius * continentSpringSearchRadius
        return springAndDamp(tiles, proximityListsFor(tiles, searchRadius))
    }

    fun springAndDamp(tiles: Map<PlanetTile, Vector3>, steps: Int) =
        (1..steps).fold(tiles) { acc, _ -> springAndDamp(acc) }

    private fun smoothPlateBoundaries(
        newTileMap: Map<Tile, PlanetTile?>,
        protectedTiles: Set<Tile>
    ) {
        val passCount = boundarySmoothingPasses.coerceIn(0, 3)
        if (passCount == 0) return

        repeat(passCount) {
            val reassignments = mutableListOf<Pair<PlanetTile, TectonicPlate>>()
            for ((tile, planetTile) in newTileMap) {
                if (tile in protectedTiles) continue

                val currentPlate = planetTile?.tectonicPlate ?: continue
                val samePlateNeighbors =
                    tile.tiles.count { neighbor -> newTileMap[neighbor]?.tectonicPlate == currentPlate }
                if (samePlateNeighbors > boundarySmoothingMinSamePlateNeighbors) continue

                var samePlateBorderLength = 0.0
                val neighborBorderLengths = mutableMapOf<TectonicPlate, Double>()
                for (border in tile.borders) {
                    val neighborPlate = newTileMap[border.oppositeTile(tile)]?.tectonicPlate ?: continue
                    if (neighborPlate == currentPlate) {
                        samePlateBorderLength += border.length
                    } else {
                        neighborBorderLengths[neighborPlate] =
                            (neighborBorderLengths[neighborPlate] ?: 0.0) + border.length
                    }
                }

                val strongestNeighbor = neighborBorderLengths.maxByOrNull { it.value } ?: continue
                if (strongestNeighbor.value > samePlateBorderLength) {
                    reassignments.add(planetTile to strongestNeighbor.key)
                }
            }

            if (reassignments.isEmpty()) return
            reassignments.forEach { (tile, plate) -> tile.tectonicPlate = plate }
        }
    }

    fun movePlanetTiles(planet: Planet) {
        val nonFiniteMovements = planet.planetTiles.values.filter { !it.movement.isFinite() }
        if (nonFiniteMovements.isNotEmpty()) {
            GD.print("Resetting ${nonFiniteMovements.size} non-finite tile movements")
            nonFiniteMovements.forEach { it.movement = Vector3.ZERO }
        }
        val searchRadius = planet.topology.averageRadius
        val originalMovedTiles = planet.planetTiles.values.filter { it.tectonicPlate != null }
            .sortedByDescending { it.elevation }
            .associateWith { tile -> tile.tile.position + tile.movement }
        val springProximityLists =
            proximityListsFor(originalMovedTiles, searchRadius * continentSpringSearchRadius)
        val movedTiles = springAndDamp(originalMovedTiles, springProximityLists)
        planet.planetTiles.forEach { (_, planetTile) ->
            planetTile.springDisplacement = movedTiles[planetTile]!! - originalMovedTiles[planetTile]!!
        }

        val convergenceZones = mutableMapOf<Tile, ConvergenceZone>()
        val newTileMap = LinkedHashMap<Tile, PlanetTile?>(planet.topology.tiles.size).also { map ->
            for (tile in planet.topology.tiles) {
                map[tile] = null
            }
        }

        val movedTilesById = arrayOfNulls<MovedTile>(planet.topology.tiles.size)
        for ((planetTile, newPosition) in movedTiles) {
            movedTilesById[planetTile.tile.id] = MovedTile(planetTile, newPosition)
        }
        val classificationProximityLists = proximityListsFor(movedTiles, searchRadius * convergenceSearchRadius)
        val possibleDivergenceZones = mutableListOf<Tile>()
        val convergenceSearchDistance = searchRadius * convergenceSearchRadius
        for (tile in planet.topology.tiles) {
            val nearestMovedTiles = nearestMovedTiles(
                classificationProximityLists[tile.id],
                movedTilesById,
                tile.position,
                convergenceSearchDistance,
                searchMaxResults
            )
            val overlappingTiles = ArrayList<MovedTile>(nearestMovedTiles.size)
            for (movedTile in nearestMovedTiles) {
                if (movedTile.newPosition.distanceTo(tile.position) < searchRadius) {
                    overlappingTiles.add(movedTile)
                }
            }
            if (overlappingTiles.isNotEmpty()) {
                val groups = overlappingTiles.groupBy { it.tile.tectonicPlate!! }
                val overridingPlate = groups.maxBy { (_, plateTiles) -> plateTiles.maxOf { it.tile.elevation } }
                val nearestMovedTile = overridingPlate.value.first()
                val nearestMovedTilesForPlate =
                    nearestMovedTiles.filter { it.tile.tectonicPlate == overridingPlate.key }
                newTileMap[tile] = nearestMovedTile.tile.copy().apply {
                    val goalElevation = Kriging.interpolate(
                        nearestMovedTilesForPlate.map { it.newPosition to it.tile.elevation },
                        tile.position,
                        tectonicElevationVariogram
                    )
                    val closestElevation = nearestMovedTilesForPlate
                        .minBy { (it.tile.elevation - goalElevation).absoluteValue }
                        .tile.elevation
                    this.elevation = lerp(goalElevation, closestElevation, 0.5)

                    this.tile = tile
                    this.movement += (tile.position - nearestMovedTile.newPosition)

                    if (groups.size > 1) {
                        val subductionSpeed =
                            overlappingTiles.sumOf {
                                it.tile.movement.dot(tile.position - it.tile.tile.position) / searchRadius
                            } / overlappingTiles.size

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

        val divergenceSignals = detectDivergenceSignals(planet, convergenceZones.keys)
        val divergenceZones = divergenceSignals
            .mapValues { (tile, signal) ->
                DivergenceZone(planet, tile.id, signal.strength, signal.divergingPlates)
            }
            .toMutableMap()
        possibleDivergenceZones.forEach { tile ->
            val (newPlanetTile, divergenceZone) = DivergenceZone.divergeTileOrFillGap(
                planet, tile, newTileMap, divergenceSignals[tile]
            )
            newTileMap[tile] = newPlanetTile
            if (divergenceZone != null) {
                divergenceZones[tile] = divergenceZone
            }
        }

        newTileMap.forEach { (tile, newPlanetTile) ->
            newPlanetTile?.tile = tile
        }

        val activeTiles = newTileMap.values.filterNotNull()
        val plateRegions = PlanetRegion(planet, activeTiles)
            .floodFillGroupBy(planetTileFn = { newTileMap[it]!! }) { it.tectonicPlate }
            .toMutableMap()
            .mapValues { (_, value) ->
                value.sortedByDescending { it.tiles.size }
            }

        for ((plate, regions) in plateRegions) {
            val regionsToRemap = regions.filter {
                plate == null || it.tiles.size < regions.first().tiles.size || it.tiles.size < minPlateSize
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

        smoothPlateBoundaries(newTileMap, convergenceZones.keys + divergenceZones.keys)

        val newPlates = PlanetRegion(planet, activeTiles)
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

        val subductionZonesRTree =
            convergenceZones.entries.toRTree { (tile, zone) -> tile.position.toPoint() to zone }

        activeTiles.forEach {
            it.elevation -= oceanicSubsidence(it.elevation)
            val convergenceAdjustment = ConvergenceZone.adjustElevation(it, subductionZonesRTree)
            it.oceanArcUplift = convergenceAdjustment.oceanArc
            it.elevation += convergenceAdjustment.total
            it.elevation = tryHotspotEruption(it)
            it.elevation = it.elevation.coerceIn(minElevation..maxElevation)
        }

        planet.planetTiles = LinkedHashMap<Int, PlanetTile>(newTileMap.size).also { tiles ->
            newTileMap.forEach { (tile, planetTile) -> tiles[tile.id] = planetTile!! }
        }
        planet.tectonicPlates.forEach { it.clean() }
        planet.tectonicPlates.removeIf { it.tiles.isEmpty() }

        val oversizedPlate = planet.tectonicPlates.firstOrNull { it.tiles.size > planet.planetTiles.size * riftCutoff }
        oversizedPlate?.rift()

        val internalPlates = planet.tectonicPlates.associateWith { it.calculateNeighborLengths() }
            .filter { (_, neighbors) -> neighbors.size == 1 }

        internalPlates.entries
            .sortedBy { (plate) -> plate.tiles.size }
            .forEach { (plate, neighbors) ->
                val neighbor = neighbors.keys.first()
                if (plate in planet.tectonicPlates && neighbor in planet.tectonicPlates) {
                    plate.mergeInto(neighbor)
                }
            }

        planet.convergenceZones = LinkedHashMap<Int, ConvergenceZone>(convergenceZones.size).also { zones ->
            convergenceZones.forEach { (tile, zone) -> zones[tile.id] = zone }
        }
        planet.divergenceZones = LinkedHashMap<Int, DivergenceZone>(divergenceZones.size).also { zones ->
            divergenceZones.forEach { (tile, zone) -> zones[tile.id] = zone }
        }
    }


    fun performErosion(planet: Planet) {
        val deposits = planet.planetTiles.values.associateWith { 0.0 }.toMutableMap()
        val waterFlow = planet.planetTiles.values.associateWith { 1.0 }.toMutableMap()
        val currentLandPercent =
            planet.planetTiles.values.count { it.isAboveWater }.toDouble() / planet.planetTiles.size
        val landPercentDepositScale =
            (desiredLandPercent.coerceIn(0.0, 1.0) / currentLandPercent.coerceIn(0.01, 1.0))
                .coerceIn(0.25, 4.0)
        val effectiveDepositMultiplier = depositMultiplier * landPercentDepositScale

        for (planetTile in planet.planetTiles.values.sortedByDescending { it.elevation }) {
            val originalElevation = planetTile.elevation
            val prominenceScale = planetTile.prominence.scaleAndCoerceIn(0.0..1000.0, 0.0..1.0)
            val surroundingAverageElevation = planetTile.neighbors.map { it.elevation }.average()
            val deposit = deposits[planetTile]!!
            val water = waterFlow[planetTile]!!
            val depositTaken =
                if (planetTile.elevation <= depositionStartHeight)
                    deposit * depositStrength * (1 - prominenceScale).pow(3)
                else 0.0
            planetTile.elevation += (depositTaken * (1 - depositLoss))
                .coerceIn(
                    0.0..max(
                        0.0,
                        (surroundingAverageElevation - planetTile.elevation)
                    )
                )

            val downhillTiles = planetTile.neighbors.mapNotNull { neighbor ->
                val decline = planetTile.elevation - neighbor.elevation
                if (decline.isFinite() && decline > 0.0) neighbor to decline else null
            }
            val sumDecline = downhillTiles.sumOf { (_, decline) -> decline }
            val erosion = max(
                0.0, min(
                    planetTile.prominence, min(
                        planetTile.elevation * maxErosionProportion,
                        planetTile.prominence.pow(0.5) * prominenceErosion +
                                planetTile.elevation.pow(2) * elevationErosion +
                                water * waterErosion
                    )
                )
            )
            val totalDepositAvailable = max(0.0, (erosion + deposit - depositTaken) * effectiveDepositMultiplier)

            planetTile.elevation -= erosion

            // deposit water
            if (sumDecline > 0.0 && planetTile.isAboveWater && water.isFinite()) {
                downhillTiles.forEach { (depositeeTile, decline) ->
                    val waterSent = water * decline / sumDecline
                    waterFlow[depositeeTile] = (waterFlow[depositeeTile] ?: 0.0) + waterSent
                }
            }

            // deposit sediment
            if (sumDecline > 0.0 && totalDepositAvailable >= 0.1 && totalDepositAvailable.isFinite()) {
                downhillTiles.forEach { (depositeeTile, decline) ->
                    val depositSent = totalDepositAvailable * decline / sumDecline
                    deposits[depositeeTile] = (deposits[depositeeTile] ?: 0.0) + depositSent
                }
            } else {
                planetTile.elevation += max(
                    0.0, min(
                        totalDepositAvailable,
                        surroundingAverageElevation - planetTile.elevation
                    )
                )
            }

            planetTile.erosionDelta = planetTile.elevation - originalElevation
            planetTile.accruedDeposit += planetTile.erosionDelta
            planetTile.depositFlow = deposits[planetTile]!!
            planetTile.waterFlow = waterFlow[planetTile]!!
        }
    }

    fun wrapMeasureTime(name: String, block: () -> Unit): Pair<String, Duration> = name to measureTime(block)

    fun stepTectonicsSimulation(planet: Planet) {
        val steps = listOf(
            wrapMeasureTime("movePlanetTiles") { movePlanetTiles(planet) },
            wrapMeasureTime("simulateGeology") { Geology.simulateGeology(planet) },
            wrapMeasureTime("impactMeteor") { impactMeteor(planet) },
            wrapMeasureTime("stepTectonicPlateForces") { stepTectonicPlateForces(planet) },
            wrapMeasureTime("performErosion") { performErosion(planet) },
            wrapMeasureTime("runGuardrails") {
                planet.planetTiles.values.forEach {
                    it.elevation = it.elevation.coerceIn(minElevation..maxElevation)
                }
                runGuardrails(planet)
            },
            wrapMeasureTime("distributeBiota") { BiotaDistribution.updatePlanet(planet) }
        )
        val timeTaken = steps.map { it.second }.sumOf { it.inWholeMilliseconds }

        planet.planetTiles.values.forEach {
            it.debugColor = if (it in planet.biotaDistributions[0].region) Color.red else Color.black
        }

        planet.tectonicAge++
        planet.terrainChangeCount++

        Gui.instance.updateInfobox()
        Gui.instance.statsGraph.update(planet)

        val percentContinental =
            planet.planetTiles.values.filter { it.isAboveWater }.size / planet.planetTiles.size.toFloat()
        GD.print("completed step ${planet.tectonicAge} in ${timeTaken}ms")
        steps.forEach { (name, time) -> GD.print(" - $name: ${time.inWholeMilliseconds}ms") }
        GD.print("continental crust: ${(percentContinental * 100).toInt()}%, ${planet.tectonicPlates.size} plates")
        GD.print("average movement: ${planet.planetTiles.values.sumOf { it.movement.length() } / planet.planetTiles.size}")

        // hacky way to stop simulation from running forever
        if (tectonicSimulationStop > 0 && planet.tectonicAge % tectonicSimulationStop == 0) {
            Main.instance.timerActive = false
        }
    }

    fun runGuardrails(planet: Planet) {
        val averageContinentalHeight = planet.planetTiles.values
            .filter { it.isContinentalCrust }
            .map { it.elevation }
            .average()

        val percentContinental =
            planet.planetTiles.values.filter { it.isAboveWater }.size / planet.planetTiles.size.toFloat()

        if (averageContinentalHeight <= minAverageContinentalHeightGuardrail &&
            percentContinental <= max(0.03, desiredLandPercent - guardrailStrictness)
        ) {
            GD.print("raising elevation — ${averageContinentalHeight}m & ${(percentContinental * 100).formatDigits()}%")
            planet.planetTiles.values.forEach { it.elevation += 100 }
        }

        if (averageContinentalHeight >= maxAverageContinentalHeightGuardrail &&
            percentContinental >= min(0.97, desiredLandPercent + guardrailStrictness)
        ) {
            GD.print("lowering elevation — ${averageContinentalHeight}m & ${(percentContinental * 100).formatDigits()}%")
            planet.planetTiles.values.forEach { it.elevation -= 100 }
        }
    }
}
