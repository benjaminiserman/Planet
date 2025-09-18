package dev.biserman.planet.utils

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.biserman.planet.planet.Planet
import godot.core.Vector3
import java.io.File


abstract class Vector3Mixin {
    @JsonIgnore
    abstract fun `getAnyPerpendicular$godot_core_library`(): Vector3
}

object Serialization {
    val objectMapper = ObjectMapper().registerKotlinModule().addMixIn(Vector3::class.java, Vector3Mixin::class.java)

    fun save(planet: Planet) {
        objectMapper.writeValue(File("planet.json"), planet)
    }

    fun load(): Planet {
        return objectMapper.readValue(File("planet.json"), Planet::class.java)
    }
}