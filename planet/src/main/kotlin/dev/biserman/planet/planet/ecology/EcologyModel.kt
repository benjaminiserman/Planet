package dev.biserman.planet.planet.ecology

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
    BLUE,
    RED,
    PURPLE,
    PALE,
    WHITE,
    COUNTERSHADE,
    ADAPTIVE
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

enum class AquaticRespirationMode {
    UNDERWATER,
    BREATH_HOLDING,
}

enum class DormancyKind {
    NONE,
    PROPAGULE,
    BURROWED_EGGS,
    SEASONAL_TORPOR,
    COLD_DARK_LEAF_DORMANCY,
    DROUGHT_DECIDUOUS,
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
    fun applyTo(context: SpeciesCompilationContext)

    data class HabitatSupport(val habitat: Habitat, val amount: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.supportHabitat(habitat, amount)
    }
    data class StrategySupport(val strategy: EcoStrategy, val amount: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.supportStrategy(strategy, amount)
    }
    data class TemperatureShift(val degreesC: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.shiftTemperature(degreesC)
    }
    data class TemperatureTolerance(val colderC: Double = 0.0, val hotterC: Double = 0.0) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) =
            context.widenTemperatureTolerance(colderC, hotterC)
    }
    data class TemperatureOptimalTolerance(
        val colderC: Double = 0.0,
        val hotterC: Double = 0.0,
    ) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) =
            context.widenOptimalTemperatureTolerance(colderC, hotterC)
    }
    data class MinimumActiveTemperature(val temperatureC: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) =
            context.requireMinimumActiveTemperature(temperatureC)
    }
    data class FrozenDormantSurvival(val fractionPerSeason: Double) : TraitEffect {
        init {
            require(fractionPerSeason in 0.0..1.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) =
            context.multiplyFrozenDormantSurvival(fractionPerSeason)
    }
    data class ThermalRegulation(val strategy: ThermalStrategy) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.regulateTemperatureWith(strategy)
    }
    data class SeasonalColdTolerance(
        val maximumBonusC: Double,
        val triggerInsolation: Double,
    ) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) =
            context.tolerateSeasonalCold(maximumBonusC, triggerInsolation)
    }

    data class WaterRequirement(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changeWaterRequirement(change)
    }
    data class MaximumWaterTolerance(
        val optimalMaximumChange: Double,
        val absoluteMaximumChange: Double,
    ) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) =
            context.changeMaximumWaterTolerance(optimalMaximumChange, absoluteMaximumChange)
    }
    data class WaterDepthTolerance(
        val optimalMaximumM: Double,
        val absoluteMaximumM: Double,
    ) : TraitEffect {
        init {
            require(optimalMaximumM >= 0.0)
            require(absoluteMaximumM > optimalMaximumM)
        }
        override fun applyTo(context: SpeciesCompilationContext) =
            context.limitWaterDepth(optimalMaximumM, absoluteMaximumM)
    }
    data class ElevationToleranceShift(val meters: Double) : TraitEffect {
        init {
            require(meters >= 0.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) = context.shiftElevationTolerance(meters)
    }
    data object SnowHydration : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.hydrateFromSnow()
    }

    data class InsolationOptimum(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.shiftInsolationOptimum(change)
    }
    data class CanopyLightEfficiency(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changeCanopyLightEfficiency(change)
    }
    data class DenseCanopyForagingPenalty(val maximumPenalty: Double) : TraitEffect {
        init {
            require(maximumPenalty in 0.0..1.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) =
            context.addDenseCanopyForagingPenalty(maximumPenalty)
    }
    data class CaptureAbility(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changeCaptureAbility(change)
    }
    data class PursuitSpeed(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changePursuitSpeed(change)
    }
    data class Defense(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changeDefense(change)
    }
    data class Camouflage(val habitat: Habitat, val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.addCamouflage(habitat, change)
    }
    data object AposematicColoration : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.enableAposematicColoration()
    }
    data class ReefUse(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changeReefUse(change)
    }
    data class ReefBuilding(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changeReefBuilding(change)
    }
    data class FruitProduction(val activeBiomassFractionPerSeason: Double) : TraitEffect {
        init {
            require(activeBiomassFractionPerSeason in 0.0..1.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) =
            context.produceFruit(activeBiomassFractionPerSeason)
    }
    data object Flowering : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.enableFlowering()
    }
    data class NectarProduction(val activeBiomassFractionPerSeason: Double) : TraitEffect {
        init {
            require(activeBiomassFractionPerSeason in 0.0..1.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) =
            context.produceNectar(activeBiomassFractionPerSeason)
    }
    data class PollinationEfficiency(val change: Double) : TraitEffect {
        init {
            require(change >= 0.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) =
            context.changePollinationEfficiency(change)
    }
    data class WasteFertilization(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changeWasteFertilization(change)
    }
    data class ReserveCapacity(val change: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.changeReserveCapacity(change)
    }
    data class NicheCompetitionSensitivity(val multiplier: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) =
            context.multiplyNicheCompetitionSensitivity(multiplier)
    }
    data class Dormancy(val kind: DormancyKind, val survivalPerSeason: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.enterDormancy(kind, survivalPerSeason)
    }
    data class DormantEntryBiomassRetention(val fraction: Double) : TraitEffect {
        init {
            require(fraction in 0.0..1.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) =
            context.multiplyDormantEntryRetention(fraction)
    }
    data class DormantReactivationMultiplier(val multiplier: Double) : TraitEffect {
        init {
            require(multiplier >= 1.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) =
            context.multiplyDormantReactivation(multiplier)
    }
    data class Dispersal(val kind: DispersalKind) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.enableDispersal(kind)
    }
    data class ReproductionMultiplier(val multiplier: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.multiplyReproduction(multiplier)
    }
    data class MetabolicDemandMultiplier(val multiplier: Double) : TraitEffect {
        init {
            require(multiplier in 0.0..1.0)
        }
        override fun applyTo(context: SpeciesCompilationContext) = context.multiplyMetabolicDemand(multiplier)
    }
    data object FreshwaterOsmoregulation : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.enableFreshwaterOsmoregulation()
    }
    data object BroadSalinityTolerance : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.enableBroadSalinityTolerance()
    }
    data class AquaticRespiration(val mode: AquaticRespirationMode) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.enableAquaticRespiration(mode)
    }
    data object PelagicAerialResidency : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.enablePelagicAerialResidency()
    }
    data object DarkWaterAdaptation : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.adaptToDarkWater()
    }
    data class ObligateResidentHabitat(val habitat: Habitat) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.requireResidentHabitat(habitat)
    }
    data object RequiresAdjacentLand : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.requireAdjacentLand()
    }
    data class MaintenanceCost(val fraction: Double) : TraitEffect {
        override fun applyTo(context: SpeciesCompilationContext) = context.addMaintenanceCost(fraction)
    }
}

sealed interface SpeciesSelector {
    data class ExactSpecies(val speciesId: String) : SpeciesSelector
    data class DescendantsOf(val ancestorSpeciesId: String) : SpeciesSelector
}

sealed interface RelationshipEffect {
    fun compile(context: RelationshipCompilationContext)

    /**
     * All ordinary feeding edges are replaced by the selected food taxa. At
     * least one selected target must be locally present for the consumer to
     * remain active.
     */
    data class ObligateFood(
        val target: SpeciesSelector,
        val attackRate: Double,
        val assimilationEfficiency: Double,
    ) : RelationshipEffect {
        override fun compile(context: RelationshipCompilationContext) {
            context.forEachTarget(target) { targetIndex ->
                context.requireProducerTarget(targetIndex)
                context.setInteraction(
                    targetIndex,
                    InteractionKind.GRAZING,
                    attackRate,
                    attackRate * assimilationEfficiency,
                    required = true,
                )
            }
        }
    }

    data class SupplementalFood(
        val target: SpeciesSelector,
        val attackRate: Double,
        val assimilationEfficiency: Double,
    ) : RelationshipEffect {
        override fun compile(context: RelationshipCompilationContext) {
            context.forEachTarget(target) { targetIndex ->
                context.setInteraction(
                    targetIndex,
                    InteractionKind.SUPPLEMENTAL_FEEDING,
                    attackRate,
                    attackRate * assimilationEfficiency,
                )
            }
        }
    }

    data class ParasiteOf(
        val target: SpeciesSelector,
        val drainRate: Double,
    ) : RelationshipEffect {
        override fun compile(context: RelationshipCompilationContext) {
            context.forEachTarget(target) { targetIndex ->
                context.setInteraction(
                    targetIndex,
                    InteractionKind.PARASITISM,
                    drainRate,
                    drainRate * 0.35,
                )
            }
        }
    }

    data class BenefitsTargetWhenFeeding(
        val target: SpeciesSelector,
        val benefitRate: Double,
    ) : RelationshipEffect {
        override fun compile(context: RelationshipCompilationContext) {
            context.forEachTarget(target) { targetIndex ->
                context.addTargetBenefit(targetIndex, benefitRate)
            }
        }
    }

    /**
     * The consumer can remain active only while at least one selected target is
     * locally present. This represents obligate hosts or symbionts without
     * prescribing what the relationship extracts from the target.
     */
    data class RequiresTarget(
        val target: SpeciesSelector,
    ) : RelationshipEffect {
        override fun compile(context: RelationshipCompilationContext) {
            context.forEachTarget(target, context::requireTarget)
        }
    }
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