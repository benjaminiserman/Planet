package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonIgnore
import godot.core.Color
import godot.core.Vector2

data class Stat(
    val name: String,
    val color: Color = Color.red,
    val yLabel: String = "",
    val range: ClosedRange<Double>? = null,
    val getter: (Planet) -> Double
)

class PlanetStats {
    @JsonIgnore
    val tectonicStats = listOf(
        Stat(
            "% tiles above water",
            range = 0.0..100.0
        ) { planet -> planet.waterCoverage * 100 },
        Stat("average tile crust age", yLabel = "Million years") { planet ->
            planet.planetTiles.values.map { planet.tectonicAge - it.formationTime }
                .average()
        },
        Stat("oldest tile crust age", yLabel = "Million years") { planet ->
            (planet.tectonicAge - planet.planetTiles.values.minOf { it.formationTime }).toDouble()
        },
        Stat("average oceanic tile depth", yLabel = "Meters") { planet ->
            planet.planetTiles.values.filter { !it.isAboveWater }.map { it.elevation }.average()
        },
        Stat("average continental tile height", yLabel = "Meters") { planet ->
            planet.planetTiles.values.filter { it.isAboveWater }.map { it.elevation }.average()
        },
        Stat("tectonic plate count") { planet -> planet.tectonicPlates.size.toDouble() },
        Stat("average tectonic plate torque") { planet -> planet.tectonicPlates.map { it.torque.length() }.average() },
        Stat("subduction zone count") { planet -> planet.convergenceZones.filter { it.value.subductionStrengths.values.average() > 0 }.size.toDouble() },
        Stat("convergent zone count") { planet -> planet.convergenceZones.filter { it.value.subductionStrengths.values.average() < 0 }.size.toDouble() },
        Stat("divergent zone count") { planet -> planet.divergenceZones.size.toDouble() },
        Stat("average slope") { planet -> planet.planetTiles.values.map { it.slope }.average() },
        Stat("max elevation") { planet -> planet.planetTiles.values.maxOf { it.elevation } },
        Stat("min elevation") { planet -> planet.planetTiles.values.minOf { it.elevation } },
        Stat("hotspot activity") { planet -> planet.hotspotActivity },
    )

    val tectonicStatValues = tectonicStats.associate { it.name to mutableListOf<Vector2>() }
}