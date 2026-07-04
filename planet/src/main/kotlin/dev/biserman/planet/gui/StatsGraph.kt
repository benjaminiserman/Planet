package dev.biserman.planet.gui

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetStats
import dev.biserman.planet.planet.Stat
import dev.biserman.planet.utils.component1
import dev.biserman.planet.utils.component2
import godot.api.CanvasItem
import godot.api.Label
import godot.api.MenuButton
import godot.core.Vector2
import godot.core.connect
import godot.global.GD
import kotlin.math.max
import kotlin.time.measureTime

class StatsGraph(val rootNode: CanvasItem) {
    val graph = rootNode.findChild("Graph2d") as StatsGraphPlot
    val menuButton = rootNode.findChild("GraphOptions") as MenuButton
    val currentValueLabel = rootNode.findChild("GraphCurrentValue") as Label

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

    var shownStat: Stat<*>? = null
        set(value) {
            field = value
            graph.clear()
            if (value != null) {
                graph.yLabel = value.yLabel
                graph.integerYLabels = value.usesIntegerValues(planet!!)
                graph.setPoints(statValues[value.name]!!)
                rescale(value)
                updateCurrentValue(value, planet!!)
                menuButton.setText(value.name + " ▽")
            } else {
                graph.yLabel = ""
                menuButton.setText("Select Graph")
                currentValueLabel.text = ""
            }
        }

    fun update(planet: Planet) {
        if (!trackStats) {
            shownStat?.let { updateCurrentValue(it, planet) }
            return
        }

        var currentShownValue: Number? = null
        val timeTaken = measureTime {
            stats.tectonicStats.forEach { stat ->
                val value = stat.getter(planet)
                statValues[stat.name]?.add(Vector2(planet.tectonicAge.toDouble(), value.toDouble()))
                if (stat == shownStat) currentShownValue = value
            }
        }
        currentShownValue?.let(::setCurrentValue)

        GD.print("Updating stats graph took ${timeTaken.inWholeMilliseconds}ms")

        if (shownStat != null) {
            val (time, value) = statValues[shownStat!!.name]!!.last()
            graph.addPoint(Vector2(time, value))
            rescale(shownStat!!)
        }
    }

    private fun updateCurrentValue(stat: Stat<*>, planet: Planet) {
        setCurrentValue(stat.getter(planet))
    }

    private fun setCurrentValue(value: Number) {
        currentValueLabel.text = when (value) {
            is Byte, is Short, is Int, is Long -> value.toLong().toString()
            else -> String.format("%.1f", value.toDouble())
        }
    }

    fun rescale(stat: Stat<*>) {
        val minX = 0
        val maxX = max(10.0, statValues[stat.name]!!.maxOfOrNull { it.x } ?: 10.0)

        val range = stat.range
        val (minY, maxY) = if (range != null) {
            range.start.toDouble() to range.endInclusive.toDouble()
        } else {
            val statMin = statValues[stat.name]!!.minOfOrNull { it.y } ?: 0.0
            val statMax = statValues[stat.name]!!.maxOfOrNull { it.y } ?: 0.0
            val padding = max(1.0, (statMax - statMin) * 0.1)
            (statMin - padding) to (statMax + padding)
        }
        graph.setBounds(minX.toDouble(), maxX, minY, maxY)
    }
}
