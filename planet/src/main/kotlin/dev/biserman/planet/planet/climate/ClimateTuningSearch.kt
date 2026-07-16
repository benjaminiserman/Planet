package dev.biserman.planet.planet.climate

import kotlin.math.abs
import kotlin.math.max

data class ClimateTuningParameter(
    val name: String,
    val min: Double,
    val max: Double,
    val step: Double,
    val minStep: Double = step / 16.0,
) {
    init {
        require(name.isNotBlank()) { "Tuning parameter name cannot be blank" }
        require(min < max) { "$name must have min < max" }
        require(step > 0.0) { "$name step must be positive" }
        require(minStep > 0.0 && minStep <= step) { "$name minStep must be in (0, step]" }
    }
}

data class ClimateTuningSpace(val parameters: List<ClimateTuningParameter>)

data class ClimateTuningEvaluation(
    val index: Int,
    val values: Map<String, Double>,
    val changedParameter: String?,
    val loss: Double,
)

data class ClimateTuningResult(
    val initialLoss: Double,
    val bestLoss: Double,
    val bestValues: Map<String, Double>,
    val evaluations: Int,
)

/**
 * Deterministic bounded coordinate search. It works well for expensive climate
 * evaluations because every accepted candidate becomes the next center point,
 * and steps shrink only after an entire pass fails to improve the score.
 */
class ClimateTuningSearch(
    parameters: List<ClimateTuningParameter>,
    initialValues: Map<String, Double>,
    private val maxEvaluations: Int,
    interactionPairs: List<Pair<String, String>> = emptyList(),
    private val evaluate: (Map<String, Double>) -> Double,
    private val afterEvaluation: (ClimateTuningEvaluation) -> Unit = {},
) {
    private val parameters = parameters.toList()
    private val initialValues = initialValues.toMap()
    private val interactionPairs = interactionPairs.toList()

    init {
        require(parameters.isNotEmpty()) { "At least one tuning parameter is required" }
        require(maxEvaluations > 0) { "maxEvaluations must be positive" }
        parameters.forEach { parameter ->
            val value = initialValues[parameter.name]
                ?: error("Missing initial value for ${parameter.name}")
            require(value in parameter.min..parameter.max) {
                "Initial ${parameter.name}=$value is outside ${parameter.min}..${parameter.max}"
            }
        }
        val parameterNames = parameters.map { it.name }.toSet()
        interactionPairs.forEach { (first, second) ->
            require(first != second) { "Interaction parameters must be different: $first" }
            require(first in parameterNames && second in parameterNames) {
                "Interaction $first+$second must use selected tuning parameters"
            }
        }
    }

    fun run(): ClimateTuningResult {
        var evaluations = 0
        var currentValues = initialValues
        var currentLoss = evaluatePoint(currentValues, null, ++evaluations)
        val initialLoss = currentLoss
        val steps = parameters.associate { it.name to it.step }.toMutableMap()
        val visited = mutableSetOf(fingerprint(currentValues))
        val reservedInteractionEvaluations = minOf(interactionPairs.size * 4, maxEvaluations - 1)
        val coordinateEvaluationLimit = maxOf(1, maxEvaluations - reservedInteractionEvaluations)

        while (evaluations < coordinateEvaluationLimit) {
            var improvedThisPass = false

            for (parameter in parameters) {
                if (evaluations >= coordinateEvaluationLimit) break
                val step = steps.getValue(parameter.name)
                if (step + EPSILON < parameter.minStep) continue

                val center = currentValues.getValue(parameter.name)
                var bestValues = currentValues
                var bestLoss = currentLoss

                for (direction in listOf(1.0, -1.0)) {
                    if (evaluations >= coordinateEvaluationLimit) break
                    val candidateValue = (center + direction * step).coerceIn(parameter.min, parameter.max)
                    if (abs(candidateValue - center) <= EPSILON) continue
                    val candidate = currentValues + (parameter.name to candidateValue)
                    if (!visited.add(fingerprint(candidate))) continue

                    val loss = evaluatePoint(candidate, parameter.name, ++evaluations)
                    if (loss < bestLoss - EPSILON) {
                        bestLoss = loss
                        bestValues = candidate
                    }
                }

                if (bestLoss < currentLoss - EPSILON) {
                    currentLoss = bestLoss
                    currentValues = bestValues
                    improvedThisPass = true
                }
            }

            if (!improvedThisPass) {
                val allAtMinimumBeforeShrinking = parameters.all {
                    steps.getValue(it.name) <= it.minStep + EPSILON
                }
                if (allAtMinimumBeforeShrinking) break
                parameters.forEach { parameter ->
                    steps[parameter.name] = max(parameter.minStep, steps.getValue(parameter.name) / 2.0)
                }
            }
        }

        for ((firstName, secondName) in interactionPairs) {
            if (evaluations >= maxEvaluations) break
            val first = parameters.first { it.name == firstName }
            val second = parameters.first { it.name == secondName }
            val center = currentValues
            var bestValues = currentValues
            var bestLoss = currentLoss

            for (firstDirection in listOf(1.0, -1.0)) {
                for (secondDirection in listOf(1.0, -1.0)) {
                    if (evaluations >= maxEvaluations) break
                    val firstValue = (center.getValue(firstName) + firstDirection * first.step)
                        .coerceIn(first.min, first.max)
                    val secondValue = (center.getValue(secondName) + secondDirection * second.step)
                        .coerceIn(second.min, second.max)
                    val candidate = center + mapOf(firstName to firstValue, secondName to secondValue)
                    if (candidate == center || !visited.add(fingerprint(candidate))) continue
                    val loss = evaluatePoint(candidate, "$firstName+$secondName", ++evaluations)
                    if (loss < bestLoss - EPSILON) {
                        bestLoss = loss
                        bestValues = candidate
                    }
                }
            }

            if (bestLoss < currentLoss - EPSILON) {
                currentLoss = bestLoss
                currentValues = bestValues
            }
        }

        return ClimateTuningResult(
            initialLoss = initialLoss,
            bestLoss = currentLoss,
            bestValues = currentValues,
            evaluations = evaluations,
        )
    }

    private fun evaluatePoint(values: Map<String, Double>, changedParameter: String?, index: Int): Double {
        val loss = evaluate(values)
        require(loss.isFinite()) { "Evaluator returned non-finite loss $loss" }
        afterEvaluation(ClimateTuningEvaluation(index, values, changedParameter, loss))
        return loss
    }

    private fun fingerprint(values: Map<String, Double>) = parameters.joinToString("|") {
        "${it.name}=${values.getValue(it.name)}"
    }

    private companion object {
        const val EPSILON = 1e-12
    }
}
