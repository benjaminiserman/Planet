package dev.biserman.planet.planet.climate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HersfeldtClassificationGraphTest {
    @Test
    fun `adjacency matrix is complete symmetric and has no isolated classifications`() {
        val graph = HersfeldtClassificationGraph
        val matrix = graph.adjacencyMatrix

        assertEquals(graph.classificationIds.sorted(), matrix.ids)
        assertEquals(matrix.ids.size, matrix.rows.size)
        matrix.rows.forEach { assertEquals(matrix.ids.size, it.size) }

        matrix.ids.indices.forEach { row ->
            assertFalse(matrix.rows[row][row])
            assertTrue(matrix.rows[row].any { it })
            matrix.ids.indices.forEach { column ->
                assertEquals(matrix.rows[row][column], matrix.rows[column][row])
            }
        }
    }

    @Test
    fun `common classifier boundary transitions are one graph hop`() {
        val graph = HersfeldtClassificationGraph

        assertEquals(1, graph.distance("Aha", "Ada")) // desert -> semidesert
        assertEquals(1, graph.distance("CAMa", "CAa")) // growth supply
        assertEquals(1, graph.distance("CEb", "CDb")) // boreal -> temperate GDD
        assertEquals(1, graph.distance("CEb", "CFb")) // boreal -> tundra GDD
        assertEquals(1, graph.distance("TUf", "TUfp")) // evaporation/monsoon
    }

    @Test
    fun `shortest path counts successive classifier conditions and is capped`() {
        val graph = HersfeldtClassificationGraph

        assertEquals(0, graph.distance("TUr", "TUr"))
        assertEquals(2, graph.distance("TUr", "TUs"))
        assertEquals(graph.distance("CAMa", "TUr"), graph.distance("TUr", "CAMa"))
        assertEquals(graph.MAX_SCORED_DISTANCE, graph.distance("UNKNOWN", "TUr"))
    }
}
