package dev.biserman.planet.planet.ecology

import kotlin.math.max

/**
 * Mutable, short-lived state used while readable trait effects are compiled
 * into the array-oriented [CompiledSpecies] representation.
 *
 * Trait effects use semantic operations here rather than depending on the
 * compiler's final layout. Cross-trait phenotype rules remain the compiler's
 * responsibility and run after all direct effects have been applied.
 */
class SpeciesCompilationContext internal constructor(
    private val speciesDisplayName: String,
    sizeTemperatureTolerance: Double,
) {
    private val habitatSupport = DoubleArray(Habitat.entries.size)
    private val strategySupport = DoubleArray(EcoStrategy.entries.size)
    private val camouflage = DoubleArray(Habitat.entries.size)
    private var temperatureShift = 0.0
    private var colderTolerance = sizeTemperatureTolerance
    private var hotterTolerance = sizeTemperatureTolerance
    private var colderOptimalTolerance = 0.0
    private var hotterOptimalTolerance = 0.0
    private var minimumActiveTemperatureC = Double.NEGATIVE_INFINITY
    private var frozenDormantSurvival = 1.0
    private var seasonalColdTolerance = 0.0
    private var seasonalColdTrigger = 0.0
    private var thermalStrategy: ThermalStrategy? = null
    private var waterRequirement = 0.25
    private var optimalMaximumWater = 1.0
    private var maximumWater = 1.0
    private var optimalMaximumWaterDepthM = Double.POSITIVE_INFINITY
    private var absoluteMaximumWaterDepthM = Double.POSITIVE_INFINITY
    private var elevationToleranceShiftM = 0.0
    private var snowHydration = false
    private var insolationOptimum = 0.8
    private var canopyLightEfficiency = 0.0
    private var denseCanopyForagingPenalty = 0.0
    private var reserveCapacity = 0.25
    private var nicheCompetitionSensitivity = 1.0
    private var dormancyKind = DormancyKind.NONE
    private var dormantSurvival = 0.0
    private var dormantEntryBiomassRetention = 1.0
    private var dormantReactivationMultiplier = 1.0
    private var dispersalKind = DispersalKind.NONE
    private var reproductionMultiplier = 1.0
    private var metabolicDemandMultiplier = 1.0
    private var maintenanceCost = 0.0
    private var captureAbility = 0.5
    private var pursuitSpeed = 0.0
    private var defense = 0.25
    private var aposematicColoration = false
    private var reefUse = 0.0
    private var reefBuilding = 0.0
    private var fruitProduction = 0.0
    private var flowering = false
    private var nectarProduction = 0.0
    private var pollinationEfficiency = 0.0
    private var wasteFertilization = 0.0
    private var pelagicAerialResident = false
    private var darkWaterAdapted = false
    private var freshwaterAdapted = false
    private var broadSalinityTolerance = false
    private var underwaterBreathing = false
    private var prolongedBreathHolding = false
    private var obligateResidentHabitat: Habitat? = null
    private var requiresAdjacentLand = false

    private var maintenanceCostScale = 1.0

    internal fun apply(trait: SpeciesTrait) {
        maintenanceCostScale = if (trait.isFoundation) 1.0 else 0.35
        trait.effects.forEach { it.applyTo(this) }
        maintenanceCostScale = 1.0
    }

    /** Applies phenotype rules that emerge from combinations of authored traits. */
    internal fun applyCrossTraitRules(
        sizeClass: SizeClass,
        commonTraits: Set<CommonTrait>,
    ) {
        when (compiledSalinityTolerance()) {
            AquaticSalinityTolerance.SALTWATER_ONLY ->
                habitatSupport[Habitat.FRESHWATER.ordinal] = 0.0
            AquaticSalinityTolerance.FRESHWATER_ONLY -> {
                habitatSupport[Habitat.COASTAL.ordinal] = 0.0
                habitatSupport[Habitat.SUNLIT_WATER.ordinal] = 0.0
                habitatSupport[Habitat.DARK_WATER.ordinal] = 0.0
            }
            AquaticSalinityTolerance.BROAD -> Unit
        }
        if (sizeClass.ordinal >= SizeClass.HUGE.ordinal) {
            habitatSupport[Habitat.FRESHWATER.ordinal] = 0.0
        }
        if (!underwaterBreathing && !prolongedBreathHolding) {
            habitatSupport[Habitat.SUNLIT_WATER.ordinal] = 0.0
            habitatSupport[Habitat.DARK_WATER.ordinal] = 0.0
        }
        obligateResidentHabitat?.let { requiredHabitat ->
            habitatSupport.indices.forEach { habitatIndex ->
                if (habitatIndex != requiredHabitat.ordinal) {
                    habitatSupport[habitatIndex] = 0.0
                }
            }
        }

        val grazingSupport = strategySupport[EcoStrategy.GRAZING.ordinal]
        val predationSupport = max(
            strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal],
            strategySupport[EcoStrategy.PURSUIT_PREDATION.ordinal],
        )
        if (
            grazingSupport >= GENERALIST_MINIMUM_METHOD_SUPPORT &&
            predationSupport >= GENERALIST_MINIMUM_METHOD_SUPPORT
        ) {
            strategySupport[EcoStrategy.GENERALIST_FORAGING.ordinal] =
                (max(grazingSupport, predationSupport) + GENERALIST_BREADTH_BONUS)
                    .coerceAtMost(1.0)
        }
        if (
            CommonTrait.NEST_PROBING_TONGUE in commonTraits &&
            CommonTrait.DIGGING_CLAWS in commonTraits
        ) {
            strategySupport[EcoStrategy.COLONY_RAIDING.ordinal] = 0.86
        }
    }

    internal fun finish(
        index: Int,
        definition: SpeciesDefinition,
        niches: List<NicheDefinition>,
        commonTraits: Set<CommonTrait>,
    ): CompiledSpecies {
        habitatSupport.indices.forEach {
            habitatSupport[it] = habitatSupport[it].coerceIn(0.0, 1.0)
        }
        strategySupport.indices.forEach {
            strategySupport[it] = strategySupport[it].coerceIn(0.0, 1.0)
        }

        val nicheFit = DoubleArray(niches.size) { nicheIndex ->
            val niche = niches[nicheIndex]
            val habitat = habitatSupport[niche.habitat.ordinal]
            val strategy = strategySupport[niche.strategy.ordinal]
            if (habitat <= 0.0 || strategy <= 0.0) 0.0 else habitat * strategy
        }
        val massKg = definition.sizeClass.typicalMassKg
        val sessilePhotosyntheticMaintenance =
            if (
                !definition.motile &&
                strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] > 0.0
            ) {
                0.30
            } else {
                1.0
            }
        val maintenanceDemand =
            massKg *
                definition.sizeClass.maintenancePerKg *
                max(0.15, 1.0 + maintenanceCost) *
                metabolicDemandMultiplier *
                sessilePhotosyntheticMaintenance
        val minimumWater = waterRequirement.coerceIn(0.0, 1.0)
        val optimalWater = optimalMaximumWater.coerceIn(minimumWater, 1.0)
        val compiledMaximumWater = maximumWater.coerceIn(optimalWater, 1.0)

        return CompiledSpecies(
            index = index,
            id = definition.id,
            displayName = definition.displayName,
            sizeClass = definition.sizeClass,
            motile = definition.motile,
            kind = definition.kind,
            ancestorSpeciesId = definition.ancestorSpeciesId,
            physiology = PhysiologyProfile(
                massKg = massKg,
                maintenanceDemand = maintenanceDemand,
                thermal = ThermalProfile(
                    outerLowC = 5.0 + temperatureShift - colderTolerance,
                    optimalLowC = 15.0 + temperatureShift - colderOptimalTolerance,
                    optimalHighC = 25.0 + temperatureShift + hotterOptimalTolerance,
                    outerHighC = 30.0 + temperatureShift + hotterTolerance,
                    minimumActiveC = minimumActiveTemperatureC,
                    frozenDormantSurvival = frozenDormantSurvival.coerceIn(0.0, 1.0),
                    seasonalColdToleranceC = seasonalColdTolerance,
                    seasonalColdTriggerInsolation = seasonalColdTrigger,
                    regulation = thermalStrategy,
                ),
                hydration = HydrationProfile(
                    minimumWater = minimumWater,
                    optimalMaximumWater = optimalWater,
                    maximumWater = compiledMaximumWater,
                    snowHydration = snowHydration,
                ),
                respiration = RespirationProfile(
                    salinityTolerance = compiledSalinityTolerance(),
                    underwaterBreathing = underwaterBreathing,
                    prolongedBreathHolding = prolongedBreathHolding,
                ),
            ),
            environment = EnvironmentalProfile(
                optimalMaximumWaterDepthM = optimalMaximumWaterDepthM,
                absoluteMaximumWaterDepthM = absoluteMaximumWaterDepthM,
                elevationToleranceShiftM = elevationToleranceShiftM,
                insolationOptimum = insolationOptimum.coerceIn(0.05, 1.0),
                canopyLightEfficiency = canopyLightEfficiency.coerceIn(0.0, 0.8),
                denseCanopyForagingPenalty =
                denseCanopyForagingPenalty.coerceIn(0.0, 1.0),
                pelagicAerialResident = pelagicAerialResident,
                darkWaterAdapted = darkWaterAdapted,
                requiresAdjacentLand = requiresAdjacentLand,
            ),
            lifeHistory = LifeHistoryProfile(
                seasonalReproduction =
                definition.sizeClass.seasonalReproduction * reproductionMultiplier,
                reserveCapacity = reserveCapacity.coerceIn(0.0, 1.5),
                nicheCompetitionSensitivity = nicheCompetitionSensitivity.coerceIn(0.0, 1.0),
                dormancyKind = dormancyKind,
                dormantSurvival = dormantSurvival,
                dormantEntryBiomassRetention =
                dormantEntryBiomassRetention.coerceIn(0.0, 1.0),
                dormantReactivationMultiplier = dormantReactivationMultiplier,
                dispersalKind = dispersalKind,
            ),
            interactions = InteractionProfile(
                captureAbility = captureAbility.coerceIn(0.05, 1.5),
                pursuitSpeed = pursuitSpeed.coerceIn(0.0, 1.0),
                defense = defense.coerceIn(0.0, 1.5),
                aposematicColoration = aposematicColoration,
                dangerousWarningModel =
                CommonTrait.VENOMOUS_STINGER in commonTraits ||
                    CommonTrait.TOXIC_SKIN in commonTraits,
                reefUse = reefUse.coerceIn(0.0, 1.0),
                reefBuilding = reefBuilding.coerceIn(0.0, 0.25),
                fruitProduction = fruitProduction.coerceIn(0.0, 0.10),
                flowering = flowering,
                nectarProduction = nectarProduction.coerceIn(0.0, 0.10),
                pollinationEfficiency = pollinationEfficiency.coerceIn(0.0, 1.0),
                wasteFertilization = wasteFertilization.coerceIn(0.0, 1.0),
            ),
            niche = NicheProfile(
                producerCompetitionLayer = when {
                    strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] <= 0.0 ->
                        ProducerCompetitionLayer.NONE
                    CommonTrait.ROOTED_BODY in commonTraits ||
                        CommonTrait.SUBSTRATE_HOLDFAST in commonTraits ->
                        ProducerCompetitionLayer.ATTACHED
                    else -> ProducerCompetitionLayer.SUSPENDED
                },
                photosyntheticColor = definition.photosyntheticColor,
                camouflageColor = definition.camouflageColor,
                habitatSupport = habitatSupport,
                strategySupport = strategySupport,
                camouflage = camouflage,
                nicheFit = nicheFit,
            ),
        )
    }

    private fun compiledSalinityTolerance(): AquaticSalinityTolerance = when {
        broadSalinityTolerance -> AquaticSalinityTolerance.BROAD
        freshwaterAdapted -> AquaticSalinityTolerance.FRESHWATER_ONLY
        else -> AquaticSalinityTolerance.SALTWATER_ONLY
    }

    fun supportHabitat(habitat: Habitat, amount: Double) {
        habitatSupport[habitat.ordinal] += amount
    }

    fun supportStrategy(strategy: EcoStrategy, amount: Double) {
        strategySupport[strategy.ordinal] += amount
    }

    fun shiftTemperature(degreesC: Double) {
        temperatureShift += degreesC
    }
    fun widenTemperatureTolerance(colderC: Double, hotterC: Double) {
        colderTolerance += colderC
        hotterTolerance += hotterC
    }
    fun widenOptimalTemperatureTolerance(colderC: Double, hotterC: Double) {
        colderOptimalTolerance += colderC
        hotterOptimalTolerance += hotterC
    }
    fun requireMinimumActiveTemperature(temperatureC: Double) {
        minimumActiveTemperatureC = max(minimumActiveTemperatureC, temperatureC)
    }
    fun multiplyFrozenDormantSurvival(fraction: Double) {
        frozenDormantSurvival *= fraction
    }
    fun regulateTemperatureWith(strategy: ThermalStrategy) {
        thermalStrategy = strategy
    }
    fun tolerateSeasonalCold(maximumBonusC: Double, triggerInsolation: Double) {
        seasonalColdTolerance += maximumBonusC
        seasonalColdTrigger = max(seasonalColdTrigger, triggerInsolation)
    }
    fun changeWaterRequirement(change: Double) {
        waterRequirement += change
    }
    fun changeMaximumWaterTolerance(optimalChange: Double, absoluteChange: Double) {
        optimalMaximumWater += optimalChange
        maximumWater += absoluteChange
    }
    fun limitWaterDepth(optimalMaximumM: Double, absoluteMaximumM: Double) {
        optimalMaximumWaterDepthM = minOf(optimalMaximumWaterDepthM, optimalMaximumM)
        absoluteMaximumWaterDepthM = minOf(absoluteMaximumWaterDepthM, absoluteMaximumM)
    }
    fun shiftElevationTolerance(meters: Double) {
        elevationToleranceShiftM += meters
    }
    fun hydrateFromSnow() {
        snowHydration = true
    }
    fun shiftInsolationOptimum(change: Double) {
        insolationOptimum += change
    }
    fun changeCanopyLightEfficiency(change: Double) {
        canopyLightEfficiency += change
    }
    fun addDenseCanopyForagingPenalty(penalty: Double) {
        denseCanopyForagingPenalty += penalty
    }
    fun changeCaptureAbility(change: Double) {
        captureAbility += change
    }
    fun changePursuitSpeed(change: Double) {
        pursuitSpeed += change
    }
    fun changeDefense(change: Double) {
        defense += change
    }
    fun addCamouflage(habitat: Habitat, change: Double) {
        camouflage[habitat.ordinal] += change
    }
    fun enableAposematicColoration() {
        aposematicColoration = true
    }
    fun changeReefUse(change: Double) {
        reefUse += change
    }
    fun changeReefBuilding(change: Double) {
        reefBuilding += change
    }
    fun produceFruit(fractionPerSeason: Double) {
        fruitProduction += fractionPerSeason
    }
    fun enableFlowering() {
        flowering = true
    }
    fun produceNectar(fractionPerSeason: Double) {
        nectarProduction += fractionPerSeason
    }
    fun changePollinationEfficiency(change: Double) {
        pollinationEfficiency += change
    }
    fun changeWasteFertilization(change: Double) {
        wasteFertilization += change
    }
    fun changeReserveCapacity(change: Double) {
        reserveCapacity += change
    }
    fun multiplyNicheCompetitionSensitivity(multiplier: Double) {
        nicheCompetitionSensitivity *= multiplier
    }
    fun enterDormancy(kind: DormancyKind, survivalPerSeason: Double) {
        require(dormancyKind == DormancyKind.NONE) {
            "$speciesDisplayName has multiple dormancy modes"
        }
        dormancyKind = kind
        dormantSurvival = survivalPerSeason
    }
    fun multiplyDormantEntryRetention(fraction: Double) {
        dormantEntryBiomassRetention *= fraction
    }
    fun multiplyDormantReactivation(multiplier: Double) {
        dormantReactivationMultiplier *= multiplier
    }
    fun enableDispersal(kind: DispersalKind) {
        if (kind.rangeClass > dispersalKind.rangeClass) dispersalKind = kind
    }
    fun multiplyReproduction(multiplier: Double) {
        reproductionMultiplier *= multiplier
    }
    fun multiplyMetabolicDemand(multiplier: Double) {
        metabolicDemandMultiplier *= multiplier
    }
    fun enableFreshwaterOsmoregulation() {
        freshwaterAdapted = true
    }
    fun enableBroadSalinityTolerance() {
        broadSalinityTolerance = true
    }
    fun enableAquaticRespiration(mode: AquaticRespirationMode) {
        when (mode) {
            AquaticRespirationMode.UNDERWATER -> underwaterBreathing = true
            AquaticRespirationMode.BREATH_HOLDING -> prolongedBreathHolding = true
        }
    }
    fun enablePelagicAerialResidency() {
        pelagicAerialResident = true
    }
    fun adaptToDarkWater() {
        darkWaterAdapted = true
    }
    fun requireResidentHabitat(habitat: Habitat) {
        require(obligateResidentHabitat == null || obligateResidentHabitat == habitat) {
            "$speciesDisplayName requires multiple exclusive resident habitats"
        }
        obligateResidentHabitat = habitat
    }
    fun requireAdjacentLand() {
        requiresAdjacentLand = true
    }
    fun addMaintenanceCost(fraction: Double) {
        maintenanceCost += fraction * maintenanceCostScale
    }

    private companion object {
        const val GENERALIST_MINIMUM_METHOD_SUPPORT = 0.20
        const val GENERALIST_BREADTH_BONUS = 0.05
    }
}