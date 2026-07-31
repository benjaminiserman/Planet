package dev.biserman.planet.planet.ecology

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class TileCommunity(val capacity: Int = 48) {
    val speciesIndices = IntArray(capacity)
    val nicheIndices = IntArray(capacity)
    val activeBiomass = DoubleArray(capacity)
    val reserves = DoubleArray(capacity)
    val dormantBiomass = DoubleArray(capacity)
    var size: Int = 0
        private set

    fun add(
        speciesIndex: Int,
        nicheIndex: Int,
        activeBiomass: Double,
        reserves: Double = 0.0,
        dormantBiomass: Double = 0.0,
    ): Int {
        require(size < capacity) { "Tile community capacity $capacity exceeded" }
        require(find(speciesIndex) < 0) { "Species $speciesIndex already exists in this tile" }
        require(activeBiomass >= 0.0 && reserves >= 0.0 && dormantBiomass >= 0.0)
        val index = size++
        speciesIndices[index] = speciesIndex
        nicheIndices[index] = nicheIndex
        this.activeBiomass[index] = activeBiomass
        this.reserves[index] = reserves
        this.dormantBiomass[index] = dormantBiomass
        return index
    }

    fun find(speciesIndex: Int): Int {
        for (index in 0 until size) {
            if (speciesIndices[index] == speciesIndex) return index
        }
        return -1
    }

    fun removeAt(index: Int) {
        require(index in 0 until size)
        val last = size - 1
        if (index != last) {
            speciesIndices[index] = speciesIndices[last]
            nicheIndices[index] = nicheIndices[last]
            activeBiomass[index] = activeBiomass[last]
            reserves[index] = reserves[last]
            dormantBiomass[index] = dormantBiomass[last]
        }
        speciesIndices[last] = 0
        nicheIndices[last] = 0
        activeBiomass[last] = 0.0
        reserves[last] = 0.0
        dormantBiomass[last] = 0.0
        size--
    }

    fun totalBiomass(): Double {
        var total = 0.0
        for (index in 0 until size) {
            total += activeBiomass[index] + dormantBiomass[index]
        }
        return total
    }
}

data class EcologyRuntimeConfig(
    val backgroundMortality: Double = EcologyGlobals.backgroundMortality,
    val stressMortality: Double = EcologyGlobals.stressMortality,
    val lethalTemperatureMortality: Double =
        EcologyGlobals.lethalTemperatureMortality,
    val maximumStarvationMortality: Double =
        EcologyGlobals.maximumStarvationMortality,
    /**
     * Species assigned to the same broad authored niche still partition food,
     * space, or time in ways this coarse model does not name explicitly.
     * Keeping this below 1 prevents tiny fitness differences from forcing
     * deterministic competitive exclusion.
     */
    val interspecificNicheCompetition: Double =
        EcologyGlobals.interspecificNicheCompetition,
    /**
     * Seasonal food throughput may exceed consumer standing biomass even
     * though only a fraction of eaten tissue is assimilated.
     */
    val maximumConsumedBiomassFraction: Double =
        EcologyGlobals.maximumConsumedBiomassFraction,
    val dormantEntryFitness: Double = EcologyGlobals.dormantEntryFitness,
    val dormantExitFitness: Double = EcologyGlobals.dormantExitFitness,
    val dormantEntryFraction: Double = EcologyGlobals.dormantEntryFraction,
    val dormantExitFraction: Double = EcologyGlobals.dormantExitFraction,
    /**
     * Each authored habitat represents a finite amount of unmodeled spatial,
     * temporal, and dietary niche space. Above this soft capacity, established
     * and especially low-abundance populations suffer competitive displacement.
     */
    val habitatDiversityBaseSlots: Double =
        EcologyGlobals.habitatDiversityBaseSlots,
    val habitatDiversityAvailabilitySlots: Double =
        EcologyGlobals.habitatDiversityAvailabilitySlots,
    val maximumHabitatDiversityMortality: Double =
        EcologyGlobals.maximumHabitatDiversityMortality,
    val strongestCompetitorMortalityFraction: Double =
        EcologyGlobals.strongestCompetitorMortalityFraction,
    // One 40,000 km² tile may hold only a few viable huge animals. The later
    // regional extinction pass is responsible for treating a handful spread
    // across several tiles as non-viable.
    val minimumViableIndividuals: Double =
        EcologyGlobals.minimumViableIndividuals,
    val unassistedRadiationChancePerSeason: Double =
        EcologyGlobals.unassistedRadiationChancePerSeason,
    val migrationRadiationChancePerSeason: Double =
        EcologyGlobals.migrationRadiationChancePerSeason,
    val neighborRadiationChancePerSeason: Double =
        EcologyGlobals.neighborRadiationChancePerSeason,
    val minimumRelativeRadiationNicheFit: Double =
        EcologyGlobals.minimumRelativeRadiationNicheFit,
) {
    init {
        require(backgroundMortality in 0.0..1.0)
        require(stressMortality in 0.0..1.0)
        require(lethalTemperatureMortality in 0.0..1.0)
        require(maximumStarvationMortality in 0.0..1.0)
        require(interspecificNicheCompetition >= 0.0)
        require(maximumConsumedBiomassFraction in 0.0..1.0)
        require(dormantEntryFitness in 0.0..1.0)
        require(dormantExitFitness in 0.0..1.0)
        require(dormantEntryFraction in 0.0..1.0)
        require(dormantExitFraction in 0.0..1.0)
        require(habitatDiversityBaseSlots >= 0.0)
        require(habitatDiversityAvailabilitySlots >= 0.0)
        require(maximumHabitatDiversityMortality in 0.0..1.0)
        require(strongestCompetitorMortalityFraction in 0.0..1.0)
        require(minimumViableIndividuals >= 0.0)
        require(unassistedRadiationChancePerSeason in 0.0..1.0)
        require(migrationRadiationChancePerSeason in 0.0..1.0)
        require(neighborRadiationChancePerSeason in 0.0..1.0)
        require(minimumRelativeRadiationNicheFit in 0.0..1.0)
    }
}

object EcologyDiversity {
    fun softHabitatCapacity(
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
        baseSlots: Double = EcologyGlobals.habitatDiversityBaseSlots,
        availabilitySlots: Double =
            EcologyGlobals.habitatDiversityAvailabilitySlots,
    ): Double {
        val availability = environment.habitatAvailability(habitat)
        return if (availability <= 0.0) {
            0.0
        } else {
            baseSlots + availabilitySlots * availability
        }
    }
}

class CellTurnFluxes {
    var carrionBiomass: Double = 0.0
        internal set
    var detritusBiomass: Double = 0.0
        internal set
    var wasteBiomass: Double = 0.0
        internal set
    var carrionConsumedBiomass: Double = 0.0
        internal set
    var detritusConsumedBiomass: Double = 0.0
        internal set
    var wasteConsumedBiomass: Double = 0.0
        internal set
    var marineSnowBiomass: Double = 0.0
        internal set
    var marineSnowConsumedBiomass: Double = 0.0
        internal set
    var reefCoverDelta: Double = 0.0
        internal set

    internal fun clear() {
        carrionBiomass = 0.0
        detritusBiomass = 0.0
        wasteBiomass = 0.0
        carrionConsumedBiomass = 0.0
        detritusConsumedBiomass = 0.0
        wasteConsumedBiomass = 0.0
        marineSnowBiomass = 0.0
        marineSnowConsumedBiomass = 0.0
        reefCoverDelta = 0.0
    }
}

/**
 * Allocation-free after construction for communities no larger than
 * [maximumPopulationsPerCell]. One instance is intended to advance cells
 * sequentially and reuse its scratch arrays.
 */
class EcologyRuntime(
    private val ecology: CompiledEcology,
    private val config: EcologyRuntimeConfig = EcologyRuntimeConfig(),
    maximumPopulationsPerCell: Int = 48,
) {
    private val effectiveActive = DoubleArray(maximumPopulationsPerCell)
    private val fitness = DoubleArray(maximumPopulationsPerCell)
    private val interactionGains = DoubleArray(maximumPopulationsPerCell)
    private val interactionLosses = DoubleArray(maximumPopulationsPerCell)
    private val relationshipBenefits = DoubleArray(maximumPopulationsPerCell)
    private val nextActive = DoubleArray(maximumPopulationsPerCell)
    private val nextReserves = DoubleArray(maximumPopulationsPerCell)
    private val nextDormant = DoubleArray(maximumPopulationsPerCell)
    private val competitiveStanding = DoubleArray(maximumPopulationsPerCell)
    private val normalizedBiomassByNicheAndSize =
        DoubleArray(ecology.niches.size * SizeClass.entries.size)
    private val populationCountByHabitat = IntArray(Habitat.entries.size)
    private val bestCompetitiveStandingByHabitat = DoubleArray(Habitat.entries.size)

    fun advanceSeason(
        community: TileCommunity,
        environment: SeasonalCellEnvironment,
        fluxes: CellTurnFluxes? = null,
        finalizeExtinctions: Boolean = true,
    ) {
        require(community.size <= effectiveActive.size)
        fluxes?.clear()
        if (fluxes != null) {
            fluxes.reefCoverDelta = -environment.reefCover * 0.015
        }
        clearScratch(community.size)
        prepareDormancyAndFitness(community, environment)
        accumulateHabitatDiversity(community, environment)
        accumulateNicheBiomass(community)
        accumulateInteractions(community, environment)
        updatePopulations(community, environment, fluxes)
        if (finalizeExtinctions) {
            finalizeLocalExtinctions(community)
        }
    }

    private fun clearScratch(populationCount: Int) {
        java.util.Arrays.fill(normalizedBiomassByNicheAndSize, 0.0)
        java.util.Arrays.fill(interactionGains, 0, populationCount, 0.0)
        java.util.Arrays.fill(interactionLosses, 0, populationCount, 0.0)
        java.util.Arrays.fill(relationshipBenefits, 0, populationCount, 0.0)
        java.util.Arrays.fill(competitiveStanding, 0, populationCount, 0.0)
        java.util.Arrays.fill(populationCountByHabitat, 0)
        java.util.Arrays.fill(bestCompetitiveStandingByHabitat, 0.0)
    }

    private fun accumulateHabitatDiversity(
        community: TileCommunity,
        environment: SeasonalCellEnvironment,
    ) {
        for (populationIndex in 0 until community.size) {
            val species = ecology.species[community.speciesIndices[populationIndex]]
            val nicheIndex = community.nicheIndices[populationIndex]
            val niche = ecology.niches[nicheIndex]
            val habitatIndex = niche.habitat.ordinal
            populationCountByHabitat[habitatIndex]++

            val carryingCapacity =
                EcologyBiomass.carryingCapacityKg(species, niche, environment)
            val relativeAbundance =
                (effectiveActive[populationIndex] / max(1.0, carryingCapacity))
                    .coerceIn(0.0, 1.0)
            val intrinsicFit = species.nicheFit[nicheIndex].coerceIn(0.0, 1.0)
            val standing =
                fitness[populationIndex] *
                    (0.55 + intrinsicFit * 0.45) *
                    (0.25 + sqrt(relativeAbundance) * 0.75)
            competitiveStanding[populationIndex] = standing
            bestCompetitiveStandingByHabitat[habitatIndex] =
                max(bestCompetitiveStandingByHabitat[habitatIndex], standing)
        }
    }

    private fun prepareDormancyAndFitness(
        community: TileCommunity,
        environment: SeasonalCellEnvironment,
    ) {
        for (populationIndex in 0 until community.size) {
            val species = ecology.species[community.speciesIndices[populationIndex]]
            val niche = ecology.niches[community.nicheIndices[populationIndex]]
            val habitatPresent =
                species.nicheFit[community.nicheIndices[populationIndex]] > 0.0 &&
                    environment.habitatAvailability(niche.habitat) > 0.0
            val requiredTargetPresent =
                EcologyAssembly.requiredTargetPresent(ecology, species.index, community)
            val populationFitness =
                if (!habitatPresent || !requiredTargetPresent) {
                    0.0
                } else {
                    (
                        EcologyFitness.combined(species, environment, niche.habitat) *
                            (1.0 + species.reefUse * environment.reefCover)
                        ).coerceIn(0.0, 1.0)
                }
            fitness[populationIndex] = populationFitness

            var active = community.activeBiomass[populationIndex]
            var dormant = community.dormantBiomass[populationIndex] * species.dormantSurvival
            if (environment.temperatureC < species.minimumActiveTemperatureC) {
                dormant *= species.frozenDormantSurvival
            }
            if (
                species.dormancyKind == DormancyKind.SEASONAL_TORPOR &&
                environment.temperatureC <= species.temperatureOuterLow -
                SEASONAL_TORPOR_COLD_BUFFER_C
            ) {
                // Torpor saves energy through ordinary winters; it does not
                // grant tropical and subtropical animals polar physiology.
                val degreesBeyondProtection =
                    species.temperatureOuterLow -
                        SEASONAL_TORPOR_COLD_BUFFER_C -
                        environment.temperatureC
                val lethalColdLoss =
                    (
                        0.35 +
                            degreesBeyondProtection /
                            SEASONAL_TORPOR_LETHAL_RAMP_C *
                            0.60
                        ).coerceIn(0.35, 0.95)
                dormant *= 1.0 - lethalColdLoss
            }
            if (
                species.dormancyKind == DormancyKind.COLD_DARK_LEAF_DORMANCY &&
                (
                    environment.temperatureC <=
                        species.temperatureOuterLow - DORMANT_LEAF_COLD_PROTECTION_C ||
                        environment.temperatureC >= species.temperatureOuterHigh
                    )
            ) {
                // Dormant buds and woody tissues tolerate substantially more
                // cold than active foliage, but dormancy is not immunity to an
                // arbitrarily cold climate or to lethal heat.
                dormant *= 1.0 - config.stressMortality
            }
            val canEnterDormancy =
                species.dormancyKind != DormancyKind.NONE &&
                    !(species.dormancyKind == DormancyKind.WHOLE_BODY_DESICCATION &&
                        niche.habitat in EcologyFitness.aquaticHabitats)
            val dormancyCondition = when (species.dormancyKind) {
                DormancyKind.COLD_DARK_LEAF_DORMANCY ->
                    environment.temperatureC >
                        species.temperatureOuterLow - DORMANT_LEAF_COLD_PROTECTION_C &&
                        (
                            environment.temperatureC < species.temperatureOptimalLow ||
                                environment.insolation < 0.45
                            )
                DormancyKind.DROUGHT_DECIDUOUS ->
                    EcologyFitness.thermal(species, environment) > 0.0 &&
                        EcologyFitness.water(species, environment, niche.habitat) <
                            config.dormantEntryFitness
                DormancyKind.SEASONAL_TORPOR ->
                    environment.temperatureC >
                        species.temperatureOuterLow - SEASONAL_TORPOR_COLD_BUFFER_C
                else -> true
            }
            if (
                populationFitness < config.dormantEntryFitness &&
                canEnterDormancy &&
                dormancyCondition
            ) {
                val entering = active * config.dormantEntryFraction
                active -= entering
                // Resting eggs, spores, and cysts may preserve a lineage using
                // much less living biomass than the active population that
                // produced them. Whole-body dormancy retains the default 1:1
                // transfer, while microscopic resting stages can opt into a
                // smaller retained fraction.
                dormant += entering * species.dormantEntryBiomassRetention
            } else if (populationFitness > config.dormantExitFitness && dormant > 0.0) {
                val exiting = dormant * config.dormantExitFraction
                dormant -= exiting
                val carryingBiomass =
                    EcologyBiomass.carryingCapacityKg(species, niche, environment)
                val ordinaryReactivation = exiting
                val bloomGrowth = min(
                    exiting * (species.dormantReactivationMultiplier - 1.0),
                    max(0.0, carryingBiomass - active - ordinaryReactivation),
                )
                active += ordinaryReactivation + bloomGrowth
            }
            effectiveActive[populationIndex] = active
            nextDormant[populationIndex] = dormant
        }
    }

    private fun accumulateNicheBiomass(community: TileCommunity) {
        for (populationIndex in 0 until community.size) {
            val species = ecology.species[community.speciesIndices[populationIndex]]
            val offset =
                community.nicheIndices[populationIndex] * SizeClass.entries.size +
                    species.sizeClass.ordinal
            normalizedBiomassByNicheAndSize[offset] +=
                effectiveActive[populationIndex] /
                    species.sizeClass.densityScale *
                    species.nicheCompetitionSensitivity
        }
    }

    private fun accumulateInteractions(
        community: TileCommunity,
        environment: SeasonalCellEnvironment,
    ) {
        val speciesCount = ecology.species.size
        for (consumerPopulation in 0 until community.size) {
            val consumerIndex = community.speciesIndices[consumerPopulation]
            val consumer = ecology.species[consumerIndex]
            val consumerBiomass = effectiveActive[consumerPopulation]
            if (consumerBiomass <= 0.0) continue

            var potentialConsumption = 0.0
            var hasPrioritySupplement = false
            for (targetPopulation in 0 until community.size) {
                if (consumerPopulation == targetPopulation) continue
                val targetIndex = community.speciesIndices[targetPopulation]
                val offset = consumerIndex * speciesCount + targetIndex
                if (ecology.interactions.kindAt(offset) == InteractionKind.NONE.ordinal) continue
                val targetBiomass = effectiveActive[targetPopulation]
                if (targetBiomass <= 0.0) continue

                val kind = InteractionKind.entries[ecology.interactions.kindAt(offset)]
                val target = ecology.species[targetIndex]
                val targetHabitat = ecology.niches[community.nicheIndices[targetPopulation]].habitat
                val authoredCamouflage = target.camouflage[targetHabitat.ordinal]
                val colorCamouflage = targetHabitat.camouflageMatch(
                    target.camouflageColor,
                    environment.snowOrIce,
                    environment.canopyCover,
                    environment.reefCover,
                )
                val concealment =
                    if (
                        kind == InteractionKind.PREDATION ||
                        kind == InteractionKind.SUPPLEMENTAL_FEEDING
                    ) {
                        (authoredCamouflage + colorCamouflage).coerceIn(0.0, 0.72)
                    } else {
                        0.0
                    }
                val lossRate = ecology.interactions.targetLossAt(offset) * (1.0 - concealment)
                val accessibilityCoefficient = kind.lowDensityAccessibilityCoefficient
                val lowDensityAccessibility =
                    if (accessibilityCoefficient > 0.0) {
                        targetBiomass /
                            (targetBiomass + consumerBiomass * accessibilityCoefficient)
                    } else {
                        1.0
                    }
                val potentialTargetLoss =
                    targetBiomass * lossRate * lowDensityAccessibility
                if (kind == InteractionKind.SUPPLEMENTAL_FEEDING) {
                    hasPrioritySupplement = true
                } else {
                    potentialConsumption += potentialTargetLoss
                }
            }
            if (potentialConsumption <= 0.0 && !hasPrioritySupplement) continue

            val consumptionScale =
                if (potentialConsumption > 0.0) {
                    val filterFeeder =
                        consumer.strategySupport[EcoStrategy.FILTER_FEEDING.ordinal] > 0.0
                    val predator =
                        consumer.strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal] > 0.0 ||
                            consumer.strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal] > 0.0
                    val metabolicThroughput = when {
                        predator && consumer.thermalStrategy == ThermalStrategy.ENDOTHERMY -> 1.50
                        predator && consumer.thermalStrategy == ThermalStrategy.HETEROTHERMY -> 1.25
                        filterFeeder && consumer.thermalStrategy == ThermalStrategy.ENDOTHERMY -> 4.0 / 3.0
                        else -> 1.0
                    }
                    min(
                        1.0,
                        consumerBiomass *
                            config.maximumConsumedBiomassFraction *
                            metabolicThroughput /
                            potentialConsumption,
                    )
                } else {
                    1.0
                }
            for (targetPopulation in 0 until community.size) {
                if (consumerPopulation == targetPopulation) continue
                val targetIndex = community.speciesIndices[targetPopulation]
                val offset = consumerIndex * speciesCount + targetIndex
                if (ecology.interactions.kindAt(offset) == InteractionKind.NONE.ordinal) continue
                val targetBiomass = effectiveActive[targetPopulation]
                if (targetBiomass <= 0.0) continue

                val kind = InteractionKind.entries[ecology.interactions.kindAt(offset)]
                val target = ecology.species[targetIndex]
                val targetHabitat = ecology.niches[community.nicheIndices[targetPopulation]].habitat
                val authoredCamouflage = target.camouflage[targetHabitat.ordinal]
                val colorCamouflage = targetHabitat.camouflageMatch(
                    target.camouflageColor,
                    environment.snowOrIce,
                    environment.canopyCover,
                    environment.reefCover,
                )
                val concealment =
                    if (
                        kind == InteractionKind.PREDATION ||
                        kind == InteractionKind.SUPPLEMENTAL_FEEDING
                    ) {
                        (authoredCamouflage + colorCamouflage).coerceIn(0.0, 0.72)
                    } else {
                        0.0
                    }
                val lossRate = ecology.interactions.targetLossAt(offset) * (1.0 - concealment)
                val accessibilityCoefficient = kind.lowDensityAccessibilityCoefficient
                val lowDensityAccessibility =
                    if (accessibilityCoefficient > 0.0) {
                        targetBiomass /
                            (targetBiomass + consumerBiomass * accessibilityCoefficient)
                    } else {
                        1.0
                    }
                // `lossRate` describes the fraction of available prey captured,
                // while `consumptionScale` enforces one total intake ceiling
                // without making species-list order determine which prey is eaten.
                // Modeled prey also become disproportionately difficult to find
                // at low density, providing the refuge needed to avoid deterministic
                // prey extinction in this coarse one-tile model.
                val edgeConsumptionScale =
                    if (kind == InteractionKind.SUPPLEMENTAL_FEEDING) 1.0
                    else consumptionScale
                val targetLoss =
                    targetBiomass * lossRate * lowDensityAccessibility * edgeConsumptionScale
                if (targetLoss <= 0.0) continue

                interactionLosses[targetPopulation] += targetLoss
                val compiledLoss = ecology.interactions.targetLossAt(offset)
                if (compiledLoss > 0.0) {
                    val efficiency = ecology.interactions.consumerGainAt(offset) / compiledLoss
                    interactionGains[consumerPopulation] += targetLoss * efficiency
                }
                relationshipBenefits[targetPopulation] +=
                    targetLoss * ecology.interactions.targetBenefitAt(offset)
            }
        }
    }

    private fun updatePopulations(
        community: TileCommunity,
        environment: SeasonalCellEnvironment,
        fluxes: CellTurnFluxes?,
    ) {
        for (populationIndex in 0 until community.size) {
            val species = ecology.species[community.speciesIndices[populationIndex]]
            val niche = ecology.niches[community.nicheIndices[populationIndex]]
            val active = effectiveActive[populationIndex]
            if (active <= 0.0) {
                nextActive[populationIndex] = 0.0
                nextReserves[populationIndex] = 0.0
                continue
            }

            val habitat = environment.habitatAvailability(niche.habitat)
            val baseResource = environment.resourceSupport(niche, species.sizeClass)
            val canUseWasteAsFertilizer =
                species.strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] > 0.0 ||
                    species.strategySupport[EcoStrategy.ABSORPTION.ordinal] > 0.0
            val resource =
                if (canUseWasteAsFertilizer && species.wasteFertilization > 0.0) {
                    (
                        baseResource +
                            species.wasteFertilization *
                            environment.resources.waste *
                            (1.0 - baseResource)
                        ).coerceIn(0.0, 1.0)
                } else {
                    baseResource
                }
            val carryingBiomass =
                EcologyBiomass.carryingCapacityKg(species, niche, environment)
            val nicheSizeOffset =
                community.nicheIndices[populationIndex] * SizeClass.entries.size
            var normalizedNicheBiomass = 0.0
            for (otherSize in SizeClass.entries) {
                val sizeDistance = abs(otherSize.ordinal - species.sizeClass.ordinal)
                val overlap = when (sizeDistance) {
                    0 -> 1.0
                    1 -> 0.35
                    2 -> 0.10
                    3 -> 0.03
                    else -> 0.01
                }
                normalizedNicheBiomass +=
                    normalizedBiomassByNicheAndSize[nicheSizeOffset + otherSize.ordinal] *
                        overlap
            }
            val normalizedActive =
                active /
                    species.sizeClass.densityScale *
                    species.nicheCompetitionSensitivity
            val competingBiomass =
                active +
                    max(0.0, normalizedNicheBiomass - normalizedActive) *
                    species.sizeClass.densityScale *
                    config.interspecificNicheCompetition *
                    species.nicheCompetitionSensitivity
            val crowding = competingBiomass /
                max(1.0, carryingBiomass)
            // The denominator offset controls saturation; it must not act as
            // food when both background resources and modeled prey are absent.
            val resourceFactor = resource / (resource + 0.08 + crowding)
            val environmentalFitness = fitness[populationIndex]
            val backgroundAssimilation =
                active *
                    (0.30 + species.seasonalReproduction) *
                    environmentalFitness *
                    resourceFactor
            // Below the viable-activity threshold an organism may endure for a
            // while, but cannot turn captured food into growth. This prevents
            // abundant prey from making a profoundly climate-mismatched animal
            // thrive while still allowing recovery during a suitable season.
            val physiologicalAssimilation =
                if (environmentalFitness < 0.35) 0.0 else sqrt(environmentalFitness)
            val interactionCapacityRemaining =
                (1.0 - active / max(1.0, carryingBiomass)).coerceIn(0.0, 1.0)
            val grossAssimilation =
                backgroundAssimilation +
                    (
                        interactionGains[populationIndex] +
                            relationshipBenefits[populationIndex]
                        ) *
                    physiologicalAssimilation *
                    interactionCapacityRemaining
            val maintenanceFraction = species.maintenanceDemand / species.massKg
            val maintenance = active * maintenanceFraction
            val stress = 1.0 - environmentalFitness
            // Moderate mismatch already reduces feeding and reproduction through
            // environmentalFitness. Direct deaths are reserved for severe
            // physiological stress so the same dry or cool season is not charged
            // twice at full strength.
            val stressLoss =
                if (
                    EcologyFitness.thermal(species, environment) <= 0.0 ||
                    EcologyFitness.elevation(species, environment, niche.habitat) <= 0.0
                ) {
                    active * config.lethalTemperatureMortality
                } else {
                    active * config.stressMortality * stress * stress * stress * stress
                }
            val backgroundLoss = active * config.backgroundMortality
            val predationLoss = min(active, interactionLosses[populationIndex])
            val softDiversityCapacity = EcologyDiversity.softHabitatCapacity(
                environment = environment,
                habitat = niche.habitat,
                baseSlots = config.habitatDiversityBaseSlots,
                availabilitySlots = config.habitatDiversityAvailabilitySlots,
            )
            val excessDiversity =
                max(
                    0.0,
                    populationCountByHabitat[niche.habitat.ordinal] -
                        softDiversityCapacity,
                )
            val diversityPressure =
                if (softDiversityCapacity <= 0.0 || excessDiversity <= 0.0) {
                    0.0
                } else {
                    val excessRatio = excessDiversity / softDiversityCapacity
                    excessRatio / (1.0 + excessRatio)
                }
            val bestStanding =
                bestCompetitiveStandingByHabitat[niche.habitat.ordinal]
            val relativeStanding =
                if (bestStanding <= 0.0) 0.0
                else (competitiveStanding[populationIndex] / bestStanding).coerceIn(0.0, 1.0)
            val competitiveDisadvantage =
                config.strongestCompetitorMortalityFraction +
                    (1.0 - config.strongestCompetitorMortalityFraction) *
                    (1.0 - relativeStanding)
            val diversityLoss =
                active *
                    config.maximumHabitatDiversityMortality *
                    diversityPressure *
                    competitiveDisadvantage

            var reserves = community.reserves[populationIndex]
            var energyBalance = grossAssimilation - maintenance
            var starvationLoss = 0.0
            var growth = 0.0
            if (energyBalance >= 0.0) {
                val reserveCapacity = active * species.reserveCapacity
                val reserveGain = min(energyBalance * 0.32, max(0.0, reserveCapacity - reserves))
                reserves += reserveGain
                energyBalance -= reserveGain
                growth = min(
                    energyBalance * 0.62,
                    active * species.seasonalReproduction,
                )
            } else {
                val reserveUse = min(reserves, -energyBalance)
                reserves -= reserveUse
                energyBalance += reserveUse
                if (energyBalance < 0.0) {
                    starvationLoss = min(
                        active * config.maximumStarvationMortality,
                        -energyBalance,
                    )
                }
            }

            val totalLoss = min(
                active + growth,
                stressLoss +
                    backgroundLoss +
                    predationLoss +
                    starvationLoss +
                    diversityLoss,
            )
            val updatedActive = max(0.0, active + growth - totalLoss)
            nextActive[populationIndex] = updatedActive
            nextReserves[populationIndex] = min(reserves, updatedActive * species.reserveCapacity)

            fluxes?.let {
                when (niche.strategy) {
                    EcoStrategy.SCAVENGING ->
                        it.carrionConsumedBiomass += backgroundAssimilation / 0.35
                    EcoStrategy.DECOMPOSITION ->
                        it.detritusConsumedBiomass += backgroundAssimilation / 0.42
                    EcoStrategy.COPROPHAGY ->
                        it.wasteConsumedBiomass += backgroundAssimilation / 0.48
                    EcoStrategy.DEPOSIT_FEEDING ->
                        it.marineSnowConsumedBiomass += backgroundAssimilation / 0.40
                    else -> Unit
                }
                val deaths = backgroundLoss + predationLoss + starvationLoss + stressLoss
                if (species.motile) {
                    // Carrion is dead motile tissue. Living motile organisms
                    // also return a fraction of assimilated food as waste.
                    it.carrionBiomass += deaths *
                        if (niche.habitat in EcologyFitness.aquaticHabitats) 0.20 else 0.35
                    it.wasteBiomass += grossAssimilation * 0.08
                } else {
                    // Dead sessile tissue enters the detritus pool rather than
                    // being treated as an animal carcass.
                    it.detritusBiomass += deaths * 0.35
                }
                if (
                    niche.habitat in EcologyFitness.aquaticHabitats &&
                    niche.habitat != Habitat.FRESHWATER
                ) {
                    it.marineSnowBiomass += deaths * 0.30
                }
                if (species.reefBuilding > 0.0 && niche.habitat in EcologyFitness.aquaticHabitats) {
                    it.reefCoverDelta +=
                        species.reefBuilding *
                            environmentalFitness *
                            (updatedActive / max(1.0, carryingBiomass)) *
                            (1.0 - environment.reefCover)
                }
            }
        }

        for (populationIndex in 0 until community.size) {
            community.activeBiomass[populationIndex] = finiteNonNegative(nextActive[populationIndex])
            community.reserves[populationIndex] = finiteNonNegative(nextReserves[populationIndex])
            community.dormantBiomass[populationIndex] = finiteNonNegative(nextDormant[populationIndex])
        }
    }

    fun finalizeLocalExtinctions(community: TileCommunity) {
        var populationIndex = community.size - 1
        while (populationIndex >= 0) {
            val species = ecology.species[community.speciesIndices[populationIndex]]
            val individuals =
                (community.activeBiomass[populationIndex] + community.dormantBiomass[populationIndex]) /
                    species.massKg
            if (individuals < config.minimumViableIndividuals) {
                community.removeAt(populationIndex)
            }
            populationIndex--
        }
    }

    private fun finiteNonNegative(value: Double): Double =
        if (value.isFinite()) max(0.0, value) else 0.0

    private companion object {
        const val DORMANT_LEAF_COLD_PROTECTION_C = 20.0
        const val SEASONAL_TORPOR_COLD_BUFFER_C = 5.0
        const val SEASONAL_TORPOR_LETHAL_RAMP_C = 10.0
    }

    private val InteractionKind.lowDensityAccessibilityCoefficient: Double
        get() = when (this) {
            InteractionKind.PREDATION -> 4.0
            InteractionKind.GRAZING -> 4.0
            InteractionKind.FILTER_FEEDING -> 0.75
            InteractionKind.NONE,
            InteractionKind.SUPPLEMENTAL_FEEDING,
            InteractionKind.PARASITISM,
            -> 0.0
        }
}

fun isGloballyExtinct(speciesIndex: Int, communities: Iterable<TileCommunity>): Boolean =
    communities.none { community ->
        val index = community.find(speciesIndex)
        index >= 0 &&
            community.activeBiomass[index] + community.dormantBiomass[index] > 0.0
    }
