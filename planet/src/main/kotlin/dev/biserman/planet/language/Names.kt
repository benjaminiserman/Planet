package dev.biserman.planet.language

import dev.biserman.planet.things.Concept
import dev.biserman.planet.things.Kind
import godot.core.Color

class Name(var text: String)
class KindNamer(val on: Concept, val name: Kind.() -> Name)
interface Namer<T> {
    fun name(thing: T): Name
}
interface NamerProvider {
    val kindNamers: Map<Concept, KindNamer>
}
