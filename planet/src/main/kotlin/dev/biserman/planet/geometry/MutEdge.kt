package dev.biserman.planet.geometry

data class MutEdge(
    val vertIndexes: MutableList<Int> = mutableListOf(),
    val faceIndexes: MutableList<Int> = mutableListOf(),
    val subdividedVertexIndexes: MutableList<Int> = mutableListOf(),
    val subdividedEdgeIndexes: MutableList<Int> = mutableListOf(),
) {
    fun oppositeTriIndex(faceIndex: Int): Int = when (faceIndex) {
        this.faceIndexes[0] -> this.faceIndexes[1]
        this.faceIndexes[1] -> this.faceIndexes[0]
        else -> throw Error("Given tri is not part of given edge.")
    }
}
