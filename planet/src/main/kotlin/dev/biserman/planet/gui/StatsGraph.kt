package dev.biserman.planet.gui

import dev.biserman.planet.planet.Planet
import godot.api.CanvasItem
import godot.api.MenuButton
import godot.api.Node
import godot.api.RefCounted
import godot.core.Color
import godot.core.NativeCallable
import godot.core.Vector2
import godot.core.connect
import godot.global.GD
import kotlin.collections.withIndex
import kotlin.math.max
import kotlin.time.measureTime

data class Stat(
    val name: String,
    val color: Color = Color.red,
    val yLabel: String = "",
    val values: MutableList<Pair<Double, Double>> = mutableListOf(),
    val range: ClosedRange<Double>? = null,
    val getter: (Planet) -> Double
)

class StatsGraph(val rootNode: CanvasItem) {
    val plot: RefCounted
    val graph2d =
        (rootNode.findChild("Graph2d") as CanvasItem).also {
            it.set("x_label", "Million years")
            plot = it.call("add_plot_item") as RefCounted
        }
    val addPoint = ({ point: Vector2 -> (plot.get("add_point") as NativeCallable).call(point) as Unit })
    val menuButton = rootNode.findChild("GraphOptions") as MenuButton

    val stats = listOf(
        Stat(
            "% tiles above water",
            range = 0.0..100.0
        ) { planet -> planet.planetTiles.values.filter { it.isAboveWater }.size / planet.planetTiles.size.toDouble() * 100 },
        Stat("average tile crust age", yLabel = "Million years") { planet ->
            planet.planetTiles.values.map { planet.tectonicAge - it.formationTime }
                .average()
        },
        Stat("oldest tile crust age", yLabel = "Million years") { planet ->
            (planet.tectonicAge - planet.oldestCrust).toDouble()
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
        Stat("hotspot activity") { planet ->
            planet.planetTiles.values.sumOf {
                planet.noise.hotspots.sample4d(
                    it.tile.position,
                    planet.tectonicAge.toDouble()
                )
            }
        },
    )

    var visible = false
        set(value) {
            rootNode.visible = value
            field = value
        }

    var trackStats = true

    init {
        stats.forEach { stat ->
            menuButton.getPopup()!!.addItem(stat.name)
        }

        menuButton.getPopup()!!.idPressed.connect { shownStat = stats[it.toInt()] }
    }

    var shownStat: Stat? = null
        set(value) {
            field = value
            plot.call("remove_all")
            if (value != null) {
                graph2d.set("y_label", value.yLabel)
                for ((x, y) in value.values) {
                    addPoint(Vector2(x, y))
                }
                rescale(value)
                menuButton.setText(value.name + " ▽")
            } else {
                graph2d.set("y_label", "")
                menuButton.setText("Select Graph")
            }
        }

    fun update(planet: Planet) {
        if (!trackStats) {
            return
        }

        val timeTaken = measureTime {
            stats.forEach {
                it.values.add(planet.tectonicAge.toDouble() to it.getter(planet))
            }
        }

        GD.print("Updating stats graph took ${timeTaken.inWholeMilliseconds}ms")

        if (shownStat != null) {
            val (time, value) = shownStat!!.values.last()
            addPoint(Vector2(time, value))
            rescale(shownStat!!)
        }
    }

    fun rescale(stat: Stat) {
        val minX = 0
        val maxX = max(10, stat.values.size)

        graph2d.set("x_min", minX)
        graph2d.set("x_max", maxX)

        val range = stat.range
        if (range != null) {
            graph2d.set("y_min", range.start)
            graph2d.set("y_max", range.endInclusive)
        } else {
            val statMin = stat.values.minOfOrNull { it.second } ?: 0.0
            val minY = statMin - 0.1 * statMin
            val statMax = stat.values.maxOfOrNull { it.second } ?: 0.0
            val maxY = statMax + 0.1 * statMax

            graph2d.set("y_min", minY)
            graph2d.set("y_max", maxY)
        }
    }
}