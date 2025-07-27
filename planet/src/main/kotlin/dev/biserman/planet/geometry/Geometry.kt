package dev.biserman.planet.geometry

import godot.api.ArrayMesh
import godot.api.Mesh
import godot.core.PackedVector3Array
import godot.core.Plane
import godot.core.Quaternion
import godot.core.VariantArray
import godot.core.Vector2
import godot.core.Vector3
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun calculateNormal(p1: Vector3, p2: Vector3, p3: Vector3): Vector3 {
    val a = p2 - p1
    val b = p3 - p1

    return -Vector3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x
    )
}

fun triArea(p0: Vector3, p1: Vector3, p2: Vector3): Double {
    val vab = p1 - p0
    val faceNormal = vab.cross(p2 - p0)
    val vabNormal = faceNormal.cross(vab).normalized()
    val height = Plane(vabNormal, p0).distanceTo(p2)
    val width = vab.length()
    return width * height * 0.5
}

fun (Random).randomUnitVector(): Vector3 {
    val theta = this.nextDouble(0.0, PI * 2)
    val phi = acos(this.nextDouble(-1.0, 1.0))
    val sinPhi = sin(phi)
    return Vector3(
        cos(theta) * sinPhi,
        sin(theta) * sinPhi,
        cos(phi)
    )
}

fun (Random).randomQuaternion(): Quaternion {
    val theta = this.nextDouble(0.0, PI * 2)
    val phi = acos(this.nextDouble(-1.0, 1.0))
    val sinPhi = sin(phi)
    val gamma = this.nextDouble(0.0, PI * 2)
    val sinGamma = sin(gamma)

    return Quaternion(
        cos(theta) * sinPhi * sinGamma,
        sin(theta) * sinPhi * sinGamma,
        cos(phi) * sinGamma,
        cos(gamma)
    )
}

fun (List<Vector3>).toMesh(): ArrayMesh {
    val surfaceArray = VariantArray<Any?>()
    surfaceArray.resize(Mesh.ArrayType.MAX.ordinal)
    surfaceArray[Mesh.ArrayType.VERTEX.ordinal] = PackedVector3Array(this)

    return ArrayMesh().apply { addSurfaceFromArrays(Mesh.PrimitiveType.POINTS, surfaceArray) }
}

fun (Vector3).copy() = Vector3(this)
fun (Vector2).copy() = Vector2(this)

fun (Double).adjustRange(oldRange: ClosedRange<Double>, newRange: ClosedRange<Double>): Double =
    (this - oldRange.start) / (oldRange.endInclusive - oldRange.start) * (newRange.endInclusive - newRange.start) + newRange.start


data class Sphere(val center: Vector3, val radius: Double) {
    companion object {
        val ZERO = Sphere(Vector3.ZERO, 0.0)
        val ONE = Sphere(Vector3.ZERO, 1.0)
    }
}
data class Ray(val origin: Vector3, val direction: Vector3) {
    fun intersectsSphere(sphere: Sphere): Boolean {
        val v1 = sphere.center - origin
        val v2 = v1.project(direction)
        return v1.distanceTo(v2) <= sphere.radius
    }
}
