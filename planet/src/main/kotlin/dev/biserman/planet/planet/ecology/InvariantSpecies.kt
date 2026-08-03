package dev.biserman.planet.planet.ecology

/**
 * Globally authored aggregate guilds used where the simulation does not model
 * their many constituent lineages separately. Their invariant status prevents
 * them from evolving; it does not exempt them from ordinary ecology.
 */
object InvariantSpecies {
    val CARPET_PLANTS = SpeciesDefinition(
        id = "invariant-carpet-plants",
        displayName = "Carpet plants",
        sizeClass = SizeClass.TINY,
        motile = false,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.INVARIANT_RESISTANCE,
            CommonTrait.THAW_DEPENDENT_GROWTH,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.FLOWERS,
            CommonTrait.NECTARIES,
        ),
        photosyntheticColor = BiologicalColor.GREEN,
        kind = SpeciesKind.INVARIANT,
    )

    val BUGS = SpeciesDefinition(
        id = "invariant-bugs",
        displayName = "Bugs",
        sizeClass = SizeClass.TINY,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.INVARIANT_RESISTANCE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
        ),
        kind = SpeciesKind.INVARIANT,
    )

    val SMALL_AQUATIC_LIFE = SpeciesDefinition(
        id = "invariant-small-aquatic-life",
        displayName = "Small aquatic life",
        sizeClass = SizeClass.TINY,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.INVARIANT_RESISTANCE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.GILL_PADS,
        ),
        kind = SpeciesKind.INVARIANT,
    )

    val PLANKTON = SpeciesDefinition(
        id = "invariant-plankton",
        displayName = "Plankton",
        sizeClass = SizeClass.MINUSCULE,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.INVARIANT_RESISTANCE,
            CommonTrait.MICROSCOPIC_RESTING_STAGES,
            CommonTrait.ECTOTHERMY,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.DIFFUSIVE_AQUATIC_GAS_EXCHANGE,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
        ),
        photosyntheticColor = BiologicalColor.GREEN,
        kind = SpeciesKind.INVARIANT,
    )

    val AEROPLANKTON = SpeciesDefinition(
        id = "invariant-aeroplankton",
        displayName = "Aeroplankton",
        sizeClass = SizeClass.MINUSCULE,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.INVARIANT_RESISTANCE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AIRBORNE_PHOTOSYNTHETIC_SURFACE,
        ),
        photosyntheticColor = BiologicalColor.GREEN,
        kind = SpeciesKind.INVARIANT,
    )

    val ALL: List<SpeciesDefinition> =
        listOf(CARPET_PLANTS, BUGS, SMALL_AQUATIC_LIFE, PLANKTON, AEROPLANKTON)
}