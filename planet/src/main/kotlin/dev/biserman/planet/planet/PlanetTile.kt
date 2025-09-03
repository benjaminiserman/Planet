package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.sigmoid
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.memo
import godot.core.Vector3
import godot.global.GD
import kotlin.compareTo
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class PlanetTile(
    val planet: Planet,
    var tile: Tile,
) {
    val density get() = -elevation.adjustRange(-5000.0..5000.0, -1.0..1.0).coerceIn(-1.0..1.0)
    var temperature = 0.0
    var moisture = 0.0
    var elevation = -100000.0 // set it really low to make errors easier to see
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

    val tectonicSprings by memo({ planet.tectonicAge }) {
        tile.tiles.map {
            val planetTile = planet.planetTiles[it]!!
//            val stiffness = if (isContinental && planetTile.isContinental) 4.0 else 0.1
            planetTile.tile to 0.0
        }
    }

    val slope get() = sqrt(neighbors.map { (it.elevation - elevation).pow(2) }.average())
    val prominence: Double
        get() {
            val computed = sqrt(neighbors.filter { it.elevation < elevation }
                .map { (it.elevation - elevation).pow(2) }
                .average())

            return if (computed.isNaN()) 0.0 else computed
        }

    val neighbors get() = tile.tiles.mapNotNull { planet.planetTiles[it] }

    constructor(other: PlanetTile) : this(
        other.planet,
        other.tile
    ) {
        this.elevation = other.elevation
        this.temperature = other.temperature
        this.moisture = other.moisture
        this.tectonicPlate = other.tectonicPlate
        this.movement = other.movement
        this.springDisplacement = other.springDisplacement
        this.formationTime = other.formationTime
        this.erosionDelta = other.erosionDelta
    }

    fun planetInit() {
        elevation = Main.noise.startingElevation
            .getNoise3dv(tile.averagePosition)
            .toDouble()
            .adjustRange(-1.0..1.0, -5000.0..5000.0)
    }

    val tectonicBoundaries by memo({ planet.tectonicAge }) {
        tile.borders.filter { border ->
            planet.planetTiles[border.oppositeTile(tile)]?.tectonicPlate != tectonicPlate
        }
    }

    val isTectonicBoundary by memo({ planet.tectonicAge }) { tectonicBoundaries.isNotEmpty() }

    fun copy(): PlanetTile = PlanetTile(this)

    val plateBoundaryForces by memo({ planet.tectonicAge }) {
        if (tectonicPlate == null) {
            return@memo Vector3.ZERO
        }

        tile.borders.fold(Vector3.ZERO) { sum, border ->
            val neighborTile = border.oppositeTile(tile)
            val neighborPlanetTile = planet.planetTiles[neighborTile]!!
            if (neighborPlanetTile.tectonicPlate == tectonicPlate) {
                return@fold sum
            }

//            val rawScalar = ((neighborPlanetTile.density - density) / 2.0) * -1
//
//            // desmos: \frac{2}{1+e^{-20\left(x-1.2\right)}}-1
//            // this scales between back-arc spreading and trench pull based on relative plate density
//            val backArcSpreadingX = (neighborPlanetTile.density + 1) / (density + 1.001)
//            val backArcSpreading = 2.0 / (1 + E.pow(-20 * (backArcSpreadingX - 1.15))) - 1
//
//            val finalScalar = if (rawScalar < 0.0) backArcSpreading * 0.05 else rawScalar
//
//            return@fold sum + (neighborTile.position - tile.position) * finalScalar * border.length

            // desmos: -\left(\left(\frac{1}{1+e^{\left(-3x-2\right)}}+\frac{1}{1+e^{\left(3x-2\right)}}\right)-1.38\right)
            val minDensity = max(neighborPlanetTile.density, density).adjustRange(-1.0..1.0, 0.0..1.0)
            val densityDiff = (neighborPlanetTile.density - density)
            val attraction =
                -2 * (sigmoid(densityDiff, -60.0, -2.0) + sigmoid(
                    densityDiff,
                    60.0,
                    -2.0
                ) - 1.2f) * (1 - minDensity).pow(2)
            return@fold sum + (neighborTile.position - tile.position) * border.length * attraction
        }
    }

    fun updateMovement() {
        if (tectonicPlate == null) {
            return
        }

        val idealMovement = // plateBoundaryForces * 0.1 +
            tectonicPlate!!.eulerPole.cross(tile.position)
        movement = (movement * (tectonicPlate?.basalDrag ?: 0.0) + idealMovement).tangent(tile.position)
        if (movement.length() > 0.15) {
            GD.printErr("movement is too big: ${movement.length()}")
        }
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

    fun (Double).formatDigits(digits: Int = 2) = "%.${digits}f".format(this)
    fun (Float).formatDigits(digits: Int = 2) = "%.${digits}f".format(this)
    fun (Vector3).formatDigits(digits: Int = 2) = "(%.${digits}f, %.${digits}f, %.${digits}f)".format(x, y, z)
    fun getInfoText(): String = """
        elevation: ${elevation.formatDigits()}
        temperature: ${temperature.formatDigits()}
        moisture: ${moisture.formatDigits()}
        movement: ${movement.formatDigits()} (${movement.length().formatDigits()})
        position: ${tile.position.formatDigits()}
        divergence: ${planet.divergenceZones[tile]?.strength?.formatDigits() ?: 0.0}
        subduction: ${planet.subductionZones[tile]?.strength?.formatDigits() ?: 0.0}
        erosion: ${erosionDelta.formatDigits()}
        slope: ${slope.formatDigits()}
        prominence: ${prominence.formatDigits()}
        formation time: $formationTime
    """.trimIndent()

    companion object {
        fun <T> (Collection<PlanetTile>).floodFillGroupBy(
            planetTileFn: ((Tile) -> PlanetTile)? = null,
            keyFn: (PlanetTile) -> T
        ): Map<T, List<Set<PlanetTile>>> {
            val visited = mutableSetOf<PlanetTile>()
            val results = mutableMapOf<T, MutableList<Set<PlanetTile>>>()

            for (tile in this) {
                if (visited.contains(tile)) {
                    continue
                }
                val tileValue = keyFn(tile)
                val found =
                    if (planetTileFn == null) {
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