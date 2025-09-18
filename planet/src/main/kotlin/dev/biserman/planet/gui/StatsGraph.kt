package dev.biserman.planet.gui

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetStats
import dev.biserman.planet.planet.Stat
import godot.api.CanvasItem
import godot.api.MenuButton
import godot.api.RefCounted
import godot.core.NativeCallable
import godot.core.Vector2
import godot.core.connect
import godot.global.GD
import kotlin.math.max
import kotlin.time.measureTime

class StatsGraph(val rootNode: CanvasItem) {
    val plot: RefCounted
    val graph2d =
        (rootNode.findChild("Graph2d") as CanvasItem).also {
            it.set("x_label", "Million years")
            plot = it.call("add_plot_item") as RefCounted
        }
    val addPoint = ({ point: Vector2 -> (plot.get("add_point") as NativeCallable).call(point) as Unit })
    val menuButton = rootNode.findChild("GraphOptions") as MenuButton

    private lateinit var stats: PlanetStats
    var planet: Planet? = null
        set(value) {
            field = value
            if (value != null) {
                stats = value.planetStats
                menuButton.getPopup()!!.clear()
                stats.tectonicStats.forEach { stat ->
                    menuButton.getPopup()!!.addItem(stat.name)
                }
                shownStat = null
            }
        }

    val statValues get() = planet!!.planetStats.tectonicStatValues

    var visible = false
        set(value) {
            rootNode.visible = value
            field = value
        }

    var trackStats = true

    init {
        menuButton.getPopup()!!.idPressed.connect { shownStat = stats.tectonicStats[it.toInt()] }
    }

    var shownStat: Stat? = null
        set(value) {
            field = value
            plot.call("remove_all")
            if (value != null) {
                graph2d.set("y_label", value.yLabel)
                for ((x, y) in statValues[value.name]!!) {
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
            stats.tectonicStats.forEach {
                statValues[it.name]!!.add(planet.tectonicAge.toDouble() to it.getter(planet))
            }
        }

        GD.print("Updating stats graph took ${timeTaken.inWholeMilliseconds}ms")

        if (shownStat != null) {
            val (time, value) = statValues[shownStat!!.name]!!.last()
            addPoint(Vector2(time, value))
            rescale(shownStat!!)
        }
    }

    fun rescale(stat: Stat) {
        val minX = 0
        val maxX = max(10, statValues[stat.name]!!.size)

        graph2d.set("x_min", minX)
        graph2d.set("x_max", maxX)

        val range = stat.range
        if (range != null) {
            graph2d.set("y_min", range.start)
            graph2d.set("y_max", range.endInclusive)
        } else {
            val statMin = statValues[stat.name]!!.minOfOrNull { it.second } ?: 0.0
            val minY = statMin - 0.1 * statMin
            val statMax = statValues[stat.name]!!.maxOfOrNull { it.second } ?: 0.0
            val maxY = statMax + 0.1 * statMax

            graph2d.set("y_min", minY)
            graph2d.set("y_max", maxY)
        }
    }
}