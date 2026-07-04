package dev.biserman.planet.gui

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Control
import godot.core.Color
import godot.core.HorizontalAlignment
import godot.core.Rect2
import godot.core.Vector2
import godot.core.connect
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

@RegisterClass
class StatsGraphPlot : Control() {
    private val points = mutableListOf<Vector2>()

    var xLabel = "Million years"
        set(value) {
            field = value
            queueRedraw()
        }
    var yLabel = ""
        set(value) {
            field = value
            queueRedraw()
        }
    var integerYLabels = false
        set(value) {
            field = value
            queueRedraw()
        }

    private var xMin = 0.0
    private var xMax = 10.0
    private var yMin = 0.0
    private var yMax = 1.0

    @RegisterFunction
    override fun _ready() {
        resized.connect { queueRedraw() }
    }

    fun clear() {
        points.clear()
        queueRedraw()
    }

    fun addPoint(point: Vector2) {
        points.add(point)
        queueRedraw()
    }

    fun setPoints(newPoints: Collection<Vector2>) {
        points.clear()
        points.addAll(newPoints)
        queueRedraw()
    }

    fun setBounds(xMin: Double, xMax: Double, yMin: Double, yMax: Double) {
        this.xMin = xMin
        this.xMax = xMax.coerceAtLeast(xMin + 1e-9)
        this.yMin = yMin
        this.yMax = yMax.coerceAtLeast(yMin + 1e-9)
        queueRedraw()
    }

    @RegisterFunction
    override fun _draw() {
        val plot = Rect2(Vector2(58.0, 24.0), Vector2(size.x - 78.0, size.y - 66.0))
        if (plot.size.x <= 0.0 || plot.size.y <= 0.0) return

        drawRect(Rect2(Vector2.ZERO, size), Color(0.0, 0.0, 0.0, 0.72))

        val font = getThemeDefaultFont()
        val fontSize = 12
        val gridColor = Color(1.0, 1.0, 1.0, 0.18)
        val axisColor = Color(1.0, 1.0, 1.0, 0.75)
        val xTickCount = if (size.x < 450.0) 5 else 8
        val yTicks = if (integerYLabels) {
            val step = max(1, ceil((yMax - yMin) / xTickCount).toInt())
            generateSequence(floor(yMax).toInt()) { it - step }
                .takeWhile { it >= ceil(yMin).toInt() }
                .map { it.toDouble() }
                .toList()
        } else {
            (0..xTickCount).map { index ->
                yMax - (yMax - yMin) * index.toDouble() / xTickCount
            }
        }

        (0..xTickCount).forEach { index ->
            val fraction = index.toDouble() / xTickCount
            val x = plot.position.x + plot.size.x * fraction
            drawLine(Vector2(x, plot.position.y), Vector2(x, plot.end.y), gridColor)

            val xValue = xMin + (xMax - xMin) * fraction
            drawString(
                font,
                Vector2(x - 28.0, plot.end.y + 17.0),
                formatTick(xValue),
                HorizontalAlignment.CENTER,
                56.0f,
                fontSize,
                axisColor
            )
        }
        yTicks.forEach { yValue ->
            val fraction = (yMax - yValue) / (yMax - yMin)
            val y = plot.position.y + plot.size.y * fraction
            drawLine(Vector2(plot.position.x, y), Vector2(plot.end.x, y), gridColor)

            drawString(
                font,
                Vector2(2.0, y + 4.0),
                formatTick(yValue, integerYLabels),
                HorizontalAlignment.RIGHT,
                50.0f,
                fontSize,
                axisColor
            )
        }

        drawLine(plot.position, Vector2(plot.position.x, plot.end.y), axisColor, 1.5f)
        drawLine(Vector2(plot.position.x, plot.end.y), plot.end, axisColor, 1.5f)
        drawString(
            font,
            Vector2(plot.position.x, size.y - 4.0),
            xLabel,
            HorizontalAlignment.CENTER,
            plot.size.x.toFloat(),
            fontSize,
            axisColor
        )
        if (yLabel.isNotEmpty()) {
            drawString(font, Vector2(plot.position.x, 16.0), yLabel, modulate = axisColor, fontSize = fontSize)
        }

        points.asSequence()
            .filter { it.x in xMin..xMax }
            .map { point ->
                Vector2(
                    plot.position.x + (point.x - xMin) / (xMax - xMin) * plot.size.x,
                    plot.end.y - (point.y.coerceIn(yMin, yMax) - yMin) / (yMax - yMin) * plot.size.y
                )
            }
            .zipWithNext()
            .forEach { (from, to) -> drawLine(from, to, Color.white, 2.0f, true) }
    }

    private fun formatTick(value: Double, integer: Boolean = false): String = when {
        integer || abs(value) >= 1000.0 -> String.format("%.0f", value)
        else -> String.format("%.1f", value)
    }
}
