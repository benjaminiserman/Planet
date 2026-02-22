package dev.biserman.planet.things

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import godot.global.GD
import kotlin.reflect.KClass

@JsonDeserialize(`as` = MutableComponentSet::class)
open class ComponentSet<T : Any>(components: Collection<T>) : Collection<T> {
    @Suppress("unused")
    constructor() : this(emptyList())

    var internalComponents = components.associateBy { it::class }.toMutableMap()

    @get:JsonIgnore
    val readonlyInternalComponents get() = internalComponents as Map<KClass<out T>, T>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified U : T> get(): U? = readonlyInternalComponents[U::class] as U?

    override val size: Int get() = internalComponents.size
    override fun isEmpty() = internalComponents.isEmpty()
    override fun contains(element: T) = element::class in internalComponents
    override fun iterator(): Iterator<T> = internalComponents.values.iterator()
    override fun containsAll(elements: Collection<T>) = internalComponents.values.containsAll(elements)

    companion object {
        fun <T : Any> componentSetOf(vararg components: T) = ComponentSet(components.toList())
    }
}

class MutableComponentSet<T : Any>(components: Collection<T>) : ComponentSet<T>(components) {
    @Suppress("unused")
    constructor() : this(emptyList())

    fun add(component: T) {
        internalComponents[component::class] = component
    }

    fun remove(component: T) {
        internalComponents.remove(component::class)
    }

    companion object {
        fun <T : Any> mutableComponentSetOf(vararg components: T) = MutableComponentSet(components.toList())
    }
}

class ComponentSetSerializer : StdSerializer<ComponentSet<*>>(ComponentSet::class.java) {
    override fun serialize(
        componentSet: ComponentSet<*>,
        jgen: JsonGenerator,
        provider: SerializerProvider
    ) {
        jgen.writeStartObject()
        jgen.writeArrayFieldStart("components")
        for (component in componentSet.internalComponents.values) {
            jgen.writeObject(component)
        }
        jgen.writeEndArray()
        jgen.writeEndObject()
    }
}

class ComponentSetDeserializer : StdDeserializer<MutableComponentSet<*>>(MutableComponentSet::class.java) {
    override fun deserialize(
        parser: JsonParser,
        context: DeserializationContext?
    ): MutableComponentSet<*> {
        val node = parser.codec.readTree<JsonNode>(parser)
        val components = mutableListOf<Any>()
        val componentsNode = node["components"]
        if (componentsNode != null && componentsNode.isArray) {
            for (componentNode in componentsNode) {
                components.add(parser.codec.treeToValue(componentNode, ResourceComponent::class.java))
            }
        }
        return MutableComponentSet(components)
    }
}