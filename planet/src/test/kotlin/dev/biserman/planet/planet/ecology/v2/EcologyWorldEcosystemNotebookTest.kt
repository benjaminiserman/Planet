package dev.biserman.planet.planet.ecology.v2

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcologyWorldEcosystemNotebookTest {
    @Test
    fun `world ecosystem notebook has stable scenarios collapse controls and focused extinctions`() {
        val notebookPath = Path.of(
            "src/main/kotlin/dev/biserman/planet/notebooks/ecology_v2_world_ecosystems.ipynb",
        )
        val notebook = ObjectMapper().readTree(notebookPath.readText())
        val scenarioCells = notebook["cells"]
            .filter { it["cell_type"].asText() == "code" }
            .map { cell -> cell["source"].joinToString("") { it.asText() } }
            .filter { source -> Regex("""runEcosystem\(\s*"""").containsMatchIn(source) }

        assertEquals(23, scenarioCells.size)
        assertEquals(2, scenarioCells.count { "intendedStable = false" in it })
        assertEquals(2, scenarioCells.count { "introductions = listOf(" in it })
        assertEquals(2, scenarioCells.count { "climateShifts = listOf(" in it })
        assertEquals(2, scenarioCells.count { "habitatShifts = listOf(" in it })
        assertEquals(1, scenarioCells.count { "populationRemovals = listOf(" in it })
        assertEquals(7, scenarioCells.count { "expectedExtinctions = setOf(" in it })
        assertEquals(1, scenarioCells.count { "includeAeroplankton = true" in it })
        scenarioCells.forEach { source ->
            assertTrue("seasonalClimate(" in source)
            assertTrue("TileTemplate(" in source)
            assertTrue("sp(" in source)
            assertTrue("repeat(4000)" !in source, "The shared harness should own the 1,000-year loop")
        }
    }

    @Test
    fun `notebook uses only implemented common traits`() {
        val notebookPath = Path.of(
            "src/main/kotlin/dev/biserman/planet/notebooks/ecology_v2_world_ecosystems.ipynb",
        )
        val source = ObjectMapper().readTree(notebookPath.readText())["cells"]
            .flatMap { cell -> cell["source"].map { it.asText() } }
            .joinToString("")
        val referencedTraits = Regex("""C\.([A-Z_]+)""")
            .findAll(source)
            .map { it.groupValues[1] }
            .toSet()
        val implementedTraits = CommonTrait.entries.map { it.name }.toSet()

        assertTrue(referencedTraits.isNotEmpty())
        assertEquals(emptySet(), referencedTraits - implementedTraits)
    }
}
