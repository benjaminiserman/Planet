package dev.biserman.planet.topology

import godot.core.Vector3

interface Border {
    val id: Int
    val corners: List<Corner>
    val borders: List<Border>
    val tiles: List<Tile>

    val length get() = corners[0].position.distanceTo(corners[1].position)
    val midpoint get() = (corners[0].position + corners[1].position) * 0.5

    fun oppositeCorner(corner: Corner) = if (corner == corners[0]) corners[1] else corners[0]
    fun oppositeTile(tile: Tile) = if (tile == tiles[0]) tiles[1] else tiles[0]

    companion object {

    }
}

class MutBorder(
    override val id: Int,
    override val corners: MutableList<MutCorner> = mutableListOf(),
    override val borders: MutableList<MutBorder> = mutableListOf(),
    override val tiles: MutableList<MutTile> = mutableListOf()
) : Border {
    override fun oppositeCorner(corner: Corner): MutCorner = super.oppositeCorner(corner) as MutCorner
    override fun oppositeTile(tile: Tile): MutTile = super.oppositeTile(tile) as MutTile
}