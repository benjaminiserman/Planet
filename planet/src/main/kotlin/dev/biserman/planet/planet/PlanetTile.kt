package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.geometry.scaleAndCoerceUnit
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.planet.ClimateSimulation.bands
import dev.biserman.planet.planet.TectonicGlobals.tileInertia
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import dev.biserman.planet.utils.UtilityExtensions.signPow
import dev.biserman.planet.utils.memo
import dev.biserman.planet.utils.sum
import godot.common.util.lerp
import godot.core.Color
import godot.core.Vector3
import kotlin.math.absoluteValue
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
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
    var temperature = 0.0
    var moisture = 0.0
    var elevation = -100000.0 // set it really low to make errors easier to see
    val airPressure by memo({ planet.tectonicAge }, { planet.daysPassed }) {
        val latitude = tile.position.toGeoPoint().latitudeDegrees
        val nearestBandAbove = bands.last { it.latitude >= latitude }
        val nearestBandBelow = bands.first { it.latitude <= latitude }

        val adjustedContinentiality = continentiality.toDouble() + 2.0

        val seasonalAdjustment = -(2 / (1 + exp(
            -(insolation - 0.6).signPow(1.01) * (adjustedContinentiality.absoluteValue + 1).pow(2.0) * 0.05
        )) - 1) * adjustedContinentiality.scaleAndCoerceIn(-2.0..2.0, 5.0..15.0)

        val elevationAdjustment = if (elevation < 2000) 0.0 else (elevation - 2000) * 0.002

        val latitudeAdjustment =
            tile.position.y.absoluteValue.pow(2).scaleAndCoerceIn(
                0.0..1.0,
                -5.0..2.0
            ) * (1 / (1 + exp(-adjustedContinentiality.signPow(0.5) * 0.3)))

        ClimateSimulation.basePressure + lerp(
            nearestBandBelow.pressureDelta,
            nearestBandAbove.pressureDelta,
            (latitude - nearestBandBelow.latitude) / (nearestBandAbove.latitude - nearestBandBelow.latitude)
        ) + seasonalAdjustment + latitudeAdjustment + elevationAdjustment
    }
    val prevailingWind by memo({ planet.tectonicAge }, { planet.daysPassed }) {
        val pressureGradientForce = neighbors
            .filter { it.airPressure < airPressure }
            .map { (it.tile.position - tile.position).tangent(tile.position) * (airPressure - it.airPressure) * 0.1 }
            .sum()

        val latitudeRadians = asin(tile.position.y)
        val coriolisParameter = 2.0 * planet.rotationRate * -sin(latitudeRadians)

        // Deflection is perpendicular to both motion and local vertical
        val coriolisDeflection = tile.position.cross(pressureGradientForce).normalized() *
                coriolisParameter * pressureGradientForce.length()

        val direction = pressureGradientForce + coriolisDeflection
        direction.normalized() * direction.length()
            .coerceIn(planet.topology.averageRadius * 0.25, planet.topology.averageRadius * 0.66)
    }

    val isIceCap
        get() = elevation >= (1 - tile.position.y.absoluteValue).pow(0.5) * 6500 ||
                planet.warpNoise.warp(
                    tile.position,
                    0.075
                ).y.absoluteValue >= 0.95

    @get:JsonIgnore
    val elevationAboveSeaLevel get() = max(elevation - planet.seaLevel, 0.0)
    var movement: Vector3 = Vector3.ZERO

    var edgeResistance: Vector3 = Vector3.ZERO
    var edgePush: Vector3 = Vector3.ZERO

    var formationTime = planet.tectonicAge

    var erosionDelta: Double = 0.0
    var springDisplacement: Vector3 = Vector3.ZERO

    var depositFlow: Double = 0.0
    var waterFlow: Double = 0.0

    var debugColor: Color = Color.black

    var tectonicPlate: TectonicPlate? = null
        set(value) {
            field?.tiles?.remove(this)
            value?.tiles?.add(this)
            field = value
        }

    @get:JsonIgnore
    val insolation
        get() = Insolation.directHorizontal(
            planet.daysPassed % Insolation.yearLength,
            tile.position.toGeoPoint().latitude
        )

    @get:JsonIgnore
    val isContinentalCrust get() = elevation > TectonicGlobals.continentElevationCutoff

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

    @get:JsonIgnore
    val edgeDepth get() = planet.edgeDepthMap[this]!!

    @get:JsonIgnore
    val continentiality get() = planet.continentialityMap[this]!!

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
        this.depositFlow = other.depositFlow
        this.waterFlow = other.waterFlow
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

    fun slopeAboveWaterTo(other: PlanetTile) = max(planet.seaLevel, other.elevation) - max(planet.seaLevel, elevation)

    fun floodFill(
        visited: MutableSet<PlanetTile> = mutableSetOf(),
        planetTileFn: (Tile) -> PlanetTile? = { planet.getTile(it) },
        planetRegion: PlanetRegion? = null,
        filterFn: (PlanetTile, Set<PlanetTile>) -> Boolean,
    ): Set<PlanetTile> {
        val visited = mutableSetOf<PlanetTile>()
        val found = mutableSetOf<PlanetTile>()
        val queue = ArrayDeque<PlanetTile>()
        queue.add(this)
        visited.add(this)
        if (filterFn(this, found)) {
            found.add(this)
        }

        val wrappedGetPlanetTile = if (planetRegion != null) ({ tile: Tile ->
            val planetTile = planetTileFn(tile)
            if (planetTile in planetRegion.tiles) {
                planetTile
            } else {
                null
            }
        }) else planetTileFn


        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbor in current.tile.tiles) {
                val neighborPlanetTile = wrappedGetPlanetTile(neighbor) ?: continue
                if (visited.contains(neighborPlanetTile)) {
                    continue
                }
                if (filterFn(neighborPlanetTile, found)) {
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
        elevation: ${elevation.formatDigits()}m (density: ${density.formatDigits()})
        temperature: ${temperature.formatDigits()}
        moisture: ${moisture.formatDigits()}
        movement: ${movement.formatDigits()} (${movement.length().formatDigits()})
        position: ${tile.position.formatDigits()} (${tile.position.toGeoPoint().formatDigits()})
        spring displacement: ${springDisplacement.formatDigits()}
        edge resistance: ${edgeResistance.formatDigits()}
        divergence: ${planet.divergenceZones[tile.id]?.strength?.formatDigits() ?: 0.0}
        subduction: ${planet.convergenceZones[tile.id]?.speed?.formatDigits() ?: 0.0}
        erosion delta: ${erosionDelta.formatDigits()}m
        slope: ${slope.formatDigits()} (${contiguousSlope.formatDigits()}|${nonContiguousSlope.formatDigits()})
        prominence: ${prominence.formatDigits()}
        formation time: $formationTime My
        plate: ${tectonicPlate?.name ?: "null"}
        insolation: ${insolation.formatDigits()}
        edge depth: $edgeDepth tiles
        continentiality: $continentiality tiles
        hotspot: ${planet.noise.hotspots.sample4d(tile.position, planet.tectonicAge.toDouble()).formatDigits()}
        deposit flow: ${depositFlow.formatDigits()}
        water flow: ${waterFlow.formatDigits()}
        airPressure: ${airPressure.formatDigits()}
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
}