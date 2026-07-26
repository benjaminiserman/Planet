package dev.biserman.planet.planet.ecology.v2

enum class StarLight {
    BLUE_WHITE,
    WHITE,
    YELLOW,
    ORANGE,
    RED,
}

enum class BiologicalColor {
    BLACK,
    BROWN,
    GREEN,
    BLUE_GREEN,
    RED,
    PURPLE,
    PALE,
}

enum class ThermalStrategy {
    ECTOTHERMY,
    ENDOTHERMY,
    HETEROTHERMY,
}

enum class AquaticSalinityTolerance {
    SALTWATER_ONLY,
    FRESHWATER_ONLY,
    BROAD,
}

enum class DormancyKind {
    NONE,
    PROPAGULE,
    BURROWED_EGGS,
    SEASONAL_TORPOR,
    WHOLE_BODY_DESICCATION,
}

enum class DispersalKind(val rangeClass: Int) {
    NONE(0),
    NEIGHBOR(1),
    SHORT_MIGRATION(2),
    REGIONAL_MIGRATION(3),
    LONG_MIGRATION(4),
}

/**
 * Invariant species are authored aggregate guilds rather than evolutionary
 * lineages. They still use the ordinary population, niche, interaction, and
 * extinction systems.
 */
enum class SpeciesKind {
    EVOLVING,
    INVARIANT,
}

data class SpeciesDefinition(
    val id: String,
    val displayName: String,
    val sizeClass: SizeClass,
    val motile: Boolean,
    val traits: List<SpeciesTrait>,
    val photosyntheticColor: BiologicalColor? = null,
    val camouflageColor: BiologicalColor? = null,
    val ancestorSpeciesId: String? = null,
    val kind: SpeciesKind = SpeciesKind.EVOLVING,
) {
    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(traits.distinct().size == traits.size) {
            "$displayName repeats a trait"
        }
    }
}

sealed interface TraitEffect {
    data class HabitatSupport(val habitat: Habitat, val amount: Double) : TraitEffect
    data class StrategySupport(val strategy: EcoStrategy, val amount: Double) : TraitEffect
    data class TemperatureShift(val degreesC: Double) : TraitEffect
    data class TemperatureTolerance(val colderC: Double = 0.0, val hotterC: Double = 0.0) : TraitEffect
    data class TemperatureOptimalTolerance(
        val colderC: Double = 0.0,
        val hotterC: Double = 0.0,
    ) : TraitEffect
    data class ThermalRegulation(val strategy: ThermalStrategy) : TraitEffect
    data class SeasonalColdTolerance(
        val maximumBonusC: Double,
        val triggerInsolation: Double,
    ) : TraitEffect

    data class WaterRequirement(val change: Double) : TraitEffect
    data class MaximumWaterTolerance(
        val optimalMaximumChange: Double,
        val absoluteMaximumChange: Double,
    ) : TraitEffect

    data class InsolationOptimum(val change: Double) : TraitEffect
    data class CanopyLightEfficiency(val change: Double) : TraitEffect
    data class CaptureAbility(val change: Double) : TraitEffect
    data class Defense(val change: Double) : TraitEffect
    data class Camouflage(val habitat: Habitat, val change: Double) : TraitEffect
    data class ReefUse(val change: Double) : TraitEffect
    data class ReefBuilding(val change: Double) : TraitEffect
    data class WasteFertilization(val change: Double) : TraitEffect
    data class ReserveCapacity(val change: Double) : TraitEffect
    data class NicheCompetitionSensitivity(val multiplier: Double) : TraitEffect
    data class Dormancy(val kind: DormancyKind, val survivalPerSeason: Double) : TraitEffect
    data class Dispersal(val kind: DispersalKind) : TraitEffect
    data class ReproductionMultiplier(val multiplier: Double) : TraitEffect
    data object FreshwaterOsmoregulation : TraitEffect
    data object BroadSalinityTolerance : TraitEffect
    data object PelagicAerialResidency : TraitEffect
    data object DarkWaterAdaptation : TraitEffect
    data class MaintenanceCost(val fraction: Double) : TraitEffect
}

sealed interface SpeciesSelector {
    data class ExactSpecies(val speciesId: String) : SpeciesSelector
    data class DescendantsOf(val ancestorSpeciesId: String) : SpeciesSelector
}

sealed interface RelationshipEffect {
    data class SupplementalFood(
        val target: SpeciesSelector,
        val attackRate: Double,
        val assimilationEfficiency: Double,
    ) : RelationshipEffect

    data class ParasiteOf(
        val target: SpeciesSelector,
        val drainRate: Double,
    ) : RelationshipEffect

    data class BenefitsTargetWhenFeeding(
        val target: SpeciesSelector,
        val benefitRate: Double,
    ) : RelationshipEffect

    /**
     * The consumer can remain active only while at least one selected target is
     * locally present. This represents obligate hosts or symbionts without
     * prescribing what the relationship extracts from the target.
     */
    data class RequiresTarget(
        val target: SpeciesSelector,
    ) : RelationshipEffect
}

data class NicheDefinition(
    val habitat: Habitat,
    val strategy: EcoStrategy,
) {
    val displayName: String =
        "${habitat.displayName} ${strategy.displayName}"
}

object EcologyNiches {
    /**
     * Strategies own the whitelist of habitats in which they make ecological
     * sense, so adding a strategy does not require a second registry.
     */
    val defaults: List<NicheDefinition> =
        EcoStrategy.entries.flatMap { strategy ->
            strategy.supportedHabitats.map { habitat ->
                NicheDefinition(habitat, strategy)
            }
        }
}
