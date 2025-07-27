package dev.biserman.planet.topology

import godot.core.Vector3

interface Corner {
    val id: Int
    val position: Vector3
    val corners: List<Corner>
    val borders: List<Border>
    val tiles: List<Tile>

    fun vectorTo(corner: MutCorner) = corner.position - position
}

data class MutCorner(
    override val id: Int,
    override var position: Vector3,
    override val corners: MutableList<MutCorner> = mutableListOf(),
    override val borders: MutableList<MutBorder> = mutableListOf(),
    override val tiles: MutableList<MutTile> = mutableListOf()
) : Corner