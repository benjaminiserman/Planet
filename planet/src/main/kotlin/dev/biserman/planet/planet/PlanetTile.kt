package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.utils.memo
import godot.core.Vector3
import kotlin.math.E
import kotlin.math.pow

class PlanetTile(val planet: Planet, var tile: Tile) {
    //    var elevation = Main.noise.getNoise3dv(tile.averagePosition * 100)
    //    var elevation = sin(tile.averagePosition.y * 90)
    var elevation = Main.noise.startingElevation.getNoise3dv(tile.averagePosition) * 1000
    val density get() = -elevation / 1000
    var temperature = 0.0
    var moisture = 0.0
    var tectonicPlate: TectonicPlate? = null
        set(value) {
            if (value == null) {
                field?.tiles?.remove(this)
            } else {
                value.tiles.add(this)
            }
            field = value
        }

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

            val rawScalar = ((neighborPlanetTile.density - density) / 2.0) * -1

            // desmos: \frac{2}{1+e^{-20\left(x-1.2\right)}}-1
            // this scales between back-arc spreading and trench pull based on relative plate density
            val backArcSpreadingX = (neighborPlanetTile.density + 1) / (density + 1.001)
            val backArcSpreading = 2.0 / (1 + E.pow(-20 * (backArcSpreadingX - 1.15))) - 1

            val finalScalar = if (rawScalar < 0.0) backArcSpreading * 0.05 else rawScalar

            return@fold sum + (neighborTile.position - tile.position) * finalScalar * border.length
        }
    }

    var movement: Vector3 = Vector3.ZERO
    fun updateMovement() {
//        val idealMovement = plateBoundaryForces * 0.1 +
//                tectonicPlate!!.averageForce +
//                (tile.position - tectonicPlate!!.region.center).cross(tile.position) * tectonicPlate!!.averageRotation
        if (tectonicPlate == null) {
            return
        }

        val idealMovement = // plateBoundaryForces * 0.1 +
            tectonicPlate!!.eulerPole.cross(tile.position)
        movement = movement.lerp(idealMovement, 1.0).tangent(tile.position)
    }

    fun oppositeTile(border: Border) = planet.planetTiles[border.oppositeTile(tile)]

    fun floodFill(filterFn: (PlanetTile) -> Boolean): Set<PlanetTile> {
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
                val neighborPlanetTile = planet.planetTiles[neighbor]!!
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
}