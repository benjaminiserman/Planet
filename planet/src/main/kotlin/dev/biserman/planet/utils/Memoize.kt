package dev.biserman.planet.utils

fun <TArg, TResult> memoize(fn: (TArg) -> TResult): (TArg) -> TResult {
    val results = mutableMapOf<TArg, TResult>()
    return {
        results.getOrPut(it) { fn(it) }
    }
}