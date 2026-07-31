package dev.biserman.planet.planet.ecology.v2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EcologyCompilerTest {
    @Test
    fun `traits compile into climate and niche parameters`() {
        val producer = producer(
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.PHOTOSYNTHETIC_SURFACE,
                CommonTrait.ROOTED_BODY,
                CommonTrait.DENSE_FUR,
            ),
        )

        val compiled = EcologyCompiler.compile(listOf(producer)).species.single()

        assertEquals(-4.0, compiled.temperatureOuterLow)
        assertEquals(27.0, compiled.temperatureOuterHigh)
        assertTrue(compiled.nicheFit.any { it > 0.0 })
        assertTrue(compiled.maintenanceDemand > 0.0)
    }

    @Test
    fun `size foundation establishes mass and slightly widens temperature range`() {
        val small = EcologyCompiler.compile(listOf(predator("small", SizeClass.SMALL))).species.single()
        val huge = EcologyCompiler.compile(listOf(predator("huge", SizeClass.HUGE))).species.single()

        assertEquals(SizeClass.SMALL.typicalMassKg, small.massKg)
        assertEquals(SizeClass.HUGE.typicalMassKg, huge.massKg)
        assertTrue(huge.temperatureOuterLow < small.temperatureOuterLow)
        assertTrue(huge.temperatureOuterHigh > small.temperatureOuterHigh)
    }

    @Test
    fun `motile species require exactly one thermal strategy`() {
        val invalid = SpeciesDefinition(
            id = "invalid",
            displayName = "Invalid swimmer",
            sizeClass = SizeClass.SMALL,
            motile = true,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.BUOYANCY_BLADDER,
                CommonTrait.GILL_PADS,
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            EcologyCompiler.compile(listOf(invalid))
        }
    }

    @Test
    fun `thermal foundation compiles to an explicit runtime strategy`() {
        val compiled = EcologyCompiler.compile(
            listOf(predator("explicit-thermal-strategy")),
        ).species.single()

        assertEquals(ThermalStrategy.ENDOTHERMY, compiled.thermalStrategy)
    }

    @Test
    fun `motile species cannot use rooted body as a land habitat shortcut`() {
        val invalid = predator("rooted-predator").copy(
            traits = predator("rooted-predator").traits
                .filterNot { it == CommonTrait.TERRESTRIAL_LOCOMOTION } +
                CommonTrait.ROOTED_BODY,
        )

        assertFailsWith<IllegalArgumentException> {
            EcologyCompiler.compile(listOf(invalid))
        }
    }

    @Test
    fun `habitat and strategy jointly derive the strongest niche`() {
        val species = SpeciesDefinition(
            id = "cloud-sieve",
            displayName = "Cloud sieve",
            sizeClass = SizeClass.TINY,
            motile = true,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ECTOTHERMY,
                CommonTrait.POWERED_FLIGHT,
                CommonTrait.GILL_PADS,
            ),
        )
        val ecology = EcologyCompiler.compile(listOf(species))
        val compiled = ecology.species.single()
        val strongest = ecology.niches[compiled.nicheFit.indices.maxBy { compiled.nicheFit[it] }]

        assertEquals(Habitat.AERIAL, strongest.habitat)
        assertEquals(EcoStrategy.FILTER_FEEDING, strongest.strategy)
    }

    @Test
    fun `plant and animal feeding adaptations derive a generalist niche`() {
        val plant = producer("plant")
        val herbivore = SpeciesDefinition(
            id = "herbivore",
            displayName = "herbivore",
            sizeClass = SizeClass.SMALL,
            motile = true,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ENDOTHERMY,
                CommonTrait.TERRESTRIAL_LOCOMOTION,
                CommonTrait.GRAZING_MOUTHPARTS,
            ),
        )
        val omnivore = predator("omnivore").copy(
            traits = predator("omnivore").traits + CommonTrait.GRAZING_MOUTHPARTS,
        )

        val ecology = EcologyCompiler.compile(listOf(plant, herbivore, omnivore))
        val compiled = ecology.species[ecology.speciesIndex("omnivore")]
        val strongest = ecology.niches[compiled.nicheFit.indices.maxBy { compiled.nicheFit[it] }]

        assertEquals(Habitat.LAND_SURFACE, strongest.habitat)
        assertEquals(EcoStrategy.GENERALIST_FORAGING, strongest.strategy)
        assertEquals(
            InteractionKind.GRAZING,
            ecology.interactions.get(compiled.index, ecology.speciesIndex("plant")).kind,
        )
        assertEquals(
            InteractionKind.PREDATION,
            ecology.interactions.get(compiled.index, ecology.speciesIndex("herbivore")).kind,
        )
        assertEquals(
            0.0,
            ecology.species[ecology.speciesIndex("herbivore")]
                .strategySupport[EcoStrategy.GENERALIST_FORAGING.ordinal],
        )
    }

    @Test
    fun `specific food creates only the requested directed edge`() {
        val cucumber = producer("aardvark-cucumber")
        val otherProducer = producer("other-producer")
        val aardvark = predator("aardvark").copy(
            traits = predator("aardvark").traits + TargetedRelationshipTrait(
                displayName = "aardvark-cucumber digestion",
                description = "A digestive specialization that makes one otherwise unusual fruit a useful supplemental food.",
                maintenanceCost = 0.03,
                relationships = listOf(
                    RelationshipEffect.SupplementalFood(
                        target = SpeciesSelector.ExactSpecies("aardvark-cucumber"),
                        attackRate = 0.04,
                        assimilationEfficiency = 0.55,
                    ),
                ),
            ),
        )

        val ecology = EcologyCompiler.compile(listOf(cucumber, otherProducer, aardvark))
        val consumer = ecology.speciesIndex("aardvark")
        val cucumberEdge = ecology.interactions.get(consumer, ecology.speciesIndex("aardvark-cucumber"))
        val otherEdge = ecology.interactions.get(consumer, ecology.speciesIndex("other-producer"))

        assertEquals(InteractionKind.SUPPLEMENTAL_FEEDING, cucumberEdge.kind)
        assertEquals(InteractionKind.NONE, otherEdge.kind)
    }

    @Test
    fun `filter feeders target minuscule motile life and huge or colossal feeders also target tiny life`() {
        val mediumFilter = aquaticFilter("medium-filter", SizeClass.MEDIUM)
        val hugeFilter = aquaticFilter("huge-filter", SizeClass.HUGE)
        val colossalFilter = aquaticFilter("colossal-filter", SizeClass.COLOSSAL)
        val minusculePrey = aquaticPrey("ordinary-minuscule-prey", SizeClass.MINUSCULE)
        val tinyPrey = aquaticPrey("ordinary-tiny-prey", SizeClass.TINY)
        val ecology = EcologyCompiler.compile(
            listOf(
                minusculePrey,
                tinyPrey,
                mediumFilter,
                hugeFilter,
                colossalFilter,
            ),
        )
        val minuscule = ecology.speciesIndex(minusculePrey.id)
        val tiny = ecology.speciesIndex(tinyPrey.id)

        assertEquals(
            InteractionKind.FILTER_FEEDING,
            ecology.interactions.get(ecology.speciesIndex(mediumFilter.id), minuscule).kind,
        )
        assertEquals(
            InteractionKind.NONE,
            ecology.interactions.get(ecology.speciesIndex(mediumFilter.id), tiny).kind,
        )
        assertEquals(
            InteractionKind.FILTER_FEEDING,
            ecology.interactions.get(ecology.speciesIndex(hugeFilter.id), minuscule).kind,
        )
        assertEquals(
            InteractionKind.FILTER_FEEDING,
            ecology.interactions.get(ecology.speciesIndex(hugeFilter.id), tiny).kind,
        )
        assertEquals(
            InteractionKind.FILTER_FEEDING,
            ecology.interactions.get(ecology.speciesIndex(colossalFilter.id), tiny).kind,
        )
    }

    @Test
    fun `huge and colossal aquatic species cannot occupy river habitat`() {
        listOf(SizeClass.HUGE, SizeClass.COLOSSAL).forEach { sizeClass ->
            val base = aquaticFilter("oversized-river-filter-${sizeClass.name}", sizeClass)
            val definition = base.copy(
                traits = base.traits + CommonTrait.EURYHALINE_OSMOREGULATION,
            )
            val ecology = EcologyCompiler.compile(listOf(definition))
            val species = ecology.species.single()
            val river = SeasonalCellEnvironment.create(
                areaKm2 = 40_000.0,
                temperatureC = 20.0,
                insolation = 0.8,
                precipitationMm = 80.0,
                isLand = true,
                adjacentToMajorRiver = true,
            )

            assertEquals(0.0, species.habitatSupport[Habitat.FRESHWATER.ordinal])
            assertEquals(-1, NicheSelection.choose(species, ecology, river))
        }
    }

    @Test
    fun `medium predators do not use tiny aggregate insects as prey`() {
        val mediumPredator = predator("medium-predator", SizeClass.MEDIUM)
        val smallPrey = predator("small-prey", SizeClass.SMALL).copy(
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ENDOTHERMY,
                CommonTrait.TERRESTRIAL_LOCOMOTION,
                CommonTrait.GRAZING_MOUTHPARTS,
            ),
        )
        val ecology = EcologyCompiler.compile(
            listOf(InvariantSpecies.BUGS, smallPrey, mediumPredator),
        )
        val consumer = ecology.speciesIndex(mediumPredator.id)

        assertEquals(
            InteractionKind.NONE,
            ecology.interactions.get(consumer, ecology.speciesIndex(InvariantSpecies.BUGS.id)).kind,
        )
        assertEquals(
            InteractionKind.PREDATION,
            ecology.interactions.get(consumer, ecology.speciesIndex(smallPrey.id)).kind,
        )
    }

    @Test
    fun `medium aquatic predators can use tiny aquatic life without opening that prey to large hunters`() {
        val mediumPredator = aquaticPredator("medium-aquatic-predator", SizeClass.MEDIUM)
        val largePredator = aquaticPredator("large-aquatic-predator", SizeClass.LARGE)
        val ecology = EcologyCompiler.compile(
            listOf(InvariantSpecies.SMALL_AQUATIC_LIFE, mediumPredator, largePredator),
        )
        val prey = ecology.speciesIndex(InvariantSpecies.SMALL_AQUATIC_LIFE.id)

        assertEquals(
            InteractionKind.PREDATION,
            ecology.interactions.get(ecology.speciesIndex(mediumPredator.id), prey).kind,
        )
        assertEquals(
            InteractionKind.NONE,
            ecology.interactions.get(ecology.speciesIndex(largePredator.id), prey).kind,
        )
    }

    @Test
    fun `terrestrial grazers consume the modeled carpet plant population`() {
        val grazer = predator("grazer", SizeClass.SMALL).copy(
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ENDOTHERMY,
                CommonTrait.TERRESTRIAL_LOCOMOTION,
                CommonTrait.GRAZING_MOUTHPARTS,
            ),
        )
        val ecology = EcologyCompiler.compile(listOf(InvariantSpecies.CARPET_PLANTS, grazer))

        assertEquals(
            InteractionKind.GRAZING,
            ecology.interactions.get(
                ecology.speciesIndex(grazer.id),
                ecology.speciesIndex(InvariantSpecies.CARPET_PLANTS.id),
            ).kind,
        )
    }

    @Test
    fun `invariant traits are unavailable to evolving species`() {
        val invalid = predator("invalid-invariant").copy(
            traits = predator("invalid-invariant").traits + CommonTrait.INVARIANT_RESISTANCE,
        )

        assertFailsWith<IllegalArgumentException> {
            EcologyCompiler.compile(listOf(invalid))
        }
    }

    @Test
    fun `invariant guilds compile as ordinary populations with explicit metadata`() {
        val ecology = EcologyCompiler.compile(InvariantSpecies.ALL)

        assertEquals(5, ecology.species.size)
        assertTrue(ecology.species.all { it.kind == SpeciesKind.INVARIANT })
        assertTrue(ecology.species.all { it.dormancyKind == DormancyKind.PROPAGULE })
        assertTrue(ecology.species.all { it.nicheCompetitionSensitivity < 0.20 })
        assertEquals(
            0.10,
            ecology.species.single { it.id == InvariantSpecies.PLANKTON.id }
                .dormantEntryBiomassRetention,
        )
        assertEquals(
            10.0,
            ecology.species.single { it.id == InvariantSpecies.PLANKTON.id }
                .dormantReactivationMultiplier,
        )
        assertTrue(
            ecology.species
                .filterNot { it.id == InvariantSpecies.PLANKTON.id }
                .all { it.dormantEntryBiomassRetention == 1.0 },
        )
    }

    @Test
    fun `all common non-foundation traits declare a benefit and a cost`() {
        CommonTrait.entries.forEach { trait ->
            assertTrue(
                trait.description.isNotBlank(),
                "${trait.displayName} has no player-facing description",
            )
        }
        CommonTrait.entries.filterNot { it.isFoundation }.forEach { trait ->
            assertTrue(
                trait.effects.any { it is TraitEffect.MaintenanceCost && it.fraction > 0.0 },
                "${trait.displayName} has no explicit cost",
            )
            assertTrue(
                trait.effects.any { it !is TraitEffect.MaintenanceCost },
                "${trait.displayName} has no benefit",
            )
        }
    }

    private fun producer(
        id: String = "producer",
        traits: List<SpeciesTrait> = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
        ),
    ) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = SizeClass.SMALL,
        motile = false,
        traits = traits,
        photosyntheticColor = BiologicalColor.GREEN,
    )

    private fun predator(
        id: String,
        sizeClass: SizeClass = SizeClass.MEDIUM,
    ) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = sizeClass,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
        ),
        camouflageColor = BiologicalColor.BROWN,
    )

    private fun aquaticFilter(
        id: String,
        sizeClass: SizeClass,
    ) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = sizeClass,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.BALEEN,
        ),
    )

    private fun aquaticPrey(
        id: String,
        sizeClass: SizeClass,
    ) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = sizeClass,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.BUOYANCY_BLADDER,
        ),
    )

    private fun aquaticPredator(
        id: String,
        sizeClass: SizeClass,
    ) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = sizeClass,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.AMBUSH_MUSCULATURE,
        ),
    )
}
