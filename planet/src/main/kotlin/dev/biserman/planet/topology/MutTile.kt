package dev.biserman.planet.topology

import dev.biserman.planet.geometry.Ray
import dev.biserman.planet.geometry.Sphere
import dev.biserman.planet.geometry.triArea
import dev.biserman.planet.utils.TrackedMutableList
import dev.biserman.planet.utils.TrackedMutableList.Companion.toTracked
import dev.biserman.planet.utils.memo
import godot.core.Plane
import godot.core.Vector3

interface Tile {
    val id: Int
    val position: Vector3
    val corners: List<Corner>
    val borders: List<Border>
    val tiles: List<Tile>

    val normal get() = position.normalized()
    val averagePosition get() = corners.fold(Vector3.ZERO) { a, b -> a + b.position } / corners.size
    val boundingSphere get() = Sphere(position, corners.maxOf { position.distanceTo(it.position) })
    val area get() = borders.sumOf { triArea(position, it.corners[0].position, it.corners[1].position) }

    fun intersectRay(ray: Ray): Boolean {
        if (!ray.intersectsSphere(boundingSphere)) {
            return false
        }

        val surface = Plane(normal, averagePosition)
        if (surface.distanceTo(ray.origin) <= 0) {
            return false
        }

        val denominator = surface.normal.dot(ray.direction)
        if (denominator == 0.0) {
            return false
        }

        val t = -(ray.origin.dot(surface.normal) + surface.d) / denominator
        val point = ray.direction * t + ray.origin
        for (i in 0..<corners.size) {
            val j = (i + 1) % corners.size
            val side = Plane(corners[j].position, corners[i].position, ray.origin)

            if (side.distanceTo(point) < 0) {
                return false
            }
        }

        return true
    }

    fun borderFor(neighbor: Tile) = borders.first { neighbor.borders.contains(it) }
}

class MutTile(
    override val id: Int,
    override var corners: TrackedMutableList<MutCorner> = mutableListOf<MutCorner>().toTracked(),
    override var borders: MutableList<MutBorder> = mutableListOf(),
    override var tiles: MutableList<MutTile> = mutableListOf(),
) : Tile {
    override val position by memo({ corners.mutationCount }) { averagePosition }
}
