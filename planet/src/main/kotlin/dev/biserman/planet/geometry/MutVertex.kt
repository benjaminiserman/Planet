package dev.biserman.planet.geometry

import godot.core.Vector3

data class MutVertex(
    var position: Vector3,
    val edgeIndexes: MutableList<Int> = mutableListOf(),
    val faceIndexes: MutableList<Int> = mutableListOf(),
    var normal: Vector3 = Vector3.ZERO
)
