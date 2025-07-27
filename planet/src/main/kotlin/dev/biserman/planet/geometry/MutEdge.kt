package dev.biserman.planet.geometry

data class MutEdge(
    val vertIndexes: MutableList<Int> = mutableListOf(),
    val triIndexes: MutableList<Int> = mutableListOf(),
    val subdividedVertexIndexes: MutableList<Int> = mutableListOf(),
    val subdividedEdgeIndexes: MutableList<Int> = mutableListOf(),
) {
    fun oppositeTriIndex(faceIndex: Int): Int = when (faceIndex) {
        this.triIndexes[0] -> this.triIndexes[1]
        this.triIndexes[1] -> this.triIndexes[0]
        else -> throw Error("Given tri is not part of given edge.")
    }
}
