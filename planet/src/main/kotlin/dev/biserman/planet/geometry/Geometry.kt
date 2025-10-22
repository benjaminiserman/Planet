package dev.biserman.planet.geometry

import com.github.davidmoten.rtreemulti.Entry
import com.github.davidmoten.rtreemulti.RTree
import com.github.davidmoten.rtreemulti.geometry.Geometry
import com.github.davidmoten.rtreemulti.geometry.Point
import dev.biserman.planet.geometry.component1
import dev.biserman.planet.geometry.component2
import dev.biserman.planet.planet.PointForce
import godot.api.ArrayMesh
import godot.api.Mesh
import godot.core.*
import kotlin.collections.fold
import kotlin.math.*
import kotlin.random.Random

fun calculateNormal(p1: Vector3, p2: Vector3, p3: Vector3): Vector3 {
    val a = p2 - p1
    val b = p3 - p1

    return -Vector3(
        a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x
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

fun centroid(verts: List<Vector3>): Vector3 = verts.fold(Vector3.ZERO) { a, b -> a + b } / verts.size

fun (Random).randomUnitVector(): Vector3 {
    val theta = this.nextDouble(0.0, PI * 2)
    val phi = acos(this.nextDouble(-1.0, 1.0))
    val sinPhi = sin(phi)
    return Vector3(
        cos(theta) * sinPhi, sin(theta) * sinPhi, cos(phi)
    )
}

fun (Random).randomQuaternion(): Quaternion {
    val theta = this.nextDouble(0.0, PI * 2)
    val phi = acos(this.nextDouble(-1.0, 1.0))
    val sinPhi = sin(phi)
    val gamma = this.nextDouble(0.0, PI * 2)
    val sinGamma = sin(gamma)

    return Quaternion(
        cos(theta) * sinPhi * sinGamma, sin(theta) * sinPhi * sinGamma, cos(phi) * sinGamma, cos(gamma)
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
        fun crossOff(origin: Vector3, vector: Vector3, magnitude: Double) = DebugVector(
            origin + vector, vector.cross(origin + vector) * magnitude
        )
    }
}

fun (List<DebugVector>).toMesh(): MutMesh {
    val mutVerts = this.flatMap { listOf(it.origin, it.origin + it.vector) }
        .withIndex()
        .map { (i, vertex) -> MutVertex(vertex, mutableListOf(i / 2)) }
        .toMutableList()
    val mutEdges = this.withIndex().map { i -> MutEdge(mutableListOf(i.index * 2, i.index * 2 + 1)) }.toMutableList()

    return MutMesh(mutVerts, mutEdges)
}

fun torque(forces: Iterable<PointForce>) = forces.fold(Vector3.ZERO) { sum, (position, force) ->
    sum + position.cross(force)
}

fun eulerPole(torque: Vector3, points: Collection<Pair<Vector3, Double>>): Vector3 {
    var inertiaTensor = Mat3.identity() * 1e-3
    for ((point, mass) in points) {
        val outer = Mat3.fromOuter(point, point)
        val contribution = Mat3.identity() - outer
        inertiaTensor += contribution * mass
    }

    return inertiaTensor.inverse() * torque
}

fun Collection<Vector3>.average(): Vector3 = this.fold(Vector3.ZERO) { sum, v -> sum + v } / this.size

fun (Iterable<Pair<Vector3, Double>>).weightedAverageInverse(reference: Vector3, maxDistance: Double) =
    this.weightedAverage(reference) { 1 - (it.distanceTo(reference) / maxDistance) }

fun (Iterable<Pair<Vector3, Double>>).weightedAverageInverse(reference: Vector3) =
    this.weightedAverage(reference) { 1 / it.distanceTo(reference) }

fun (Iterable<Pair<Vector3, Double>>).weightedAverage(reference: Vector3) =
    this.weightedAverage(reference) { point -> reference.distanceTo(point) }

fun <T> (Iterable<Pair<T, Double>>).weightedAverage(
    reference: Vector3, contributionFn: (T) -> Double
): Double {
    val contributions = this.map { (point, value) -> Pair(value, contributionFn(point)) }
    val contributionSum = contributions.sumOf { it.second }
    if (contributionSum == 0.0) {
        return 0.0
    }
    return contributions.sumOf { it.first * it.second } / contributionSum
}

fun (Vector3).toPoint(): Point = Point.create(this.x, this.y, this.z)
fun (Vector2).toPoint(): Point = Point.create(this.x, this.y)
fun (Point).toVector3(): Vector3 {
    val values = this.values()
    return Vector3(values[0], values[1], values[2])
}

fun (Point).toVector2(): Vector2 {
    val values = this.values()
    return Vector2(values[0], values[1])
}

fun <T, U> (Iterable<T>).toRTree(dimensions: Int = 3, getFn: (T) -> Pair<Point, U>): RTree<U, Point> {
    return RTree.star().dimensions(dimensions).create<U, Point>().add(this.map {
        val (point, value) = getFn(it)
        Entry.entry(value, point)
    })
}

// in radians
data class GeoPoint(val latitude: Double, val longitude: Double) {
    constructor (point: Vector3) : this(
        asin(point.y),
        -atan2(point.z, point.x)
    )

    constructor (pixel: Vector2) : this(
        pixel.y * PI,
        pixel.x * PI * 2
    )

    fun toVector3(): Vector3 {
        return Vector3(
            cos(longitude) * cos(latitude),
            sin(-latitude),
            sin(longitude) * cos(latitude)
        )
    }

    fun toVector2(): Vector2 {
        return Vector2(
            longitude / (PI * 2),
            latitude / PI
        )
    }

    val latitudeDegrees get() = latitude * 180 / PI
    val longitudeDegrees get() = longitude * 180 / PI

    fun formatDigits(digits: Int = 2): String {
        val northSouth = if (latitudeDegrees >= 0) "N" else "S"
        val eastWest = if (longitudeDegrees >= 0) "E" else "W"

        return "%.${digits}f $northSouth, %.${digits}f $eastWest".format(
            latitudeDegrees.absoluteValue,
            longitudeDegrees.absoluteValue
        )
    }

    companion object {
        fun fromDegrees(latitudeDegrees: Double, longitudeDegrees: Double): GeoPoint {
            return GeoPoint(
                latitudeDegrees * PI / 180,
                longitudeDegrees * PI / 180
            )
        }
    }
}

fun (Vector2).toGeoPoint() = GeoPoint(this)
fun (Vector3).toGeoPoint() = GeoPoint(this)

//fun <T> (Iterable<T>).toRTree(getFn: (T) -> Point): RTree<T, Point> = RTree
//    .star()
//    .dimensions(3)
//    .create<T, Point>()
//    .add(this.map { Entry.entry(it, getFn(it)) })

fun sigmoid(x: Double, xScalar: Double = -1.0, xOffset: Double = 0.0) = 1.0 / (1 + E.pow(xScalar * (x + xOffset)))
fun sigmoid(x: Float, xScalar: Float = -1.0f, xOffset: Float = 0.0f) =
    1f / (1 + E.toFloat().pow(xScalar * (x + xOffset)))

fun intersectRaySphere(
    rayOrigin: Vector3, rayDir: Vector3, sphereCenter: Vector3, sphereRadius: Double
): Double? {
    val oc = rayOrigin - sphereCenter
    val a = rayDir.dot(rayDir)
    val b = 2.0 * oc.dot(rayDir)
    val c = oc.dot(oc) - sphereRadius * sphereRadius
    val discriminant = b * b - 4 * a * c

    return if (discriminant < 0) null
    else (-b - sqrt(discriminant)) / (2f * a)
}

operator fun <T, S : Geometry> (Entry<T, S>).component1(): T = this.value()
operator fun <T, S : Geometry> (Entry<T, S>).component2(): S = this.geometry()

fun (Double).scaleAndCoerceIn(expectedRange: ClosedRange<Double>, newRange: ClosedRange<Double>) =
    this.adjustRange(expectedRange, newRange).coerceIn(newRange)

fun (Double).scaleAndCoerce01(expectedRange: ClosedRange<Double>) = this.scaleAndCoerceIn(expectedRange, 0.0..1.0)
fun (Double).scaleAndCoerceUnit(expectedRange: ClosedRange<Double>) = this.scaleAndCoerceIn(expectedRange, -1.0..1.0)
