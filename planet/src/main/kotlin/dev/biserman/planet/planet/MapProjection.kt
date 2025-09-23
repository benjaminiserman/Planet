package dev.biserman.planet.planet

import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.weightedAverage
import dev.biserman.planet.geometry.weightedAverageInverse
import godot.core.Color
import godot.core.Vector2
import godot.core.Vector3
import godot.global.GD
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MapProjection(val forward: (Vector3) -> Vector2, val backward: (Vector2) -> Vector3) {
    companion object {
        fun make(
            forward: (Vector2) -> Vector2,
            backward: (Vector2) -> Vector2,
        ) = MapProjection(
            { point: Vector3 -> forward(pointToGeo(point)) },
            { geo: Vector2 -> geoToPoint(backward(geo)) },
        )
    }
}

fun geoToPoint(latitudeLongitude: Vector2): Vector3 {
    val phi = latitudeLongitude.y * 2 * Math.PI
    val theta = latitudeLongitude.x * 2 * Math.PI

    return Vector3(
        cos(theta) * sin(phi),
        cos(phi),
        sin(theta) * sin(phi)
    )
}

fun pointToGeo(point: Vector3): Vector2 {
    val latitude = asin(point.z) * 180 / Math.PI
    val longitude = atan2(point.y, point.x) * 180 / Math.PI

    return Vector2(longitude, latitude)
}

object MapProjections {
    val EQUIDISTANT = MapProjection.make({ latLong: Vector2 -> latLong }, { pixel: Vector2 -> pixel })

    fun (MapProjection).projectPoints(
        planet: Planet,
        imageName: String,
        imageX: Int,
        imageY: Int,
        colorFn: (Planet).(Vector3) -> Color
    ) {
        val image = BufferedImage(imageX, imageY, BufferedImage.TYPE_INT_ARGB)
        for (x in 0..<imageX) {
            for (y in 0..<imageY) {
                image.setRGB(
                    x,
                    y,
                    planet.colorFn(
                        this.backward(
                            Vector2(
                                1 - (x.toDouble() / imageX),
                                (y.toDouble() / imageY) * 0.5,
                            )
                        )
                    )
                        .toARGB32()
                )
            }
        }

        ImageIO.write(image, "png", File(imageName))
    }

    fun (MapProjection).projectTiles(
        planet: Planet,
        imageName: String,
        imageX: Int,
        imageY: Int,
        sampleRadius: Double = planet.topology.averageRadius,
        variogram: (Double) -> Double = Kriging.variogram(sampleRadius, 1.0, 0.0),
        colorFn: (PlanetTile) -> Color,
    ) {
        this.projectPoints(
            planet,
            imageName,
            imageX,
            imageY
        ) { point ->
            val nearest = this.topology.rTree.nearest(point.toPoint(), sampleRadius, 10)
                .map { it.value().position to colorFn(planet.getTile(it.value())) }
            val nearestR = nearest.map { (position, color) -> position to color.r }
            val nearestG = nearest.map { (position, color) -> position to color.g }
            val nearestB = nearest.map { (position, color) -> position to color.b }
            val nearestA = nearest.map { (position, color) -> position to color.a }

//            Color(
//                Kriging.interpolate(nearestR, point, variogram),
//                Kriging.interpolate(nearestG, point, variogram),
//                Kriging.interpolate(nearestB, point, variogram),
//                Kriging.interpolate(nearestA, point, variogram),
//            )
            Color(
//                nearestR.weightedAverageInverse(point, sampleRadius),
//                nearestG.weightedAverageInverse(point, sampleRadius),
//                nearestB.weightedAverageInverse(point, sampleRadius),
//                nearestA.weightedAverageInverse(point, sampleRadius),
                nearestR.first().second,
                nearestG.first().second,
                nearestB.first().second,
                nearestA.first().second,
            )
        }
    }
}