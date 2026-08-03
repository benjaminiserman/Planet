package dev.biserman.planet.planet.ecology

import kotlin.math.max

object NicheSelection {
    fun choose(
        species: CompiledSpecies,
        ecology: CompiledEcology,
        environment: SeasonalCellEnvironment,
        competitionByNiche: DoubleArray = DoubleArray(ecology.niches.size),
        minimumRelativeIntrinsicFit: Double = 0.0,
        competitionAffectsSelection: Boolean = true,
    ): Int {
        require(competitionByNiche.size == ecology.niches.size)
        require(minimumRelativeIntrinsicFit in 0.0..1.0)
        val bestIntrinsicFit = ecology.niches.indices
            .asSequence()
            .filter { nicheIndex ->
                val habitat = ecology.niches[nicheIndex].habitat
                environment.habitatAvailability(habitat) > 0.0 &&
                    EcologyFitness.habitat(species, environment, habitat) > 0.0 &&
                    !(
                        !environment.isLand &&
                            habitat == Habitat.AERIAL &&
                            !species.environment.pelagicAerialResident
                        ) &&
                    !(
                        !environment.isLand &&
                            species.environment.pelagicAerialResident &&
                            habitat != Habitat.AERIAL
                        ) &&
                    !(habitat == Habitat.DARK_WATER && !species.environment.darkWaterAdapted)
            }
            .maxOfOrNull { species.niche.fitFor(it) }
            ?: 0.0
        var bestIndex = -1
        var bestScore = 0.0
        ecology.niches.indices.forEach { nicheIndex ->
            val niche = ecology.niches[nicheIndex]
            if (
                species.niche.fitFor(nicheIndex) <
                bestIntrinsicFit * minimumRelativeIntrinsicFit
            ) {
                return@forEach
            }
            if (EcologyFitness.habitat(species, environment, niche.habitat) <= 0.0) {
                return@forEach
            }
            if (
                !environment.isLand &&
                niche.habitat == Habitat.AERIAL &&
                !species.environment.pelagicAerialResident
            ) {
                return@forEach
            }
            if (
                !environment.isLand &&
                species.environment.pelagicAerialResident &&
                niche.habitat != Habitat.AERIAL
            ) {
                return@forEach
            }
            if (niche.habitat == Habitat.DARK_WATER && !species.environment.darkWaterAdapted) {
                return@forEach
            }
            // A temporarily empty carrion, marine-snow, or seasonal resource
            // pool should not make an otherwise valid niche impossible to
            // establish. Reserves and subsequent resource production decide
            // whether the population actually persists.
            val establishmentResource =
                max(0.01, environment.resourceSupport(niche, species.sizeClass))
            val score =
                species.niche.fitFor(nicheIndex) *
                    environment.habitatAvailability(niche.habitat) *
                    establishmentResource /
                    if (competitionAffectsSelection) {
                        1.0 + competitionByNiche[nicheIndex]
                    } else {
                        1.0
                    }
            if (score > bestScore) {
                bestScore = score
                bestIndex = nicheIndex
            }
        }
        return bestIndex
    }
}