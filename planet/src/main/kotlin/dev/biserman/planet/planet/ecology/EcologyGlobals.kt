package dev.biserman.planet.planet.ecology

/**
 * Reloadable ecology tuning values.
 *
 * These are authoring globals rather than per-planet state. The refresh-config
 * action reads them from ecology_config.json, and newly constructed runtimes
 * take a validated snapshot through [EcologyRuntimeConfig].
 */
@Suppress("MayBeConstant")
object EcologyGlobals {
    /** Spectrum emitted by the planet's star and used by photosynthetic pigments. */
    var starLight = StarLight.YELLOW

    /** Fraction of every active population lost each season even under ideal conditions. */
    var backgroundMortality = 0.012

    /** Maximum seasonal mortality caused by environmental stress; actual loss scales with stress⁴. */
    var stressMortality = 0.42

    /** Active biomass lost in one season beyond the species' lethal temperature boundary. */
    var lethalTemperatureMortality = 0.90

    /** Lowest elevation at which ordinary ground-dwelling motile species retain full fitness. */
    var normalMinimumElevationM = 0.0

    /** Elevation at which ordinary ground-dwelling motile species reach zero fitness below their optimal band. */
    var lethalMinimumElevationM = -1_000.0

    /** Highest elevation at which ordinary ground-dwelling motile species retain full fitness. */
    var normalElevationLimitM = 2_000.0

    /** Elevation at which ordinary ground-dwelling motile species reach zero fitness. */
    var lethalElevationLimitM = 3_000.0

    /** Maximum fraction of active biomass that starvation can remove in one season. */
    var maximumStarvationMortality = 0.72

    /** Strength of competition from other species occupying the same authored niche. */
    var interspecificNicheCompetition = 0.15

    /** Maximum fraction of a prey population that all consumers may remove in one season. */
    var maximumConsumedBiomassFraction = 0.75

    /** Fraction of consumed nectar biomass that becomes usable consumer energy. */
    var nectarAssimilationEfficiency = 0.68

    /** Maximum share of flowering producer biomass added as seasonal growth by complete pollination service. */
    var maximumPollinationBenefitFraction = 0.04

    /** Exponent relating terrestrial water availability to producer carrying capacity. */
    var terrestrialProducerWaterExponent = 1.25

    /** Minimum productivity retained by extremely drought-adapted terrestrial producers. */
    var minimumTerrestrialProducerWaterProductivity = 0.03

    /** Carrion from SMALL-or-larger animals per km² represented by a resource level of one. */
    var carrionFullLevelBiomassKgKm2 = 100.0

    /** Sessile detritus biomass per km² represented by a resource level of one. */
    var detritusFullLevelBiomassKgKm2 = 100_000.0

    /** Waste from TINY-or-larger animals per km² represented by a resource level of one. */
    var wasteFullLevelBiomassKgKm2 = 100.0

    /** Marine-snow biomass per km² represented by a resource level of one. */
    var marineSnowFullLevelBiomassKgKm2 = 50_000.0

    /** Fitness below which a dormancy-capable species begins becoming dormant. */
    var dormantEntryFitness = 0.35

    /** Fitness above which dormant biomass begins returning to active biomass. */
    var dormantExitFitness = 0.58

    /** Fraction of active biomass moved into dormancy during an unfavorable season. */
    var dormantEntryFraction = 0.88

    /** Fraction of dormant biomass reactivated during a favorable season. */
    var dormantExitFraction = 0.55

    /** Minimum number of species slots available in any habitat that exists on a tile. */
    var habitatDiversityBaseSlots = 3.0

    /** Additional species slots granted in proportion to that habitat's tile availability. */
    var habitatDiversityAvailabilitySlots = 7.0

    /** Maximum seasonal mortality caused by exceeding a habitat's soft species capacity. */
    var maximumHabitatDiversityMortality = 0.90

    /** Share of diversity pressure also paid by the habitat's strongest competitor. */
    var strongestCompetitorMortalityFraction = 0.20

    /** Populations below this approximate number of individuals become locally extinct. */
    var minimumViableIndividuals = 2.0

    /** Seasonal chance that a species without a dispersal trait attempts to colonize a neighbor. */
    var unassistedRadiationChancePerSeason = 0.002

    /** Seasonal neighboring-colonization chance for species with a migration trait. */
    var migrationRadiationChancePerSeason = 0.06

    /** Seasonal neighboring-colonization chance for species specialized for local dispersal. */
    var neighborRadiationChancePerSeason = 0.012

    /**
     * Lowest intrinsic niche fit a colonist may choose, expressed as a fraction
     * of its best available niche fit on the destination tile.
     */
    var minimumRelativeRadiationNicheFit = 0.80

    /**
     * Multiplier applied to soft habitat capacity when accepting colonists.
     * Values above one allow competitive displacement, rather than insertion
     * order, to determine which species survive.
     */
    var establishmentCapacityMultiplier = 1.20

    /** Predation multiplier applied to prey displaying an aposematic warning signal. */
    var aposematicPredationMultiplier = 0.20

    /** Cold protection, in °C, provided by dormant leaves and buds. */
    var dormantLeafColdProtectionC = 20.0

    /** Cold buffer, in °C, before seasonal torpor becomes vulnerable to lethal cold. */
    var seasonalTorporColdBufferC = 5.0

    /** Temperature span, in °C, over which seasonal torpor cold mortality ramps. */
    var seasonalTorporLethalRampC = 10.0

    fun validate() {
        require(backgroundMortality in 0.0..1.0)
        require(stressMortality in 0.0..1.0)
        require(lethalTemperatureMortality in 0.0..1.0)
        require(lethalMinimumElevationM < normalMinimumElevationM)
        require(normalMinimumElevationM <= normalElevationLimitM)
        require(lethalElevationLimitM > normalElevationLimitM)
        require(maximumStarvationMortality in 0.0..1.0)
        require(interspecificNicheCompetition >= 0.0)
        require(maximumConsumedBiomassFraction in 0.0..1.0)
        require(nectarAssimilationEfficiency in 0.0..1.0)
        require(maximumPollinationBenefitFraction in 0.0..1.0)
        require(terrestrialProducerWaterExponent > 0.0)
        require(minimumTerrestrialProducerWaterProductivity in 0.0..1.0)
        require(carrionFullLevelBiomassKgKm2 > 0.0)
        require(detritusFullLevelBiomassKgKm2 > 0.0)
        require(wasteFullLevelBiomassKgKm2 > 0.0)
        require(marineSnowFullLevelBiomassKgKm2 > 0.0)
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
        require(establishmentCapacityMultiplier >= 1.0)
        require(aposematicPredationMultiplier >= 0.0)
        require(dormantLeafColdProtectionC >= 0.0)
        require(seasonalTorporColdBufferC >= 0.0)
        require(seasonalTorporLethalRampC > 0.0)
    }
}