package dev.biserman.planet.planet.ecology

data class SpeciesSuitability(
    val speciesId: String,
    val nicheIndex: Int,
    val score: Double,
    val meanAnnualFitness: Double,
    val bestSeasonFitness: Double,
    val viableSeasonFraction: Double,
    val suitable: Boolean,
    val issues: List<String>,
)

/**
 * Resource-independent establishment suitability.
 *
 * This answers whether the organism can physically operate in a habitat over a
 * year. Food-web closure and competition are evaluated separately when an
 * ecosystem is assembled or simulated.
 */
object EcologySuitability {
    const val MINIMUM_ACTIVE_FITNESS = 0.35
    private const val MINIMUM_ANNUAL_MEAN_FITNESS = 0.22
    private const val MINIMUM_DORMANT_ANNUAL_MEAN_FITNESS = 0.15
    private const val MINIMUM_VIABLE_SEASON_FRACTION = 0.25

    fun evaluate(
        species: CompiledSpecies,
        ecology: CompiledEcology,
        annualEnvironments: List<SeasonalCellEnvironment>,
        habitat: Habitat? = null,
    ): SpeciesSuitability {
        require(annualEnvironments.isNotEmpty())
        val candidate = ecology.niches.indices
            .asSequence()
            .filter { nicheIndex ->
                val niche = ecology.niches[nicheIndex]
                (habitat == null || niche.habitat == habitat) &&
                    species.niche.fitFor(nicheIndex) > 0.0 &&
                    annualEnvironments.any {
                        it.habitatAvailability(niche.habitat) > 0.0
                    } &&
                    !(
                        annualEnvironments.all { !it.isLand } &&
                            niche.habitat == Habitat.AERIAL &&
                            !species.environment.pelagicAerialResident
                        ) &&
                    !(niche.habitat == Habitat.DARK_WATER && !species.environment.darkWaterAdapted)
            }
            .map { nicheIndex ->
                val niche = ecology.niches[nicheIndex]
                val seasonalFitness = annualEnvironments.map { environment ->
                    (
                        EcologyFitness.combined(species, environment, niche.habitat) *
                            EcologyFitness.reefAssociationMultiplier(species, environment)
                        ).coerceIn(0.0, 1.0)
                }
                NicheSuitability(
                    nicheIndex = nicheIndex,
                    seasonalFitness = seasonalFitness,
                    structuralFit = (
                        species.niche.fitFor(nicheIndex) *
                            annualEnvironments
                                .map { it.habitatAvailability(niche.habitat) }
                                .average()
                        ).coerceIn(0.0, 1.0),
                )
            }
            .maxByOrNull { it.score }

        if (candidate == null) {
            return SpeciesSuitability(
                speciesId = species.id,
                nicheIndex = -1,
                score = 0.0,
                meanAnnualFitness = 0.0,
                bestSeasonFitness = 0.0,
                viableSeasonFraction = 0.0,
                suitable = false,
                issues = listOf("no physically supported habitat and strategy"),
            )
        }

        val mean = candidate.seasonalFitness.average()
        val best = candidate.seasonalFitness.max()
        val viableFraction =
            candidate.seasonalFitness.count { it >= MINIMUM_ACTIVE_FITNESS }.toDouble() /
                candidate.seasonalFitness.size
        val torporExceedsColdProtection =
            species.lifeHistory.dormancyKind == DormancyKind.SEASONAL_TORPOR &&
                annualEnvironments.any {
                    it.temperatureC <=
                        species.physiology.thermal.outerLowC - SEASONAL_TORPOR_COLD_BUFFER_C
                }
        val lifecycleCanProtectWorstSeason = when (species.lifeHistory.dormancyKind) {
            DormancyKind.NONE -> false
            DormancyKind.SEASONAL_TORPOR -> !torporExceedsColdProtection
            else -> true
        }
        val seasonalLifecycle =
            lifecycleCanProtectWorstSeason &&
                best >= 0.50 &&
                viableFraction > 0.0
        val unprotectedLethalSeason =
            !lifecycleCanProtectWorstSeason &&
                annualEnvironments.any {
                    EcologyFitness.thermal(species, it) <= 0.0
                }
        val minimumAnnualMean =
            if (seasonalLifecycle) {
                MINIMUM_DORMANT_ANNUAL_MEAN_FITNESS
            } else {
                MINIMUM_ANNUAL_MEAN_FITNESS
            }
        val suitable =
            !torporExceedsColdProtection &&
                !unprotectedLethalSeason &&
                best >= MINIMUM_ACTIVE_FITNESS &&
                mean >= minimumAnnualMean &&
                (
                    viableFraction >= MINIMUM_VIABLE_SEASON_FRACTION ||
                        seasonalLifecycle
                    )
        val issues = buildList {
            if (torporExceedsColdProtection) {
                add("winter cold exceeds seasonal torpor protection")
            }
            if (unprotectedLethalSeason) {
                add("season outside lethal temperature range without a protective lifecycle")
            }
            if (best < MINIMUM_ACTIVE_FITNESS) add("no viable active season")
            if (mean < minimumAnnualMean) add("annual climate fitness is too low")
            if (
                viableFraction < MINIMUM_VIABLE_SEASON_FRACTION &&
                !seasonalLifecycle
            ) {
                add("too few viable seasons without a protective lifecycle")
            }
        }
        val score =
            (
                mean *
                    (0.50 + best * 0.50) *
                    (0.50 + viableFraction * 0.50) *
                    (0.50 + candidate.structuralFit * 0.50)
                ).coerceIn(0.0, 1.0)
        return SpeciesSuitability(
            speciesId = species.id,
            nicheIndex = candidate.nicheIndex,
            score = score,
            meanAnnualFitness = mean,
            bestSeasonFitness = best,
            viableSeasonFraction = viableFraction,
            suitable = suitable,
            issues = issues,
        )
    }

    private data class NicheSuitability(
        val nicheIndex: Int,
        val seasonalFitness: List<Double>,
        val structuralFit: Double,
    ) {
        val score: Double =
            seasonalFitness.average() *
                (0.5 + seasonalFitness.max() * 0.5) *
                (0.5 + structuralFit * 0.5)
    }

    private const val SEASONAL_TORPOR_COLD_BUFFER_C = 5.0
}