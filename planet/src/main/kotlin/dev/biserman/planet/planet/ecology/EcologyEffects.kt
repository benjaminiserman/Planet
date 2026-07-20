package dev.biserman.planet.planet.ecology

import kotlin.math.pow

/** Total biomass keyed by species id. */
typealias Biomass = Map<String, Double>

/** Marker interface for the declarative effects attached to a species trait. */
sealed interface TraitEffect

/** Broad environmental conditions used to bias establishment without replacing numeric climate stress. */
enum class EnvironmentTag {
    COLD,
    HOT,
    DRY,
    WET,
    ASEASONAL,
    SEASONAL,
    HYPERSEASONAL,
    FOREST,
    GRASSLAND,
    WETLAND,
    DESERT,
    SNOWY,
    HIGH_INSOLATION,
    LOW_LIGHT,
}

/** Signed affinity contributed by a concrete trait toward one environmental condition. */
data class EnvironmentAffinityEffect(
    val tag: EnvironmentTag,
    val affinity: Double,
) : TraitEffect {
    init { require(affinity.isFinite()) }
}

/** Capability and behavior labels used by requirements, conflicts, and counters. */
enum class EffectTag {
    AUTOTROPHY,      // Can gain biomass without eating another species.
    FEEDING,         // Can gain biomass through a derived diet.
    MOTILE,          // Has a mobile body and is available as motile prey.
    SESSILE,         // Is fixed in place.
    PACK_HUNTING,    // Hunts cooperatively and can tackle relatively larger prey.
    AMBUSH_HUNTING,  // Uses ambush tactics and partly counters some defenses.
    CHASE_HUNTING,   // Uses sustained pursuit.
    FLIGHT,          // Can fly and fully counters flight-based evasion.
    AERIAL_PREY_CAPTURE, // Can reliably seize small flying prey without flying itself.
    BURROWING,       // Can burrow and fully counters burrow-based evasion.
    NOCTURNAL,       // Is active at night and counters nocturnal evasion.
    WINTER_SURVIVAL, // Protects perennial plant tissue through freezing seasons.
}

/** Trait families in which a blueprint may select at most one member. */
enum class ExclusiveGroup {
    GROWTH_SPEED,     // Fast-growing and slow-growing are alternatives.
    HUNTING_STRATEGY, // Pack, ambush, and chase strategies are alternatives.
    COAT_DENSITY,     // Thick fur and a sparse coat are mutually exclusive.
    CAMOUFLAGE,       // Only one fixed or adaptive camouflage strategy may be selected.
}

/** Numerical solver parameters that trait effects may override or multiply. */
enum class NumericTarget {
    AUTOTROPHY_GROWTH_RATE,   // Intrinsic logistic growth rate of autotrophic biomass.
    CARRYING_CAPACITY,        // Baseline autotrophic biomass capacity per unit land area.
    SEASONAL_AMPLITUDE,       // Fractional seasonal oscillation in carrying capacity.
    MAX_CONSUMPTION_RATE,     // Maximum food biomass ingested per feeder biomass per year.
    ASSIMILATION_EFFICIENCY,  // Fraction of ingested biomass converted into feeder biomass.
    MORTALITY_RATE,           // Baseline feeder biomass lost per unit biomass per year.
    METABOLIC_DEMAND_RATE,    // Assimilated food needed per feeder biomass per year to avoid starvation.
    STARVATION_MORTALITY_RATE, // Maximum fractional biomass loss per year at zero food intake.
    DENSITY_DEPENDENCE,       // Strength of crowding, disease, and territorial losses.
    INTERFERENCE,             // Strength with which crowded feeders obstruct feeding.
    RESPONSE_EXPONENT,        // Exponent of the feeding response; values above one protect scarce prey.
    MIN_OPTIMUM_TEMPERATURE_C, // Lower edge of the no-stress temperature interval in degrees Celsius.
    MAX_OPTIMUM_TEMPERATURE_C, // Upper edge of the no-stress temperature interval in degrees Celsius.
    MIN_OPTIMUM_MOISTURE_MM,   // Lowest no-stress monthly precipitation in millimetres.
    MAX_OPTIMUM_MOISTURE_MM,   // Highest no-stress monthly precipitation; infinity means no upper limit.
    MIN_OPTIMUM_INSOLATION_W_M2, // Lowest no-stress mean insolation in watts per square metre.
    MAX_OPTIMUM_INSOLATION_W_M2, // Highest no-stress mean insolation in watts per square metre.
    PHOTOSYNTHESIS_HALF_SATURATION_W_M2, // Insolation producing half of the maximum light response.
    CLIMATE_STRESS_SENSITIVITY, // Multiplier applied to mortality caused by climate outside the niche.
}

/** Abstract resource categories used to match feeding niches with edible features. */
enum class FoodResource {
    LOW_FOLIAGE,    // Leaves and shoots reachable by ground-level foragers.
    CANOPY_FOLIAGE, // Elevated foliage accessible to high browsers.
    COARSE_GRASS,   // Fibrous grass best exploited by fermenting grazers.
    BAMBOO,         // Bamboo leaves, shoots, and culms targeted by bamboo specialists.
    FRUIT,        // Renewable fruit offered without standing-biomass loss.
    SEED,         // Seeds, with only a small standing-biomass cost.
    WOOD,         // Woody tissue.
    MOTILE_PREY,  // The body biomass of a motile organism.
}

/** Adds a capability or countermeasure label to a blueprint. */
data class GrantsTag(
    /** Tag made available to validation and interaction effects. */
    val tag: EffectTag,
) : TraitEffect

/** Declares that another effect on the same blueprint must grant a tag. */
data class RequiresTag(
    /** Tag that must be present after all traits are combined. */
    val tag: EffectTag,
) : TraitEffect

/** Declares that another effect on the same blueprint must not grant a tag. */
data class ConflictsWithTag(
    /** Tag whose presence makes the blueprint invalid. */
    val tag: EffectTag,
) : TraitEffect

/** Places a trait into a mutually exclusive family. */
data class ExclusiveEffect(
    /** Family in which only one effect may occur on a blueprint. */
    val group: ExclusiveGroup,
) : TraitEffect

/** Composably caps a numerical parameter after offsets and multipliers are applied. */
data class NumericUpperBound(
    /** Solver parameter being capped. */
    val target: NumericTarget,
    /** Maximum allowed value; multiple bounds stack by choosing the smallest. */
    val maximum: Double,
) : TraitEffect

/** Multiplies a numerical parameter after all additive effects are summed. */
data class NumericMultiplier(
    /** Solver parameter being scaled. */
    val target: NumericTarget,
    /** Factor multiplied into the selected parameter value. */
    val multiplier: Double,
) : TraitEffect

/** Adds a signed amount to a numerical parameter before multipliers are applied. */
data class NumericOffset(
    /** Solver parameter being shifted. */
    val target: NumericTarget,
    /** Signed shift in the target parameter's own units. */
    val offset: Double,
) : TraitEffect

/** Adds an always-paid energetic and structural biomass cost to a trait. */
data class MaintenanceCost(
    /** Fractional biomass lost per year while maintaining this adaptation. */
    val biomassLossRatePerYear: Double,
) : TraitEffect {
    init {
        require(biomassLossRatePerYear >= 0.0)
    }
}

/** Exposes part of a species as a resource that feeding traits can target. */
data class EdibleAs(
    /** Resource category exposed by this trait. */
    val resource: FoodResource,
    /** Multiplier describing how available or attractive the resource is. */
    val preferenceMultiplier: Double = 1.0,
    /** Fraction of ingested biomass removed from the prey's standing biomass. */
    val preyLossFraction: Double = 1.0,
    /** Maximum renewable food produced per unit producer biomass per year; null means standing biomass. */
    val renewableProductionRate: Double? = null,
) : TraitEffect {
    init {
        require(preferenceMultiplier > 0.0)
        require(preyLossFraction in 0.0..1.0)
        require(renewableProductionRate == null || renewableProductionRate > 0.0)
    }
}

/** Defines a feeding niche for one resource category. */
data class FeedsOn(
    /** Resource category sought by the feeder. */
    val resource: FoodResource,
    /** Base preference before prey features and defenses are applied. */
    val preference: Double,
    /** Optional absolute upper size rank for eligible prey. */
    val maximumPreySizeRank: Int? = null,
    /** Optional smallest prey-minus-feeder size-rank difference. */
    val minimumRelativeSize: Int? = null,
    /** Optional largest prey-minus-feeder size-rank difference. */
    val maximumRelativeSize: Int? = null,
) : TraitEffect

/** Expands a relative-size feeding niche, as cooperative hunting does. */
data class RelativeSizeModifier(
    /** Number of additional prey size ranks that can be attacked. */
    val maximumRelativeSizeBonus: Int = 0,
) : TraitEffect

/** Reduces preference for a defended prey unless the feeder has a counter. */
data class DefenseEffect(
    /** Multiplier used when the feeder has no applicable countermeasure. */
    val defaultPreferenceMultiplier: Double,
    /** Preference multipliers selected when the feeder grants a matching counter tag. */
    val counterMultipliers: Map<EffectTag, Double> = emptyMap(),
    /** Optional feeder size advantage at which this defense stops mattering. */
    val ignoredWhenFeederSizeAdvantageAtLeast: Int? = null,
) : TraitEffect

/** Habitat dimensions that traits can require or use as refuges. */
enum class HabitatAxis {
    CANOPY_CLOSURE,       // Fraction of overhead light intercepted by large canopies.
    WETLAND_AVAILABILITY, // Fractional wetland availability derived from current precipitation.
}

/** Makes a sessile producer intercept light before it reaches shorter plants. */
data class CanopyEffect(
    /** Fraction of incident light intercepted when this canopy fills its size guild. */
    val maximumLightInterception: Double,
) : TraitEffect {
    init { require(maximumLightInterception in 0.0..1.0) }
}

/** Adds mortality when a habitat axis lies outside a trait's usable interval. */
data class HabitatPreferenceEffect(
    val axis: HabitatAxis,
    val minimum: Double = 0.0,
    val maximum: Double = 1.0,
    val mortalityAtExtreme: Double,
) : TraitEffect {
    init {
        require(minimum in 0.0..1.0 && maximum in minimum..1.0)
        require(mortalityAtExtreme >= 0.0)
    }
}

/** Reduces predation while the prey can use a sufficiently available habitat refuge. */
data class HabitatRefugeEffect(
    val axis: HabitatAxis,
    val refugeStartsAt: Double,
    val predatorPreferenceAtFullRefuge: Double,
) : TraitEffect {
    init {
        require(refugeStartsAt in 0.0..<1.0)
        require(predatorPreferenceAtFullRefuge in 0.0..1.0)
    }
}

/** Extra cold injury caused by exposed tissue or poorly insulated extremities. */
data class ColdExposureEffect(
    val mortalityPerDegreeBelowFreezing: Double,
) : TraitEffect {
    init { require(mortalityPerDegreeBelowFreezing >= 0.0) }
}

/** Extra overheating caused by insulation that cannot be shed in hot conditions. */
data class HeatRetentionEffect(
    val mortalityPerDegreeAboveThreshold: Double,
    val thresholdC: Double = 20.0,
) : TraitEffect {
    init { require(mortalityPerDegreeAboveThreshold >= 0.0) }
}

/** Raises the elevation above which hypoxia adds mortality. */
data class AltitudeToleranceEffect(
    val maximumOptimalAltitudeMeters: Double,
    val mortalityPerKilometerAbove: Double = 0.45,
) : TraitEffect {
    init {
        require(maximumOptimalAltitudeMeters >= 0.0)
        require(mortalityPerKilometerAbove >= 0.0)
    }
}

/** Visual background against which a fixed camouflage pattern is effective. */
enum class CamouflageHabitat { SNOW, DESERT, GRASSLAND, FOREST, ADAPTIVE }

/** Modifies hunting success and vulnerability according to current background match. */
data class CamouflageEffect(
    val habitat: CamouflageHabitat,
    val predatorMultiplierWhenMatched: Double = 1.15,
    val preyMultiplierWhenMatched: Double = 0.55,
    val predatorMultiplierWhenMismatched: Double = 0.72,
    val preyMultiplierWhenMismatched: Double = 1.40,
) : TraitEffect

/** Always-visible mate signaling that also makes hunting and concealment harder. */
data class ConspicuousDisplayEffect(
    val predatorHuntingMultiplier: Double,
    val preyVulnerabilityMultiplier: Double,
) : TraitEffect

/** A composable item that may be placed in a species blueprint. */
sealed interface SpeciesTrait {
    /** Declarative effects contributed when this trait appears in a blueprint. */
    val effects: List<TraitEffect>
}

/** Body-size trait, including the biomass represented by one individual. */
enum class SizeClass(
    /** Ordered size rank used by predator-prey compatibility rules. */
    val rank: Int,
    /** Biomass of one representative individual in simulation biomass units. */
    val individualBiomass: Double,
    /** Default shared carrying-capacity density for sessile producers of this size. */
    val defaultSessileCarryingCapacityDensity: Double,
    /** Temperature-niche shift predicted by Bergmann's law for motile organisms. */
    val bergmannTemperatureShiftC: Double,
) : SpeciesTrait {
    MINUSCULE(0, 0.0001, 1.00, 4.0),
    TINY(1, 0.01, 0.80, 2.5),
    SMALL(2, 1.0, 1.10, 1.0),
    MEDIUM(3, 20.0, 1.40, 0.0),
    LARGE(4, 200.0, 1.70, -1.5),
    GIANT(5, 2_000.0, 2.00, -3.0),
    COLOSSAL(6, 20_000.0, 2.40, -4.5);

    /** Size classes contribute their numbers directly rather than descriptor effects. */
    override val effects: List<TraitEffect> = emptyList()

    /** Allometric multiplier used by baseline feeding and mortality rates. */
    val feedingAllometry: Double = individualBiomass.pow(-0.08)
}
