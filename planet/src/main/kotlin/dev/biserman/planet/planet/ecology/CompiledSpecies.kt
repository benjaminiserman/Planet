package dev.biserman.planet.planet.ecology

class CompiledSpecies(
    val index: Int,
    val id: String,
    val displayName: String,
    val sizeClass: SizeClass,
    val motile: Boolean,
    val kind: SpeciesKind,
    val ancestorSpeciesId: String?,
    val physiology: PhysiologyProfile,
    val environment: EnvironmentalProfile,
    val lifeHistory: LifeHistoryProfile,
    val interactions: InteractionProfile,
    val niche: NicheProfile,
)

data class PhysiologyProfile(
    val massKg: Double,
    val maintenanceDemand: Double,
    val thermal: ThermalProfile,
    val hydration: HydrationProfile,
    val respiration: RespirationProfile,
)

data class ThermalProfile(
    val outerLowC: Double,
    val optimalLowC: Double,
    val optimalHighC: Double,
    val outerHighC: Double,
    val minimumActiveC: Double,
    val frozenDormantSurvival: Double,
    val seasonalColdToleranceC: Double,
    val seasonalColdTriggerInsolation: Double,
    val regulation: ThermalStrategy?,
)

data class HydrationProfile(
    val minimumWater: Double,
    val optimalMaximumWater: Double,
    val maximumWater: Double,
    val snowHydration: Boolean,
)

data class RespirationProfile(
    val salinityTolerance: AquaticSalinityTolerance,
    val underwaterBreathing: Boolean,
    val prolongedBreathHolding: Boolean,
)

data class EnvironmentalProfile(
    val optimalMaximumWaterDepthM: Double,
    val absoluteMaximumWaterDepthM: Double,
    val elevationToleranceShiftM: Double,
    val insolationOptimum: Double,
    val canopyLightEfficiency: Double,
    val denseCanopyForagingPenalty: Double,
    val pelagicAerialResident: Boolean,
    val darkWaterAdapted: Boolean,
    val requiresAdjacentLand: Boolean,
)

data class LifeHistoryProfile(
    val seasonalReproduction: Double,
    val reserveCapacity: Double,
    val nicheCompetitionSensitivity: Double,
    val dormancyKind: DormancyKind,
    val dormantSurvival: Double,
    val dormantEntryBiomassRetention: Double,
    val dormantReactivationMultiplier: Double,
    val dispersalKind: DispersalKind,
)

data class InteractionProfile(
    val captureAbility: Double,
    val pursuitSpeed: Double,
    val defense: Double,
    val aposematicColoration: Boolean,
    val dangerousWarningModel: Boolean,
    val reefUse: Double,
    val reefBuilding: Double,
    val fruitProduction: Double,
    val flowering: Boolean,
    val nectarProduction: Double,
    val pollinationEfficiency: Double,
    val wasteFertilization: Double,
)

class NicheProfile internal constructor(
    val producerCompetitionLayer: ProducerCompetitionLayer,
    val photosyntheticColor: BiologicalColor?,
    val camouflageColor: BiologicalColor?,
    habitatSupport: DoubleArray,
    strategySupport: DoubleArray,
    camouflage: DoubleArray,
    nicheFit: DoubleArray,
) {
    private val habitatSupport = habitatSupport.copyOf()
    private val strategySupport = strategySupport.copyOf()
    private val camouflage = camouflage.copyOf()
    private val nicheFit = nicheFit.copyOf()

    val nicheCount: Int
        get() = nicheFit.size

    fun supportFor(habitat: Habitat): Double = habitatSupport[habitat.ordinal]

    fun supports(habitat: Habitat): Boolean = supportFor(habitat) > 0.0

    fun supportFor(strategy: EcoStrategy): Double = strategySupport[strategy.ordinal]

    fun camouflageFor(habitat: Habitat): Double = camouflage[habitat.ordinal]

    fun fitFor(nicheIndex: Int): Double = nicheFit[nicheIndex]

    fun hasViableNiche(): Boolean = nicheFit.any { it > 0.0 }

    fun bestNicheIndex(): Int = nicheFit.indices.maxBy { nicheFit[it] }
}

/**
 * Internal spatial partitioning for photosynthetic competitors. Suspended
 * producers occupy the water column; attached producers occupy substrate.
 * This is compiled from descriptive traits rather than authored by species.
 */
enum class ProducerCompetitionLayer {
    NONE,
    SUSPENDED,
    ATTACHED,
}