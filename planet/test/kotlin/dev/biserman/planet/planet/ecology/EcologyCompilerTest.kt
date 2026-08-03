package dev.biserman.planet.planet.ecology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EcologyCompilerTest {
    @Test
    fun `compiled niche profile owns its optimized arrays`() {
        val habitatSupport = DoubleArray(Habitat.entries.size).also {
            it[Habitat.LAND_SURFACE.ordinal] = 0.75
        }
        val strategySupport = DoubleArray(EcoStrategy.entries.size).also {
            it[EcoStrategy.GRAZING.ordinal] = 0.60
        }
        val camouflage = DoubleArray(Habitat.entries.size).also {
            it[Habitat.LAND_SURFACE.ordinal] = 0.40
        }
        val nicheFit = doubleArrayOf(0.45)
        val profile = NicheProfile(
            producerCompetitionLayer = ProducerCompetitionLayer.NONE,
            photosyntheticColor = null,
            camouflageColor = BiologicalColor.BROWN,
            habitatSupport = habitatSupport,
            strategySupport = strategySupport,
            camouflage = camouflage,
            nicheFit = nicheFit,
        )

        habitatSupport[Habitat.LAND_SURFACE.ordinal] = 0.0
        strategySupport[EcoStrategy.GRAZING.ordinal] = 0.0
        camouflage[Habitat.LAND_SURFACE.ordinal] = 0.0
        nicheFit[0] = 0.0

        assertEquals(0.75, profile.supportFor(Habitat.LAND_SURFACE))
        assertEquals(0.60, profile.supportFor(EcoStrategy.GRAZING))
        assertEquals(0.40, profile.camouflageFor(Habitat.LAND_SURFACE))
        assertEquals(0.45, profile.fitFor(0))
    }

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

        assertEquals(-4.0, compiled.physiology.thermal.outerLowC)
        assertEquals(27.0, compiled.physiology.thermal.outerHighC)
        assertTrue(compiled.niche.hasViableNiche())
        assertTrue(compiled.physiology.maintenanceDemand > 0.0)
    }

    @Test
    fun `size foundation establishes mass and slightly widens temperature range`() {
        val small = EcologyCompiler.compile(listOf(predator("small", SizeClass.SMALL))).species.single()
        val huge = EcologyCompiler.compile(listOf(predator("huge", SizeClass.HUGE))).species.single()

        assertEquals(SizeClass.SMALL.typicalMassKg, small.physiology.massKg)
        assertEquals(SizeClass.HUGE.typicalMassKg, huge.physiology.massKg)
        assertTrue(huge.physiology.thermal.outerLowC < small.physiology.thermal.outerLowC)
        assertTrue(huge.physiology.thermal.outerHighC > small.physiology.thermal.outerHighC)
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

        assertEquals(ThermalStrategy.ENDOTHERMY, compiled.physiology.thermal.regulation)
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
        val strongest = ecology.niches[compiled.niche.bestNicheIndex()]

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
        val strongest = ecology.niches[compiled.niche.bestNicheIndex()]

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
                .niche.supportFor(EcoStrategy.GENERALIST_FORAGING),
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
    fun `authored relationship effects compose on one interaction edge`() {
        val producer = producer("flowering-producer")
        val consumer = predator("specialist").copy(
            traits = predator("specialist").traits + TargetedRelationshipTrait(
                displayName = "specialist relationship",
                description = "Multiple authored effects on the same target.",
                maintenanceCost = 0.03,
                relationships = listOf(
                    RelationshipEffect.BenefitsTargetWhenFeeding(
                        SpeciesSelector.ExactSpecies(producer.id),
                        benefitRate = 0.07,
                    ),
                    RelationshipEffect.RequiresTarget(
                        SpeciesSelector.ExactSpecies(producer.id),
                    ),
                    RelationshipEffect.SupplementalFood(
                        SpeciesSelector.ExactSpecies(producer.id),
                        attackRate = 0.04,
                        assimilationEfficiency = 0.55,
                    ),
                ),
            ),
        )

        val ecology = EcologyCompiler.compile(listOf(producer, consumer))
        val interaction = ecology.interactions.get(
            ecology.speciesIndex(consumer.id),
            ecology.speciesIndex(producer.id),
        )

        assertEquals(InteractionKind.SUPPLEMENTAL_FEEDING, interaction.kind)
        assertEquals(0.07, interaction.targetBenefitRate)
        assertTrue(interaction.targetRequired)
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
                adjacentToMajorRiver = 1.0,
            )

            assertEquals(0.0, species.niche.supportFor(Habitat.FRESHWATER))
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
    fun `separated marine predator tiers are not treated as intraguild competitors`() {
        val definitions = listOf("antarctic-silverfish", "harbor-seal", "orca").map { id ->
            EarthSpeciesCatalog.ALL.single { it.id == id }
        }
        val ecology = EcologyCompiler.compile(definitions)
        val orca = ecology.species.single { it.id == "orca" }
        val seal = ecology.species.single { it.id == "harbor-seal" }
        val silverfish = ecology.species.single { it.id == "antarctic-silverfish" }

        fun undiscountedAttack(consumer: CompiledSpecies, target: CompiledSpecies): Double {
            val support = maxOf(
                consumer.niche.supportFor(EcoStrategy.AMBUSH_PREDATION),
                consumer.niche.supportFor(EcoStrategy.PURSUIT_PREDATION),
            )
            val pursuit =
                consumer.niche.supportFor(EcoStrategy.PURSUIT_PREDATION) >
                    consumer.niche.supportFor(EcoStrategy.AMBUSH_PREDATION)
            val capture = consumer.interactions.captureAbility + if (pursuit) consumer.interactions.pursuitSpeed else 0.0
            val defense = target.interactions.defense + if (pursuit) target.interactions.pursuitSpeed else 0.0
            return (0.07 * support * capture / maxOf(0.25, defense)).coerceIn(0.0, 0.25)
        }

        listOf(orca to seal, seal to silverfish).forEach { (consumer, target) ->
            val interaction = ecology.interactions.get(consumer.index, target.index)
            val attack = undiscountedAttack(consumer, target)
            assertEquals(InteractionKind.PREDATION, interaction.kind)
            assertEquals(attack, interaction.targetLossRate, 1.0e-12)
            assertEquals(attack * 1.30, interaction.consumerGainRate, 1.0e-12)
        }
    }

    @Test
    fun `attached and suspended photosynthesizers compile to separate competition layers`() {
        val waterLily = EarthSpeciesCatalog.ALL.single { it.id == "white-water-lily" }
        val ecology = EcologyCompiler.compile(listOf(waterLily, InvariantSpecies.PLANKTON))

        assertEquals(
            ProducerCompetitionLayer.ATTACHED,
            ecology.species.single { it.id == waterLily.id }.niche.producerCompetitionLayer,
        )
        assertEquals(
            ProducerCompetitionLayer.SUSPENDED,
            ecology.species.single { it.id == InvariantSpecies.PLANKTON.id }
                .niche.producerCompetitionLayer,
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
        assertTrue(ecology.species.all { it.lifeHistory.dormancyKind == DormancyKind.PROPAGULE })
        assertTrue(ecology.species.all { it.lifeHistory.nicheCompetitionSensitivity < 0.20 })
        assertEquals(
            0.10,
            ecology.species.single { it.id == InvariantSpecies.PLANKTON.id }
                .lifeHistory.dormantEntryBiomassRetention,
        )
        assertEquals(
            10.0,
            ecology.species.single { it.id == InvariantSpecies.PLANKTON.id }
                .lifeHistory.dormantReactivationMultiplier,
        )
        assertTrue(
            ecology.species
                .filterNot { it.id == InvariantSpecies.PLANKTON.id }
                .all { it.lifeHistory.dormantEntryBiomassRetention == 1.0 },
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