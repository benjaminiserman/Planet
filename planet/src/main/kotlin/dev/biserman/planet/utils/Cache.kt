package dev.biserman.planet.utils

import kotlin.reflect.KProperty

fun <T : Any> cache(initialValue: T? = null, factory: () -> T): Cache<T> = Cache(initialValue, factory)

class Cache<T : Any>(initialValue: T? = null, val factory: () -> T) {
    private var value: T? = initialValue

    operator fun getValue(owner: Any, property: KProperty<*>): T {
        if (value == null) {
            value = factory()
        }
        return value!!
    }

    operator fun setValue(owner: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun invalidate() {
        value = null
    }
}

class Memo<TValue : Any>(val dependencyFunctions: List<() -> Any>, private val factory: () -> TValue) {
    private val cache = Cache(null, factory)
    private var dependencies: List<Any> = dependencyFunctions.map { it() }

    operator fun getValue(owner: Any, property: KProperty<*>): TValue {
        
        cache.getValue(owner, property)
    }

    operator fun setValue(owner: Any, property: KProperty<*>, value: TValue) = cache.setValue(owner, property, value)
}