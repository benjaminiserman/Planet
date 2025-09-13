package dev.biserman.planet.gui

import godot.api.Node
import godot.api.RefCounted
import godot.core.NativeCallable

class StatsGraph(val graph2d: Node) {
    var plot: RefCounted? = null
    val addPoint by lazy { plot!!.get("add_point") as NativeCallable }
    var x: Double = 0.0
}