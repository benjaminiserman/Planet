package dev.biserman.planet.planet.ecology.v2

/**
 * Reloadable ecology tuning values.
 *
 * These are authoring globals rather than per-planet state. The refresh-config
 * action reads them from ecology_config.json, and newly constructed runtimes
 * take a validated snapshot through [EcologyRuntimeConfig].
 */
@Suppress("MayBeConstant")
object EcologyGlobals {
    /** Fraction of every active population lost each season even under ideal conditions. */
    var backgroundMortality = 0.012

    /** Maximum seasonal mortality caused by environmental stress; actual loss scales with stress⁴. */
    var stressMortality = 0.42

    /** Active biomass lost in one season beyond the species' lethal temperature boundary. */
    var lethalTemperatureMortality = 0.90

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

    fun validate() {
        require(backgroundMortality in 0.0..1.0)
        require(stressMortality in 0.0..1.0)
        require(lethalTemperatureMortality in 0.0..1.0)
        require(normalElevationLimitM >= 0.0)
        require(lethalElevationLimitM > normalElevationLimitM)
        require(maximumStarvationMortality in 0.0..1.0)
        require(interspecificNicheCompetition >= 0.0)
        require(maximumConsumedBiomassFraction in 0.0..1.0)
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
    }
}
