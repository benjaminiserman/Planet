package dev.biserman.planet.things

abstract class Kind(val id: Int) {
    constructor() : this(KindRegistry.kinds.size) {
        KindRegistry.kinds += id to this
    }
}

object KindRegistry {
    var kinds = mapOf<Int, Kind>()
}