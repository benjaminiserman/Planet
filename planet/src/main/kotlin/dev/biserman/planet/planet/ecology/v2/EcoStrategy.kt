package dev.biserman.planet.planet.ecology.v2

import kotlin.math.sqrt

enum class EcoStrategy(
    val displayName: String,
    val supportedHabitats: Set<Habitat>,
) {
    PHOTOSYNTHESIS(
        "photosynthesis",
        setOf(
            Habitat.LAND_SURFACE,
            Habitat.CANOPY,
            Habitat.AERIAL,
            Habitat.FRESHWATER,
            Habitat.COASTAL,
            Habitat.SUNLIT_WATER,
        ),
    ),
    FILTER_FEEDING(
        "filter-feeding",
        setOf(Habitat.FRESHWATER, Habitat.COASTAL, Habitat.SUNLIT_WATER, Habitat.DARK_WATER, Habitat.AERIAL),
    ),
    GRAZING(
        "grazing",
        setOf(Habitat.LAND_SURFACE, Habitat.CANOPY, Habitat.FRESHWATER, Habitat.COASTAL, Habitat.SUNLIT_WATER),
    ),
    GENERALIST_FORAGING(
        "generalist-foraging",
        setOf(Habitat.LAND_SURFACE, Habitat.CANOPY, Habitat.FRESHWATER, Habitat.COASTAL, Habitat.SUNLIT_WATER),
    ),
    AMBUSH_PREDATION(
        "ambush-predation",
        setOf(
            Habitat.LAND_SURFACE,
            Habitat.CANOPY,
            Habitat.AERIAL,
            Habitat.FRESHWATER,
            Habitat.COASTAL,
            Habitat.SUNLIT_WATER,
            Habitat.DARK_WATER,
        ),
    ),
    PURSUIT_PREDATION(
        "pursuit-predation",
        setOf(
            Habitat.LAND_SURFACE,
            Habitat.AERIAL,
            Habitat.FRESHWATER,
            Habitat.COASTAL,
            Habitat.SUNLIT_WATER,
            Habitat.DARK_WATER,
        ),
    ),
    SCAVENGING(
        "scavenging",
        setOf(Habitat.LAND_SURFACE, Habitat.COASTAL, Habitat.SUNLIT_WATER, Habitat.DARK_WATER, Habitat.AERIAL),
    ),
    DECOMPOSITION(
        "decomposition",
        setOf(
            Habitat.LAND_SURFACE,
            Habitat.CANOPY,
            Habitat.FRESHWATER,
            Habitat.COASTAL,
            Habitat.SUNLIT_WATER,
            Habitat.DARK_WATER,
        ),
    ),
    COPROPHAGY(
        "coprophagy",
        setOf(Habitat.LAND_SURFACE, Habitat.FRESHWATER, Habitat.COASTAL, Habitat.SUNLIT_WATER),
    ),
    DEPOSIT_FEEDING(
        "deposit-feeding",
        setOf(Habitat.COASTAL, Habitat.SUNLIT_WATER, Habitat.DARK_WATER),
    ),
    PARASITISM("parasitism", Habitat.entries.toSet()),
    ABSORPTION(
        "absorption",
        setOf(Habitat.LAND_SURFACE, Habitat.FRESHWATER, Habitat.COASTAL, Habitat.DARK_WATER),
    );

    fun resourceSupport(
        environment: SeasonalCellEnvironment,
        habitat: Habitat,
        consumerSize: SizeClass,
    ): Double = when (this) {
        PHOTOSYNTHESIS ->
            environment.fertility * environment.lightAt(habitat)
        // These strategies obtain food from modeled populations through the
        // precompiled interaction matrix, not from background resource pools.
        FILTER_FEEDING -> 0.0
        GRAZING -> 0.0
        // This niche groups omnivores for competition; their food still comes
        // from the ordinary grazing and predation interaction edges.
        GENERALIST_FORAGING -> 0.0
        AMBUSH_PREDATION, PURSUIT_PREDATION -> 0.0
        // Carcasses are spatially concentrated rather than evenly diluted
        // across a tile. Long-range scavengers can still locate useful patches
        // when the tile-wide carrion index is low.
        SCAVENGING -> sqrt(environment.resources.carrion)
        DECOMPOSITION -> environment.resources.detritus
        COPROPHAGY -> environment.resources.waste
        DEPOSIT_FEEDING -> environment.resources.marineSnow
        PARASITISM -> 0.15
        ABSORPTION -> environment.fertility * environment.waterAvailability
    }.coerceIn(0.0, 1.0)

}
