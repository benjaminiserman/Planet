package dev.biserman.planet.utils

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.biserman.planet.planet.Planet
import godot.core.Vector2
import godot.core.Vector3
import godot.global.GD
import java.io.File


@Suppress("FunctionName", "unused")
abstract class Vector3Mixin {
    @JsonIgnore
    abstract fun `getAnyPerpendicular$godot_core_library`(): Vector3

    @JsonIgnore
    abstract fun isZeroApprox(): Boolean

    @JsonIgnore
    abstract fun isFinite(): Boolean

    @JsonIgnore
    abstract fun isNormalized(): Boolean
}


@Suppress("unused")
abstract class Vector2Mixin {
    @JsonIgnore
    abstract fun isZeroApprox(): Boolean

    @JsonIgnore
    abstract fun isFinite(): Boolean

    @JsonIgnore
    abstract fun isNormalized(): Boolean
}

object Serialization {
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    init {
        objectMapper
            .registerKotlinModule()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
            .addMixIn(Vector3::class.java, Vector3Mixin::class.java)
            .addMixIn(Vector2::class.java, Vector2Mixin::class.java)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.WRAP_EXCEPTIONS)
            .enable(DeserializationFeature.WRAP_EXCEPTIONS).also {
                it.factory.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature())
            }
    }

    fun save(planet: Planet) {
        objectMapper.writeValue(File("planet.json"), planet)
    }

    fun load(): Planet {
        GD.print("Loading planet.json")
        val planet = objectMapper.readValue(File("planet.json"), Planet::class.java)
        return planet
    }
}