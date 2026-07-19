package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import dev.biserman.planet.geometry.*
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.OceanCurrent
import dev.biserman.planet.planet.ecology.TaxonomicOrder
import dev.biserman.planet.planet.tectonics.ConvergenceZone
import dev.biserman.planet.planet.tectonics.DivergenceZone
import dev.biserman.planet.planet.tectonics.TectonicGlobals.biotaDistributionCount
import dev.biserman.planet.planet.tectonics.TectonicPlate
import dev.biserman.planet.planet.tectonics.Tectonics
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.topology.toTopology
import dev.biserman.planet.utils.AStar
import dev.biserman.planet.utils.UtilityExtensions.contains
import dev.biserman.planet.utils.memo
import kotlin.random.Random
import dev.biserman.planet.utils.VectorWarpNoise
import dev.biserman.planet.utils.floodFillPartitionForest
import godot.core.Vector3
import kotlin.collections.average
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class, property = "id")
class Planet(val seed: Int, val size: Int) {
    @JsonIgnore
    val random = Random(seed)

    @JsonIgnore
    val noise = NoiseMaps(seed, random)

    @JsonIgnore
    val topology = makeTopology(size)

    var worldKinds = WorldKinds().also {
        it.generateStoneTypes(this)
    }

    var planetTiles = topology.tiles.associate { tile -> tile.id to PlanetTile(this, tile.id) }

    @get:JsonIgnore
    val contiguousRegions by memo({ terrainChangeCount }) {
        PlanetRegion(
            this,
            planetTiles.values.toMutableSet()
        ).floodFillGroupBy { it.isAboveWater || it.isIceCap }.flatMap { it.value }
    }

    @get:JsonIgnore
    val landRegions by memo({ terrainChangeCount }) {
        PlanetRegion(
            this,
            planetTiles.values.toMutableSet()
        ).floodFillGroupBy { it.isAboveWater }[true] ?: emptyList()
    }

    @get:JsonIgnore
    val edgeDepthMap by memo({ terrainChangeCount }) {
        PlanetRegion(this, planetTiles.values.toMutableSet()).calculateEdgeDepthMap { it.isAboveWater }
    }

    @get:JsonIgnore
    val riverBasinMap by memo({ terrainChangeCount }) {
        val pointElevations = planetTiles.values.flatMap { it.tile.corners }
            .distinctBy { it.position }
            .associateWith { it.tiles.map { tile -> getTile(tile).elevation }.average() }

        val riverSegments =
            pointElevations.keys
                .map { it to it.corners.minBy { corner -> pointElevations[corner]!! } }
                .filter { riverSegment ->
                    riverSegment.toList()
                        .count { corner -> corner.tiles.any { !planetTiles[it.id]!!.isAboveWater } } <= 1
                }

        val riverPoints = riverSegments.flatMap { it.toList() }.sortedBy { pointElevations[it] }

        val riverBasins = floodFillPartitionForest(riverPoints) { node ->
            node.corners.filter { otherNode ->
                riverSegments.any { node in it && otherNode in it }
            }
        }.mapValues { (_, basin) ->
            basin.flatMap { riverNode ->
                riverSegments.filter { it.contains(riverNode) }
            }.toSet()
        }

        riverBasins
    }

    @get:JsonIgnore
    val riverUpstreamSegmentCounts by memo({ terrainChangeCount }) {
        upstreamSegmentCounts(riverBasinMap.values.flatten().toSet())
    }

    @get:JsonIgnore
    val continentialityMap by memo({ terrainChangeCount }) {
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

        planetTiles.values.associate { tile ->
            val edgeDepth = continentEdgeDepth[tile]
            tile.tileId to when {
                edgeDepth == null -> null
                isContinental[tile]!! -> edgeDepth
                else -> -edgeDepth - 1
            }
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

    val hotspotActivity by memo({ tectonicAge }) {
        planetTiles.values.sumOf {
            noise.hotspots.sample4d(
                it.tile.position,
                tectonicAge.toDouble()
            )
        }
    }

    val pointNemo by memo({ terrainChangeCount }) {
        planetTiles.values.minBy { it.continentiality }
    }

    val internationalDateLine by memo({ terrainChangeCount }) {
        bestOceanCorridorDateLineDegrees(
            projectedMeridianLandCoverage(),
            planetTiles.values
                .filter { it.isAboveWater }
                .map { it.tile.position.toGeoPoint().longitudeDegrees to it.tile.area }
        ) * PI / 180.0
    }

    private fun projectedMeridianLandCoverage(): IntArray {
        val sampleRadius = topology.averageRadius * 1.5
        return IntArray(DATE_LINE_SAMPLE_WIDTH) { column ->
            val longitude = column.toDouble() * 360.0 / DATE_LINE_SAMPLE_WIDTH
            (0..<DATE_LINE_SAMPLE_HEIGHT).count { row ->
                val latitude = (0.5 - (row + 0.5) / DATE_LINE_SAMPLE_HEIGHT) * 180.0
                val samplePoint = GeoPoint.fromDegrees(latitude, longitude).toVector3()
                val nearestTile = topology.rTree
                    .nearest(samplePoint.toPoint(), sampleRadius, 1)
                    .firstOrNull()
                    ?.value()
                nearestTile != null && getTile(nearestTile).isAboveWater
            }
        }
    }

    val waterCoverage by memo({ terrainChangeCount }) {
        1.0 - planetTiles.values.filter { it.isAboveWater }.size / planetTiles.size.toDouble()
    }

    val rotationRate = 1.0

    fun getTile(tile: Tile) = planetTiles[tile.id]!!

    companion object {
        private const val DATE_LINE_SAMPLE_WIDTH = 720
        private const val DATE_LINE_SAMPLE_HEIGHT = 360
    }

    var planetStats = PlanetStats()

    var tectonicPlates: MutableList<TectonicPlate>

    var convergenceZones: MutableMap<Int, ConvergenceZone> = mutableMapOf()
    var divergenceZones: MutableMap<Int, DivergenceZone> = mutableMapOf()

    var oceanCurrents: MutableMap<Int, OceanCurrent> = mutableMapOf()

    var biotaDistributions: List<BiotaDistribution> = listOf()

    var tectonicAge = 0
        set(value) {
            field = value
            Gui.instance.tectonicAgeLabel.setText("$tectonicAge My")
        }

    var daysPassed = 0
    var historyTurn = 0L
    var nextPlateId = 0

    var seaLevel: Double = 0.0

    val radiusMeters = 6378137.0
    var terrainChangeCount: ULong = 0uL

    val oldestCrust by memo({ tectonicAge }) { planetTiles.values.minOf { it.formationTime } }
    val youngestCrust by memo({ tectonicAge }) { planetTiles.values.maxOf { it.formationTime } }

    var lastMeteorImpact = 0

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
        val assignedBiotaOrders = mutableSetOf<TaxonomicOrder>()
        biotaDistributions = (1..biotaDistributionCount).map {
            BiotaDistribution.random(this, assignedBiotaOrders).also { distribution ->
                assignedBiotaOrders += distribution.taxonomicOrder
            }
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

fun <T> upstreamSegmentCounts(segments: Collection<Pair<T, T>>): Map<Pair<T, T>, Int> {
    val upstreamByPoint = segments.groupBy { it.second }
    val counts = mutableMapOf<Pair<T, T>, Int>()

    fun countUpstream(segment: Pair<T, T>, active: MutableSet<Pair<T, T>>): Int {
        counts[segment]?.let { return it }
        if (!active.add(segment)) return 0

        val count = upstreamByPoint[segment.first].orEmpty()
            .filter { it != segment }
            .sumOf { upstream -> 1 + countUpstream(upstream, active) }
        active.remove(segment)
        counts[segment] = count
        return count
    }

    segments.forEach { countUpstream(it, mutableSetOf()) }
    return counts
}
