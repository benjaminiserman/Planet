package dev.biserman.planet.things

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import dev.biserman.planet.planet.PlanetTile
import godot.core.Color


open class Resource(var components: ComponentSet<ResourceComponent>, var colors: List<Color>, override var concepts: List<Concept>) :
    Kind(concepts)

@JsonIdentityInfo(
    generator = ObjectIdGenerators.IntSequenceGenerator::class,
    scope = ResourceComponent::class,
    property = "id",
)
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface ResourceComponent



