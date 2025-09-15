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

    fun invalidate() {
        value = null
    }
}