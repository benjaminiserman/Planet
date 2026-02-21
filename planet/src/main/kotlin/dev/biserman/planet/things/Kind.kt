package dev.biserman.planet.things

abstract class Kind(var id: Int, open var concepts: List<Concept>) {
    constructor(concepts: List<Concept>) : this(KindRegistry.kinds.size, concepts) {
        KindRegistry.kinds += id to this
    }
}

object KindRegistry {
    var kinds = mapOf<Int, Kind>()
}