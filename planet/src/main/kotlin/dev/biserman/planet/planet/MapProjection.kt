package dev.biserman.planet.planet

import dev.biserman.planet.geometry.GeoPoint
import dev.biserman.planet.geometry.Kriging
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.toRTree
import dev.biserman.planet.geometry.toVector2
import dev.biserman.planet.geometry.toVector3
import dev.biserman.planet.geometry.weightedAverageInverse
import godot.core.Color
import godot.core.Vector2
import godot.core.Vector3
import godot.global.GD
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class MapProjection(val forward: (GeoPoint) -> Vector2, val backward: (Vector2) -> GeoPoint)

object MapProjections {
    val EQUIDISTANT = MapProjection({ geoPoint -> geoPoint.toVector2() }, { vector2 -> GeoPoint(vector2) })

    fun (MapProjection).projectPoints(
        planet: Planet,
        imageName: String,
        imageX: Int,
        imageY: Int,
        colorFn: (Planet).(Vector3) -> Color
    ): BufferedImage {
        val image = BufferedImage(imageX, imageY, BufferedImage.TYPE_INT_ARGB)
        for (x in 0..<imageX) {
            for (y in 0..<imageY) {
                image.setRGB(
                    x,
                    y,
                    planet.colorFn(
                        this.backward(
                            Vector2(
                                1 - ((x.toDouble() / imageX) - 0.5),
                                y.toDouble() / imageY - 0.5,
                            )
                        ).toVector3()
                    ).toARGB32()
                )
            }
        }

        ImageIO.write(image, "png", File(imageName))
        return image
    }

    fun (MapProjection).projectTiles(
        planet: Planet,
        imageName: String,
        imageX: Int,
        imageY: Int,
        useKriging: Boolean = true,
        sampleRadius: Double = planet.topology.averageRadius,
        variogram: (Double) -> Double = Kriging.variogram(sampleRadius, 1.0, 0.0),
        colorFn: (PlanetTile) -> Color,
    ): BufferedImage =
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

            if (useKriging) {
                Color(
                    Kriging.interpolate(nearestR, point, variogram),
                    Kriging.interpolate(nearestG, point, variogram),
                    Kriging.interpolate(nearestB, point, variogram),
                    Kriging.interpolate(nearestA, point, variogram),
                )
            } else {
                Color(
                    nearestR.first().second,
                    nearestG.first().second,
                    nearestB.first().second,
                    nearestA.first().second,
                )
            }
        }

    fun (MapProjection).applyValueTo(planet: Planet, imageName: String, modifyFn: (PlanetTile).(Color) -> Unit) {
        val image = ImageIO.read(File(imageName))
        val imageRTree = (0..<image.width).flatMap { x ->
            (0..<image.height).map { y ->
                Pair(
                    Vector2(x.toDouble() / image.width - 0.5, (image.height - y.toDouble() - 1) / image.height - 0.5),
                    image.getRGB(x, y).toRGB()
                )
            }
        }.toRTree(dimensions = 2) { it.first.toPoint() to it.second }

        val testPoints =
            planet.topology.rTree.nearest(Vector3.RIGHT.toPoint(), planet.topology.averageRadius * 10, 2)
        val distanceGuess = min(
            this.forward(testPoints.first().value().position.toGeoPoint())
                .distanceTo(this.forward(testPoints.last().value().position.toGeoPoint())),
            max(1.0 / image.width, 1.0 / image.height)
        ) * 1.5

        planet.planetTiles.values.forEach { tile ->
            val samples =
                imageRTree.nearest(this.forward(tile.tile.position.toGeoPoint()).toPoint(), distanceGuess, 1)
            val nearest = samples.firstOrNull()?.value()
            if (nearest != null) {
                modifyFn(tile, nearest)
            }
        }
    }

    // turn java RGB int to Godot color
    private fun Int.toRGB(): Color {
        val r = (this shr 16) and 0xFF
        val g = (this shr 8) and 0xFF
        val b = this and 0xFF
        return Color(r / 255.0, g / 255.0, b / 255.0)
    }
}