package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.utils.memo
import dev.biserman.planet.utils.randomHsv
import godot.core.Color
import godot.core.Vector3
import kotlin.math.absoluteValue

class TectonicPlate(planet: Planet) {
    val biomeColor = Color.fromHsv(Main.random.nextDouble(0.15, 0.4), Main.random.nextDouble(0.7, 0.9), 0.5, 1.0)
    val debugColor = Color.randomHsv()
    val region = PlanetRegion(planet)
    val tiles by region::tiles

    val averageForce by memo({ tiles.mutationCount }) {
        tiles.fold(Vector3.ZERO) { sum, tile ->
            sum + tile.plateBoundaryForces * tile.plateBoundaryForces.normalized().dot(
                tile.tile.position - tile.tectonicPlate!!.region.center
            ).absoluteValue
        } / tiles.size
    }

    val averageRotation by memo({ tiles.mutationCount }) {
        tiles.fold(0.0) { sum, tile -> sum + tile.rotationalForce } / tiles.size
    }
}