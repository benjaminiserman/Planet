package dev.biserman.planet.planet.ecology

enum class SizeClass(
    val typicalMassKg: Double,
    val maintenancePerKg: Double,
    val seasonalReproduction: Double,
    val densityScale: Double,
) {
    MINUSCULE(0.000_001, 0.34, 1.20, 8.0),
    TINY(0.01, 0.25, 0.90, 5.0),
    SMALL(1.0, 0.18, 0.60, 2.5),
    MEDIUM(50.0, 0.12, 0.34, 1.0),
    LARGE(1_000.0, 0.08, 0.18, 0.38),
    HUGE(10_000.0, 0.055, 0.10, 0.12),
    COLOSSAL(100_000.0, 0.040, 0.06, 0.04),
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
            TraitEffect.NicheCompetitionSensitivity(0.15),
            TraitEffect.Dormancy(DormancyKind.PROPAGULE, 0.9995),
            TraitEffect.BroadSalinityTolerance,
            TraitEffect.ReproductionMultiplier(0.88),
            TraitEffect.MaintenanceCost(0.08),
        ),
        invariantOnly = true,
    ),
    MICROSCOPIC_RESTING_STAGES(
        "microscopic resting stages",
        "A small fraction of the active population forms durable cysts, spores, or resting eggs that preserve the lineage through dark or otherwise unproductive seasons.",
        listOf(
            TraitEffect.DormantEntryBiomassRetention(0.10),
            TraitEffect.DormantReactivationMultiplier(10.00),
            TraitEffect.ReproductionMultiplier(1.03),
            TraitEffect.MaintenanceCost(0.02),
        ),
        invariantOnly = true,
    ),
    THAW_DEPENDENT_GROWTH(
        "thaw-dependent growth",
        "Living ground cover can overwinter below freezing, but requires liquid water and a thawed growing season to renew its tissues.",
        listOf(
            TraitEffect.MinimumActiveTemperature(0.0),
            TraitEffect.FrozenDormantSurvival(0.98),
            TraitEffect.ReproductionMultiplier(1.03),
            TraitEffect.MaintenanceCost(0.02),
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
    SLOW_METABOLISM(
        "extremely slow metabolism",
        "Low-throughput digestion and cellular metabolism extract energy from poor food while sharply limiting growth and reproduction.",
        listOf(
            TraitEffect.MetabolicDemandMultiplier(0.55),
            TraitEffect.ReproductionMultiplier(0.65),
            TraitEffect.MaintenanceCost(0.03),
            TraitEffect.PursuitSpeed(-0.5)
        ),
    ),
    EXTENDED_PARENTAL_CARE(
        "extended parental care",
        "Young remain with experienced adults for years, gaining protection and learned foraging skills at the cost of producing offspring slowly.",
        listOf(
            TraitEffect.ReproductionMultiplier(0.074),
            TraitEffect.Defense(0.08),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    PHOTOSYNTHETIC_SURFACE(
        "photosynthetic surface",
        "A broad light-harvesting body surface containing photosynthetic pigments.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.PHOTOSYNTHESIS, 1.0),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    AIRBORNE_PHOTOSYNTHETIC_SURFACE(
        "airborne photosynthetic surface",
        "A minute drifting body whose exposed pigments harvest light while suspended in the atmosphere.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.PHOTOSYNTHESIS, 1.0),
            TraitEffect.HabitatSupport(Habitat.AERIAL, 0.78),
            TraitEffect.PelagicAerialResidency,
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
    ENLARGED_CARDIOPULMONARY_SYSTEM(
        "enlarged heart and lungs",
        "An unusually large heart, lungs, and pulmonary exchange surface sustain oxygen delivery in thin air.",
        listOf(
            TraitEffect.ElevationToleranceShift(2_500.0),
            TraitEffect.MaintenanceCost(0.09),
        ),
    ),
    HIGH_AFFINITY_HEMOGLOBIN(
        "high-affinity hemoglobin",
        "Respiratory pigments bind oxygen effectively at the low partial pressures found at high elevation.",
        listOf(
            TraitEffect.ElevationToleranceShift(2_500.0),
            TraitEffect.CaptureAbility(-0.03),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    HYPOXIA_RESPONSIVE_METABOLISM(
        "hypoxia-responsive metabolism",
        "Oxygen-sensing pathways adjust circulation and cellular energy use during chronic exposure to thin air.",
        listOf(
            TraitEffect.ElevationToleranceShift(3_500.0),
            TraitEffect.ReproductionMultiplier(0.97),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    SEA_ICE_LOCOMOTION(
        "sea-ice locomotion",
        "Broad feet, claws, body posture, or equivalent adaptations support travel and hunting across floating sea ice.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.SEA_ICE, 0.72),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    AQUATIC_FLIPPERS(
        "aquatic flippers",
        "Broad propulsive limbs or fins that support controlled swimming in open water.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.72),
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.52),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    GILLS(
        "gills",
        "Thin, blood-supplied folds extract dissolved respiratory gases from water as it passes over them.",
        listOf(
            TraitEffect.AquaticRespiration(AquaticRespirationMode.UNDERWATER),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    DIFFUSIVE_AQUATIC_GAS_EXCHANGE(
        "diffusive aquatic gas exchange",
        "A thin body surface exchanges dissolved respiratory gases directly with surrounding water without dedicated gills.",
        listOf(
            TraitEffect.AquaticRespiration(AquaticRespirationMode.UNDERWATER),
            TraitEffect.Defense(-0.04),
            TraitEffect.MaintenanceCost(0.02),
        ),
    ),
    PROLONGED_BREATH_HOLDING(
        "prolonged breath-holding",
        "Large internal oxygen stores and dive responses sustain repeated activity far from an immediately accessible shore.",
        listOf(
            TraitEffect.AquaticRespiration(AquaticRespirationMode.BREATH_HOLDING),
            TraitEffect.ReserveCapacity(0.08),
            TraitEffect.ReproductionMultiplier(0.97),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    SEA_ICE_ROOKERY(
        "sea-ice rookery",
        "Breeding colonies occupy persistent sea ice close enough to land for repeated access to stable resting and nesting grounds.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.SEA_ICE, 0.82),
            TraitEffect.ObligateResidentHabitat(Habitat.SEA_ICE),
            TraitEffect.RequiresAdjacentLand,
            TraitEffect.ReproductionMultiplier(0.94),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    DEEP_DIVING_PHYSIOLOGY(
        "deep-diving physiology",
        "Pressure-tolerant tissues, collapsible gas spaces, oxygen stores, or equivalent adaptations permit prolonged activity below the sunlit surface layer.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.DARK_WATER, 0.65),
            TraitEffect.DarkWaterAdaptation,
            // Deep water is usually cooler and less seasonally variable than
            // the surface represented by the tile's single temperature.
            TraitEffect.TemperatureOptimalTolerance(hotterC = 4.0, colderC = 4.0),
            TraitEffect.TemperatureTolerance(hotterC = 8.0, colderC = 8.0),
            TraitEffect.ReserveCapacity(0.08),
            TraitEffect.MaintenanceCost(0.12),
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
    FRUIT_BEARING(
        "fruit-bearing reproductive structures",
        "Energy-rich fruits surround or accompany propagules, recruiting mobile animals to disperse them.",
        listOf(
            TraitEffect.FruitProduction(0.0025),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    FLOWERS(
        "flowers",
        "Specialized reproductive structures expose pollen and ovules while advertising to mobile visitors or releasing pollen into the environment.",
        listOf(
            TraitEffect.Flowering,
            TraitEffect.ReproductionMultiplier(1.05),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    NECTARIES(
        "nectaries",
        "Secretory tissues offer an energy-rich liquid reward that attracts animals to reproductive structures.",
        listOf(
            TraitEffect.NectarProduction(0.025),
            TraitEffect.WaterRequirement(0.02),
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.05),
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
            TraitEffect.FreshwaterOsmoregulation,
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    EURYHALINE_OSMOREGULATION(
        "euryhaline osmoregulation",
        "Adjustable membranes, kidneys, salt glands, or analogous organs permit repeated transitions between dilute freshwater and salty water.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.FRESHWATER, 0.68),
            TraitEffect.BroadSalinityTolerance,
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.07),
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
    FLOATING_FRONDS(
        "floating fronds",
        "Long buoyant photosynthetic blades rise from an aquatic anchor into well-lit surface water.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.40),
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.28),
            TraitEffect.InsolationOptimum(-0.06),
            TraitEffect.MaintenanceCost(0.07),
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
    PELAGIC_SOARING_WINGS(
        "pelagic soaring wings",
        "Long, efficient wings and wind-harvesting flight allow extended foraging far from land without exhausting energy reserves.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.AERIAL, 0.10),
            TraitEffect.PelagicAerialResidency,
            TraitEffect.ReserveCapacity(0.08),
            TraitEffect.MaintenanceCost(0.08),
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
    INSULATED_BURROW_REFUGE(
        "insulated burrow refuge",
        "A sheltered burrow or rock-crevice retreat buffers its occupant from the coldest exposed-air temperatures.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 5.0),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    DRY_BURROW_NEST(
        "dry burrow nest",
        "A nest chamber whose eggs, young, stored food, or respiratory surfaces require a well-drained burrow.",
        listOf(
            TraitEffect.TemperatureTolerance(hotterC = 5.0, colderC = 3.0),
            TraitEffect.WaterRequirement(-0.06),
            TraitEffect.MaximumWaterTolerance(
                optimalMaximumChange = -0.66,
                absoluteMaximumChange = -0.33,
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
    KRILL_SIEVING_TEETH(
        "krill-sieving teeth",
        "Interlocking, multi-cusped teeth form a sieve that retains small swimming prey as water is expelled from the mouth.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.FILTER_FEEDING, 0.84),
            TraitEffect.CaptureAbility(0.05),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    GILL_PADS(
        "gill pads",
        "Broad ciliated or mucus-coated respiratory surfaces that also trap minuscule food from flowing water.",
        listOf(
            TraitEffect.AquaticRespiration(AquaticRespirationMode.UNDERWATER),
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
    BENTHIC_SUCTION_FEEDING(
        "benthic suction-feeding mouth",
        "A muscular tongue, sealed lips, and a vaulted mouth expose and suction soft-bodied prey from seafloor sediment.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.52),
            TraitEffect.CaptureAbility(0.10),
            TraitEffect.MaintenanceCost(0.06),
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
    FRUIT_EATING_MOUTHPARTS(
        "fruit-eating mouthparts and digestion",
        "Grasping, biting, crushing, or swallowing structures and digestion suited to energy-rich fruits.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.FRUGIVORY, 0.78),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.18),
            TraitEffect.CaptureAbility(-0.04),
            TraitEffect.MaintenanceCost(0.05),
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
    SWIFT_LIMBS(
        "swift limbs",
        "Long, powerful, or rapidly cycling legs or fins increase running or swimming speed, helping hunters close distance and prey escape pursuit.",
        listOf(
            TraitEffect.PursuitSpeed(0.18),
            TraitEffect.WaterRequirement(0.03),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    MOTION_TRACKING_SENSES(
        "motion-tracking senses",
        "Vision, hearing, scent, vibration detection, or equivalent senses allow a hunter to follow moving prey through a sustained chase.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.PURSUIT_PREDATION, 0.70),
            TraitEffect.CaptureAbility(0.18),
            TraitEffect.MaintenanceCost(0.13),
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
            TraitEffect.DarkWaterAdaptation,
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
    WOOLLY_UNDERCOAT(
        "woolly undercoat",
        "A second layer of fine, densely packed hairs traps additional insulating air beneath the outer coat.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 5.0, hotterC = -2.0),
            TraitEffect.MaintenanceCost(0.06),
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
            TraitEffect.TemperatureTolerance(colderC = -5.0, hotterC = 6.0),
            TraitEffect.Defense(-0.03),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    CONCENTRATED_URINE(
        "concentrated urine",
        "Highly water-retentive kidneys excrete dissolved wastes in a small volume of concentrated urine.",
        listOf(
            TraitEffect.MaintenanceCost(0.03),
            TraitEffect.WaterRequirement(-0.1),
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
            TraitEffect.SeasonalColdTolerance(maximumBonusC = 18.0, triggerInsolation = 0.58),
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
    SNOW_AND_ICE_LICKING(
        "snow and ice licking",
        "The organism deliberately consumes snow or surface ice when liquid drinking water is unavailable.",
        listOf(
            TraitEffect.SnowHydration,
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    PREY_DERIVED_WATER(
        "prey-derived water",
        "Concentrated kidneys and digestive physiology recover most required water from prey rather than free-standing sources.",
        listOf(
            TraitEffect.WaterRequirement(-0.12),
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.08),
            TraitEffect.StrategySupport(EcoStrategy.PURSUIT_PREDATION, 0.08),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    FOOD_DERIVED_WATER(
        "food-derived water",
        "Efficient kidneys and digestion obtain nearly all required water from moist food or metabolically produced water.",
        listOf(
            TraitEffect.WaterRequirement(-0.18),
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
    FROST_SENSITIVE_SUCCULENT_TISSUES(
        "frost-sensitive succulent tissues",
        "Large water-filled cells tolerate extreme heat and drought but are readily damaged when their fluids freeze.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = -5.0, hotterC = 2.0),
            TraitEffect.MaintenanceCost(0.02),
        ),
    ),
    DROUGHT_DECIDUOUS_LEAVES(
        "drought-deciduous leaves",
        "Photosynthetic surfaces are shed during dry seasons and regrown when water becomes available.",
        listOf(
            TraitEffect.WaterRequirement(-0.12),
            TraitEffect.Dormancy(DormancyKind.DROUGHT_DECIDUOUS, 0.999),
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    SEASONAL_LEAF_DORMANCY(
        "seasonal leaf dormancy",
        "Growth and exposed foliage are withdrawn during the cold or dark season while protected living tissues persist.",
        listOf(
            TraitEffect.Dormancy(DormancyKind.COLD_DARK_LEAF_DORMANCY, 0.999),
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
            TraitEffect.HabitatSupport(Habitat.LAND_SURFACE, -0.5),
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    BLUBBER(
        "blubber",
        "A thick subcutaneous fat layer that insulates the body in water and doubles as an energy reserve.",
        listOf(
            TraitEffect.TemperatureShift(-10.0),
            TraitEffect.TemperatureTolerance(colderC = 6.0),
            TraitEffect.ReserveCapacity(0.28),
            TraitEffect.CaptureAbility(-0.03),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    ANTIFREEZE_PROTEINS(
        "antifreeze proteins",
        "Circulating molecules inhibit destructive ice-crystal growth in exposed body fluids.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 12.0, hotterC = -1.0),
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.12),
        ),
    ),
    COLD_ACTIVE_ENZYMES(
        "cold-active enzymes",
        "Specialized metabolic enzymes retain useful reaction rates in cold water but become unstable at ordinary warm temperatures.",
        listOf(
            TraitEffect.TemperatureShift(-13.0),
            TraitEffect.ReproductionMultiplier(0.90),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    HEAT_STABLE_ENZYMES(
        "heat-stable enzymes",
        "Proteins and cell membranes remain functional through sustained hot conditions without shifting the organism's entire biochemical regime.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = -2.0, hotterC = 10.0),
            TraitEffect.ReproductionMultiplier(0.94),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    WARM_WATER_ENZYMES(
        "warm-water enzymes",
        "Metabolic enzymes and membranes remain stable and active in persistently warm water, at the cost of poor cold performance.",
        listOf(
            TraitEffect.TemperatureShift(5.0),
            TraitEffect.ReproductionMultiplier(0.94),
            TraitEffect.MaintenanceCost(0.04),
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
    SHALLOW_WATER_PHOTOSYMBIOSIS(
        "shallow-water photosymbiosis",
        "Light-dependent symbionts nourish a sessile host anchored close to the illuminated seafloor.",
        listOf(
            TraitEffect.WaterDepthTolerance(
                optimalMaximumM = 30.0,
                absoluteMaximumM = 80.0,
            ),
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    REEF_NESTING(
        "reef nesting",
        "Reproduction or shelter depends on cavities and protected surfaces within an aquatic reef.",
        listOf(
            TraitEffect.ReefUse(0.75),
            TraitEffect.Defense(0.08),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    REEF_CAMOUFLAGE(
        "reef camouflage",
        "Color, texture, and body outline resemble the varied surfaces of an aquatic reef.",
        listOf(
            TraitEffect.ReefUse(0.33),
            TraitEffect.Camouflage(Habitat.COASTAL, 0.18),
            TraitEffect.Camouflage(Habitat.SUNLIT_WATER, 0.18),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    REEF_SHELTER_DEPENDENCE(
        "reef shelter dependence",
        "Feeding, refuge, and daily activity depend on the dense cavities and broken sight-lines of a living reef.",
        listOf(
            TraitEffect.ReefUse(1.0),
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    REEF_BORING(
        "reef-boring mouthparts",
        "Hard scraping or drilling structures open cavities and expose food within reef material.",
        listOf(
            TraitEffect.ReefUse(0.75),
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.28),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    GLIDING_MEMBRANE(
        "gliding membrane",
        "A broad skin membrane or flattened body turns height and forward speed into controlled unpowered flight.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.AERIAL, 0.48),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.32),
            TraitEffect.CaptureAbility(0.04),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    WEB_SILK(
        "prey-catching silk web",
        "Strong adhesive fibers are arranged into traps that intercept moving prey.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.58),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.18),
            TraitEffect.CaptureAbility(0.24),
            TraitEffect.ReproductionMultiplier(0.94),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    VENOM_DELIVERY(
        "venom delivery",
        "Fangs, stingers, spines, or saliva introduce toxins that rapidly disable prey.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.16),
            TraitEffect.StrategySupport(EcoStrategy.PURSUIT_PREDATION, 0.08),
            TraitEffect.CaptureAbility(0.24),
            TraitEffect.ReproductionMultiplier(0.95),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    CONSTRICTING_BODY(
        "constricting body",
        "A long muscular body coils around captured prey and prevents breathing or circulation.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.24),
            TraitEffect.CaptureAbility(0.20),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    HOOKED_TALONS(
        "hooked talons",
        "Curved claws seize, carry, and kill struggling prey.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.12),
            TraitEffect.CaptureAbility(0.18),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    ECHOLOCATION(
        "echolocation",
        "The organism emits sound and reconstructs nearby surfaces and moving prey from returning echoes.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.DARK_WATER, 0.12),
            TraitEffect.CaptureAbility(0.20),
            TraitEffect.MaintenanceCost(0.09),
        ),
    ),
    ELECTRORECEPTION(
        "electroreception",
        "Sensitive organs detect the weak electrical fields produced by hidden or moving organisms in water.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.10),
            TraitEffect.HabitatSupport(Habitat.DARK_WATER, 0.16),
            TraitEffect.CaptureAbility(0.18),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    ARMORED_HIDE(
        "armored hide",
        "Thick skin reinforced with plates or embedded bone resists bites, claws, and impacts.",
        listOf(
            TraitEffect.Defense(0.34),
            TraitEffect.CaptureAbility(-0.07),
            TraitEffect.ReproductionMultiplier(0.92),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    PROTECTIVE_SHELL(
        "protective shell",
        "A rigid external shell encloses vulnerable tissues and can withstand crushing or abrasion.",
        listOf(
            TraitEffect.Defense(0.46),
            TraitEffect.CaptureAbility(-0.14),
            TraitEffect.ReproductionMultiplier(0.86),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    QUILLS(
        "defensive quills",
        "Long rigid hairs or spines make biting and grappling dangerous.",
        listOf(
            TraitEffect.Defense(0.32),
            TraitEffect.CaptureAbility(-0.08),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    TOXIC_SKIN(
        "toxic skin",
        "Skin glands or accumulated compounds make the organism poisonous or intensely distasteful.",
        listOf(
            TraitEffect.Defense(0.28),
            TraitEffect.ReproductionMultiplier(0.93),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    INK_CLOUD(
        "defensive ink cloud",
        "A released cloud obscures vision and confuses chemical senses during escape.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.10),
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.10),
            TraitEffect.Defense(0.22),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    JET_PROPULSION(
        "jet propulsion",
        "Water is forcefully expelled from a muscular chamber for rapid acceleration and maneuvering.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.26),
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.30),
            TraitEffect.CaptureAbility(0.12),
            TraitEffect.MaintenanceCost(0.12),
        ),
    ),
    GRASPING_TENTACLES(
        "grasping tentacles",
        "Flexible muscular appendages explore crevices and restrain several prey items at once.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.16),
            TraitEffect.CaptureAbility(0.18),
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.12),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    BIOLUMINESCENT_LURE(
        "bioluminescent lure",
        "A controlled light organ attracts curious prey in otherwise dark water.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.DARK_WATER, 0.40),
            TraitEffect.DarkWaterAdaptation,
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.24),
            TraitEffect.CaptureAbility(0.18),
            TraitEffect.MaintenanceCost(0.09),
        ),
    ),
    RUMINANT_STOMACH(
        "ruminant stomach",
        "Several fermentation chambers and repeated chewing extract energy from fibrous photosynthetic tissue.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.20),
            TraitEffect.ReserveCapacity(0.10),
            TraitEffect.ReproductionMultiplier(0.95),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    FERMENTING_HINDGUT(
        "fermenting hindgut",
        "A large microbe-rich hindgut digests cellulose after food has passed through the stomach.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.16),
            TraitEffect.ReserveCapacity(0.07),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    SEED_CRACKING_MOUTHPARTS(
        "seed-cracking mouthparts",
        "Deep reinforced jaws or a stout beak crush hard seeds and nuts from ground and canopy producers.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.54),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.16),
            TraitEffect.CaptureAbility(0.04),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    NECTAR_SIPPING_TONGUE(
        "nectar-sipping tongue",
        "An elongated tongue or proboscis reaches energy-rich secretions within elevated reproductive structures.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.NECTAR_FEEDING, 0.70),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.24),
            TraitEffect.CaptureAbility(-0.04),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    POLLEN_CARRYING_SURFACES(
        "pollen-carrying surfaces",
        "Branched hairs, scales, feathers, or other textured body surfaces retain pollen while an animal moves among flowers.",
        listOf(
            TraitEffect.PollinationEfficiency(0.70),
            TraitEffect.CaptureAbility(-0.02),
            TraitEffect.MaintenanceCost(0.03),
        ),
    ),
    APOSEMATIC_COLORATION(
        "aposematic coloration",
        "Conspicuous colors advertise a dangerous or distasteful organism—or mimic another local organism carrying the same warning colors.",
        listOf(
            TraitEffect.AposematicColoration,
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    RAPID_GROWTH(
        "rapid growth",
        "Exceptionally fast production of new shoots and tissues allows an organism to replace losses and spread quickly when conditions are favorable.",
        listOf(
            TraitEffect.ReproductionMultiplier(1.75),
            TraitEffect.MaintenanceCost(0.35),
        ),
    ),
    NEST_PROBING_TONGUE(
        "nest-probing tongue",
        "An extremely elongated adhesive tongue reaches ants and termites through narrow passages in opened colony nests.",
        listOf(
            TraitEffect.CaptureAbility(0.28),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    PROJECTILE_TONGUE(
        "projectile tongue",
        "A rapidly projected adhesive tongue lets a stationary hunter seize small moving prey before it can escape.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.62),
            TraitEffect.CaptureAbility(0.22),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    SAP_SUCKING_PROBOSCIS(
        "sap-sucking proboscis",
        "A narrow piercing mouthpart taps fluids from the living tissues of a host organism.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.PARASITISM, 0.58),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.18),
            TraitEffect.CaptureAbility(-0.04),
            TraitEffect.MaintenanceCost(0.04),
        ),
    ),
    LONG_NECK(
        "long browsing neck",
        "An elongated neck reaches foliage beyond the feeding height of most ground animals.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.42),
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.10),
            TraitEffect.WaterRequirement(0.04),
            TraitEffect.MaintenanceCost(0.09),
        ),
    ),
    PREHENSILE_TRUNK(
        "prehensile trunk",
        "A muscular mobile appendage manipulates branches, uproots food, and draws water without lowering the whole body.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.18),
            TraitEffect.CaptureAbility(0.10),
            TraitEffect.WaterRequirement(0.03),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    COOPERATIVE_HUNTING(
        "cooperative hunting",
        "Several individuals coordinate pursuit, encirclement, or ambush rather than attacking independently.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.12),
            TraitEffect.StrategySupport(EcoStrategy.PURSUIT_PREDATION, 0.18),
            TraitEffect.CaptureAbility(0.18),
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    COLONY_LIVING(
        "defended social colony",
        "Many related individuals share shelter, defense, and food information in a persistent colony.",
        listOf(
            TraitEffect.Defense(0.18),
            TraitEffect.ReproductionMultiplier(1.08),
            TraitEffect.MaintenanceCost(0.09),
        ),
    ),
    COLONY_THERMOREGULATION(
        "colony thermoregulation",
        "Workers cluster and generate metabolic heat in winter, then fan and evaporate water to cool the shared nest during hot weather.",
        listOf(
            TraitEffect.SeasonalColdTolerance(maximumBonusC = 24.0, triggerInsolation = 0.62),
            TraitEffect.TemperatureTolerance(hotterC = 5.0),
            TraitEffect.WaterRequirement(0.04),
            TraitEffect.ReproductionMultiplier(0.96),
            TraitEffect.MaintenanceCost(0.14),
        ),
    ),
    VENOMOUS_STINGER(
        "defensive venomous stinger",
        "A barbed or reusable ovipositor-derived weapon injects venom into attackers, strongly deterring predation on the colony.",
        listOf(
            TraitEffect.Defense(0.45),
            TraitEffect.ReproductionMultiplier(0.97),
            TraitEffect.MaintenanceCost(0.06),
        ),
    ),
    HONEY_STORES(
        "communal honey stores",
        "Workers concentrate floral sugars into stable comb stores that feed the colony through winter or other seasonal shortages.",
        listOf(
            TraitEffect.ReserveCapacity(0.90),
            TraitEffect.ReproductionMultiplier(0.93),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    OPEN_COUNTRY_HERDING(
        "open-country herding",
        "Social groups rely on long sight lines, collective vigilance, and coordinated travel through open vegetation.",
        listOf(
            TraitEffect.DenseCanopyForagingPenalty(0.82),
            TraitEffect.Defense(0.08),
            TraitEffect.MaintenanceCost(0.05),
        ),
    ),
    EXTENDED_BROOD_CARE(
        "extended brood care",
        "Parents protect, feed, teach, or transport offspring through a prolonged vulnerable period.",
        listOf(
            TraitEffect.Defense(0.05),
            TraitEffect.ReproductionMultiplier(1.12),
            TraitEffect.MaintenanceCost(0.11),
        ),
    ),
    WATERPROOF_PLUMAGE(
        "waterproof plumage",
        "Overlapping oiled feathers retain insulating air and shed water during swimming and rain.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.34),
            TraitEffect.HabitatSupport(Habitat.FRESHWATER, 0.30),
            TraitEffect.TemperatureTolerance(colderC = 2.0),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    DIGGING_CLAWS(
        "digging claws",
        "Broad reinforced claws rapidly excavate soil, tear apart nests, and expose concealed food.",
        listOf(
            TraitEffect.TemperatureTolerance(colderC = 2.0, hotterC = 2.0),
            TraitEffect.CaptureAbility(0.10),
            TraitEffect.WaterRequirement(-0.04),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    LEAPING_LEGS(
        "powerful leaping legs",
        "Elongated spring-like limbs cross obstacles and produce abrupt escapes or attacks.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.LAND_SURFACE, 0.18),
            TraitEffect.HabitatSupport(Habitat.CANOPY, 0.14),
            TraitEffect.CaptureAbility(0.08),
            TraitEffect.MaintenanceCost(0.08),
        ),
    ),
    TOOL_USING_FORELIMBS(
        "tool-using forelimbs",
        "Dexterous grasping limbs manipulate stones, sticks, containers, or other objects to obtain defended food.",
        listOf(
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.08),
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.08),
            TraitEffect.CaptureAbility(0.14),
            TraitEffect.MaintenanceCost(0.10),
        ),
    ),
    CRUSHING_CLAWS(
        "crushing claws",
        "Opposed hardened claws crack shells, cut plant tissue, and restrain struggling prey.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.18),
            TraitEffect.StrategySupport(EcoStrategy.GRAZING, 0.10),
            TraitEffect.StrategySupport(EcoStrategy.AMBUSH_PREDATION, 0.12),
            TraitEffect.CaptureAbility(0.12),
            TraitEffect.MaintenanceCost(0.07),
        ),
    ),
    SUCTION_CUPS(
        "gripping suction cups",
        "Pressure-sealing discs attach to rock, prey, and other bodies under water.",
        listOf(
            TraitEffect.HabitatSupport(Habitat.COASTAL, 0.24),
            TraitEffect.HabitatSupport(Habitat.SUNLIT_WATER, 0.14),
            TraitEffect.Defense(0.08),
            TraitEffect.CaptureAbility(0.10),
            TraitEffect.MaintenanceCost(0.06),
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