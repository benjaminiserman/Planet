package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import dev.biserman.planet.Main
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.scaleAndCoerceUnit
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.planet.TectonicGlobals.tileInertia
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import dev.biserman.planet.utils.memo
import godot.core.Vector3
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@JsonIdentityInfo(
    generator = ObjectIdGenerators.IntSequenceGenerator::class,
    scope = PlanetTile::class,
    property = "id"
)
class PlanetTile(
    val planet: Planet,
    var tileId: Int
) {
    @get:JsonIgnore
    var tile
        get() = planet.topology.tiles[tileId]
        set(value) {
            tileId = value.id
        }

    val density get() = -elevation.scaleAndCoerceUnit(-5000.0..5000.0)
    val temperature get() = 1 - tile.position.y.absoluteValue
    var moisture = 0.0
    var elevation = -100000.0 // set it really low to make errors easier to see

    @get:JsonIgnore
    val elevationAboveSeaLevel get() = max(elevation - planet.seaLevel, 0.0)
    var movement: Vector3 = Vector3.ZERO

    var edgeResistance: Vector3 = Vector3.ZERO
    var edgePush: Vector3 = Vector3.ZERO

    var formationTime = planet.tectonicAge

    var erosionDelta: Double = 0.0
    var springDisplacement: Vector3 = Vector3.ZERO

    var tectonicPlate: TectonicPlate? = null
        set(value) {
            field?.tiles?.remove(this)
            value?.tiles?.add(this)
            field = value
        }

    @get:JsonIgnore
    val insolation
        get() = Insolation.directHorizontal(
            planet.tectonicAge % Insolation.yearLength.toInt(),
            tile.position.toGeoPoint().latitude
        )

    @get:JsonIgnore
    val isContinental get() = elevation >= TectonicGlobals.continentElevationCutoff

    @get:JsonIgnore
    val isAboveWater get() = elevation > planet.seaLevel

    @get:JsonIgnore
    val contiguousSlope by memo({ planet.tectonicAge }) {
        sqrt(neighbors.filter { it.isAboveWater == isAboveWater }.map { (it.elevation - elevation).pow(2) }.average())
    }

    @get:JsonIgnore
    val nonContiguousSlope by memo({ planet.tectonicAge }) {
        sqrt(neighbors.filter { it.isAboveWater != isAboveWater }.map { (it.elevation - elevation).pow(2) }.average())
    }

    @get:JsonIgnore
    val slope by memo({ planet.tectonicAge }) { sqrt(neighbors.map { (it.elevation - elevation).pow(2) }.average()) }

    @get:JsonIgnore
    val prominence by memo({ planet.tectonicAge }) {
        val computed =
            sqrt(
                neighbors
                    .filter { it.elevationAboveSeaLevel < elevationAboveSeaLevel }
                    .map { (it.elevationAboveSeaLevel - elevationAboveSeaLevel).pow(2) }
                    .average())

        if (computed.isNaN()) 0.0 else computed
    }

    @get:JsonIgnore
    val neighbors get() = tile.tiles.map { planet.getTile(it) }

    var edgeDepth: Int = -1

    constructor(other: PlanetTile) : this(
        other.planet, other.tile.id
    ) {
        this.elevation = other.elevation
//        this.temperature = other.temperature
        this.moisture = other.moisture
        this.tectonicPlate = other.tectonicPlate
        this.movement = other.movement
        this.springDisplacement = other.springDisplacement
        this.edgeResistance = other.edgeResistance
        this.edgePush = other.edgePush
        this.formationTime = other.formationTime
        this.erosionDelta = other.erosionDelta
    }

    fun planetInit() {
        elevation = planet.noise.startingElevation.getNoise3dv(tile.averagePosition)
            .toDouble()
            .adjustRange(-1.0..1.0, -5000.0..3500.0)
//        elevation = 0.0
    }

    @get:JsonIgnore
    val tectonicBoundaries by memo({ planet.tectonicAge }) {
        tile.borders.filter { border ->
            val otherPlate = planet.getTile(border.oppositeTile(tile)).tectonicPlate
            otherPlate != tectonicPlate
        }
    }

    @get:JsonIgnore
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

    fun getEdgeForces() =
        neighbors
            .filter { it.tectonicPlate != tectonicPlate }
            .map { otherTile ->
                val delta = tile.position - otherTile.tile.position
//                val movementDelta = otherTile.movement - movement
                val force = max(0.0, otherTile.movement.dot(delta))
                val densityDiff = min((otherTile.density - density).absoluteValue * 2, 1.0)
                val thisDensityFactor = min((-(density * 2) + 1), 1.0)
                delta.normalized() * force * (1 - densityDiff) * thisDensityFactor
            }

    fun oppositeTile(border: Border) = planet.getTile(border.oppositeTile(tile))

    fun floodFill(
        visited: MutableSet<PlanetTile> = mutableSetOf(),
        planetTileFn: (Tile) -> PlanetTile = { planet.getTile(it) },
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

    @JsonIgnore
    fun getInfoText(): String = """
        elevation: ${elevation.formatDigits()} (density: ${density.formatDigits()})
        temperature: ${temperature.formatDigits()}
        moisture: ${moisture.formatDigits()}
        movement: ${movement.formatDigits()} (${movement.length().formatDigits()})
        position: ${tile.position.formatDigits()}
        spring displacement: ${springDisplacement.formatDigits()}
        edge resistance: ${edgeResistance.formatDigits()}
        divergence: ${planet.divergenceZones[tile.id]?.strength?.formatDigits() ?: 0.0}
        subduction: ${planet.convergenceZones[tile.id]?.speed?.formatDigits() ?: 0.0}
        erosion: ${erosionDelta.formatDigits()}
        slope: ${slope.formatDigits()} (${contiguousSlope.formatDigits()}|${nonContiguousSlope.formatDigits()})
        prominence: ${prominence.formatDigits()}
        formation time: $formationTime
        plate: ${tectonicPlate?.name ?: "null"}
        insolation: ${insolation.formatDigits()}
        edge depth: $edgeDepth
        hotspot: ${planet.noise.hotspots.sample4d(tile.position, planet.tectonicAge.toDouble()).formatDigits()}
    """.trimIndent() + if (planet.convergenceZones.contains(tile.id)) {
        val convergenceZone = planet.convergenceZones[tile.id]!!
        "\n" + """
        CONVERGENCE
        speed: ${convergenceZone.speed.formatDigits()}
        strength: ${convergenceZone.subductionStrengths[tectonicPlate!!.id]!!.formatDigits()}
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