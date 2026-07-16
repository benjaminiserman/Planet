package dev.biserman.planet.planet.climate

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.biserman.planet.geometry.GeoPoint
import dev.biserman.planet.geometry.toGeoPoint
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.geometry.toVector2
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import godot.core.Color
import godot.core.Vector2
import java.awt.image.BufferedImage
import java.io.File
import java.lang.reflect.Modifier
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrNull
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Scores simulated land climates against the painted equirectangular reference. */
object HersfeldtReference {
    const val defaultFilename = "earth_hersfeldt_reference.png"

    data class ClimateLabel(val id: String, val name: String, val color: String)

    data class ConfusionEntry(
        val reference: ClimateLabel,
        val simulated: ClimateLabel,
        val count: Int,
        val conditionDistance: Int,
    )

    data class ClassFrequency(
        val climate: ClimateLabel,
        val referenceCount: Int,
        val simulatedCount: Int,
        val simulatedMinusReference: Int,
    )

    data class BandScore(
        val band: String,
        val sampledTiles: Int,
        val exactMatches: Int,
        val matchPercent: Double,
        val meanConditionDistance: Double,
    )

    data class FamilyScore(
        val family: String,
        val referenceCount: Int,
        val simulatedCount: Int,
        val truePositives: Int,
        val precision: Double,
        val recall: Double,
        val f1: Double,
    )

    data class DiagnosticAverages(
        val winterTemperature: Double?,
        val summerTemperature: Double?,
        val averageTemperature: Double?,
        val annualPrecipitation: Double?,
        val temperatureRange: Double?,
        val precipitationRange: Double?,
        val potentialEvapotranspiration: Double?,
        val actualEvapotranspiration: Double?,
        val aridityFactor: Double?,
        val growthAridityFactor: Double?,
        val growthSupply: Double?,
        val evaporationRatio: Double?,
        val totalGdd: Double?,
        val totalGddz: Double?,
        val totalGint: Double?,
        val winterTypes: Map<String, Int>,
        val summerTypes: Map<String, Int>,
    )

    data class MismatchSummary(
        val reference: ClimateLabel,
        val simulated: ClimateLabel,
        val count: Int,
        val conditionDistance: Int,
        val simulatedClassifierInputs: DiagnosticAverages,
    )

    data class ClassCount(val climate: ClimateLabel, val count: Int)

    data class ScoreDelta(
        val lossDelta: Double,
        val matchPercentDelta: Double,
        val correctedTiles: Int,
        val regressedTiles: Int,
        val changedSimulatedClasses: Int,
        val correctedByReferenceClass: List<ClassCount>,
        val regressedByReferenceClass: List<ClassCount>,
    )

    data class TileComparison(
        val tileId: Int,
        val reference: ClimateLabel,
        val simulated: ClimateLabel,
        val exact: Boolean,
        val conditionDistance: Int,
        val latitudeDegrees: Double,
        val longitudeDegrees: Double,
        val elevationMeters: Double,
        @get:JsonIgnore val diagnostics: Hersfeldt.Diagnostics,
    )

    data class Score(
        val landTiles: Int,
        val sampledTiles: Int,
        val exactMatches: Int,
        val referenceMisses: Int,
        val referenceCoveragePercent: Double,
        val meanConditionDistance: Double,
        val conditionDistanceCounts: Map<Int, Int>,
        val meanSpatialTolerancePixels: Double,
        val confusionMatrix: List<ConfusionEntry>,
        val classFrequencies: List<ClassFrequency>,
        val latitudeBands: List<BandScore>,
        val elevationBands: List<BandScore>,
        val priorityRegions: List<BandScore>,
        val mediterraneanFamily: FamilyScore,
        val largestMismatches: List<MismatchSummary>,
        @get:JsonIgnore val tileComparisons: Map<Int, TileComparison>,
    ) {
        val matchPercent get() = exactMatches * 100.0 / sampledTiles
        val mismatchRate get() = 1.0 - exactMatches.toDouble() / sampledTiles

        /** Climate-only loss. Reference-mask misses are reported but excluded. */
        val loss get() = mismatchRate + CONDITION_DISTANCE_WEIGHT * meanConditionDistance / MAX_CONDITION_DISTANCE

        fun summary() =
            "Earth land: %.1f%% match (loss %.4f, %d/%d covered, %d mask misses, mean error %.3f)".format(
                matchPercent,
                loss,
                sampledTiles,
                landTiles,
                referenceMisses,
                meanConditionDistance,
            )

        fun deltaFrom(baseline: Score): ScoreDelta {
            val commonTileIds = tileComparisons.keys intersect baseline.tileComparisons.keys
            val corrected = commonTileIds.mapNotNull { tileId ->
                val before = baseline.tileComparisons.getValue(tileId)
                val after = tileComparisons.getValue(tileId)
                after.takeIf { !before.exact && after.exact }
            }
            val regressed = commonTileIds.mapNotNull { tileId ->
                val before = baseline.tileComparisons.getValue(tileId)
                val after = tileComparisons.getValue(tileId)
                after.takeIf { before.exact && !after.exact }
            }
            return ScoreDelta(
                lossDelta = loss - baseline.loss,
                matchPercentDelta = matchPercent - baseline.matchPercent,
                correctedTiles = corrected.size,
                regressedTiles = regressed.size,
                changedSimulatedClasses = commonTileIds.count { tileId ->
                    tileComparisons.getValue(tileId).simulated.id !=
                        baseline.tileComparisons.getValue(tileId).simulated.id
                },
                correctedByReferenceClass = corrected.toClassCounts(),
                regressedByReferenceClass = regressed.toClassCounts(),
            )
        }
    }

    data class RenderedMaps(val simulatedMap: String, val differenceMap: String)

    fun score(planet: Planet, filename: String = defaultFilename): Score? {
        val file = File(filename)
        if (!file.isFile) return null
        val palette = hersfeldtPalette()
        HersfeldtClassificationGraph.validate(palette.map { it.id })
        val raster = ReferenceRaster(ImageIO.read(file) ?: return null, palette)
        val landTiles = planet.planetTiles.values.filter { it.isAboveWater }
        val comparisons = mutableListOf<TileComparison>()
        var referenceMisses = 0
        var totalSpatialTolerancePixels = 0

        landTiles.forEach { tile ->
            val simulated = tile.hersfeldt.getOrNull() ?: return@forEach
            val tolerancePixels = tile.spatialTolerancePixels(raster.image)
            totalSpatialTolerancePixels += tolerancePixels
            val reference = raster.majorityClimate(
                tile.tile.position.toGeoPoint().toVector2(),
                tolerancePixels,
            )
            if (reference == null) {
                referenceMisses++
                return@forEach
            }
            val datum = planet.climateMap[tile.tileId] ?: return@forEach
            val geoPoint = tile.tile.position.toGeoPoint()
            comparisons += TileComparison(
                tileId = tile.tileId,
                reference = reference.toLabel(),
                simulated = simulated.toLabel(),
                exact = reference.id == simulated.id,
                conditionDistance = HersfeldtClassificationGraph.distance(reference.id, simulated.id),
                latitudeDegrees = geoPoint.latitudeDegrees,
                longitudeDegrees = geoPoint.longitudeDegrees,
                elevationMeters = tile.elevationAboveSeaLevel,
                diagnostics = Hersfeldt.diagnostics(datum),
            )
        }

        if (comparisons.isEmpty()) return null
        val exactMatches = comparisons.count { it.exact }
        return Score(
            landTiles = landTiles.size,
            sampledTiles = comparisons.size,
            exactMatches = exactMatches,
            referenceMisses = referenceMisses,
            referenceCoveragePercent = comparisons.size * 100.0 / landTiles.size,
            meanConditionDistance = comparisons.map { it.conditionDistance }.average(),
            conditionDistanceCounts = comparisons
                .groupingBy { it.conditionDistance }
                .eachCount()
                .toSortedMap(),
            meanSpatialTolerancePixels = totalSpatialTolerancePixels.toDouble() / landTiles.size,
            confusionMatrix = comparisons.confusionMatrix(),
            classFrequencies = comparisons.classFrequencies(),
            latitudeBands = comparisons.bandScores { latitudeBand(it.latitudeDegrees) },
            elevationBands = comparisons.bandScores { elevationBand(it.elevationMeters) },
            priorityRegions = PRIORITY_REGIONS.mapNotNull { region ->
                comparisons.filter(region::contains).takeIf { it.isNotEmpty() }
                    ?.toBandScore(region.name)
            },
            mediterraneanFamily = comparisons.familyScore(
                family = "Mediterranean",
                isMember = { it.name.contains("mediterranean", ignoreCase = true) },
            ),
            largestMismatches = comparisons.mismatchSummaries(),
            tileComparisons = comparisons.associateBy { it.tileId },
        )
    }

    fun renderMaps(
        planet: Planet,
        filename: String = defaultFilename,
        outputDirectory: File,
        prefix: String,
    ): RenderedMaps? {
        val referenceFile = File(filename)
        if (!referenceFile.isFile) return null
        val palette = hersfeldtPalette()
        HersfeldtClassificationGraph.validate(palette.map { it.id })
        val raster = ReferenceRaster(ImageIO.read(referenceFile) ?: return null, palette)
        val simulated = BufferedImage(raster.image.width, raster.image.height, BufferedImage.TYPE_INT_ARGB)
        val difference = BufferedImage(raster.image.width, raster.image.height, BufferedImage.TYPE_INT_ARGB)
        val white = 0xFFFFFFFF.toInt()

        for (x in 0 until raster.image.width) {
            for (y in 0 until raster.image.height) {
                val reference = raster.climateAt(x, y)
                if (reference == null) {
                    simulated.setRGB(x, y, white)
                    difference.setRGB(x, y, white)
                    continue
                }
                val projected = Vector2(
                    0.5 - (x + 0.5) / raster.image.width,
                    0.5 - (y + 0.5) / raster.image.height,
                )
                val nearestTile = planet.topology.rTree.nearest(
                    GeoPoint(projected).toVector3().toPoint(),
                    planet.topology.averageRadius * 2.0,
                    1,
                ).firstOrNull()?.value()?.let(planet::getTile)
                val simulatedClimate = nearestTile
                    ?.takeIf { it.isAboveWater }
                    ?.hersfeldt
                    ?.getOrNull()
                if (simulatedClimate == null) {
                    simulated.setRGB(x, y, MASK_MISMATCH_ARGB)
                    difference.setRGB(x, y, MASK_MISMATCH_ARGB)
                    continue
                }

                simulated.setRGB(x, y, simulatedClimate.color.toArgb())
                difference.setRGB(
                    x,
                    y,
                    conditionDistanceArgb(
                        HersfeldtClassificationGraph.distance(reference.id, simulatedClimate.id),
                    ),
                )
            }
        }

        outputDirectory.mkdirs()
        val simulatedFile = File(outputDirectory, "$prefix-simulated.png")
        val differenceFile = File(outputDirectory, "$prefix-difference.png")
        ImageIO.write(simulated, "png", simulatedFile)
        ImageIO.write(difference, "png", differenceFile)
        return RenderedMaps(simulatedFile.absolutePath, differenceFile.absolutePath)
    }

    private class ReferenceRaster(
        val image: BufferedImage,
        private val palette: List<ClimateClassification>,
    ) {
        private val colorCache = mutableMapOf<Int, ClimateClassification>()

        fun climateAt(x: Int, y: Int): ClimateClassification? {
            val argb = image.getRGB(x.floorMod(image.width), y.coerceIn(0, image.height - 1))
            if ((argb ushr 24) < 128 || (argb and 0xFFFFFF) == 0xFFFFFF) return null
            val rgb = argb and 0xFFFFFF
            return colorCache.getOrPut(rgb) {
                val color = argb.toColor()
                palette.minWith(compareBy<ClimateClassification> { it.color.distanceTo(color) }.thenBy { it.id })
            }
        }

        fun majorityClimate(point: Vector2, tolerancePixels: Int): ClimateClassification? {
            val center = point.toReferencePixel(image)
            val counts = mutableMapOf<String, Int>()
            for (deltaY in -tolerancePixels..tolerancePixels) {
                for (deltaX in -tolerancePixels..tolerancePixels) {
                    if (deltaX * deltaX + deltaY * deltaY > tolerancePixels * tolerancePixels) continue
                    val climate = climateAt(center.first.roundToInt() + deltaX, center.second.roundToInt() + deltaY)
                        ?: continue
                    counts[climate.id] = counts.getOrDefault(climate.id, 0) + 1
                }
            }
            val selectedId = counts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .firstOrNull()
                ?.key
                ?: return null
            return palette.first { it.id == selectedId }
        }
    }

    private fun hersfeldtPalette(): List<ClimateClassification> = Hersfeldt::class.java.declaredFields
        .asSequence()
        .filter { it.type == ClimateClassification::class.java }
        .mapNotNull { field ->
            field.trySetAccessible()
            field.get(if (Modifier.isStatic(field.modifiers)) null else Hersfeldt) as? ClimateClassification
        }
        .filter { it.id != UNKNOWN_CLIMATE.id }
        .distinctBy { it.id }
        .sortedBy { it.id }
        .toList()
        .also { require(it.isNotEmpty()) { "Could not discover the Hersfeldt color palette" } }

    private fun List<TileComparison>.confusionMatrix() = groupBy { it.reference.id to it.simulated.id }
        .map { (_, tiles) ->
            ConfusionEntry(
                tiles.first().reference,
                tiles.first().simulated,
                tiles.size,
                tiles.first().conditionDistance,
            )
        }
        .sortedWith(compareByDescending<ConfusionEntry> { it.count }.thenBy { it.reference.id }.thenBy { it.simulated.id })

    private fun List<TileComparison>.classFrequencies(): List<ClassFrequency> {
        val labels = flatMap { listOf(it.reference, it.simulated) }.associateBy { it.id }
        val referenceCounts = groupingBy { it.reference.id }.eachCount()
        val simulatedCounts = groupingBy { it.simulated.id }.eachCount()
        return labels.values.map { label ->
            val referenceCount = referenceCounts[label.id] ?: 0
            val simulatedCount = simulatedCounts[label.id] ?: 0
            ClassFrequency(label, referenceCount, simulatedCount, simulatedCount - referenceCount)
        }.sortedWith(compareByDescending<ClassFrequency> { kotlin.math.abs(it.simulatedMinusReference) }.thenBy { it.climate.id })
    }

    private fun List<TileComparison>.bandScores(selector: (TileComparison) -> String) = groupBy(selector)
        .map { (band, tiles) ->
            BandScore(
                band = band,
                sampledTiles = tiles.size,
                exactMatches = tiles.count { it.exact },
                matchPercent = tiles.count { it.exact } * 100.0 / tiles.size,
                meanConditionDistance = tiles.map { it.conditionDistance }.average(),
            )
        }.sortedBy { it.band }

    private fun List<TileComparison>.mismatchSummaries() = filterNot { it.exact }
        .groupBy { it.reference.id to it.simulated.id }
        .map { (_, tiles) ->
            MismatchSummary(
                reference = tiles.first().reference,
                simulated = tiles.first().simulated,
                count = tiles.size,
                conditionDistance = tiles.first().conditionDistance,
                simulatedClassifierInputs = tiles.map { it.diagnostics }.averages(),
            )
        }
        .sortedByDescending { it.count }
        .take(MAX_REPORTED_MISMATCHES)

    private fun List<Hersfeldt.Diagnostics>.averages() = DiagnosticAverages(
        winterTemperature = finiteAverage { it.winterTemperature },
        summerTemperature = finiteAverage { it.summerTemperature },
        averageTemperature = finiteAverage { it.averageTemperature },
        annualPrecipitation = finiteAverage { it.annualPrecipitation },
        temperatureRange = finiteAverage { it.temperatureRange },
        precipitationRange = finiteAverage { it.precipitationRange },
        potentialEvapotranspiration = finiteAverage { it.potentialEvapotranspiration },
        actualEvapotranspiration = finiteAverage { it.actualEvapotranspiration },
        aridityFactor = finiteAverage { it.aridityFactor },
        growthAridityFactor = finiteAverage { it.growthAridityFactor },
        growthSupply = finiteAverage { it.growthSupply },
        evaporationRatio = finiteAverage { it.evaporationRatio },
        totalGdd = finiteAverage { it.totalGdd },
        totalGddz = finiteAverage { it.totalGddz },
        totalGint = finiteAverage { it.totalGint },
        winterTypes = groupingBy { it.winterType.name }.eachCount().toSortedMap(),
        summerTypes = groupingBy { it.summerType.name }.eachCount().toSortedMap(),
    )

    private fun List<Hersfeldt.Diagnostics>.finiteAverage(selector: (Hersfeldt.Diagnostics) -> Double): Double? =
        map(selector).filter { it.isFinite() }.takeIf { it.isNotEmpty() }?.average()

    private fun List<TileComparison>.toClassCounts() = groupBy { it.reference.id }
        .map { (_, tiles) -> ClassCount(tiles.first().reference, tiles.size) }
        .sortedByDescending { it.count }
        .take(MAX_REPORTED_CLASS_DELTAS)

    private fun latitudeBand(latitude: Double): String {
        val lower = (floor((latitude + 90.0) / 10.0) * 10.0 - 90.0).toInt().coerceIn(-90, 80)
        return "%+03d..%+03d deg".format(lower, lower + 10)
    }

    private data class GeographicRegion(
        val name: String,
        val minLatitude: Double,
        val maxLatitude: Double,
        val minLongitude: Double,
        val maxLongitude: Double,
    ) {
        fun contains(comparison: TileComparison) =
            comparison.latitudeDegrees in minLatitude..maxLatitude &&
                comparison.longitudeDegrees in minLongitude..maxLongitude
    }

    private fun List<TileComparison>.toBandScore(name: String) = BandScore(
        band = name,
        sampledTiles = size,
        exactMatches = count { it.exact },
        matchPercent = count { it.exact } * 100.0 / size,
        meanConditionDistance = map { it.conditionDistance }.average(),
    )

    private fun List<TileComparison>.familyScore(
        family: String,
        isMember: (ClimateLabel) -> Boolean,
    ): FamilyScore {
        val referenceCount = count { isMember(it.reference) }
        val simulatedCount = count { isMember(it.simulated) }
        val truePositives = count { isMember(it.reference) && isMember(it.simulated) }
        val precision = truePositives.toDouble() / simulatedCount.coerceAtLeast(1)
        val recall = truePositives.toDouble() / referenceCount.coerceAtLeast(1)
        val f1 = if (precision + recall == 0.0) 0.0 else 2.0 * precision * recall / (precision + recall)
        return FamilyScore(
            family = family,
            referenceCount = referenceCount,
            simulatedCount = simulatedCount,
            truePositives = truePositives,
            precision = precision,
            recall = recall,
            f1 = f1,
        )
    }

    private fun elevationBand(elevation: Double) = when {
        elevation < 500.0 -> "0000..0499 m"
        elevation < 1500.0 -> "0500..1499 m"
        elevation < 3000.0 -> "1500..2999 m"
        else -> "3000+ m"
    }

    private fun PlanetTile.spatialTolerancePixels(image: BufferedImage): Int {
        val center = tile.position.toGeoPoint().toVector2().toReferencePixel(image)
        val averageNeighborDistance = neighbors.map { neighbor ->
            val other = neighbor.tile.position.toGeoPoint().toVector2().toReferencePixel(image)
            val deltaX = (center.first - other.first).absoluteWrappedDistance(image.width)
            val deltaY = center.second - other.second
            sqrt(deltaX * deltaX + deltaY * deltaY)
        }.average()
        return ceil(averageNeighborDistance * 0.5).toInt().coerceIn(1, MAX_SPATIAL_TOLERANCE_PIXELS)
    }

    private fun Vector2.toReferencePixel(image: BufferedImage) = Pair(
        (0.5 - x) * image.width,
        (0.5 - y) * image.height,
    )

    private fun ClimateClassification.toLabel() = ClimateLabel(id, name, color.toHex())

    private fun Int.toColor() = Color(
        ((this shr 16) and 0xFF) / 255.0,
        ((this shr 8) and 0xFF) / 255.0,
        (this and 0xFF) / 255.0,
        ((this ushr 24) and 0xFF) / 255.0,
    )

    private fun Color.toArgb(): Int =
        (255 shl 24) or
            ((r * 255).roundToInt().coerceIn(0, 255) shl 16) or
            ((g * 255).roundToInt().coerceIn(0, 255) shl 8) or
            (b * 255).roundToInt().coerceIn(0, 255)

    private fun Color.toHex() = "#%02X%02X%02X".format(
        (r * 255).roundToInt().coerceIn(0, 255),
        (g * 255).roundToInt().coerceIn(0, 255),
        (b * 255).roundToInt().coerceIn(0, 255),
    )

    private fun Color.distanceTo(other: Color): Double = sqrt(
        (r - other.r) * (r - other.r) +
            (g - other.g) * (g - other.g) +
            (b - other.b) * (b - other.b),
    )

    private fun conditionDistanceArgb(distance: Int): Int = when (distance) {
        0 -> EXACT_ARGB
        1 -> ONE_OFF_ARGB
        2 -> TWO_OFF_ARGB
        3 -> THREE_OFF_ARGB
        4 -> FOUR_OFF_ARGB
        else -> FIVE_PLUS_OFF_ARGB
    }

    private fun Double.absoluteWrappedDistance(width: Int): Double {
        val distance = kotlin.math.abs(this)
        return minOf(distance, width - distance)
    }

    private fun Int.floorMod(modulus: Int) = ((this % modulus) + modulus) % modulus

    // GeoPoint longitudes are positive west and negative east.
    private val PRIORITY_REGIONS = listOf(
        GeographicRegion("Mediterranean (30-46N, 10W-40E)", 30.0, 46.0, -40.0, 10.0),
        GeographicRegion("Andes (55S-12N, 82-65W)", -55.0, 12.0, 65.0, 82.0),
        GeographicRegion("Northern Myanmar (20-30N, 92-102E)", 20.0, 30.0, -102.0, -92.0),
    )

    private const val MAX_SPATIAL_TOLERANCE_PIXELS = 16
    private const val MAX_REPORTED_MISMATCHES = 100
    private const val MAX_REPORTED_CLASS_DELTAS = 10
    private const val CONDITION_DISTANCE_WEIGHT = 0.25
    private const val MAX_CONDITION_DISTANCE = 5.0
    private const val EXACT_ARGB = 0xFF2EAD4F.toInt()
    private const val ONE_OFF_ARGB = 0xFFA8E635.toInt()
    private const val TWO_OFF_ARGB = 0xFFFFD700.toInt()
    private const val THREE_OFF_ARGB = 0xFFFF8C00.toInt()
    private const val FOUR_OFF_ARGB = 0xFFE53935.toInt()
    private const val FIVE_PLUS_OFF_ARGB = 0xFF8E24AA.toInt()
    private const val MASK_MISMATCH_ARGB = 0xFFFF00FF.toInt()
}
