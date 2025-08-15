package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.topology.Tile
import godot.core.Vector3
import kotlin.math.E
import kotlin.math.pow

class PlanetTile(val planet: Planet, var tile: Tile) {
    //    var elevation = Main.noise.getNoise3dv(tile.averagePosition * 100)
    //    var elevation = sin(tile.averagePosition.y * 90)
    var elevation = Main.noise.startingElevation.getNoise3dv(tile.averagePosition)
    var temperature = 0.0
    var moisture = 0.0
    var density = -Main.noise.startingElevation.getNoise3dv(tile.averagePosition)
    var tectonicPlate: TectonicPlate? = null
        set(value) {
            if (value == null) {
                field?.tiles?.remove(this)
            } else {
                value.tiles.add(this)
            }
            field = value
        }

    fun getPlateBoundaryForces(): Vector3 = tile.borders.fold(Vector3.ZERO) { sum, border ->
        val neighborTile = border.oppositeTile(tile)
        val neighborPlanetTile = planet.planetTiles[neighborTile]!!
        if (neighborPlanetTile.tectonicPlate == tectonicPlate) {
            return@fold sum
        }

        val rawScalar = ((neighborPlanetTile.density - density) / 2.0) * -100

        // desmos: \frac{2}{1+e^{-20\left(x-1.2\right)}}-1
        // this scales between back-arc spreading and trench pull based on relative plate density
        val backArcSpreadingX = (neighborPlanetTile.density + 1) / (density + 1.001)
        val backArcSpreading = 2.0 / (1 + E.pow(-20 * (backArcSpreadingX - 1.2))) - 1

        val finalScalar = if (rawScalar < 0.0) backArcSpreading * 5 else rawScalar

        return@fold sum + (neighborTile.position - tile.position) * finalScalar * border.length
    }
}