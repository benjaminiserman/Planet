package dev.biserman.planet.utils

import dev.biserman.planet.utils.WeightedBag.WeightedBagEntry
import kotlin.random.Random


fun <U> Iterable<U>.toWeightedBag(random: Random, weightFn: (U) -> Number): WeightedBag<U> =
    WeightedBag(random, this.map { WeightedBagEntry(it, weightFn(it).toDouble()) }.toMutableList())

class WeightedBag<T>(val random: Random, private val entries: MutableList<WeightedBagEntry<T>> = mutableListOf()) {
    var weightSum = entries.sumOf { it.weight }

    data class WeightedBagEntry<T>(val item: T, val weight: Double)

    fun add(item: T, weight: Number) {
        weightSum += weight.toDouble()
        entries.add(WeightedBagEntry(item, weight.toDouble()))
    }

    fun grab(): T? {
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

    val size get() = entries.size
}