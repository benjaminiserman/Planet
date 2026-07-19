package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.ClimateDatumSample
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Taxonomic order used as the production-level pool assigned to a BiotaDistribution. */
enum class TaxonomicOrder {
    POALES, FABALES, ASTERALES, ERICALES, ROSALES, SAPINDALES, ARECALES,
    FAGALES, MALVALES, PINALES, MALPIGHIALES, CARYOPHYLLALES,
    DINOPHYSIALES, SACOGLOSSA, EUPHAUSIACEA, LAGOMORPHA, RODENTIA,
    ORTHOPTERA, LEPIDOPTERA, COLEOPTERA, DIPTERA, ANURA, SQUAMATA,
    CARNIVORA, STRIGIFORMES, ACCIPITRIFORMES, PASSERIFORMES, COLUMBIFORMES,
    GALLIFORMES, PICIFORMES, ARTIODACTYLA, PRIMATES, PILOSA, PERISSODACTYLA,
    PROBOSCIDEA, CROCODILIA, SPHENISCIFORMES, SCORPIONES, DIPROTODONTIA,
    CETACEA, LAMNIFORMES, SCOMBRIFORMES, MYLIOBATIFORMES, LOPHIIFORMES,
    TESTUDINES, ORECTOLOBIFORMES, PROCELLARIIFORMES,
    LAMINARIALES, ALISMATALES, BACILLARIALES, CALANOIDA,
    SEMAEOSTOMEAE, CAMARODONTA, MYTILIDA, DECAPODA, OEGOPSIDA, OCTOPODA,
    CLUPEIFORMES, GADIFORMES, SALMONIFORMES, LABRIFORMES,
    SCLERACTINIA, HAPLOSCLERIDA,
}

/** Stable catalog membership used to construct blueprints and distribution pools. */
val speciesIdsByTaxonomicOrder: Map<TaxonomicOrder, Set<String>> = mapOf(
    TaxonomicOrder.POALES to setOf("grass", "bamboo", "reeds"),
    TaxonomicOrder.FABALES to setOf("clover", "mesquite", "acacia"),
    TaxonomicOrder.ASTERALES to setOf("wildflowers", "edelweiss"),
    TaxonomicOrder.ERICALES to setOf("shrub", "berry_bush"),
    TaxonomicOrder.ROSALES to setOf("apple_tree", "fig_tree"),
    TaxonomicOrder.SAPINDALES to setOf("mango_tree"),
    TaxonomicOrder.ARECALES to setOf("date_palm"),
    TaxonomicOrder.FAGALES to setOf("oak"),
    TaxonomicOrder.MALVALES to setOf("kapok_tree"),
    TaxonomicOrder.PINALES to setOf("spruce"),
    TaxonomicOrder.MALPIGHIALES to setOf("dwarf_willow"),
    TaxonomicOrder.CARYOPHYLLALES to setOf("cactus", "sundew"),
    TaxonomicOrder.DINOPHYSIALES to setOf("phytoplankton"),
    TaxonomicOrder.SACOGLOSSA to setOf("solar_slug"),
    TaxonomicOrder.EUPHAUSIACEA to setOf("krill"),
    TaxonomicOrder.LAGOMORPHA to setOf("rabbit", "hare"),
    TaxonomicOrder.RODENTIA to setOf("mouse", "vole", "beaver", "capybara", "jerboa"),
    TaxonomicOrder.ORTHOPTERA to setOf("grasshopper", "cricket"),
    TaxonomicOrder.LEPIDOPTERA to setOf("caterpillar"),
    TaxonomicOrder.COLEOPTERA to setOf("beetle"),
    TaxonomicOrder.DIPTERA to setOf("fruit_fly"),
    TaxonomicOrder.ANURA to setOf("frog"),
    TaxonomicOrder.SQUAMATA to setOf("snake", "anaconda", "monitor_lizard"),
    TaxonomicOrder.CARNIVORA to setOf(
        "weasel", "fox", "coyote", "lynx", "wolf", "bear", "fennec_fox",
        "jaguar", "lion", "hyena", "tiger", "giant_panda", "wolverine",
        "arctic_fox", "polar_bear", "harbor_seal", "walrus",
    ),
    TaxonomicOrder.STRIGIFORMES to setOf("owl"),
    TaxonomicOrder.ACCIPITRIFORMES to setOf("hawk", "eagle"),
    TaxonomicOrder.PASSERIFORMES to setOf("sparrow"),
    TaxonomicOrder.COLUMBIFORMES to setOf("pigeon"),
    TaxonomicOrder.GALLIFORMES to setOf("quail"),
    TaxonomicOrder.PICIFORMES to setOf("toucan"),
    TaxonomicOrder.ARTIODACTYLA to setOf(
        "deer", "boar", "dromedary", "bactrian_camel", "oryx", "giraffe",
        "wildebeest", "moose", "caribou", "ibex", "bison", "saiga",
        "musk_ox", "yak",
    ),
    TaxonomicOrder.PRIMATES to setOf("gorilla"),
    TaxonomicOrder.PILOSA to setOf("sloth"),
    TaxonomicOrder.PERISSODACTYLA to setOf("tapir", "zebra"),
    TaxonomicOrder.PROBOSCIDEA to setOf("elephant", "mammoth"),
    TaxonomicOrder.CROCODILIA to setOf("crocodile"),
    TaxonomicOrder.SPHENISCIFORMES to setOf("emperor_penguin"),
    TaxonomicOrder.SCORPIONES to setOf("scorpion"),
    TaxonomicOrder.DIPROTODONTIA to setOf("kangaroo"),
    TaxonomicOrder.CETACEA to setOf("orca", "blue_whale", "dolphin"),
    TaxonomicOrder.LAMNIFORMES to setOf("great_white_shark"),
    TaxonomicOrder.SCOMBRIFORMES to setOf("tuna", "mackerel"),
    TaxonomicOrder.MYLIOBATIFORMES to setOf("manta_ray"),
    TaxonomicOrder.LOPHIIFORMES to setOf("anglerfish"),
    TaxonomicOrder.TESTUDINES to setOf("sea_turtle"),
    TaxonomicOrder.ORECTOLOBIFORMES to setOf("whale_shark"),
    TaxonomicOrder.PROCELLARIIFORMES to setOf("albatross"),
    TaxonomicOrder.LAMINARIALES to setOf("kelp"),
    TaxonomicOrder.ALISMATALES to setOf("seagrass"),
    TaxonomicOrder.BACILLARIALES to setOf("diatoms"),
    TaxonomicOrder.CALANOIDA to setOf("zooplankton"),
    TaxonomicOrder.SEMAEOSTOMEAE to setOf("jellyfish"),
    TaxonomicOrder.CAMARODONTA to setOf("sea_urchin"),
    TaxonomicOrder.MYTILIDA to setOf("mussel"),
    TaxonomicOrder.DECAPODA to setOf("crab", "shrimp"),
    TaxonomicOrder.OEGOPSIDA to setOf("squid"),
    TaxonomicOrder.OCTOPODA to setOf("octopus"),
    TaxonomicOrder.CLUPEIFORMES to setOf("sardine", "anchovy"),
    TaxonomicOrder.GADIFORMES to setOf("cod"),
    TaxonomicOrder.SALMONIFORMES to setOf("salmon"),
    TaxonomicOrder.LABRIFORMES to setOf("parrotfish"),
    TaxonomicOrder.SCLERACTINIA to setOf("reef_coral"),
    TaxonomicOrder.HAPLOSCLERIDA to setOf("sea_sponge"),
)

/** Reverse lookup required while constructing a blueprint from its stable catalog id. */
val taxonomicOrderBySpeciesId: Map<String, TaxonomicOrder> =
    speciesIdsByTaxonomicOrder.entries.flatMap { (order, ids) -> ids.map { it to order } }.toMap()

/** Trait-only, production-facing description of a procedurally generated species. */
data class SpeciesBlueprint(
    /** Stable identifier used in biomass maps and food-web references. */
    val id: String,
    /** Taxonomic order controlling which BiotaDistribution pools may generate this species. */
    val taxonomicOrder: TaxonomicOrder,
    /** Complete set of size, niche, body, behavior, and defense traits. */
    val traits: Set<SpeciesTrait>,
) {
    init {
        require(id.isNotBlank()) { "Species id must not be blank" }
        require(traits.filterIsInstance<SizeClass>().size == 1) {
            id + " must have exactly one size-class trait"
        }
    }

    /** The blueprint's required and unique size-class trait. */
    val sizeClass: SizeClass
        get() = traits.filterIsInstance<SizeClass>().single()

    /** Flattened effect descriptors contributed by all traits. */
    val effects: List<TraitEffect>
        get() = traits.flatMap { it.effects }

    /** Capability and countermeasure tags granted by the flattened effects. */
    val effectTags: Set<EffectTag>
        get() = effects.filterIsInstance<GrantsTag>().map { it.tag }.toSet()

    /** Returns whether this blueprint explicitly contains a named trait. */
    fun has(trait: Trait): Boolean = trait in traits

    /** Returns whether its effects grant a capability or countermeasure tag. */
    fun hasTag(tag: EffectTag): Boolean = tag in effectTags
}

/** Convenience constructor that deduplicates a vararg collection of traits. */
fun speciesBlueprint(id: String, vararg traits: SpeciesTrait): SpeciesBlueprint =
    SpeciesBlueprint(id, taxonomicOrderBySpeciesId.getValue(id), traits.toSet())

/** Constructor for procedurally generated species whose id is not in the fixed catalog mapping. */
fun speciesBlueprint(
    id: String,
    taxonomicOrder: TaxonomicOrder,
    vararg traits: SpeciesTrait,
): SpeciesBlueprint = SpeciesBlueprint(id, taxonomicOrder, traits.toSet())

/** One independently contested edible resource exposed by a species. */
data class FoodSource(
    /** Species producing or embodying the food. */
    val preyId: String,
    /** Exact tissue, product, or prey-body resource being consumed. */
    val resource: FoodResource,
)

/** Compiled feeding relationship between one feeder and one exact food source. */
data class DietEntry(
    /** Relative targeting weight before prey abundance is considered. */
    val preference: Double,
    /** Prey density at which this food becomes readily exploitable. */
    val halfSaturation: Double,
    /** Fraction of ingestion removed from the food species' standing biomass. */
    val preyLossFraction: Double = 1.0,
    /** Renewable production cap per producer biomass per year, or null for standing biomass. */
    val renewableProductionRate: Double? = null,
) {
    init {
        require(preference > 0.0)
        require(halfSaturation > 0.0)
        require(preyLossFraction in 0.0..1.0)
        require(renewableProductionRate == null || renewableProductionRate > 0.0)
    }
}

/** Numerical parameters for biomass gained through autotrophy. */
data class AutotrophyParameters(
    /** Intrinsic logistic growth rate per year. */
    val growthRate: Double,
    /** Default carrying-capacity density before simulation overrides and seasons. */
    val baseCarryingCapacity: Double,
    /** Fractional amplitude of seasonal carrying-capacity change. */
    val seasonalAmplitude: Double,
    /** Position of the seasonal peak within a year. */
    val seasonalPhase: Double,
    /** Insolation in W/m² at which photosynthesis reaches half its light-limited maximum. */
    val photosynthesisHalfSaturationWm2: Double,
)

/** Numerical parameters for feeding, metabolism, mortality, and crowding. */
data class FeedingParameters(
    /** Maximum food biomass ingested per feeder biomass per year. */
    val maxConsumptionRate: Double,
    /** Fraction of ingested biomass converted into feeder biomass. */
    val assimilationEfficiency: Double,
    /** Baseline fractional biomass mortality per year. */
    val mortalityRate: Double,
    /** Assimilated food biomass required per feeder biomass per year. */
    val metabolicDemandRate: Double,
    /** Additional fractional mortality per year when metabolic demand is completely unmet. */
    val starvationMortalityRate: Double,
    /** Strength of quadratic self-limitation per unit land area. */
    val densityDependence: Double,
    /** Strength of feeder interference in the functional response. */
    val interference: Double,
    /** Shape exponent of the low-prey feeding response. */
    val responseExponent: Double,
)

/** Trait-derived climate interval in which a species has no climate mortality. */
data class ClimateNiche(
    /** Lowest temperature with no climate stress, in degrees Celsius. */
    val minOptimalTemperatureC: Double,
    /** Highest temperature with no climate stress, in degrees Celsius. */
    val maxOptimalTemperatureC: Double,
    /** Lowest monthly precipitation with no moisture stress, in millimetres. */
    val minOptimalMoistureMm: Double,
    /** Optional upper no-stress precipitation bound; null means no upper bound. */
    val maxOptimalMoistureMm: Double?,
    /** Lowest no-stress mean insolation in W/m². */
    val minOptimalInsolationWm2: Double,
    /** Highest no-stress mean insolation in W/m². */
    val maxOptimalInsolationWm2: Double,
    /** Multiplier controlling additional mortality outside the optimum interval. */
    val stressSensitivity: Double,
) {
    init {
        require(minOptimalTemperatureC < maxOptimalTemperatureC)
        require(minOptimalMoistureMm >= 0.0)
        require(maxOptimalMoistureMm == null || maxOptimalMoistureMm > minOptimalMoistureMm)
        require(minOptimalInsolationWm2 >= 0.0)
        require(maxOptimalInsolationWm2 > minOptimalInsolationWm2)
        require(stressSensitivity >= 0.0)
    }
}

/** Fully compiled numerical species consumed by the simulation solver. */
data class SpeciesDefinition(
    /** Trait-only source from which every other field was derived. */
    val blueprint: SpeciesBlueprint,
    /** Derived autotrophy capability, or null when the species lacks it. */
    val autotrophy: AutotrophyParameters?,
    /** Derived feeding capability, or null when the species lacks it. */
    val feeding: FeedingParameters?,
    /** Trait- and size-derived temperature and moisture tolerances. */
    val climateNiche: ClimateNiche,
    /** Baseline plus additive trait upkeep paid as fractional biomass loss per year. */
    val maintenanceRate: Double,
    /** Derived food-web entries keyed by prey species and exact resource. */
    val diet: Map<FoodSource, DietEntry>,
) {
    /** Stable species id delegated from the blueprint. */
    val id: String get() = blueprint.id

    /** Taxonomic order delegated from the blueprint for distribution-level filtering. */
    val taxonomicOrder: TaxonomicOrder get() = blueprint.taxonomicOrder

    /** Original immutable trait set delegated from the blueprint. */
    val traits: Set<SpeciesTrait> get() = blueprint.traits

    /** Flattened declarative effects delegated from the blueprint. */
    val effects: List<TraitEffect> get() = blueprint.effects

    /** Unique body-size class delegated from the blueprint. */
    val sizeClass: SizeClass get() = blueprint.sizeClass

    /** Biomass represented by one individual of this species. */
    val individualBiomass: Double get() = sizeClass.individualBiomass

    init {
        require(autotrophy != null || feeding != null) {
            id + " needs at least one biomass-acquisition capability"
        }
        require(feeding == null || diet.isNotEmpty()) {
            "Feeding species " + id + " needs at least one derived food"
        }
        require(feeding != null || diet.isEmpty()) {
            "Non-feeding species " + id + " should not have a diet"
        }
    }
}

/** Returns one resource-specific diet entry, if this species can consume it. */
fun SpeciesDefinition.dietEntry(
    preyId: String,
    resource: FoodResource,
): DietEntry? = diet[FoodSource(preyId, resource)]

/** Strongest preference across all resources supplied by one prey species. */
fun SpeciesDefinition.maximumPreferenceFor(preyId: String): Double = diet
    .filterKeys { it.preyId == preyId }
    .values
    .maxOfOrNull { it.preference } ?: 0.0

/** One possible feeder-resource pathway before constrained allocation. */
data class FeedingOpportunity(
    /** Species attempting to consume this food source. */
    val feeder: String,
    /** Exact resource pathway being considered. */
    val source: FoodSource,
    /** Relative abundance-and-preference weight used during reallocation. */
    val signal: Double,
    /** Maximum ingestion rate allowed by this pathway's own Type III response. */
    val pathwayCapacity: Double,
    /** Fraction of accepted ingestion removed from standing prey biomass. */
    val preyLossFraction: Double,
    /** Fraction of accepted ingestion assimilated by the feeder. */
    val assimilationEfficiency: Double,
    /** Shared renewable production rate, or null for standing-biomass foods. */
    val renewableProductionRate: Double?,
)

/** One instantaneous transfer of biomass caused by feeding. */
data class FeedingFlux(
    /** Species receiving assimilated biomass. */
    val feeder: String,
    /** Exact species resource supplying the ingestion. */
    val source: FoodSource,
    /** Food biomass processed by the feeder. */
    val ingested: Double,
    /** Standing biomass actually removed from the prey species. */
    val preyLoss: Double,
    /** Ingested biomass converted into new feeder biomass. */
    val assimilated: Double,
    /** Renewable production limit used to cap all consumers sharing this source. */
    val renewableProductionRate: Double? = null,
) {
    /** Species whose standing biomass pays any prey loss. */
    val prey: String get() = source.preyId
}

/** Returns whether this compiled species participates in a sessile producer size guild. */
fun SpeciesDefinition.isSessileProducer(): Boolean =
    autotrophy != null && blueprint.hasTag(EffectTag.SESSILE)

/** Shared seasonal-capacity pattern compiled for one sessile size guild. */
data class SeasonalCapacityPattern(
    /** Fractional seasonal oscillation of the shared capacity. */
    val amplitude: Double,
    /** Average position of the guild's seasonal peak within a year. */
    val phase: Double,
)

/** Cosine similarity of two consumers' available diet-preference vectors. */
fun consumerNicheOverlap(
    first: SpeciesDefinition,
    second: SpeciesDefinition,
    availableFoodSources: Set<FoodSource>,
): Double {
    if (first.id == second.id) return 1.0
    var dotProduct = 0.0
    var firstSquared = 0.0
    var secondSquared = 0.0
    for (source in availableFoodSources) {
        val firstWeight = first.diet[source]?.preference ?: 0.0
        val secondWeight = second.diet[source]?.preference ?: 0.0
        dotProduct += firstWeight * secondWeight
        firstSquared += firstWeight * firstWeight
        secondSquared += secondWeight * secondWeight
    }
    val denominator = sqrt(firstSquared * secondSquared)
    return if (denominator > 0.0) {
        (dotProduct / denominator).coerceIn(0.0, 1.0)
    } else 0.0
}

/** Precomputes static diet overlap used to couple consumers' feeding interference. */
fun deriveConsumerNicheOverlaps(
    species: List<SpeciesDefinition>,
): Map<String, Map<String, Double>> {
    val availableSpeciesIds = species.map { it.id }.toSet()
    val consumers = species.filter { it.feeding != null }
    val availableFoodSources = consumers
        .flatMap { it.diet.keys }
        .filter { it.preyId in availableSpeciesIds }
        .toSet()
    return consumers.associate { first ->
        first.id to consumers.associate { second ->
            second.id to consumerNicheOverlap(first, second, availableFoodSources)
        }
    }
}

/** Immutable configuration and compiled species for one isolated tile simulation. */
data class EcosystemModel(
    /** Species participating in this tile's ecosystem. */
    val species: List<SpeciesDefinition>,
    /** Whether carrying capacities vary seasonally. */
    val seasonsEnabled: Boolean = true,
    /** Tile land area; biomass totals scale with this value. */
    val landArea: Double = 1.0,
    /** Repeating monthly climate forcing; defaults to the notebook's oceanic biome. */
    val climate: ClimateDatum = oceanicTemperateClimate,
    /** Tile elevation above sea level; local temperature uses a 6.5 C/km lapse rate. */
    val altitudeMeters: Double = 500.0,
    /** Optional shared carrying-capacity density overrides keyed by sessile size class. */
    val sessileCarryingCapacityDensities: Map<SizeClass, Double> = emptyMap(),
) {
    /** Sessile autotrophs grouped into the size guilds that share capacity. */
    val sessileProducerGroups: Map<SizeClass, List<SpeciesDefinition>> =
        species.filter { it.isSessileProducer() }.groupBy { it.sizeClass }

    /** Fast lookup used by dynamic predation and habitat effects. */
    val speciesById: Map<String, SpeciesDefinition> = species.associateBy { it.id }

    /** Shared seasonal pattern for each present sessile size guild. */
    val sessileSeasonality: Map<SizeClass, SeasonalCapacityPattern> =
        sessileProducerGroups.mapValues { (_, definitions) ->
            SeasonalCapacityPattern(
                amplitude = definitions.map { it.autotrophy!!.seasonalAmplitude }.average(),
                phase = definitions.map { it.autotrophy!!.seasonalPhase }.average(),
            )
        }

    /** Static pairwise diet overlap coefficients used only in feeding interference. */
    val consumerNicheOverlaps: Map<String, Map<String, Double>> =
        deriveConsumerNicheOverlaps(species)

    init {
        require(landArea > 0.0) { "Land area must be positive" }
        require(altitudeMeters in -500.0..9_000.0) { "Altitude must be between -500 m and 9000 m" }
        require(sessileCarryingCapacityDensities.values.all { it > 0.0 }) {
            "Sessile carrying-capacity densities must be positive"
        }
    }

    /** Returns the simulation override for a size guild, or its size-derived default. */
    fun baseSessileCarryingCapacityDensity(sizeClass: SizeClass): Double =
        sessileCarryingCapacityDensities[sizeClass]
            ?: sizeClass.defaultSessileCarryingCapacityDensity

    /** Returns the shared, seasonally adjusted capacity density of a sessile size guild. */
    fun sessileCarryingCapacityDensity(sizeClass: SizeClass, year: Double): Double {
        val pattern = sessileSeasonality[sizeClass] ?: SeasonalCapacityPattern(
            amplitude = 0.18,
            phase = 0.14 + 0.025 * sizeClass.rank,
        )
        val seasonalOffset = if (seasonsEnabled) {
            pattern.amplitude * sin(2.0 * PI * (year - pattern.phase))
        } else 0.0
        return baseSessileCarryingCapacityDensity(sizeClass) * (1.0 + seasonalOffset)
    }
}

/** Samples climate at tile elevation, treating ClimateDatum temperature as sea-level forcing. */
fun EcosystemModel.localClimateAt(year: Double): ClimateDatumSample {
    val sample = climate.sampleAt(year)
    return sample.copy(averageTemperature = sample.averageTemperature - 0.0065 * altitudeMeters)
}

/** Converts total biomass into an approximate (possibly fractional) individual count. */
fun approximateIndividuals(definition: SpeciesDefinition, biomass: Double): Double =
    biomass / definition.individualBiomass

/** Fraction of overhead light intercepted by canopies taller than an optional target rank. */
fun canopyClosure(
    year: Double,
    state: Biomass,
    model: EcosystemModel,
    tallerThanSizeRank: Int? = null,
): Double = model.species.sumOf { definition ->
    if (tallerThanSizeRank != null && definition.sizeClass.rank <= tallerThanSizeRank) {
        return@sumOf 0.0
    }
    val interception = definition.effects.filterIsInstance<CanopyEffect>()
        .sumOf { it.maximumLightInterception }
        .coerceAtMost(1.0)
    if (interception <= 0.0) return@sumOf 0.0
    val guildCapacity = model.sessileCarryingCapacityDensity(
        definition.sizeClass,
        year,
    ) * model.landArea
    interception * state.getValue(definition.id).coerceAtLeast(0.0) / guildCapacity
}.coerceIn(0.0, 0.98)

/** Remaining incident light at the top of a producer's own height layer. */
fun availableLightFraction(
    definition: SpeciesDefinition,
    year: Double,
    state: Biomass,
    model: EcosystemModel,
): Double = 1.0 - canopyClosure(year, state, model, definition.sizeClass.rank)

/** Current normalized value of one habitat dimension. */
fun habitatAxisValue(
    axis: HabitatAxis,
    year: Double,
    state: Biomass,
    model: EcosystemModel,
): Double = when (axis) {
    HabitatAxis.CANOPY_CLOSURE -> canopyClosure(year, state, model)
    HabitatAxis.WETLAND_AVAILABILITY ->
        (model.localClimateAt(year).precipitation / 100.0).coerceIn(0.0, 1.0)
}

/** Computes each habitat dimension once for reuse throughout one derivative evaluation. */
fun habitatAxisValues(
    year: Double,
    state: Biomass,
    model: EcosystemModel,
): Map<HabitatAxis, Double> = HabitatAxis.entries.associateWith { axis ->
    habitatAxisValue(axis, year, state, model)
}

/** Additional mortality from trait-declared habitat mismatch. */
fun habitatStressMortalityRate(
    definition: SpeciesDefinition,
    habitatValues: Map<HabitatAxis, Double>,
): Double = definition.effects.filterIsInstance<HabitatPreferenceEffect>().sumOf { effect ->
    val value = habitatValues.getValue(effect.axis)
    val mismatch = when {
        value < effect.minimum && effect.minimum > 0.0 ->
            (effect.minimum - value) / effect.minimum
        value > effect.maximum && effect.maximum < 1.0 ->
            (value - effect.maximum) / (1.0 - effect.maximum)
        else -> 0.0
    }
    effect.mortalityAtExtreme * mismatch.coerceIn(0.0, 1.0)
}

/** Dynamic multiplier on predation when prey can occupy a trait-declared refuge. */
fun habitatRefugePreferenceMultiplier(
    prey: SpeciesDefinition,
    habitatValues: Map<HabitatAxis, Double>,
): Double = prey.effects.filterIsInstance<HabitatRefugeEffect>().fold(1.0) { product, effect ->
    val availability = habitatValues.getValue(effect.axis)
    val refugeStrength = ((availability - effect.refugeStartsAt) /
        (1.0 - effect.refugeStartsAt)).coerceIn(0.0, 1.0)
    val multiplier = 1.0 - refugeStrength *
        (1.0 - effect.predatorPreferenceAtFullRefuge)
    product * multiplier
}

/** Fractional match between a camouflage pattern and this tile's current background. */
fun camouflageMatch(
    habitat: CamouflageHabitat,
    year: Double,
    state: Biomass,
    model: EcosystemModel,
    habitatValues: Map<HabitatAxis, Double>,
): Double {
    if (habitat == CamouflageHabitat.ADAPTIVE) return 1.0
    val climate = model.localClimateAt(year)
    val canopy = habitatValues.getValue(HabitatAxis.CANOPY_CLOSURE)
    val lowPlantCapacity = model.sessileProducerGroups[SizeClass.MINUSCULE]
        ?.sumOf { state.getValue(it.id).coerceAtLeast(0.0) }
        ?.div(model.sessileCarryingCapacityDensity(SizeClass.MINUSCULE, year) * model.landArea)
        ?.coerceIn(0.0, 1.0) ?: 0.0
    return when (habitat) {
        CamouflageHabitat.SNOW -> if (climate.averageTemperature < 0.0) {
            ((-climate.averageTemperature / 10.0).coerceIn(0.25, 1.0) *
                (climate.precipitation / 20.0).coerceIn(0.0, 1.0))
        } else 0.0
        CamouflageHabitat.DESERT ->
            (1.0 - climate.precipitation / 45.0).coerceIn(0.0, 1.0) * (1.0 - canopy)
        CamouflageHabitat.GRASSLAND -> lowPlantCapacity * (1.0 - canopy)
        CamouflageHabitat.FOREST -> canopy
        CamouflageHabitat.ADAPTIVE -> 1.0
    }
}

/** Hunting or vulnerability multiplier from camouflage and conspicuous displays. */
fun visualInteractionMultiplier(
    definition: SpeciesDefinition,
    asPredator: Boolean,
    year: Double,
    state: Biomass,
    model: EcosystemModel,
    habitatValues: Map<HabitatAxis, Double>,
): Double {
    val camouflage = definition.effects.filterIsInstance<CamouflageEffect>().fold(1.0) { product, effect ->
        val match = camouflageMatch(effect.habitat, year, state, model, habitatValues)
        val matched = if (asPredator) effect.predatorMultiplierWhenMatched else effect.preyMultiplierWhenMatched
        val mismatched = if (asPredator) effect.predatorMultiplierWhenMismatched else effect.preyMultiplierWhenMismatched
        product * (mismatched + match * (matched - mismatched))
    }
    return definition.effects.filterIsInstance<ConspicuousDisplayEffect>().fold(camouflage) { product, effect ->
        product * if (asPredator) effect.predatorHuntingMultiplier else effect.preyVulnerabilityMultiplier
    }
}
