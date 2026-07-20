package dev.biserman.planet.topology

import dev.biserman.planet.geometry.distortTriangles
import dev.biserman.planet.geometry.makeIcosahedron
import dev.biserman.planet.geometry.reorderVerts
import dev.biserman.planet.geometry.subdivideIcosahedron
import dev.biserman.planet.planet.tectonics.TectonicGlobals.tectonicAreaScale
import godot.core.Plane
import godot.core.Vector3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TopologyAreaTest {
    @Test
    fun `generated dual tiles have complete borders and positive area`() {
        val topology = generatedTopology()

        val incomplete = topology.tiles.filter { tile ->
            tile.borders.size != tile.corners.size || tile.tiles.size != tile.corners.size
        }
        assertTrue(incomplete.isEmpty()) {
            "Incomplete dual tiles: " + incomplete.take(10).joinToString { tile ->
                "id=${tile.id}, corners=${tile.corners.size}, borders=${tile.borders.size}, " +
                    "neighbors=${tile.tiles.size}, area=${tile.area}"
            }
        }

        val zeroArea = topology.tiles.filter { it.area <= 0.0 || !it.area.isFinite() }
        assertTrue(zeroArea.isEmpty()) {
            "Non-positive dual tile areas: " + zeroArea.take(10).joinToString { tile ->
                "id=${tile.id}, corners=${tile.corners.size}, borders=${tile.borders.size}, area=${tile.area}"
            }
        }
    }

    @Test
    fun `tectonic area scale is least squares fit to legacy plate inertia`() {
        val topology = generatedTopology()
        val legacyAreaByTileId = topology.tiles.associate { it.id to legacyTileArea(it) }
        var numerator = 0.0
        var denominator = 0.0

        repeat(20) { seed ->
            val random = Random(seed)
            val centers = topology.tiles.shuffled(random).take(random.nextInt(10, 15))
            val plates = topology.tiles.groupBy { tile ->
                centers.minBy { center -> center.position.distanceSquaredTo(tile.position) }
            }.values
            plates.forEach { tiles ->
                val corrected = inertiaContribution(tiles) { it.area }
                val legacy = inertiaContribution(tiles) { legacyAreaByTileId.getValue(it.id) }
                numerator += dot(corrected, legacy)
                denominator += dot(corrected, corrected)
            }
        }

        val fittedScale = numerator / denominator
        assertEquals(fittedScale, tectonicAreaScale, 1e-12)
    }

    private fun inertiaContribution(tiles: List<Tile>, area: (Tile) -> Double): DoubleArray {
        val result = DoubleArray(9)
        tiles.forEach { tile ->
            val coordinates = doubleArrayOf(tile.position.x, tile.position.y, tile.position.z)
            val mass = area(tile)
            for (row in 0..2) {
                for (column in 0..2) {
                    result[row * 3 + column] += mass * (
                        (if (row == column) 1.0 else 0.0) -
                            coordinates[row] * coordinates[column]
                    )
                }
            }
        }
        return result
    }

    private fun dot(left: DoubleArray, right: DoubleArray): Double =
        left.indices.sumOf { index -> left[index] * right[index] }

    private fun legacyTileArea(tile: Tile): Double = tile.borders.sumOf { border ->
        val p0 = tile.position
        val p1 = border.corners[0].position
        val p2 = border.corners[1].position
        val edge = p1 - p0
        val faceNormal = edge.cross(p2 - p0)
        val edgeNormal = faceNormal.cross(edge).normalized()
        edge.length() * Plane(edgeNormal, p0).distanceTo(p2) * 0.5
    }

    private fun generatedTopology(): Topology {
        val mesh = makeIcosahedron().subdivideIcosahedron(35)
        mesh.distortTriangles(0.5)
        mesh.reorderVerts()
        return mesh.toTopology()
    }
}
