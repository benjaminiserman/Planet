package dev.biserman.planet.planet.ecology

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

data class SimulationPoint(val year: Double, val biomass: Biomass)

val minimumViableIndividuals = 2.0

fun applyIndividualExtinctionThreshold(
    state: Biomass,
    model: EcosystemModel,
    minimumIndividuals: Double = minimumViableIndividuals,
): Biomass {
    require(minimumIndividuals >= 0.0)
    return model.species.associate { definition ->
        val biomass = state.getValue(definition.id).coerceAtLeast(0.0)
        val extinctionBiomass = minimumIndividuals * definition.individualBiomass
        definition.id to if (biomass < extinctionBiomass) 0.0 else biomass
    }
}

data class PopulationNoise(
    val environmentalVolatility: Double = 0.04,
    val speciesVolatility: Double = 0.06,
) {
    init {
        require(environmentalVolatility >= 0.0)
        require(speciesVolatility >= 0.0)
    }

    companion object {
        val NONE = PopulationNoise(0.0, 0.0)
    }
}

val unitVarianceUniformLimit = sqrt(3.0)

fun Random.nextUnitVarianceNoise(): Double =
    nextDouble(-unitVarianceUniformLimit, unitVarianceUniformLimit)

fun applyPopulationNoise(
    state: Biomass,
    dt: Double,
    noise: PopulationNoise,
    random: Random,
): Biomass {
    if (noise == PopulationNoise.NONE) return state
    val stepScale = sqrt(dt)
    val environmentalShock = random.nextUnitVarianceNoise()

    return state.mapValues { (_, biomass) ->
        val speciesShock = random.nextUnitVarianceNoise()
        val proportionalChange = stepScale * (
            noise.environmentalVolatility * environmentalShock +
                noise.speciesVolatility * speciesShock
            )
        biomass * (1.0 + proportionalChange).coerceAtLeast(0.0)
    }
}

data class Disturbance(
    val year: Double,
    val remainingFraction: Map<String, Double>,
) {
    init {
        require(year >= 0.0)
        require(remainingFraction.values.all { it in 0.0..1.0 })
    }
}

fun addScaled(state: Biomass, rate: Biomass, scale: Double): Biomass =
    state.mapValues { (id, biomass) ->
        (biomass + rate.getValue(id) * scale).coerceAtLeast(0.0)
    }

/** Second-order midpoint Runge-Kutta step requiring two derivative evaluations. */
fun rk2Step(year: Double, state: Biomass, dt: Double, model: EcosystemModel): Biomass {
    EcologyProfiler.current?.rk2Steps?.increment()
    val initialRate = derivatives(year, state, model)
    val midpointState = addScaled(state, initialRate, dt / 2.0)
    val midpointRate = derivatives(year + dt / 2.0, midpointState, model)
    return addScaled(state, midpointRate, dt)
}

/** Fourth-order Runge-Kutta step retained for accuracy comparisons and notebook experiments. */
fun rk4Step(year: Double, state: Biomass, dt: Double, model: EcosystemModel): Biomass {
    EcologyProfiler.current?.rk4Steps?.increment()
    val k1 = derivatives(year, state, model)
    val k2 = derivatives(year + dt / 2.0, addScaled(state, k1, dt / 2.0), model)
    val k3 = derivatives(year + dt / 2.0, addScaled(state, k2, dt / 2.0), model)
    val k4 = derivatives(year + dt, addScaled(state, k3, dt), model)

    return state.mapValues { (id, biomass) ->
        val change = dt * (
            k1.getValue(id) + 2.0 * k2.getValue(id) +
                2.0 * k3.getValue(id) + k4.getValue(id)
            ) / 6.0
        (biomass + change).coerceAtLeast(0.0)
    }
}

fun simulate(
    model: EcosystemModel,
    initial: Biomass,
    years: Double,
    dt: Double = 1.0 / 365.0,
    sampleEverySteps: Int = 5,
    disturbances: List<Disturbance> = emptyList(),
    noise: PopulationNoise = PopulationNoise(),
    random: Random = Random.Default,
    minimumIndividuals: Double = minimumViableIndividuals,
    startYear: Double = 0.0,
): List<SimulationPoint> {
    require(dt > 0.0 && dt <= 1.0 / 52.0) { "Use weekly or smaller ecological substeps" }
    require(minimumIndividuals >= 0.0)
    require(initial.keys == model.species.map { it.id }.toSet())
    require(initial.values.all { it >= 0.0 })

    val totalSteps = (years / dt).roundToInt()
    val disturbancesByStep = disturbances.groupBy { (it.year / dt).roundToInt() }
    val result = mutableListOf<SimulationPoint>()
    var state = applyIndividualExtinctionThreshold(initial, model, minimumIndividuals)

    for (step in 0..totalSteps) {
        for (disturbance in disturbancesByStep[step].orEmpty()) {
            state = applyIndividualExtinctionThreshold(
                state.mapValues { (id, biomass) ->
                    biomass * (disturbance.remainingFraction[id] ?: 1.0)
                },
                model,
                minimumIndividuals,
            )
        }

        if (step % sampleEverySteps == 0 || step in disturbancesByStep) {
            result += SimulationPoint(startYear + step * dt, state.toMap())
        }
        if (step < totalSteps) {
            val deterministicState = applyIndividualExtinctionThreshold(
                rk2Step(startYear + step * dt, state, dt, model),
                model,
                minimumIndividuals,
            )
            state = applyIndividualExtinctionThreshold(
                applyPopulationNoise(deterministicState, dt, noise, random),
                model,
                minimumIndividuals,
            )
        }
    }

    return result
}
