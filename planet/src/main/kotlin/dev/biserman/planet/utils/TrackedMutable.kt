package dev.biserman.planet.utils

open class TrackedMutable {
    var mutationCount = 0

    fun dirty() = mutationCount++
}

class TrackedMutableList<T>(private val list: MutableList<T> = mutableListOf()) : TrackedMutable(), MutableList<T> by list {
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

}