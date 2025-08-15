package dev.biserman.planet.utils

open class TrackedMutable {
    var mutationCount = 0

    fun dirty() = mutationCount++
}

class TrackedMutableList<T>(private val list: MutableList<T> = mutableListOf()) : TrackedMutable(),
    MutableList<T> by list {
    override fun add(element: T): Boolean {
        dirty()
        return list.add(element)
    }

    override fun remove(element: T): Boolean {
        dirty()
        return list.remove(element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        dirty()
        return list.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        dirty()
        return list.addAll(index, elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        dirty()
        return list.removeAll(elements)
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        dirty()
        return list.retainAll(elements)
    }

    override fun clear() {
        dirty()
        return list.clear()
    }

    override fun set(index: Int, element: T): T {
        dirty()
        return list.set(index, element)
    }

    override fun add(index: Int, element: T) {
        dirty()
        return list.add(index, element)
    }

    override fun removeAt(index: Int): T {
        dirty()
        return list.removeAt(index)
    }

    companion object {
        fun <T> MutableList<T>.toTracked() = TrackedMutableList(this)
    }
}

class TrackedMutableSet<T>(private val set: MutableSet<T> = mutableSetOf()) : TrackedMutable(), MutableSet<T> by set {
    override fun iterator(): MutableIterator<T> {
        return object : MutableIterator<T> by set.iterator() {
            override fun remove() {
                throw UnsupportedOperationException("Use remove(element: T) instead")
            }
        }
    }

    override fun add(element: T): Boolean {
        dirty()
        return set.add(element)
    }

    override fun remove(element: T): Boolean {
        dirty()
        return set.remove(element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        dirty()
        return set.addAll(elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        dirty()
        return set.removeAll(elements)
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        dirty()
        return set.retainAll(elements)
    }

    override fun clear() {
        dirty()
        return set.clear()
    }

    companion object {
        fun <T> MutableSet<T>.toTracked() = TrackedMutableSet(this)
    }
}