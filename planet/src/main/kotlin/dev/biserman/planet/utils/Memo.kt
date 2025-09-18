package dev.biserman.planet.utils

import kotlin.reflect.KProperty

fun <T : Any> memo(vararg dependencyFunctions: () -> Any? = arrayOf(), factory: () -> T): Memo<T> =
    Memo(dependencyFunctions.toList(), factory)

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