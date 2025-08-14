package dev.biserman.planet.planet

import dev.biserman.planet.Main
import dev.biserman.planet.utils.randomHsv
import dev.biserman.planet.utils.randomRgb
import godot.core.Color

class TectonicPlate() {
    val debugColor = Color.fromHsv(Main.random.nextDouble(0.15, 0.4), Main.random.nextDouble(0.7, 0.9), 0.5, 1.0)
    val region = PlanetRegion()
    val tiles by region::tiles
}