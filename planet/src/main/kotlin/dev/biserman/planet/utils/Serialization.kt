package dev.biserman.planet.utils

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetDeserializer
import dev.biserman.planet.planet.PlanetStats
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.planet.TectonicPlate
import dev.biserman.planet.planet.TectonicPlateKeyDeserializer
import dev.biserman.planet.utils.Serialization.objectMapper
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

object Serialization {
    val objectMapper: ObjectMapper = ObjectMapper()

    init {
        objectMapper
            .registerModule(kotlinModule().apply {
                addKeyDeserializer(
                    TectonicPlate::class.java,
                    TectonicPlateKeyDeserializer()
                )
                addDeserializer(Planet::class.java, PlanetDeserializer(objectMapper))
            })
            .addMixIn(Vector3::class.java, Vector3Mixin::class.java)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.WRAP_EXCEPTIONS)
            .enable(DeserializationFeature.WRAP_EXCEPTIONS).also {
                it.factory.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature())
            }
    }

    fun save(planet: Planet) {
        objectMapper.writeValue(File("planet.json"), planet)

        val introspector = objectMapper.serializationConfig.introspect(objectMapper.constructType(Planet::class.java))
        GD.print("Introspecting: $introspector")
        val props = introspector.findProperties()
        props.forEach { p ->
            GD.print("Property: ${p.name}, type: ${p.primaryType.typeName} notes: $p")
        }
    }

    fun load(): Planet {
        try {
            val planet = objectMapper.readValue(File("planet.json"), Planet::class.java)
            return planet
        } catch (e: JsonMappingException) {
            GD.printErr("Error: ${e.message}")
            GD.printErr("Path: ${e.pathReference}")
            e.path.forEach { ref ->
                GD.printErr(" -> field='${ref.fieldName}' index=${ref.index} from ${ref.from?.javaClass?.simpleName}")
            }
            throw e
        }
    }
}