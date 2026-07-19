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
    private var historyMode = false
    private val activeStats: List<Stat<*>>
        get() = if (historyMode) stats.historyStats else stats.tectonicStats

    var planet: Planet? = null
        set(value) {
            field = value
            if (value != null) {
                stats = value.planetStats
                rebuildMenu()
            }
        }

    val statValues get() = planet!!.planetStats.allStatValues

    var visible = true
        set(value) {
            rootNode.visible = value
            field = value
        }

    var trackStats = true

    init {
        menuButton.getPopup()!!.idPressed.connect { shownStat = activeStats[it.toInt()] }
    }

    fun setHistoryMode(enabled: Boolean) {
        historyMode = enabled
        graph.xLabel = if (enabled) "Years" else "Million years"
        if (::stats.isInitialized) rebuildMenu()
    }

    private fun rebuildMenu() {
        menuButton.getPopup()!!.clear()
        activeStats.forEach { stat -> menuButton.getPopup()!!.addItem(stat.name) }
        shownStat = null
    }

    var shownStat: Stat<*>? = null
        set(value) {
            field = value
            graph.clear()
            if (value != null) {
                graph.yLabel = value.yLabel
                graph.integerYLabels = value.usesIntegerValues(planet!!)
                graph.setPoints(statValues[value.name] ?: emptyList())
                rescale(value)
                updateCurrentValue(value, planet!!)
                menuButton.setText(value.name + " ▽")
            } else {
                graph.yLabel = ""
                menuButton.setText("Select Graph")
                currentValueLabel.text = ""
            }
        }

    fun update(planet: Planet) = updateStats(planet, stats.tectonicStats)

    fun updateHistory(planet: Planet) = updateStats(planet, stats.historyStats)

    private fun updateStats(planet: Planet, statsToUpdate: List<Stat<*>>) {
        if (!trackStats) {
            shownStat?.let { updateCurrentValue(it, planet) }
            return
        }

        var currentShownValue: Number? = null
        val timeTaken = measureTime {
            statsToUpdate.forEach { stat ->
                val value = stat.getter(planet)
                val time = if (stat in stats.historyStats) {
                    planet.historyTurn / 4.0
                } else {
                    planet.tectonicAge.toDouble()
                }
                val values = statValues[stat.name] ?: return@forEach
                val point = Vector2(time, value.toDouble())
                if (values.lastOrNull()?.x == time) {
                    values[values.lastIndex] = point
                } else {
                    values.add(point)
                }
                if (stat == shownStat) currentShownValue = value
            }
        }
        currentShownValue?.let(::setCurrentValue)

        GD.print("Updating stats graph took ${timeTaken.inWholeMilliseconds}ms")

        if (shownStat in statsToUpdate) {
            graph.setPoints(statValues[shownStat!!.name]!!)
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
