package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.utils.randomHsv
import godot.core.Color

class TectonicPlate(planet: Planet) {
    val biomeColor = Color.fromHsv(Main.random.nextDouble(0.15, 0.4), Main.random.nextDouble(0.7, 0.9), 0.5, 1.0)
    val debugColor = Color.randomHsv()
    val region = PlanetRegion(planet)
    val tiles by region::tiles
}