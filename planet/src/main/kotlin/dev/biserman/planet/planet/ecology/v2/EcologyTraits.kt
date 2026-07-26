package dev.biserman.planet.planet.ecology.v2

enum class SizeClass(
    val typicalMassKg: Double,
    val maintenancePerKg: Double,
    val seasonalReproduction: Double,
    val densityScale: Double,
) {
    MINUSCULE(0.000_001, 0.34, 1.20, 8.0),
    TINY(0.001, 0.25, 0.90, 5.0),
    SMALL(0.1, 0.18, 0.60, 2.5),
    MEDIUM(10.0, 0.12, 0.34, 1.0),
    LARGE(500.0, 0.08, 0.18, 0.38),
    HUGE(10_000.0, 0.055, 0.10, 0.12),
}

sealed interface SpeciesTrait {
    val displayName: String
    val description: String
    val effects: List<TraitEffect>
    val relationships: List<RelationshipEffect>
        get() = emptyList()
    val isFoundation: Boolean
        get() = false
    val invariantOnly: Boolean
        get() = false
}

data class TargetedRelationshipTrait(
    override val displayName: String,
    override val description: String,
    override val relationships: List<RelationshipEffect>,
    val maintenanceCost: Double,
) : SpeciesTrait {
    init {
        require(displayName.isNotBlank())
        require(description.isNotBlank())
        require(relationships.isNotEmpty())
        require(maintenanceCost > 0.0)
    }

    override val effects: List<TraitEffect> =
        listOf(TraitEffect.MaintenanceCost(maintenanceCost))
}

/**
 * A deliberately small starter library. Adding content means adding a readable
 * entry here (or another SpeciesTrait implementation), not changing the turn loop.
 */
enum class CommonTrait(
    override val displayName: String,
    override val description: String,
    override val effects: List<TraitEffect>,
    override val isFoundation: Boolean = false,
    override val invariantOnly: Boolean = false,
) : SpeciesTrait {
    TEMPERATE_BIOCHEMISTRY(
        "temperate biochemistry",
        "Cellular chemistry that functions best at moderate temperatures.",
        listOf(TraitEffect.MaintenanceCost(0.0)),
        true,
    ),
    FRIGID_BIOCHEMISTRY(
        "frigid biochemistry",
        "Cellular chemistry built around reactions and structures that remain viable in persistently frigid climates.",
        listOf(
            TraitEffect.TemperatureShift(-25.0),
            TraitEffect.ReproductionMultiplier(0.88),
            TraitEffect.MaintenanceCost(0.04),
        ),
        true,
    ),
    HOT_BIOCHEMISTRY(
        "hot biochemistry",
        "Cellular chemistry whose molecules and membranes remain stable in persistently hot climates.",
        listOf(
            TraitEffect.TemperatureShift(28.0),
            TraitEffect.WaterRequirement(0.08),
            TraitEffect.MaintenanceCost(0.05),
        ),
        true,
    ),
    INVARIANT_RESISTANCE(
        "invariant guild resilience",
        "Broad tolerance representing many locally adapted, interchangeable species grouped into one aggregate population.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 32.0, hotterC = 30.0),
            TraitEffect.TemperatureOptimalTolerance(colderC = 30.0, hotterC = 20.0),
            TraitEffect.WaterRequirement(-0.25),
            TraitEffect.ReserveCapacity(0.15),
            TraitEffect.Dormancy(DormancyKind.PROPAGULE, 0.995),
            TraitEffect.ReproductionMultiplier(0.88),
            TraitEffect.MaintenanceCost(0.08),
        ),
        invariantOnly = true,
    ),
    ECTOTHERMY(
        "ectothermy",
        "Body activity and temperature depend primarily on heat exchanged with the surrounding environment.",
        listOf(
            TraitEffect.ThermalRegulation(ThermalStrategy.ECTOTHERMY),
            TraitEffect.MaintenanceCost(-0.22),
            TraitEffect.TemperatureTolerance(colderC = -2.0, hotterC = -2.0),
        ),
        true,
    ),
    ENDOTHERMY(
        "endothermy",
        "Metabolism produces enough heat to regulate the body substantially independently of ambient temperature.",
        listOf(
            TraitEffect.ThermalRegulation(ThermalStrategy.ENDOTHERMY),
            TraitEffect.TemperatureTolerance(colderC = 8.0, hotterC = 2.0),
            TraitEffect.MaintenanceCost(0.25),
        ),
        true,
    ),
    HETEROTHERMY(
        "heterothermy",
        "Body temperature is actively regulated at some times but allowed to vary during torpor, rest, or unfavorable seasons.",
        listOf(
            TraitEffect.ThermalRegulation(ThermalStrategy.HETEROTHERMY),
            TraitEffect.TemperatureTolerance(colderC = 5.0, hotterC = 1.0),
            TraitEffect.ReserveCapacity(0.20),
            TraitEffect.MaintenanceCost(0.08),
        ),
        true,
    ),
    PHOTOSYNTHETIC_SURFACE(
        "photosynthetic surface",
        "A broad light-harvesting body surface containing photosynthetic pigments.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.PHOTOSYNTHESIS, 1.0),
            TraitEffect.HabitatSupport(Habitat.LAND_SURFACE, 0.35),
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.35),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    AIRBORNE_PHOTOSYNTHETIC_SURFACE(
        "airborne photosynthetic surface",
        "A minute drifting body whose exposed pigments harvest light while suspended in the atmosphere.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.PHOTOSYNTHESIS, 1.0),
            TraitEffect.HabitatSupport(Habitat.AERIAL, 0.78),
            TraitEffect.WaterRequirement(-0.15),
            TraitEffect.MaintenanceCost(0.08),
        ),
        invariantOnly = true,
    ),
    ROOTED_BODY(
        "rooted body",
        "A largely stationary body anchored into its substrate for support and resource uptake.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.LAND_SURFACE, 0.65),
            TraitEffect.StrategySupport(EcoStrategy.ABSORPTION, 0.25),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    TERRESTRIAL_LOCOMOTION(
        "terrestrial locomotion",
        "Load-bearing limbs, muscular pads, or equivalent structures that support deliberate movement across solid ground.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.LAND_SURFACE, 0.65),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    AQUATIC_FLIPPERS(
        "aquatic flippers",
        "Broad propulsive limbs or fins that support controlled swimming in open water.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.72),
            TraitEffect.HabitatSupport(Habitat.DARK_WATER, 0.58),
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.52),
            TraitEffect.HabitatSupport(Habitat.FRESHWATER, 0.48),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    AMPHIBIOUS_LIMBS(
        "amphibious limbs",
        "Load-bearing limbs and swimming surfaces that permit regular movement between land and shallow water.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.LAND_SURFACE, 0.44),
            TraitEffect.HabitatSupport(Habitat.FRESHWATER, 0.58),
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.54),
            TraitEffect.MaintenanceCost(0.11),
        ),
    ),
    CLIMBING_LIMBS(
        "climbing limbs",
        "Grasping limbs, claws, pads, or a prehensile body that supports deliberate movement through a canopy.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.62),
            TraitEffect.CaptureAbility(-0.03),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    CANOPY_GROWTH(
        "canopy growth",
        "A tall or climbing growth form that places much of the organism within an elevated living canopy.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.80),
            TraitEffect.CanopyLightEfficiency(0.22),
            TraitEffect.WaterRequirement(0.08),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    SHADE_FRONDS(
        "broad shade fronds",
        "Wide light-catching surfaces specialized for dim conditions beneath other organisms.",
        listOf(
            TraitEffect.CanopyLightEfficiency(0.38),
            TraitEffect.InsolationOptimum(-0.22),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    BUOYANCY_BLADDER(
        "buoyancy bladder",
        "A gas- or fluid-regulating chamber that controls position in the water column without continuous swimming.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.70),
            TraitEffect.HabitatSupport(Habitat.DARK_WATER, 0.35),
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.55),
            TraitEffect.Defense(-0.05),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    FRESHWATER_OSMOREGULATION(
        "freshwater osmoregulation",
        "Membranes and excretory structures that maintain internal chemistry in dilute freshwater.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.FRESHWATER, 0.80),
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    COASTAL_CLINGING_FEET(
        "coastal clinging feet",
        "Gripping limbs or attachment pads that resist waves and currents in shallow coastal habitats.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.78),
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.55),
            TraitEffect.CaptureAbility(-0.05),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    SUBSTRATE_HOLDFAST(
        "aquatic holdfast",
        "A tough anchoring structure that secures a sessile body to rock or reef under waves and currents.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.68),
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.56),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    POWERED_FLIGHT(
        "powered flight",
        "Actively driven wings or analogous structures capable of sustained movement through the air.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.AERIAL, 0.85),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.20),
            TraitEffect.HabitatSupport(Habitat.LAND_SURFACE, 0.12),
            TraitEffect.CaptureAbility(0.12),
            TraitEffect.MaintenanceCost(0.24),
        ),
    ),
    SUBTERRANEAN_BURROWING(
        "subterranean burrowing",
        "Anatomy and behavior for excavating, navigating, and sheltering within soil or soft substrate.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 3.0, hotterC = 3.0),
            TraitEffect.WaterRequirement(-0.06),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    DRY_BURROW_NEST(
        "dry burrow nest",
        "A nest chamber whose eggs, young, stored food, or respiratory surfaces require a well-drained burrow.",
        listOf(
            TraitEffect.MaximumWaterTolerance(
                optimalMaximumChange = -0.32,
                absoluteMaximumChange = -0.12,
            ),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    BALEEN(
        "baleen",
        "Dense flexible plates that strain suspended organisms from water passing through the mouth.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.FILTER_FEEDING, 0.88),
            TraitEffect.CaptureAbility(0.06),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    GILL_PADS(
        "gill pads",
        "Broad ciliated or mucus-coated respiratory surfaces that also trap minuscule food from flowing water.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.FILTER_FEEDING, 0.82),
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.30),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    SIEVE_TEETH(
        "sieve-like teeth",
        "Interlocking teeth that strain minuscule suspended prey while water escapes between them.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.FILTER_FEEDING, 0.80),
            TraitEffect.CaptureAbility(-0.06),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    GRAZING_MOUTHPARTS(
        "grazing mouthparts",
        "Scraping, cropping, grinding, or rasping structures for repeatedly harvesting attached or rooted food.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.82),
            TraitEffect.CaptureAbility(-0.06),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    BROWSING_MOUTHPARTS(
        "browsing mouthparts",
        "Lips, teeth, beaks, or cutting jaws specialized for selectively cropping leaves and twigs above ground level.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.74),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.24),
            TraitEffect.CaptureAbility(-0.04),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    AMBUSH_MUSCULATURE(
        "burst ambush musculature",
        "Muscles specialized for short, explosive attacks launched from concealment or stillness.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.58),
            TraitEffect.CaptureAbility(0.20),
            TraitEffect.MaintenanceCost(0.13),
        ),
    ),
    PURSUIT_LIMBS(
        "endurance pursuit limbs",
        "Efficient propulsive limbs or fins adapted to sustained chases rather than brief bursts.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.PURSUIT_PREDATION, 0.70),
            TraitEffect.CaptureAbility(0.18),
            TraitEffect.WaterRequirement(0.06),
            TraitEffect.MaintenanceCost(0.19),
        ),
    ),
    CAMOUFLAGE_PATTERN(
        "camouflage pattern",
        "Body colors and markings that break up the organism's outline against common backgrounds.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.36),
            TraitEffect.Camouflage(Habitat.LAND_SURFACE, 0.20),
            TraitEffect.Camouflage(Habitat.CANOPY, 0.18),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    SCAVENGING_SENSES(
        "long-range carrion senses",
        "Sensory organs capable of locating dead organisms across a broad area.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.SCAVENGING, 0.82),
            TraitEffect.MaintenanceCost(0.09),
        ),
    ),
    EXPANDABLE_CROP(
        "expandable food crop",
        "A distensible storage chamber holds a large meal after brief access to an unpredictable carcass.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.SCAVENGING, 0.08),
            TraitEffect.ReserveCapacity(0.42),
            TraitEffect.ReproductionMultiplier(0.97),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    DECOMPOSING_ENZYMES(
        "external decomposing enzymes",
        "Secreted enzymes break down dead sessile tissue outside the body so its nutrients can be absorbed.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.DECOMPOSITION, 0.86),
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    DETRITUS_DIGESTIVE_TRACT(
        "detritus-digesting gut",
        "A long digestive tract and microbial community extract energy from fragments of dead sessile organisms.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.DECOMPOSITION, 0.78),
            TraitEffect.CaptureAbility(-0.04),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    DUNG_FEEDING_MOUTHPARTS(
        "dung-feeding mouthparts",
        "Mouthparts and chemical senses are specialized for locating and consuming nutrient-rich animal waste.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.COPROPHAGY, 0.84),
            TraitEffect.CaptureAbility(-0.05),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    WASTE_ABSORBING_ROOTS(
        "waste-absorbing roots",
        "Root membranes and associated microbes rapidly capture nutrients released from nearby animal waste.",
        listOf(
            TraitEffect.WasteFertilization(0.48),
            TraitEffect.ReproductionMultiplier(0.97),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    MARINE_SNOW_PALPS(
        "marine-snow collecting palps",
        "Fine appendages that gather sinking organic particles from dark or deep water.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.DEPOSIT_FEEDING, 0.84),
            TraitEffect.HabitatSupport(Habitat.DARK_WATER, 0.30),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    PARASITIC_PROBOSCIS(
        "parasitic proboscis",
        "A piercing or anchoring feeding organ that extracts resources from a living host.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.PARASITISM, 0.82),
            TraitEffect.ReproductionMultiplier(1.10),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    ABSORPTIVE_FILAMENTS(
        "absorptive filaments",
        "A branching external network that digests or absorbs dissolved nutrients across a large surface.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.ABSORPTION, 0.82),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    DENSE_FUR(
        "dense fur",
        "A thick layer of hairlike insulation that traps still air close to the body.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 9.0, hotterC = -3.0),
            TraitEffect.WaterRequirement(-0.04),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    INSULATING_PLUMAGE(
        "insulating plumage",
        "Dense overlapping feathers trap air around the body while remaining lighter than an equally thick fur coat.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 8.0, hotterC = -2.0),
            TraitEffect.WaterRequirement(-0.03),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    THIN_FUR(
        "thin fur",
        "A sparse protective coat that provides some covering while readily releasing body heat.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = -3.0, hotterC = 7.0),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    BARE_HEAT_DISSIPATING_SKIN(
        "bare heat-dissipating skin",
        "Exposed, well-supplied skin sheds heat efficiently but sacrifices insulation and physical protection.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = -2.0, hotterC = 5.0),
            TraitEffect.Defense(-0.03),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    SWEAT_GLANDS(
        "sweat glands",
        "Skin glands that cool the body by evaporating secreted water.",
        listOf(
            TraitEffect.TemperatureTolerance(hotterC = 9.0),
            TraitEffect.WaterRequirement(0.08),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    MASSIVE_EARS(
        "massive heat-radiating ears",
        "Large thin appendages with rich circulation that exchange heat rapidly with the air.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = -3.0, hotterC = 6.0),
            TraitEffect.Defense(-0.04),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    SEASONAL_WINTER_COAT(
        "seasonal winter coat",
        "Insulation grown in response to the low-insolation portion of the year and shed as light returns.",
        listOf(
            TraitEffect.SeasonalColdTolerance(maximumBonusC = 10.0, triggerInsolation = 0.58),
            TraitEffect.TemperatureTolerance(hotterC = -1.5),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    WATER_STORAGE_TISSUE(
        "water-storage tissue",
        "Specialized tissues that retain a usable water reserve through dry periods.",
        listOf(
            TraitEffect.WaterRequirement(-0.22),
            TraitEffect.CaptureAbility(-0.04),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    PREY_DERIVED_WATER(
        "prey-derived water",
        "Concentrated kidneys and digestive physiology recover most required water from prey rather than free-standing sources.",
        listOf(
            TraitEffect.WaterRequirement(-0.25),
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.08),
            TraitEffect.StrategySupport(EcoStrategy.PURSUIT_PREDATION, 0.08),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    FOOD_DERIVED_WATER(
        "food-derived water",
        "Efficient kidneys and digestion obtain nearly all required water from moist food or metabolically produced water.",
        listOf(
            TraitEffect.WaterRequirement(-0.25),
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    DEEP_ROOT_SYSTEM(
        "deep root system",
        "A long or extensively branching anchoring network that reaches water retained below the surface.",
        listOf(
            TraitEffect.WaterRequirement(-0.18),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    SUCCULENT_STEM(
        "succulent stem",
        "A thick photosynthetic or supporting body that stores water through long dry intervals.",
        listOf(
            TraitEffect.WaterRequirement(-0.28),
            TraitEffect.ReproductionMultiplier(0.88),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    WAXY_CUTICLE(
        "waxy cuticle",
        "A reflective, water-resistant outer surface limits evaporation and shields living tissue from intense heat.",
        listOf(
            TraitEffect.TemperatureTolerance(hotterC = 10.0),
            TraitEffect.WaterRequirement(-0.08),
            TraitEffect.CanopyLightEfficiency(-0.04),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    DROUGHT_DECIDUOUS_LEAVES(
        "drought-deciduous leaves",
        "Photosynthetic surfaces are shed during dry seasons and regrown when water becomes available.",
        listOf(
            TraitEffect.WaterRequirement(-0.12),
            TraitEffect.Dormancy(DormancyKind.SEASONAL_TORPOR, 0.999),
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    SEASONAL_LEAF_DORMANCY(
        "seasonal leaf dormancy",
        "Growth and exposed foliage are withdrawn during the cold or dark season while protected living tissues persist.",
        listOf(
            TraitEffect.Dormancy(DormancyKind.SEASONAL_TORPOR, 0.999),
            TraitEffect.ReproductionMultiplier(0.94),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    FROST_HARDENED_TISSUES(
        "frost-hardened tissues",
        "Seasonal changes in cell fluids and exposed tissues reduce damage from freezing without shifting the organism's entire biochemistry.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 12.0, hotterC = -1.0),
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    SALT_EXCLUDING_ROOTS(
        "salt-excluding roots",
        "Root membranes limit the uptake of dissolved salts while drawing water from coastal sediment.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.58),
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    BLUBBER(
        "blubber",
        "A thick subcutaneous fat layer that insulates the body in water and doubles as an energy reserve.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 9.0, hotterC = -4.0),
            TraitEffect.ReserveCapacity(0.28),
            TraitEffect.CaptureAbility(-0.03),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    ANTIFREEZE_PROTEINS(
        "antifreeze proteins",
        "Circulating molecules inhibit destructive ice-crystal growth in exposed body fluids.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 8.0, hotterC = -1.0),
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    COLD_ACTIVE_ENZYMES(
        "cold-active enzymes",
        "Specialized metabolic enzymes retain useful reaction rates in cold water but become unstable at ordinary warm temperatures.",
        listOf(
            TraitEffect.TemperatureShift(-8.0),
            TraitEffect.ReproductionMultiplier(0.90),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    FAT_RESERVES(
        "seasonal fat reserves",
        "Energy-dense tissues accumulated during abundance and consumed when intake later falls.",
        listOf(
            TraitEffect.ReserveCapacity(0.45),
            TraitEffect.CaptureAbility(-0.04),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    PERENNIAL_STORAGE_TISSUE(
        "perennial storage tissue",
        "Long-lived stems, roots, or analogous organs store energy across unfavorable seasons and rebuild active tissue later.",
        listOf(
            TraitEffect.ReserveCapacity(0.35),
            TraitEffect.ReproductionMultiplier(0.94),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    CACHED_FOOD(
        "cached food",
        "Surplus food is hidden or otherwise stored during abundance and recovered during later scarcity.",
        listOf(
            TraitEffect.ReserveCapacity(0.32),
            TraitEffect.ReproductionMultiplier(0.97),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    DESICCATION_RESISTANT_PROPAGULES(
        "desiccation-resistant propagules",
        "Seeds, spores, cysts, or other dispersal bodies that remain viable after losing most of their water.",
        listOf(
            TraitEffect.Dormancy(DormancyKind.PROPAGULE, 0.97),
            TraitEffect.ReproductionMultiplier(0.90),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    BURROWING_EGGS(
        "burrowing eggs",
        "A seasonal lifecycle protected by placing resistant eggs or equivalent propagules below the surface.",
        listOf(
            TraitEffect.Dormancy(DormancyKind.BURROWED_EGGS, 0.985),
            TraitEffect.ReproductionMultiplier(0.88),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    SEASONAL_TORPOR(
        "seasonal torpor",
        "A reversible low-activity state that sharply reduces ecological activity during an unfavorable season.",
        listOf(
            TraitEffect.Dormancy(DormancyKind.SEASONAL_TORPOR, 0.99),
            TraitEffect.CaptureAbility(-0.05),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    WHOLE_BODY_ANHYDROBIOSIS(
        "whole-body anhydrobiosis",
        "The active organism can dry into a nearly ametabolic state and revive after water returns.",
        listOf(
            TraitEffect.Dormancy(DormancyKind.WHOLE_BODY_DESICCATION, 0.90),
            TraitEffect.ReproductionMultiplier(0.78),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    REEF_BUILDING(
        "mineral reef skeleton",
        "Persistent hard material deposited by living bodies accumulates into a three-dimensional aquatic reef.",
        listOf(
            TraitEffect.ReefBuilding(0.08),
            TraitEffect.ReproductionMultiplier(0.90),
            TraitEffect.MaintenanceCost(0.09),
        ),
    ),
    REEF_NESTING(
        "reef nesting",
        "Reproduction or shelter depends on cavities and protected surfaces within an aquatic reef.",
        listOf(
            TraitEffect.ReefUse(0.22),
            TraitEffect.Defense(0.08),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    REEF_CAMOUFLAGE(
        "reef camouflage",
        "Color, texture, and body outline resemble the varied surfaces of an aquatic reef.",
        listOf(
            TraitEffect.ReefUse(0.20),
            TraitEffect.Camouflage(Habitat.COASTAL, 0.18),
            TraitEffect.Camouflage(Habitat.SUNLIT_WATER, 0.18),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    REEF_BORING(
        "reef-boring mouthparts",
        "Hard scraping or drilling structures open cavities and expose food within reef material.",
        listOf(
            TraitEffect.ReefUse(0.18),
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.28),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    NEIGHBOR_DISPERSAL(
        "neighboring-range dispersal",
        "Individuals or propagules routinely spread into nearby suitable territory without a fixed seasonal destination.",
        listOf(
            TraitEffect.Dispersal(DispersalKind.NEIGHBOR),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    SHORT_MIGRATION(
        "short seasonal migration",
        "A recurring seasonal movement between nearby ranges with learned or inherited destinations.",
        listOf(
            TraitEffect.Dispersal(DispersalKind.SHORT_MIGRATION),
            TraitEffect.ReserveCapacity(0.10),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    REGIONAL_MIGRATION(
        "regional seasonal migration",
        "A recurring seasonal journey between ranges separated across a substantial region.",
        listOf(
            TraitEffect.Dispersal(DispersalKind.REGIONAL_MIGRATION),
            TraitEffect.ReserveCapacity(0.18),
            TraitEffect.MaintenanceCost(0.13),
        ),
    ),
    LONG_MIGRATION(
        "long-distance seasonal migration",
        "A recurring seasonal journey linking widely separated parts of a planet.",
        listOf(
            TraitEffect.Dispersal(DispersalKind.LONG_MIGRATION),
            TraitEffect.ReserveCapacity(0.28),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
}
