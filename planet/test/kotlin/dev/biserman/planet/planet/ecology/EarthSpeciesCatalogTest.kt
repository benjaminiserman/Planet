package dev.biserman.planet.planet.ecology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EarthSpeciesCatalogTest {
    @Test
    fun `coral grouper depends on living reef cover`() {
        val grouperDefinition = EarthSpeciesCatalog.ALL.single { it.id == "coral-grouper" }
        val grouper = EcologyCompiler.compile(listOf(grouperDefinition)).species.single()
        fun environment(reefCover: Double) = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 27.0,
            insolation = 0.8,
            precipitationMm = 0.0,
            isLand = false,
            waterDepthM = 40.0,
            reefCover = reefCover,
        )

        assertEquals(1.0, grouper.interactions.reefUse)
        assertEquals(0.0, EcologyFitness.reefAssociationMultiplier(grouper, environment(0.0)))
        assertTrue(EcologyFitness.reefAssociationMultiplier(grouper, environment(0.75)) > 1.0)
    }

    @Test
    fun `bees use nectar feeding and return pollination to flowering producers`() {
        val ecology = EcologyCompiler.compile(EarthSpeciesCatalog.ALL + InvariantSpecies.ALL)
        val bee = ecology.species.single { it.id == "western-honey-bee" }
        val sunflower = ecology.species.single { it.id == "common-sunflower" }

        assertTrue(bee.niche.supportFor(EcoStrategy.NECTAR_FEEDING) > 0.0)
        assertEquals(0.0, bee.niche.supportFor(EcoStrategy.GRAZING))
        assertTrue(bee.interactions.pollinationEfficiency > 0.0)
        assertTrue(sunflower.interactions.flowering)
        assertTrue(sunflower.interactions.nectarProduction > 0.0)
    }

    @Test
    fun `southern ocean specialists compile into the intended food web`() {
        val definitions = EarthSpeciesCatalog.ALL.associateBy { it.id }
        val silverfish = definitions.getValue("antarctic-silverfish")
        val weddellSeal = definitions.getValue("weddell-seal")
        val crabeaterSeal = definitions.getValue("crabeater-seal")
        val orca = definitions.getValue("orca")
        val ecology = EcologyCompiler.compile(
            listOf(InvariantSpecies.PLANKTON, silverfish, weddellSeal, crabeaterSeal, orca),
        )

        assertEquals(
            InteractionKind.FILTER_FEEDING,
            ecology.interactions.get(
                ecology.speciesIndex(crabeaterSeal.id),
                ecology.speciesIndex(InvariantSpecies.PLANKTON.id),
            ).kind,
        )
        assertEquals(
            InteractionKind.PREDATION,
            ecology.interactions.get(
                ecology.speciesIndex(weddellSeal.id),
                ecology.speciesIndex(silverfish.id),
            ).kind,
        )
        listOf(weddellSeal, crabeaterSeal).forEach { seal ->
            assertEquals(
                InteractionKind.PREDATION,
                ecology.interactions.get(
                    ecology.speciesIndex(orca.id),
                    ecology.speciesIndex(seal.id),
                ).kind,
            )
        }

        val compiledOrca = ecology.species[ecology.speciesIndex(orca.id)]
        val ordinaryOrca = EcologyCompiler.compile(
            listOf(
                orca.copy(
                    id = "orca-without-extended-parental-care",
                    traits = orca.traits - CommonTrait.EXTENDED_PARENTAL_CARE,
                ),
            ),
        ).species.single()
        assertTrue(CommonTrait.DEEP_DIVING_PHYSIOLOGY in orca.traits)
        assertTrue(compiledOrca.niche.supportFor(Habitat.DARK_WATER) > 0.0)
        assertTrue(compiledOrca.lifeHistory.seasonalReproduction < ordinaryOrca.lifeHistory.seasonalReproduction)
    }

    @Test
    fun `documented arid mammals conserve water with concentrated urine`() {
        val definitions = EarthSpeciesCatalog.ALL.associateBy { it.id }
        val adaptedIds = listOf(
            "dromedary-camel",
            "fennec-fox",
            "jerboa",
            "red-kangaroo",
        )

        adaptedIds.forEach { speciesId ->
            val adapted = definitions.getValue(speciesId)
            assertTrue(
                CommonTrait.CONCENTRATED_URINE in adapted.traits,
                "$speciesId should have concentrated urine",
            )
            val baseline = adapted.copy(
                id = "$speciesId-without-concentrated-urine",
                traits = adapted.traits - CommonTrait.CONCENTRATED_URINE,
            )
            val compiled = EcologyCompiler.compile(listOf(adapted, baseline)).species
            assertTrue(compiled[0].physiology.hydration.minimumWater < compiled[1].physiology.hydration.minimumWater)
            assertTrue(compiled[0].physiology.maintenanceDemand > compiled[1].physiology.maintenanceDemand)
        }
    }

    @Test
    fun `giant bamboo rapid growth trades maintenance for reproduction`() {
        val bamboo = EarthSpeciesCatalog.ALL.single { it.id == "giant-bamboo" }
        assertTrue(CommonTrait.RAPID_GROWTH in bamboo.traits)
        val ordinaryGrowth = bamboo.copy(
            id = "ordinary-growth-bamboo",
            traits = bamboo.traits - CommonTrait.RAPID_GROWTH,
        )
        val compiled = EcologyCompiler.compile(listOf(bamboo, ordinaryGrowth)).species

        assertTrue(compiled[0].lifeHistory.seasonalReproduction > compiled[1].lifeHistory.seasonalReproduction)
        assertTrue(compiled[0].physiology.maintenanceDemand > compiled[1].physiology.maintenanceDemand)
    }

    @Test
    fun `anteater raids minuscule colonies while bee stingers and honey provide defense and reserves`() {
        val definitions = EarthSpeciesCatalog.ALL.associateBy { it.id }
        val anteater = definitions.getValue("giant-anteater")
        val ant = definitions.getValue("leafcutter-ant")
        val termite = definitions.getValue("termite")
        val bee = definitions.getValue("western-honey-bee")
        val chameleon = definitions.getValue("veiled-chameleon")
        val solitaryInsect = ant.copy(
            id = "solitary-insect",
            displayName = "Solitary insect",
            traits = ant.traits - CommonTrait.COLONY_LIVING,
        )

        assertTrue(CommonTrait.NEST_PROBING_TONGUE in anteater.traits)
        assertTrue(CommonTrait.PROJECTILE_TONGUE !in anteater.traits)
        assertTrue(CommonTrait.PROJECTILE_TONGUE in chameleon.traits)
        assertTrue(CommonTrait.AMBUSH_MUSCULATURE !in anteater.traits)
        assertTrue(CommonTrait.VENOMOUS_STINGER in bee.traits)
        assertTrue(CommonTrait.HONEY_STORES in bee.traits)
        assertTrue(CommonTrait.COLONY_THERMOREGULATION in bee.traits)
        assertTrue(CommonTrait.APOSEMATIC_COLORATION in bee.traits)
        assertTrue(
            CommonTrait.APOSEMATIC_COLORATION in
                definitions.getValue("poison-dart-frog").traits,
        )

        val ecology = EcologyCompiler.compile(
            listOf(anteater, ant, termite, solitaryInsect, bee, chameleon),
        )
        val anteaterIndex = ecology.speciesIndex(anteater.id)
        assertEquals(
            InteractionKind.PREDATION,
            ecology.interactions.get(anteaterIndex, ecology.speciesIndex(ant.id)).kind,
        )
        assertEquals(
            InteractionKind.PREDATION,
            ecology.interactions.get(anteaterIndex, ecology.speciesIndex(termite.id)).kind,
        )
        assertEquals(
            InteractionKind.NONE,
            ecology.interactions.get(anteaterIndex, ecology.speciesIndex(solitaryInsect.id)).kind,
        )
        val compiledAnteater = ecology.species[anteaterIndex]
        assertTrue(compiledAnteater.niche.supportFor(EcoStrategy.COLONY_RAIDING) > 0.0)
        assertEquals(0.0, compiledAnteater.niche.supportFor(EcoStrategy.AMBUSH_PREDATION))
        assertEquals(
            0.0,
            ecology.species[ecology.speciesIndex(chameleon.id)]
                .niche.supportFor(EcoStrategy.COLONY_RAIDING),
        )

        val undefendedBee = bee.copy(
            id = "undefended-bee",
            traits = bee.traits - CommonTrait.VENOMOUS_STINGER - CommonTrait.HONEY_STORES,
        )
        val beeComparison = EcologyCompiler.compile(listOf(bee, undefendedBee)).species
        assertTrue(beeComparison[0].interactions.defense > beeComparison[1].interactions.defense)
        assertTrue(beeComparison[0].lifeHistory.reserveCapacity > beeComparison[1].lifeHistory.reserveCapacity)
        assertTrue(beeComparison[0].lifeHistory.seasonalReproduction < beeComparison[1].lifeHistory.seasonalReproduction)
    }

    @Test
    fun `sloth slow metabolism trades reproductive speed for lower energy demand`() {
        val sloth = EarthSpeciesCatalog.ALL.single { it.id == "three-toed-sloth" }
        assertTrue(CommonTrait.SLOW_METABOLISM in sloth.traits)
        val ordinaryMetabolism = sloth.copy(
            id = "ordinary-metabolism-sloth",
            traits = sloth.traits - CommonTrait.SLOW_METABOLISM,
        )
        val compiled = EcologyCompiler.compile(listOf(sloth, ordinaryMetabolism)).species

        assertTrue(compiled[0].physiology.maintenanceDemand < compiled[1].physiology.maintenanceDemand)
        assertTrue(compiled[0].lifeHistory.seasonalReproduction < compiled[1].lifeHistory.seasonalReproduction)
    }

    @Test
    fun `fruit specialists and fruit-bearing producers use the frugivory system`() {
        val definitions = EarthSpeciesCatalog.ALL.associateBy { it.id }
        listOf("bornean-orangutan", "large-flying-fox").forEach { speciesId ->
            val species = definitions.getValue(speciesId)
            assertTrue(CommonTrait.FRUIT_EATING_MOUTHPARTS in species.traits)
            assertTrue(CommonTrait.BROWSING_MOUTHPARTS !in species.traits)
        }
        listOf("strangler-fig", "african-baobab", "english-oak").forEach { speciesId ->
            assertTrue(CommonTrait.FRUIT_BEARING in definitions.getValue(speciesId).traits)
        }

        val compiled = EcologyCompiler.compile(
            listOf(definitions.getValue("bornean-orangutan")),
        ).species.single()
        assertTrue(compiled.niche.supportFor(EcoStrategy.FRUGIVORY) > 0.0)
        assertTrue(
            compiled.niche.supportFor(EcoStrategy.FRUGIVORY) >
                compiled.niche.supportFor(EcoStrategy.GRAZING),
        )
    }

    @Test
    fun `swift legs improve pursuit capture and pursuit evasion without making prey predatory`() {
        val cheetah = EarthSpeciesCatalog.ALL.single { it.id == "cheetah" }
        val gazelle = EarthSpeciesCatalog.ALL.single { it.id == "thomsons-gazelle" }

        assertTrue(CommonTrait.SWIFT_LIMBS in cheetah.traits)
        assertTrue(CommonTrait.MOTION_TRACKING_SENSES in cheetah.traits)
        assertTrue(CommonTrait.SWIFT_LIMBS in gazelle.traits)
        assertTrue(CommonTrait.MOTION_TRACKING_SENSES !in gazelle.traits)

        val compiledGazelle = EcologyCompiler.compile(listOf(gazelle)).species.single()
        assertEquals(
            0.0,
            compiledGazelle.niche.supportFor(EcoStrategy.PURSUIT_PREDATION),
        )

        val slowCheetah = cheetah.copy(
            id = "slow-cheetah",
            traits = cheetah.traits - CommonTrait.SWIFT_LIMBS,
        )
        val slowGazelle = gazelle.copy(
            id = "slow-gazelle",
            traits = gazelle.traits - CommonTrait.SWIFT_LIMBS,
        )
        val swiftHunterAgainstSlowPrey = predationRate(cheetah, slowGazelle)
        val slowHunterAgainstSlowPrey = predationRate(slowCheetah, slowGazelle)
        val swiftHunterAgainstSwiftPrey = predationRate(cheetah, gazelle)

        assertTrue(swiftHunterAgainstSlowPrey > slowHunterAgainstSlowPrey)
        assertTrue(swiftHunterAgainstSwiftPrey < swiftHunterAgainstSlowPrey)
    }

    @Test
    fun `regional biodiversity additions cover six underrepresented environments`() {
        val definitions = EarthSpeciesCatalog.ALL.associateBy { it.id }
        val regionalSpecies = mapOf(
            "Siberia" to listOf("siberian-larch", "siberian-musk-deer", "sable"),
            "Himalayan plateau" to listOf("himalayan-juniper", "wild-yak", "himalayan-pika"),
            "Rockies" to listOf("lodgepole-pine", "rocky-mountain-elk", "mountain-goat"),
            "Andes" to listOf("ichu-grass", "vicuna", "andean-condor"),
            "Sahara" to listOf("saharan-cypress", "addax", "fennec-fox"),
            "Canadian Shield" to listOf("black-spruce", "woodland-caribou", "canada-lynx"),
        )

        regionalSpecies.forEach { (region, speciesIds) ->
            val species = speciesIds.map { requireNotNull(definitions[it]) { "$region is missing $it" } }
            assertEquals(1, species.count { !it.motile }, "$region plant coverage")
            assertEquals(2, species.count { it.motile }, "$region animal coverage")
        }

        val coldRegionIds = regionalSpecies
            .filterKeys { it != "Sahara" }
            .values
            .flatten()
        coldRegionIds.forEach { speciesId ->
            val traits = definitions.getValue(speciesId).traits
            assertTrue(
                traits.any {
                    it == CommonTrait.DENSE_FUR ||
                        it == CommonTrait.INSULATING_PLUMAGE ||
                        it == CommonTrait.FROST_HARDENED_TISSUES ||
                        it == CommonTrait.SEASONAL_WINTER_COAT
                },
                "$speciesId lacks an explicit cold-climate adaptation",
            )
        }
    }

    @Test
    fun `Himalayan specialists are physically viable in a cold semidesert at 4500 meters`() {
        val ecology = EcologyCompiler.compile(EarthSpeciesCatalog.ALL)
        val temperatures = listOf(
            -16.1, -11.5, -3.8, 2.7, 6.1, 7.0,
            6.9, 6.1, 3.3, -2.7, -10.1, -15.6,
        )
        val insolations = listOf(
            0.49, 0.55, 0.65, 0.75, 0.81, 0.83,
            0.82, 0.78, 0.70, 0.60, 0.52, 0.47,
        )
        val precipitation = listOf(2.0, 2.0, 3.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0)
        val annualEnvironments = temperatures.indices.map { month ->
            SeasonalCellEnvironment.create(
                areaKm2 = 40_000.0,
                temperatureC = temperatures[month],
                annualAverageTemperatureC = -2.31,
                insolation = insolations[month],
                precipitationMm = precipitation[month],
                isLand = true,
                elevationM = 4_500.0,
            )
        }

        listOf("wild-yak", "himalayan-pika", "snow-leopard").forEach { speciesId ->
            val species = ecology.species.single { it.id == speciesId }
            val suitability = EcologySuitability.evaluate(species, ecology, annualEnvironments)
            assertTrue(
                suitability.suitable,
                "$speciesId: score=${suitability.score}, issues=${suitability.issues}",
            )
        }
    }

    @Test
    fun `catalog corrections keep feeding and insulation anatomy explicit`() {
        val definitions = EarthSpeciesCatalog.ALL.associateBy { it.id }
        val walrus = requireNotNull(definitions["walrus"])
        val manatee = requireNotNull(definitions["west-indian-manatee"])

        assertTrue(CommonTrait.BENTHIC_SUCTION_FEEDING in walrus.traits)
        assertTrue(CommonTrait.SIEVE_TEETH !in walrus.traits)
        assertTrue(CommonTrait.BLUBBER !in manatee.traits)
        assertTrue("tardigrade" !in definitions)

        val ecology = EcologyCompiler.compile(EarthSpeciesCatalog.ALL)
        val compiledWalrus = ecology.species.single { it.id == "walrus" }
        assertEquals(
            0.0,
            compiledWalrus.niche.supportFor(EcoStrategy.FILTER_FEEDING),
        )
        assertTrue(
            compiledWalrus.niche.supportFor(EcoStrategy.AMBUSH_PREDATION) > 0.0,
        )
    }

    @Test
    fun `open country herding and benthic suction feeding cover matching animals`() {
        val definitions = EarthSpeciesCatalog.ALL.associateBy { it.id }
        val openCountryHerders = setOf(
            "plains-zebra",
            "blue-wildebeest",
            "thomsons-gazelle",
            "red-kangaroo",
            "american-bison",
            "common-ostrich",
        )
        val benthicSuctionFeeders = setOf("walrus", "common-carp")

        openCountryHerders.forEach { speciesId ->
            assertTrue(
                CommonTrait.OPEN_COUNTRY_HERDING in requireNotNull(definitions[speciesId]).traits,
                speciesId,
            )
        }
        benthicSuctionFeeders.forEach { speciesId ->
            assertTrue(
                CommonTrait.BENTHIC_SUCTION_FEEDING in
                    requireNotNull(definitions[speciesId]).traits,
                speciesId,
            )
        }

        val ecology = EcologyCompiler.compile(EarthSpeciesCatalog.ALL)
        val carp = ecology.species.single { it.id == "common-carp" }
        assertTrue(carp.niche.supportFor(EcoStrategy.GENERALIST_FORAGING) > 0.0)
        assertEquals(0.0, carp.niche.supportFor(EcoStrategy.FILTER_FEEDING))
    }

    @Test
    fun `marine freshwater and euryhaline species compile to distinct water chemistry`() {
        val definitions = listOf(
            EarthSpeciesCatalog.MAMMALS.single { it.id == "blue-whale" },
            EarthSpeciesCatalog.FISH.single { it.id == "common-carp" },
            EarthSpeciesCatalog.FISH.single { it.id == "atlantic-salmon" },
        )
        val ecology = EcologyCompiler.compile(definitions)
        val blueWhale = ecology.species.single { it.id == "blue-whale" }
        val carp = ecology.species.single { it.id == "common-carp" }
        val salmon = ecology.species.single { it.id == "atlantic-salmon" }

        assertEquals(AquaticSalinityTolerance.SALTWATER_ONLY, blueWhale.physiology.respiration.salinityTolerance)
        assertEquals(0.0, blueWhale.niche.supportFor(Habitat.FRESHWATER))
        assertTrue(blueWhale.niche.supportFor(Habitat.SUNLIT_WATER) > 0.0)

        assertEquals(AquaticSalinityTolerance.FRESHWATER_ONLY, carp.physiology.respiration.salinityTolerance)
        assertTrue(carp.niche.supportFor(Habitat.FRESHWATER) > 0.0)
        assertEquals(0.0, carp.niche.supportFor(Habitat.SUNLIT_WATER))

        assertEquals(AquaticSalinityTolerance.BROAD, salmon.physiology.respiration.salinityTolerance)
        assertTrue(salmon.niche.supportFor(Habitat.FRESHWATER) > 0.0)
        assertTrue(salmon.niche.supportFor(Habitat.SUNLIT_WATER) > 0.0)
    }

    @Test
    fun `orca echolocation does not create an aerial land niche`() {
        val orca = EcologyCompiler.compile(
            listOf(EarthSpeciesCatalog.MAMMALS.single { it.id == "orca" }),
        ).species.single()
        val land = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 18.0,
            insolation = 0.8,
            precipitationMm = 900.0,
            isLand = true,
        )

        assertEquals(0.0, orca.niche.supportFor(Habitat.AERIAL))
        assertEquals(
            -1,
            NicheSelection.choose(
                orca,
                EcologyCompiler.compile(
                    listOf(EarthSpeciesCatalog.MAMMALS.single { it.id == "orca" }),
                ),
                land
            )
        )
    }

    @Test
    fun `saguaro is frost sensitive and emperor penguin is heat limited`() {
        val ecology = EcologyCompiler.compile(
            listOf(
                EarthSpeciesCatalog.PRODUCERS_AND_FUNGI.single { it.id == "saguaro" },
                EarthSpeciesCatalog.BIRDS.single { it.id == "emperor-penguin" },
            ),
        )
        val saguaro = ecology.species.single { it.id == "saguaro" }
        val penguin = ecology.species.single { it.id == "emperor-penguin" }
        val tropicalReef = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 29.0,
            annualAverageTemperatureC = 28.0,
            insolation = 0.82,
            precipitationMm = 1_800.0,
            isLand = false,
            waterDepthM = 50.0,
        )

        assertTrue(saguaro.physiology.thermal.outerLowC > 0.0)
        assertTrue(EcologyFitness.thermal(penguin, tropicalReef) < 0.35)
    }

    @Test
    fun `emperor penguin cannot persist on tropical reef prey`() {
        val ecology = EcologyCompiler.compile(
            listOf(
                EarthSpeciesCatalog.BIRDS.single { it.id == "emperor-penguin" },
                InvariantSpecies.SMALL_AQUATIC_LIFE,
                InvariantSpecies.PLANKTON,
            ),
        )
        val tropicalReef = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 29.0,
            annualAverageTemperatureC = 28.0,
            insolation = 0.82,
            precipitationMm = 1_800.0,
            surfaceFertilityModifier = 0.35,
            isLand = false,
            waterDepthM = 50.0,
            reefCover = 0.75,
        )
        val community = TileCommunity()
        ecology.species.forEach { species ->
            val nicheIndex = NicheSelection.choose(species, ecology, tropicalReef)
            if (nicheIndex < 0) return@forEach
            val capacity = EcologyBiomass.carryingCapacityKg(
                species,
                ecology.niches[nicheIndex],
                tropicalReef,
            )
            community.add(species.index, nicheIndex, capacity * 0.50)
        }

        val runtime = EcologyRuntime(ecology)
        repeat(4_000) {
            runtime.advanceSeason(community, tropicalReef)
        }

        assertEquals(-1, community.find(ecology.speciesIndex("emperor-penguin")))
    }

    @Test
    fun `catalog is broad unique and compiler-valid`() {
        val definitions = EarthSpeciesCatalog.ALL
        println(
            "EARTH_SPECIES_CATALOG total=${definitions.size} traits=${CommonTrait.entries.size} " +
                "mammals=${EarthSpeciesCatalog.MAMMALS.size} extinct=${EarthSpeciesCatalog.EXTINCT_SPECIES.size} " +
                "birds=${EarthSpeciesCatalog.BIRDS.size} reptiles_amphibians=${EarthSpeciesCatalog.REPTILES_AND_AMPHIBIANS.size} " +
                "fish=${EarthSpeciesCatalog.FISH.size} invertebrates=${EarthSpeciesCatalog.INVERTEBRATES.size} " +
                "producers_fungi=${EarthSpeciesCatalog.PRODUCERS_AND_FUNGI.size}",
        )

        assertTrue(definitions.size >= 140)
        assertEquals(definitions.size, definitions.map { it.id }.distinct().size)
        assertEquals(definitions.size, definitions.map { it.displayName }.distinct().size)
        assertTrue(EarthSpeciesCatalog.MAMMALS.size >= 40)
        assertTrue(EarthSpeciesCatalog.EXTINCT_SPECIES.size >= 12)
        assertTrue(EarthSpeciesCatalog.BIRDS.size >= 20)
        assertTrue(EarthSpeciesCatalog.REPTILES_AND_AMPHIBIANS.size >= 15)
        assertTrue(EarthSpeciesCatalog.FISH.size >= 18)
        assertTrue(EarthSpeciesCatalog.INVERTEBRATES.size >= 24)
        assertTrue(EarthSpeciesCatalog.PRODUCERS_AND_FUNGI.size >= 20)

        val ecology = EcologyCompiler.compile(definitions)
        ecology.species.forEach { species ->
            assertTrue(
                species.niche.hasViableNiche(),
                "${species.displayName} has no supported niche",
            )
        }
    }

    @Test
    fun `catalog covers recognizable organism archetypes`() {
        val ids = (EarthSpeciesCatalog.ALL + EarthSpeciesCatalog.EXTINCT_SPECIES)
            .mapTo(hashSetOf()) { it.id }
        val expected = setOf(
            "african-elephant",
            "african-lion",
            "blue-whale",
            "bald-eagle",
            "emperor-penguin",
            "nile-crocodile",
            "king-cobra",
            "poison-dart-frog",
            "great-white-shark",
            "ocellaris-clownfish",
            "common-octopus",
            "western-honey-bee",
            "orb-weaver-spider",
            "staghorn-coral",
            "english-oak",
            "giant-kelp",
            "field-mushroom",
            "tyrannosaurus-rex",
            "woolly-mammoth",
            "trilobite",
        )

        assertTrue(ids.containsAll(expected), "Missing ${expected - ids}")
    }

    @Test
    fun `photosynthetic method does not make land plants aquatic or kelp terrestrial`() {
        val ecology = EcologyCompiler.compile(EarthSpeciesCatalog.ALL)
        val baobab = ecology.species.single { it.id == "african-baobab" }
        val kelp = ecology.species.single { it.id == "giant-kelp" }

        assertEquals(0.0, baobab.niche.supportFor(Habitat.SUNLIT_WATER))
        assertEquals(0.0, baobab.niche.supportFor(Habitat.DARK_WATER))
        assertEquals(0.0, kelp.niche.supportFor(Habitat.LAND_SURFACE))
        assertEquals(0.0, kelp.niche.supportFor(Habitat.CANOPY))
        assertTrue(kelp.niche.supportFor(Habitat.SUNLIT_WATER) > 0.0)
    }

    private fun predationRate(
        predator: SpeciesDefinition,
        prey: SpeciesDefinition,
    ): Double {
        val ecology = EcologyCompiler.compile(listOf(predator, prey))
        val predatorIndex = ecology.speciesIndex(predator.id)
        val preyIndex = ecology.speciesIndex(prey.id)
        val offset = predatorIndex * ecology.species.size + preyIndex
        assertEquals(InteractionKind.PREDATION.ordinal, ecology.interactions.kindAt(offset))
        return ecology.interactions.targetLossAt(offset)
    }
}