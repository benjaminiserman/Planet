package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.torque
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.toWeightedBag
import godot.common.util.lerp

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
        patchHoles(planet)
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

    fun patchHoles(planet: Planet) {
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
            val averageDensity = (plate.tiles.sumOf { it.density.toDouble() } / plate.tiles.size).toFloat()
            val adjustedDensity = lerp(averageDensity, random.nextDouble(-1.0, 0.5).toFloat(), 0.75f)

            plate.tiles.forEach {
                it.elevation = lerp(it.elevation, adjustedDensity * 1000, 0.33f)
            }
        }
    }

    fun stepTectonicsSimulation(planet: Planet) {
        planet.tectonicPlates.forEach { plate ->
            val oldTorqueWithDrag = plate.lastTorque * plate.basalDrag
            val mantleConvectionTorque = torque(plate.tiles.map { tile ->
                Pair(
                    tile.tile.position,
                    planet.noise.mantleConvection.sample4d(tile.tile.position, planet.tectonicAge.toDouble())
                )
            })
            val slabPull = torque(planet.subductionZones.filter { it.tectonicPlate == plate }.map { tile ->
                Pair(
                    tile.tile.position,
                    (tile.tile.position - tile.tectonicPlate!!.region.center).normalized() * tile.tile.area
                )
            })
            val ridgePush = torque(planet.divergenceZones.filter { it.tectonicPlate == plate }.map { tile ->
                Pair(
                    tile.tile.position,
                    (tile.tectonicPlate!!.region.center - tile.tile.position).normalized() * tile.tile.area
                )
            })


        }


        planet.planetTiles.values.forEach {
            it.updateMovement()
        }
    }
}