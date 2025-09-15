package dev.biserman.planet.planet

import com.esotericsoftware.kryo.DefaultSerializer
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.scaleAndCoerceUnit
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.planet.TectonicGlobals.tileInertia
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.NoArg
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import dev.biserman.planet.utils.memo
import godot.core.Vector3
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

@DefaultSerializer(TaggedFieldSerializer::class)
@NoArg
class PlanetTile(
    val planet: Planet,
    var tile: Tile,
) {
    val density get() = -elevation.scaleAndCoerceUnit(-5000.0..5000.0)
    val temperature get() = 1 - tile.position.y.absoluteValue
    var moisture = 0.0
    var elevation = -100000.0 // set it really low to make errors easier to see
    val elevationAboveSeaLevel get() = max(elevation - planet.seaLevel, 0.0)
    var movement: Vector3 = Vector3.ZERO

    var formationTime = planet.tectonicAge

    var erosionDelta: Double = 0.0
    var springDisplacement: Vector3 = Vector3.ZERO

    var tectonicPlate: TectonicPlate? = null
        set(value) {
            field?.tiles?.remove(this)
            value?.tiles?.add(this)
            field = value
        }

    val isContinental get() = elevation >= TectonicGlobals.continentElevationCutoff
    val isAboveWater get() = elevation > planet.seaLevel

    @delegate:Transient
    val contiguousSlope by memo({ planet.tectonicAge }) {
        sqrt(neighbors.filter { it.isAboveWater == isAboveWater }.map { (it.elevation - elevation).pow(2) }.average())
    }
    @delegate:Transient
    val nonContiguousSlope by memo({ planet.tectonicAge }) {
        sqrt(neighbors.filter { it.isAboveWater != isAboveWater }.map { (it.elevation - elevation).pow(2) }.average())
    }
    @delegate:Transient
    val slope by memo({ planet.tectonicAge }) { sqrt(neighbors.map { (it.elevation - elevation).pow(2) }.average()) }
    @delegate:Transient
    val prominence by memo({ planet.tectonicAge }) {
        val computed =
            sqrt(
                neighbors
                    .filter { it.elevationAboveSeaLevel < elevationAboveSeaLevel }
                    .map { (it.elevationAboveSeaLevel - elevationAboveSeaLevel).pow(2) }
                    .average())

        if (computed.isNaN()) 0.0 else computed
    }

    val neighbors get() = tile.tiles.mapNotNull { planet.planetTiles[it] }

    constructor(other: PlanetTile) : this(
        other.planet, other.tile
    ) {
        this.elevation = other.elevation
//        this.temperature = other.temperature
        this.moisture = other.moisture
        this.tectonicPlate = other.tectonicPlate
        this.movement = other.movement
        this.springDisplacement = other.springDisplacement
        this.formationTime = other.formationTime
        this.erosionDelta = other.erosionDelta
    }

    fun planetInit() {
        elevation = Main.noise.startingElevation.getNoise3dv(tile.averagePosition)
            .toDouble()
            .adjustRange(-1.0..1.0, -5000.0..5000.0)
//        elevation = 0.0
    }

    @delegate:Transient
    val tectonicBoundaries by memo({ planet.tectonicAge }) {
        tile.borders.filter { border ->
            val otherPlate = planet.planetTiles[border.oppositeTile(tile)]?.tectonicPlate
            otherPlate != tectonicPlate
        }
    }

    @delegate:Transient
    val isTectonicBoundary by memo({ planet.tectonicAge }) { tectonicBoundaries.isNotEmpty() }

    fun copy(): PlanetTile = PlanetTile(this)

    fun updateMovement() {
        if (tectonicPlate == null) {
            return
        }

        val idealMovement = // plateBoundaryForces * 0.1 +
            tectonicPlate!!.eulerPole.cross(tile.position)
        movement = (movement * tileInertia + idealMovement).tangent(tile.position)
    }

    fun oppositeTile(border: Border) = planet.planetTiles[border.oppositeTile(tile)]

    fun floodFill(
        visited: MutableSet<PlanetTile> = mutableSetOf(),
        planetTileFn: (Tile) -> PlanetTile = { planet.planetTiles[it]!! },
        filterFn: (PlanetTile) -> Boolean
    ): Set<PlanetTile> {
        val visited = mutableSetOf<PlanetTile>()
        val found = mutableSetOf<PlanetTile>()
        val queue = ArrayDeque<PlanetTile>()
        queue.add(this)
        visited.add(this)
        if (filterFn(this)) {
            found.add(this)
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbor in current.tile.tiles) {
                val neighborPlanetTile = planetTileFn(neighbor)
                if (visited.contains(neighborPlanetTile)) {
                    continue
                }
                if (filterFn(neighborPlanetTile)) {
                    queue.addLast(neighborPlanetTile)
                    visited.add(neighborPlanetTile)
                    found.add(neighborPlanetTile)
                }
            }
        }

        return found
    }

    fun getInfoText(): String = """
        elevation: ${elevation.formatDigits()}
        temperature: ${temperature.formatDigits()}
        moisture: ${moisture.formatDigits()}
        movement: ${movement.formatDigits()} (${movement.length().formatDigits()})
        position: ${tile.position.formatDigits()}
        divergence: ${planet.divergenceZones[tile]?.strength?.formatDigits() ?: 0.0}
        subduction: ${planet.convergenceZones[tile]?.speed?.formatDigits() ?: 0.0}
        erosion: ${erosionDelta.formatDigits()}
        slope: ${slope.formatDigits()} (${contiguousSlope.formatDigits()}|${nonContiguousSlope.formatDigits()})
        prominence: ${prominence.formatDigits()}
        formation time: $formationTime
        plate: ${tectonicPlate?.name ?: "null"}
        hotspot: ${planet.noise.hotspots.sample4d(tile.position, planet.tectonicAge.toDouble()).formatDigits()}
    """.trimIndent() + if (planet.convergenceZones.contains(tile)) {
        val convergenceZone = planet.convergenceZones[tile]!!
        "\n" + """
        CONVERGENCE
        speed: ${convergenceZone.speed.formatDigits()}
        strength: ${convergenceZone.subductionStrengths.values.average().formatDigits()}
        subducting plates: ${convergenceZone.subductingPlates.size}
        subducting mass: ${convergenceZone.subductingMass.formatDigits()}
        """.trimIndent()
    } else ""

    companion object {
        fun <T> (Collection<PlanetTile>).floodFillGroupBy(
            planetTileFn: ((Tile) -> PlanetTile)? = null, keyFn: (PlanetTile) -> T
        ): Map<T, List<Set<PlanetTile>>> {
            val visited = mutableSetOf<PlanetTile>()
            val results = mutableMapOf<T, MutableList<Set<PlanetTile>>>()

            for (tile in this) {
                if (visited.contains(tile)) {
                    continue
                }
                val tileValue = keyFn(tile)
                val found = if (planetTileFn == null) {
                    tile.floodFill { keyFn(it) == tileValue }
                } else {
                    tile.floodFill(planetTileFn = planetTileFn) {
                        keyFn(it) == tileValue
                    }
                }
                visited.addAll(found)
                results[tileValue] = (results[tileValue] ?: mutableListOf()).also { it.add(found) }
            }

            return results
        }
    }
}