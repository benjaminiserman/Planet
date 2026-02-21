package dev.biserman.planet.things

import godot.core.Color

open class Resource(val components: ComponentSet<ResourceComponent>, val colors: List<Color>, override var concepts: List<Concept>) :
    Kind(concepts) {

}

interface ResourceComponent



