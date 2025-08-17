package dev.biserman.planet.utils

import kotlin.reflect.KProperty

fun <T : Any> cache(initialValue: T? = null, factory: () -> T): Cache<T> = Cache(initialValue, factory)
fun <T : Any> memo(vararg dependencyFunctions: () -> Any? = arrayOf(), factory: () -> T): Memo<T> =
    Memo(dependencyFunctions.toList(), factory)

class Cache<T : Any>(initialValue: T? = null, val factory: () -> T) {
    private var value: T? = initialValue

    operator fun getValue(owner: Any, property: KProperty<*>): T {
        if (value == null) {
            value = factory()
        }
        return value!!
    }

    fun invalidate() {
        value = null
    }
}

class Memo<TValue : Any>(val dependencyFunctions: List<() -> Any?>, factory: () -> TValue) {
    private val cache = Cache(null, factory)
    private var dependencies: List<Any?> = dependencyFunctions.map { it() }

    operator fun getValue(owner: Any, property: KProperty<*>): TValue {
        val newDependencies = dependencyFunctions.map { it() }
        if (dependencies.zip(newDependencies).any { (old, new) -> old != new }) {
            dependencies = newDependencies
            cache.invalidate()
        }

        return cache.getValue(owner, property)
    }
}