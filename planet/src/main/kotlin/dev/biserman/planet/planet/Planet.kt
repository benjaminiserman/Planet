package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import dev.biserman.planet.geometry.*
import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.OceanCurrent
import dev.biserman.planet.planet.tectonics.ConvergenceZone
import dev.biserman.planet.planet.tectonics.DivergenceZone
import dev.biserman.planet.planet.tectonics.TectonicPlate
import dev.biserman.planet.planet.tectonics.Tectonics
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.topology.toTopology
import dev.biserman.planet.utils.Path
import dev.biserman.planet.utils.memo
import kotlin.random.Random
import dev.biserman.planet.utils.VectorWarpNoise

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "id")
class Planet(val seed: Int, val size: Int) {
    @JsonIgnore
    val random = Random(seed)

    @JsonIgnore
    val noise = NoiseMaps(seed, random)

    @JsonIgnore
    val topology = makeTopology(size)

    var planetTiles = topology.tiles.associate { tile -> tile.id to PlanetTile(this, tile.id) }

    @get:JsonIgnore
    val contiguousRegions by memo({ tectonicAge }) {
        PlanetRegion(
            this,
            planetTiles.values.toMutableSet()
        ).floodFillGroupBy { it.isAboveWater || it.isIceCap }.flatMap { it.value }
    }

    @get:JsonIgnore
    val edgeDepthMap by memo({ tectonicAge }) {
        PlanetRegion(this, planetTiles.values.toMutableSet()).calculateEdgeDepthMap { it.isAboveWater }
    }

    @get:JsonIgnore
    val continentialityMap by memo({ tectonicAge }) {
        val tilesToFlip = contiguousRegions
            .filter { region -> region.tiles.maxOf { it.edgeDepth } < 1 }
            .flatMap { it.tiles }
            .toSet()

        val isContinental = planetTiles.values.associateWith { tile ->
            if (tile in tilesToFlip) {
                !tile.isAboveWater
            } else {
                tile.isAboveWater
            }
        }

        val continentEdgeDepth =
            PlanetRegion(this, planetTiles.values.toMutableSet()).calculateEdgeDepthMap { isContinental[it]!! }

        planetTiles.values.associateWith { tile ->
            val edgeDepth = continentEdgeDepth[tile]!!
            if (isContinental[tile]!!) edgeDepth else -edgeDepth - 1
        }
    }

    var warmCurrentDistanceMap: Map<Int, Int> = mapOf()
    var coolCurrentDistanceMap: Map<Int, Int> = mapOf()
    var climateMap: Map<Int, ClimateDatum> = mapOf()
    var itczDistanceMap: Map<Int, Int> = mapOf()

    val warpNoise by memo({ tectonicAge }) {
        VectorWarpNoise(
            tectonicAge,
            3f
        )
    }

    val rotationRate = 1.0

    fun getTile(tile: Tile) = planetTiles[tile.id]!!

    var planetStats = PlanetStats()

    var tectonicPlates: MutableList<TectonicPlate>

    var convergenceZones: MutableMap<Int, ConvergenceZone> = mutableMapOf()
    var divergenceZones: MutableMap<Int, DivergenceZone> = mutableMapOf()

    var oceanCurrents: MutableMap<Int, OceanCurrent> = mutableMapOf()

    var tectonicAge = 0
    var daysPassed = 0
    var nextPlateId = 0

    var seaLevel: Double = 0.0

    val radiusMeters = 6378137.0

    val oldestCrust by memo({ tectonicAge }) { planetTiles.values.minOf { it.formationTime } }

    val youngestCrust by memo({ tectonicAge }) { planetTiles.values.maxOf { it.formationTime } }

    init {
        planetTiles.values.forEach {
            it.planetInit()
        }
        tectonicPlates = Tectonics.seedPlates(this, random.nextInt(10, 15))
        Tectonics.voronoiPlates(this)
        Tectonics.assignDensities(this)
        tectonicPlates.forEach { plate ->
            plate.torque = noise.mantleConvection.sample4d(plate.region.tiles.first().tile.position, 0.0)
        }
    }

    fun makeTopology(degree: Int): Topology {
        val icosahedron = makeIcosahedron()
        val sub = icosahedron.subdivideIcosahedron(degree)
        sub.distortTriangles(0.5)
        sub.relaxRepeatedly(500)
        sub.reorderVerts()
        return sub.toTopology()
    }
}
