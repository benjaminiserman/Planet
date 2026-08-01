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
    internal val habitatSupport = DoubleArray(Habitat.entries.size)
    internal val strategySupport = DoubleArray(EcoStrategy.entries.size)
    internal val camouflage = DoubleArray(Habitat.entries.size)
    internal var temperatureShift = 0.0
    internal var colderTolerance = sizeTemperatureTolerance
    internal var hotterTolerance = sizeTemperatureTolerance
    internal var colderOptimalTolerance = 0.0
    internal var hotterOptimalTolerance = 0.0
    internal var minimumActiveTemperatureC = Double.NEGATIVE_INFINITY
    internal var frozenDormantSurvival = 1.0
    internal var seasonalColdTolerance = 0.0
    internal var seasonalColdTrigger = 0.0
    internal var thermalStrategy: ThermalStrategy? = null
    internal var waterRequirement = 0.25
    internal var optimalMaximumWater = 1.0
    internal var maximumWater = 1.0
    internal var optimalMaximumWaterDepthM = Double.POSITIVE_INFINITY
    internal var absoluteMaximumWaterDepthM = Double.POSITIVE_INFINITY
    internal var elevationToleranceShiftM = 0.0
    internal var snowHydration = false
    internal var insolationOptimum = 0.8
    internal var canopyLightEfficiency = 0.0
    internal var denseCanopyForagingPenalty = 0.0
    internal var reserveCapacity = 0.25
    internal var nicheCompetitionSensitivity = 1.0
    internal var dormancyKind = DormancyKind.NONE
    internal var dormantSurvival = 0.0
    internal var dormantEntryBiomassRetention = 1.0
    internal var dormantReactivationMultiplier = 1.0
    internal var dispersalKind = DispersalKind.NONE
    internal var reproductionMultiplier = 1.0
    internal var metabolicDemandMultiplier = 1.0
    internal var maintenanceCost = 0.0
    internal var captureAbility = 0.5
    internal var pursuitSpeed = 0.0
    internal var defense = 0.25
    internal var aposematicColoration = false
    internal var reefUse = 0.0
    internal var reefBuilding = 0.0
    internal var fruitProduction = 0.0
    internal var flowering = false
    internal var nectarProduction = 0.0
    internal var pollinationEfficiency = 0.0
    internal var wasteFertilization = 0.0
    internal var pelagicAerialResident = false
    internal var darkWaterAdapted = false
    internal var freshwaterAdapted = false
    internal var broadSalinityTolerance = false
    internal var underwaterBreathing = false
    internal var prolongedBreathHolding = false
    internal var obligateResidentHabitat: Habitat? = null
    internal var requiresAdjacentLand = false

    private var maintenanceCostScale = 1.0

    internal fun apply(trait: SpeciesTrait) {
        maintenanceCostScale = if (trait.isFoundation) 1.0 else 0.35
        trait.effects.forEach { it.applyTo(this) }
        maintenanceCostScale = 1.0
    }

    fun supportHabitat(habitat: Habitat, amount: Double) {
        habitatSupport[habitat.ordinal] += amount
    }

    fun supportStrategy(strategy: EcoStrategy, amount: Double) {
        strategySupport[strategy.ordinal] += amount
    }

    fun shiftTemperature(degreesC: Double) { temperatureShift += degreesC }
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
    fun multiplyFrozenDormantSurvival(fraction: Double) { frozenDormantSurvival *= fraction }
    fun regulateTemperatureWith(strategy: ThermalStrategy) { thermalStrategy = strategy }
    fun tolerateSeasonalCold(maximumBonusC: Double, triggerInsolation: Double) {
        seasonalColdTolerance += maximumBonusC
        seasonalColdTrigger = max(seasonalColdTrigger, triggerInsolation)
    }
    fun changeWaterRequirement(change: Double) { waterRequirement += change }
    fun changeMaximumWaterTolerance(optimalChange: Double, absoluteChange: Double) {
        optimalMaximumWater += optimalChange
        maximumWater += absoluteChange
    }
    fun limitWaterDepth(optimalMaximumM: Double, absoluteMaximumM: Double) {
        optimalMaximumWaterDepthM = minOf(optimalMaximumWaterDepthM, optimalMaximumM)
        absoluteMaximumWaterDepthM = minOf(absoluteMaximumWaterDepthM, absoluteMaximumM)
    }
    fun shiftElevationTolerance(meters: Double) { elevationToleranceShiftM += meters }
    fun hydrateFromSnow() { snowHydration = true }
    fun shiftInsolationOptimum(change: Double) { insolationOptimum += change }
    fun changeCanopyLightEfficiency(change: Double) { canopyLightEfficiency += change }
    fun addDenseCanopyForagingPenalty(penalty: Double) { denseCanopyForagingPenalty += penalty }
    fun changeCaptureAbility(change: Double) { captureAbility += change }
    fun changePursuitSpeed(change: Double) { pursuitSpeed += change }
    fun changeDefense(change: Double) { defense += change }
    fun addCamouflage(habitat: Habitat, change: Double) { camouflage[habitat.ordinal] += change }
    fun enableAposematicColoration() { aposematicColoration = true }
    fun changeReefUse(change: Double) { reefUse += change }
    fun changeReefBuilding(change: Double) { reefBuilding += change }
    fun produceFruit(fractionPerSeason: Double) { fruitProduction += fractionPerSeason }
    fun enableFlowering() { flowering = true }
    fun produceNectar(fractionPerSeason: Double) { nectarProduction += fractionPerSeason }
    fun changePollinationEfficiency(change: Double) { pollinationEfficiency += change }
    fun changeWasteFertilization(change: Double) { wasteFertilization += change }
    fun changeReserveCapacity(change: Double) { reserveCapacity += change }
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
    fun multiplyDormantEntryRetention(fraction: Double) { dormantEntryBiomassRetention *= fraction }
    fun multiplyDormantReactivation(multiplier: Double) { dormantReactivationMultiplier *= multiplier }
    fun enableDispersal(kind: DispersalKind) {
        if (kind.rangeClass > dispersalKind.rangeClass) dispersalKind = kind
    }
    fun multiplyReproduction(multiplier: Double) { reproductionMultiplier *= multiplier }
    fun multiplyMetabolicDemand(multiplier: Double) { metabolicDemandMultiplier *= multiplier }
    fun enableFreshwaterOsmoregulation() { freshwaterAdapted = true }
    fun enableBroadSalinityTolerance() { broadSalinityTolerance = true }
    fun enableAquaticRespiration(mode: AquaticRespirationMode) {
        when (mode) {
            AquaticRespirationMode.UNDERWATER -> underwaterBreathing = true
            AquaticRespirationMode.BREATH_HOLDING -> prolongedBreathHolding = true
        }
    }
    fun enablePelagicAerialResidency() { pelagicAerialResident = true }
    fun adaptToDarkWater() { darkWaterAdapted = true }
    fun requireResidentHabitat(habitat: Habitat) {
        require(obligateResidentHabitat == null || obligateResidentHabitat == habitat) {
            "$speciesDisplayName requires multiple exclusive resident habitats"
        }
        obligateResidentHabitat = habitat
    }
    fun requireAdjacentLand() { requiresAdjacentLand = true }
    fun addMaintenanceCost(fraction: Double) { maintenanceCost += fraction * maintenanceCostScale }
}
