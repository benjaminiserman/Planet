package dev.biserman.planet.geometry

import com.github.davidmoten.rtreemulti.geometry.Point
import godot.api.ArrayMesh
import godot.api.Mesh
import godot.core.*
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

fun centroid(verts: List<Vector3>): Vector3 =
    verts.fold(Vector3.ZERO) { a, b -> a + b } / verts.size

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

fun (Vector3).tangent(v: Vector3) = this - v * this.dot(v)


fun (Double).adjustRange(oldRange: ClosedRange<Double>, newRange: ClosedRange<Double>): Double =
    (this - oldRange.start) / (oldRange.endInclusive - oldRange.start) * (newRange.endInclusive - newRange.start) + newRange.start

fun (Float).adjustRange(oldRange: ClosedRange<Float>, newRange: ClosedRange<Float>): Float =
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

data class DebugVector(val origin: Vector3, val vector: Vector3, val color: Color = Color.red) {
    fun crossOff(magnitude: Double) = crossOff(origin, vector, magnitude)

    companion object {
        fun crossOff(origin: Vector3, vector: Vector3, magnitude: Double) =
            DebugVector(
                origin + vector,
                vector.cross(origin + vector) * magnitude
            )
    }
}

fun (List<DebugVector>).toMesh(): MutMesh {
    val mutVerts = this.flatMap { listOf(it.origin, it.origin + it.vector) }.withIndex()
        .map { (i, vertex) -> MutVertex(vertex, mutableListOf(i / 2)) }.toMutableList()
    val mutEdges = this.withIndex().map { i -> MutEdge(mutableListOf(i.index * 2, i.index * 2 + 1)) }.toMutableList()

    return MutMesh(mutVerts, mutEdges)
}

fun torque(forces: Iterable<Pair<Vector3, Vector3>>) = forces.fold(Vector3.ZERO) { sum, (position, force) ->
    sum + position.cross(force)
}

fun eulerPole(torque: Vector3, points: Iterable<Pair<Vector3, Double>>): Vector3 {
    var inertiaTensor = Mat3.zero()
    for ((point, mass) in points) {
        val outer = Mat3.fromOuter(point, point)
        val contribution = Mat3.identity() - outer
        inertiaTensor += contribution * mass
    }

    return inertiaTensor.inverse() * torque
}

fun (Vector3).toPoint(): Point = Point.create(this.x, this.y, this.z)
fun (Point).toVector3(): Vector3 {
    val values = this.values()
    return Vector3(values[0], values[1], values[2])
}

