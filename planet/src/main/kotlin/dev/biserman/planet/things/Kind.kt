package dev.biserman.planet.things

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators

@JsonIdentityInfo(
    generator = ObjectIdGenerators.None::class,
    scope = Resource::class,
    property = "id",
)
abstract class Kind(var id: Int, open var concepts: List<Concept>) {
    constructor(concepts: List<Concept>) : this(KindRegistry.kinds.size, concepts) {
        KindRegistry.kinds += id to this
    }
}

object KindRegistry {
    var kinds = mapOf<Int, Kind>()
}