package dev.biserman.planet.planet.climate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClimateTuningSearchTest {
    @Test
    fun `coordinate search improves and shrinks its step`() {
        val evaluations = mutableListOf<ClimateTuningEvaluation>()
        val result = ClimateTuningSearch(
            parameters = listOf(
                ClimateTuningParameter("temperature", min = 0.0, max = 10.0, step = 2.0, minStep = 0.25),
            ),
            initialValues = mapOf("temperature" to 0.0),
            maxEvaluations = 9,
            evaluate = { values -> (values.getValue("temperature") - 3.0).let { it * it } },
            afterEvaluation = evaluations::add,
        ).run()

        assertEquals(9.0, result.initialLoss)
        assertEquals(0.0, result.bestLoss)
        assertEquals(3.0, result.bestValues.getValue("temperature"))
        assertEquals(result.evaluations, evaluations.size)
    }

    @Test
    fun `coordinate search respects its evaluation budget and bounds`() {
        val seen = mutableListOf<Double>()
        val result = ClimateTuningSearch(
            parameters = listOf(
                ClimateTuningParameter("moisture", min = 0.0, max = 1.0, step = 0.75),
            ),
            initialValues = mapOf("moisture" to 0.5),
            maxEvaluations = 2,
            evaluate = { values ->
                values.getValue("moisture").also(seen::add)
            },
        ).run()

        assertEquals(2, result.evaluations)
        assertEquals(2, seen.size)
        assertTrue(seen.all { it in 0.0..1.0 })
    }

    @Test
    fun `interaction trials find improvements hidden from coordinate moves`() {
        val result = ClimateTuningSearch(
            parameters = listOf(
                ClimateTuningParameter("temperature", min = -1.0, max = 1.0, step = 1.0),
                ClimateTuningParameter("moisture", min = -1.0, max = 1.0, step = 1.0),
            ),
            initialValues = mapOf("temperature" to 0.0, "moisture" to 0.0),
            maxEvaluations = 9,
            interactionPairs = listOf("temperature" to "moisture"),
            evaluate = { values ->
                if (values.getValue("temperature") == 1.0 && values.getValue("moisture") == 1.0) 0.0 else 1.0
            },
        ).run()

        assertEquals(0.0, result.bestLoss)
        assertEquals(1.0, result.bestValues.getValue("temperature"))
        assertEquals(1.0, result.bestValues.getValue("moisture"))
        assertTrue(result.evaluations <= 9)
    }
}
