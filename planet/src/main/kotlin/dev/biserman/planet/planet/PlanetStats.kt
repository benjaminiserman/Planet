package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.biserman.planet.things.Stone
import dev.biserman.planet.things.StonePlacementType
import dev.biserman.planet.planet.tectonics.StonePlacement
import dev.biserman.planet.things.Resource
import dev.biserman.planet.things.StoneType
import godot.core.Color
import godot.core.Vector2

data class Stat<T>(
    val name: String,
    val color: Color = Color.red,
    val yLabel: String = "",
    val range: ClosedRange<T>? = null,
    val getter: (Planet) -> T
) where T : Number, T : Comparable<T> {
    fun usesIntegerValues(planet: Planet): Boolean = when (getter(planet)) {
        is Byte, is Short, is Int, is Long -> true
        else -> false
    }
}

class PlanetStats {
    @JsonIgnore
    val tectonicStats: List<Stat<*>> = listOf(
        Stat(
            "% tiles above water",
            range = 0.0..100.0
        ) { planet -> (1 - planet.waterCoverage) * 100 },
        Stat("average tile crust age", yLabel = "Million years") { planet ->
            planet.planetTiles.values.map { planet.tectonicAge - it.formationTime }
                .average()
        },
        Stat("oldest tile crust age", yLabel = "Million years") { planet ->
            (planet.tectonicAge - planet.planetTiles.values.minOf { it.formationTime })
        },
        Stat("average oceanic tile depth", yLabel = "Meters") { planet ->
            planet.planetTiles.values.filter { !it.isAboveWater }.map { it.elevation }.average()
        },
        Stat("average continental tile height", yLabel = "Meters") { planet ->
            planet.planetTiles.values.filter { it.isAboveWater }.map { it.elevation }.average()
        },
        Stat("tectonic plate count") { planet -> planet.tectonicPlates.size },
        Stat("number of continents") { planet ->
            planet.landRegions.count { it.tiles.size > 150 }
        },
        Stat("average tectonic plate torque") { planet -> planet.tectonicPlates.map { it.torque.length() }.average() },
        Stat("subduction zone count") { planet -> planet.convergenceZones.count { it.value.subductionStrengths.values.average() > 0 } },
        Stat("convergent zone count") { planet -> planet.convergenceZones.count { it.value.subductionStrengths.values.average() < 0 } },
        Stat("divergent zone count") { planet -> planet.divergenceZones.size },
        Stat("average slope") { planet -> planet.planetTiles.values.map { it.slope }.average() },
        Stat("max elevation") { planet -> planet.planetTiles.values.maxOf { it.elevation } },
        Stat("min elevation") { planet -> planet.planetTiles.values.minOf { it.elevation } },
        Stat("hotspot activity") { planet -> planet.hotspotActivity },
        Stat("%igneous surface rock", range=0.0..100.0) { planet ->
            planet.planetTiles.values.count { it.stoneColumn.surface.stoneComponent.placementType.stoneType == StoneType.Igneous }
                .toDouble() * 100 / planet.planetTiles.size
        },
        Stat("%metamorphic surface rock", range=0.0..100.0) { planet ->
            planet.planetTiles.values.count { it.stoneColumn.surface.stoneComponent.placementType.stoneType == StoneType.Metamorphic }
                .toDouble() * 100 / planet.planetTiles.size
        },
        Stat("%sedimentary surface rock", range=0.0..100.0) { planet ->
            planet.planetTiles.values.count { it.stoneColumn.surface.stoneComponent.placementType.stoneType == StoneType.Sedimentary }
                .toDouble() * 100 / planet.planetTiles.size
        },
        Stat("deposition-erosion balance") { planet ->
            planet.planetTiles.values.sumOf { it.erosionDelta }
        }
    )

    val tectonicStatValues = tectonicStats.associate { it.name to mutableListOf<Vector2>() }.toMutableMap()
}
