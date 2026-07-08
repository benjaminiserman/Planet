package dev.biserman.planet.utils

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.things.ComponentSet
import dev.biserman.planet.things.ComponentSetDeserializer
import dev.biserman.planet.things.ComponentSetSerializer
import dev.biserman.planet.things.MutableComponentSet
import godot.core.Vector2
import godot.core.Vector3
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


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

class DedupingObjectIdResolver : SimpleObjectIdResolver() {
    override fun bindItem(id: ObjectIdGenerator.IdKey?, ob: Any?) {
        _items[id] = ob
    }

    override fun newForDeserialization(context: Any?): ObjectIdResolver {
        return DedupingObjectIdResolver().also { it._items = HashMap<ObjectIdGenerator.IdKey, Any?>() }
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DedupeDuplicateIds
class DedupingDeserializerModifier : BeanDeserializerModifier() {
    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {
        val objectIdInfo = beanDesc.objectIdInfo ?: return deserializer
        if (beanDesc.classInfo.getAnnotation(DedupeDuplicateIds::class.java) == null) {
            return deserializer
        }

        val idProp = beanDesc.findProperties()
            .find { it.name == objectIdInfo.propertyName.simpleName }
            ?: return deserializer

        val scopeClass = objectIdInfo.scope ?: beanDesc.beanClass

        @Suppress("UNCHECKED_CAST")
        return GenericDedupingDeserializer(
            deserializer as JsonDeserializer<Any>,
            scopeClass,
            idProp
        )
    }
}

class GenericDedupingDeserializer(
    private val delegate: JsonDeserializer<Any>,
    private val scopeClass: Class<*>,
    private val idProperty: BeanPropertyDefinition
) : JsonDeserializer<Any>(), ResolvableDeserializer {

    private val accessor: AnnotatedMember? = idProperty.accessor

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Any {
        val built = delegate.deserialize(p, ctxt)
        val id = accessor?.getValue(built) ?: return built

        val key = scopeClass to id

        @Suppress("UNCHECKED_CAST")
        val seen = ctxt.getAttribute(DEDUP_ATTR_KEY) as? MutableMap<Pair<Class<*>, Any>, Any>
            ?: HashMap<Pair<Class<*>, Any>, Any>().also {
                ctxt.setAttribute(DEDUP_ATTR_KEY, it)
            }

        return seen.getOrPut(key) { built }
    }

    // BeanDeserializer relies on resolve() being forwarded to wire up forward references
    // between properties. Skipping this can silently break unrelated identity refs.
    override fun resolve(ctxt: DeserializationContext) {
        if (delegate is ResolvableDeserializer) {
            delegate.resolve(ctxt)
        }
    }

    companion object {
        private const val DEDUP_ATTR_KEY = "genericIdDedup"
    }
}

object Serialization {
    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerKotlinModule()
        .registerModule(
            SimpleModule()
                .addSerializer(ComponentSet::class.java, ComponentSetSerializer())
                .addDeserializer(ComponentSet::class.java, ComponentSetDeserializer())
                .addDeserializer(MutableComponentSet::class.java, ComponentSetDeserializer())
        )
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
        .addMixIn(Vector3::class.java, Vector3Mixin::class.java)
        .addMixIn(Vector2::class.java, Vector2Mixin::class.java)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.WRAP_EXCEPTIONS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.WRAP_EXCEPTIONS).also {
            it.factory.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature())
        }

    val configMapper: ObjectMapper = jacksonObjectMapper()
        .registerKotlinModule { enable(KotlinFeature.SingletonSupport) }
        .addMixIn(Vector3::class.java, Vector3Mixin::class.java)
        .addMixIn(Vector2::class.java, Vector2Mixin::class.java)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.WRAP_EXCEPTIONS)
        .enable(MapperFeature.SORT_CREATOR_PROPERTIES_BY_DECLARATION_ORDER)
        .enable(DeserializationFeature.WRAP_EXCEPTIONS).also {
            it.factory.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature())
        }

    fun save(filename: String, value: Any) {
        GZIPOutputStream(File(filename).outputStream()).use { gzipOut ->
            objectMapper.writeValue(gzipOut, value)
        }
    }

    fun <T> load(filename: String, valueType: Class<T>): T {
        val file = File(filename)
        val isGzipped = file.inputStream().use { stream ->
            val header = ByteArray(2)
            val bytesRead = stream.read(header)
            bytesRead == 2 && header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte()
        }

        return if (isGzipped) {
            GZIPInputStream(file.inputStream()).use { objectMapper.readValue(it, valueType) }
        } else {
            objectMapper.readValue(file, valueType)
        }
    }

    fun load(filename: String): Planet = load(filename, Planet::class.java)
}