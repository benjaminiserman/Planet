package dev.biserman.planet.things

import kotlin.reflect.KClass


open class ComponentSet<T : Any>(components: Collection<T>) : Collection<T> {
    protected var internalComponents = components.associateBy { it::class }.toMutableMap()
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
    fun add(component: T) {
        internalComponents[component::class] = component
    }

    fun remove(component: T) {
        internalComponents.remove(component::class)
    }
}