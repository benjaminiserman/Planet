package dev.biserman.planet.language

import dev.biserman.planet.things.Concept
import dev.biserman.planet.things.Kind

class Name(val text: String)
class KindNamer(val on: Concept, val name: Kind.() -> Name)
interface NamerProvider {
    val kindNamers: Map<Concept, KindNamer>
}
