package dev.biserman.planet.utils

import dev.biserman.planet.utils.WeightedBag.Companion.toWeightedBag
import kotlin.random.Random

fun <U> weightedBagOf(vararg items: Pair<U, Number>): WeightedBag<U> = items.toList().toWeightedBag()

class WeightedBag<T> private constructor(private val entries: MutableList<WeightedBagEntry<T>> = mutableListOf()) {
    var weightSum = entries.sumOf { it.weight }

    data class WeightedBagEntry<T>(val item: T, val weight: Double)

    fun add(item: T, weight: Number) {
        weightSum += weight.toDouble()
        entries.add(WeightedBagEntry(item, weight.toDouble()))
    }

    fun tryGrab(random: Random): T? {
        val r = random.nextDouble(weightSum)
        var runningSum = 0.0
        for (entry in entries) {
            if (r < entry.weight + runningSum) {
                return entry.item
            }

            runningSum += entry.weight
        }

        return null
    }

    fun grab(random: Random): T = tryGrab(random) ?: throw NoSuchElementException()

    val size get() = entries.size

    companion object {
        fun <U, V> Iterable<U>.toWeightedBag(keyFn: (U) -> V, weightFn: (U) -> Number): WeightedBag<V> =
            WeightedBag(this.map { WeightedBagEntry(keyFn(it), weightFn(it).toDouble()) }.toMutableList())

        fun <U> Iterable<U>.toWeightedBag(weightFn: (U) -> Number): WeightedBag<U> =
            WeightedBag(this.map { WeightedBagEntry(it, weightFn(it).toDouble()) }.toMutableList())

        fun <U> Iterable<Pair<U, Number>>.toWeightedBag(): WeightedBag<U> =
            WeightedBag(this.map { WeightedBagEntry(it.first, it.second.toDouble()) }.toMutableList())
    }
}