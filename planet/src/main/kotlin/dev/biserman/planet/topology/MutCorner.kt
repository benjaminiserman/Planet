package dev.biserman.planet.topology

import godot.core.Vector3

data class MutCorner(
    val id: Int,
    var position: Vector3,
    val corners: MutableList<MutCorner> = mutableListOf(),
    val borders: MutableList<MutBorder> = mutableListOf(),
    val tiles: MutableList<MutTile> = mutableListOf()
) {
    fun vectorTo(corner: MutCorner) = corner.position - position
}