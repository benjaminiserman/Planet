package dev.biserman.planet.topology

data class MutBorder(
    val id: Int,
    val corners: MutableList<MutCorner> = mutableListOf(),
    val borders: MutableList<MutBorder> = mutableListOf(),
    val tiles: MutableList<MutTile> = mutableListOf()
) {
    fun oppositeCorner(corner: MutCorner) = if (corner == corners[0]) corners[1] else corners[0]
    fun oppositeTile(tile: MutTile) = if (tile == tiles[0]) tiles[1] else tiles[0]

    val length get() = corners[0].position.distanceTo(corners[1].position)
}