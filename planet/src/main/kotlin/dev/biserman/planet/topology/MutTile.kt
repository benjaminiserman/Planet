package dev.biserman.planet.topology

import dev.biserman.planet.geometry.Ray
import dev.biserman.planet.geometry.Sphere
import godot.core.Plane
import godot.core.Vector3

data class MutTile(
    var id: Int,
    var position: Vector3,
    var corners: MutableList<MutCorner> = mutableListOf(),
    var borders: MutableList<MutBorder> = mutableListOf(),
    var tiles: MutableList<MutTile> = mutableListOf(),
    var boundingSphere: Sphere = Sphere.Companion.ZERO
) {
    val normal get() = Vector3.ZERO // $$$
    val averagePosition get() = Vector3.ZERO // $$$

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
}
