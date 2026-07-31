package dev.biserman.planet.planet.ecology.v2

/**
 * Broad standing-live-biomass scales, expressed per square kilometre before
 * local habitat and productivity modifiers.
 *
 * Terrestrial producer biomass rises strongly with body size because large
 * sessile organisms retain years or centuries of structural tissue. Aquatic
 * producer standing stock is lower because plankton and algae turn over much
 * faster. These are deliberately order-of-magnitude anchors, not biome-specific
 * empirical fits.
 */
object EcologyBiomass {
    val terrestrialProducerDensityKgKm2: Map<SizeClass, Double> = mapOf(
        SizeClass.MINUSCULE to 100_000.0,
        SizeClass.TINY to 500_000.0,
        SizeClass.SMALL to 2_000_000.0,
        SizeClass.MEDIUM to 5_000_000.0,
        SizeClass.LARGE to 12_000_000.0,
        SizeClass.HUGE to 25_000_000.0,
        SizeClass.COLOSSAL to 40_000_000.0,
    )

    val aquaticProducerDensityKgKm2: Map<SizeClass, Double> = mapOf(
        SizeClass.MINUSCULE to 1_000_000.0,
        SizeClass.TINY to 100_000.0,
        SizeClass.SMALL to 300_000.0,
        SizeClass.MEDIUM to 800_000.0,
        SizeClass.LARGE to 2_000_000.0,
        SizeClass.HUGE to 5_000_000.0,
        SizeClass.COLOSSAL to 8_000_000.0,
    )

    /**
     * Fraction of standing producer tissue exposed to ordinary grazing.
     * Increasingly large terrestrial producers store most of their mass in
     * trunks, roots, and other long-lived structure rather than edible growth.
     */
    val terrestrialGrazingAccessibilityBySize: Map<SizeClass, Double> = mapOf(
        SizeClass.MINUSCULE to 0.80,
        SizeClass.TINY to 0.40,
        SizeClass.SMALL to 0.20,
        SizeClass.MEDIUM to 0.08,
        SizeClass.LARGE to 0.03,
        SizeClass.HUGE to 0.01,
        SizeClass.COLOSSAL to 0.005,
    )

    val filterFeedingEfficiencyBySize: Map<SizeClass, Double> = mapOf(
        SizeClass.MINUSCULE to 0.55,
        SizeClass.TINY to 0.50,
        SizeClass.SMALL to 0.25,
        SizeClass.MEDIUM to 0.42,
        SizeClass.LARGE to 0.20,
        SizeClass.HUGE to 0.103,
        SizeClass.COLOSSAL to 0.08,
    )

    val aquaticFilterFeederProducerFractionBySize: Map<SizeClass, Double> = mapOf(
        SizeClass.MINUSCULE to 0.10,
        SizeClass.TINY to 0.05,
        SizeClass.SMALL to 0.02,
        SizeClass.MEDIUM to 0.01,
        SizeClass.LARGE to 0.003,
        SizeClass.HUGE to 0.0005,
        SizeClass.COLOSSAL to 0.00025,
    )

    private val terrestrialProducerDensity =
        terrestrialProducerDensityKgKm2.toSizeClassArray()
    private val aquaticProducerDensity =
        aquaticProducerDensityKgKm2.toSizeClassArray()
    private val terrestrialGrazingAccessibility =
        terrestrialGrazingAccessibilityBySize.toSizeClassArray()
    private val filterFeedingEfficiency =
        filterFeedingEfficiencyBySize.toSizeClassArray()
    private val aquaticFilterFeederProducerFraction =
        aquaticFilterFeederProducerFractionBySize.toSizeClassArray()

    fun carryingCapacityKg(
        species: CompiledSpecies,
        niche: NicheDefinition,
        environment: SeasonalCellEnvironment,
    ): Double {
        val habitat = environment.habitatAvailability(niche.habitat).coerceAtLeast(0.02)
        val photosynthetic =
            species.strategySupport[EcoStrategy.PHOTOSYNTHESIS.ordinal] > 0.0
        val aquaticFilterFeeder =
            niche.strategy == EcoStrategy.FILTER_FEEDING &&
                niche.habitat in EcologyFitness.aquaticHabitats
        val resource = when {
            aquaticFilterFeeder ->
                (environment.fertility * environment.lightAt(niche.habitat)).coerceAtLeast(0.04)
            niche.strategy.foodComesFromModeledPopulations -> 1.0
            else ->
                environment.resourceSupport(niche, species.sizeClass).coerceAtLeast(0.04)
        }
        val density = if (photosynthetic) {
            when {
                niche.habitat == Habitat.AERIAL ->
                    220.0 * species.sizeClass.densityScale
                niche.habitat in EcologyFitness.aquaticHabitats ->
                    aquaticProducerDensity[species.sizeClass.ordinal]
                else ->
                    terrestrialProducerDensity[species.sizeClass.ordinal]
            }
        } else if (aquaticFilterFeeder) {
            aquaticProducerDensity[SizeClass.MINUSCULE.ordinal] *
                aquaticFilterFeederProducerFraction[species.sizeClass.ordinal]
        } else {
            220.0 * species.sizeClass.densityScale
        }
        val fertility =
            if (photosynthetic) 1.0
            else environment.fertility
        return environment.areaKm2 * density * fertility * habitat * resource
    }

    fun grazingAccessibility(species: CompiledSpecies): Double {
        val terrestrialSupport = maxOf(
            species.habitatSupport[Habitat.LAND_SURFACE.ordinal],
            species.habitatSupport[Habitat.CANOPY.ordinal],
        )
        val aquaticSupport = EcologyFitness.aquaticHabitats.maxOf { habitat ->
            species.habitatSupport[habitat.ordinal]
        }
        return if (terrestrialSupport >= aquaticSupport) {
            terrestrialGrazingAccessibility[species.sizeClass.ordinal]
        } else {
            0.70
        }
    }

    fun filterFeedingEfficiency(sizeClass: SizeClass): Double =
        filterFeedingEfficiency[sizeClass.ordinal]

    private fun Map<SizeClass, Double>.toSizeClassArray(): DoubleArray {
        require(keys == SizeClass.entries.toSet())
        return DoubleArray(SizeClass.entries.size) { ordinal ->
            getValue(SizeClass.entries[ordinal])
        }
    }
}
