package dev.biserman.planet.planet.ecology

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
            "src/main/kotlin/dev/biserman/planet/notebooks/ecology_world_ecosystems.ipynb",
        )
        val notebook = ObjectMapper().readTree(notebookPath.readText())
        val source = notebook["cells"]
            .flatMap { cell -> cell["source"].map { it.asText() } }
            .joinToString("")
        val scenarios = AuthoredEcosystems.ALL

        assertEquals(22, scenarios.size)
        assertEquals(1, scenarios.count { !it.intendedStable })
        assertEquals(1, scenarios.count { it.introductions.isNotEmpty() })
        assertEquals(2, scenarios.count { it.climateShifts.isNotEmpty() })
        assertEquals(2, scenarios.count { it.habitatShifts.isNotEmpty() })
        assertEquals(2, scenarios.count { it.populationRemovals.isNotEmpty() })
        assertEquals(6, scenarios.count { it.expectedExtinctions.isNotEmpty() })
        assertEquals(1, scenarios.count { it.tile.includeAeroplankton })
        assertEquals(
            scenarios.size,
            Regex("""runEcosystem\(AuthoredEcosystems\.\w+\)""").findAll(source).count(),
        )
        assertTrue("FunctionalResourceDynamics.update(" in source)
        assertEquals(1, Regex("""repeat\(4000\)""").findAll(source).count())
    }

    @Test
    fun `notebook uses only Earth species catalog entries`() {
        val notebookPath = Path.of(
            "src/main/kotlin/dev/biserman/planet/notebooks/ecology_world_ecosystems.ipynb",
        )
        val source = ObjectMapper().readTree(notebookPath.readText())["cells"]
            .flatMap { cell -> cell["source"].map { it.asText() } }
            .joinToString("")
        val referencedSpecies = AuthoredEcosystems.ALL
            .flatMap { scenario -> scenario.species.map { it.id } }
            .toSet()
        val catalogSpecies = EarthSpeciesCatalog.ALL.map { it.id }.toSet()

        assertTrue(referencedSpecies.isNotEmpty())
        assertEquals(emptySet(), referencedSpecies - catalogSpecies)
        assertTrue("SpeciesDefinition(" !in source)
        assertTrue("TargetedRelationshipTrait(" !in source)
    }
}