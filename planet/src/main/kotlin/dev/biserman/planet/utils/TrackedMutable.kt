package dev.biserman.planet.utils

import java.util.RandomAccess

interface TrackedMutable {
    var mutationCount: Int

    fun dirty() = mutationCount++
}

class TrackedMutableList<T>(private val list: MutableList<T> = mutableListOf()) :
    AbstractMutableList<T>(), RandomAccess, TrackedMutable {
    override var mutationCount = 0
    override val size get() = list.size
    override fun get(index: Int): T = list[index]
    override fun add(index: Int, element: T) {
        dirty()
        list.add(index, element)
    }
    override fun removeAt(index: Int): T {
        dirty()
        return list.removeAt(index)
    }
    override fun set(index: Int, element: T): T {
        dirty()
        return list.set(index, element)
    }

    companion object {
        fun <T> MutableList<T>.toTracked() = TrackedMutableList(this)
    }
}

class TrackedMutableSet<T>(private val set: MutableSet<T> = mutableSetOf()) :
    MutableSet<T> by set, TrackedMutable {
    override var mutationCount = 0

    override fun iterator(): MutableIterator<T> {
        val iterator = set.iterator()
        return object : MutableIterator<T> by iterator {
            override fun remove() {
                iterator.remove()
                dirty()
            }
        }
    }

    override fun add(element: T): Boolean {
        val changed = set.add(element)
        if (changed) dirty()
        return changed
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val changed = set.addAll(elements)
        if (changed) dirty()
        return changed
    }

    override fun remove(element: T): Boolean {
        val changed = set.remove(element)
        if (changed) dirty()
        return changed
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val changed = set.removeAll(elements)
        if (changed) dirty()
        return changed
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val changed = set.retainAll(elements)
        if (changed) dirty()
        return changed
    }

    override fun clear() {
        if (set.isNotEmpty()) {
            set.clear()
            dirty()
        }
    }

    companion object {
        fun <T> MutableSet<T>.toTracked() = TrackedMutableSet(this)
    }
}
