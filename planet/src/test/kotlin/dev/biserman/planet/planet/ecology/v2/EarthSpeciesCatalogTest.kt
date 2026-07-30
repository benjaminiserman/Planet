package dev.biserman.planet.planet.ecology.v2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EarthSpeciesCatalogTest {
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
            compiledWalrus.strategySupport[EcoStrategy.FILTER_FEEDING.ordinal],
        )
        assertTrue(
            compiledWalrus.strategySupport[EcoStrategy.AMBUSH_PREDATION.ordinal] > 0.0,
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
        assertTrue(carp.strategySupport[EcoStrategy.GENERALIST_FORAGING.ordinal] > 0.0)
        assertEquals(0.0, carp.strategySupport[EcoStrategy.FILTER_FEEDING.ordinal])
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

        assertEquals(AquaticSalinityTolerance.SALTWATER_ONLY, blueWhale.aquaticSalinityTolerance)
        assertEquals(0.0, blueWhale.habitatSupport[Habitat.FRESHWATER.ordinal])
        assertTrue(blueWhale.habitatSupport[Habitat.SUNLIT_WATER.ordinal] > 0.0)

        assertEquals(AquaticSalinityTolerance.FRESHWATER_ONLY, carp.aquaticSalinityTolerance)
        assertTrue(carp.habitatSupport[Habitat.FRESHWATER.ordinal] > 0.0)
        assertEquals(0.0, carp.habitatSupport[Habitat.SUNLIT_WATER.ordinal])

        assertEquals(AquaticSalinityTolerance.BROAD, salmon.aquaticSalinityTolerance)
        assertTrue(salmon.habitatSupport[Habitat.FRESHWATER.ordinal] > 0.0)
        assertTrue(salmon.habitatSupport[Habitat.SUNLIT_WATER.ordinal] > 0.0)
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

        assertEquals(0.0, orca.habitatSupport[Habitat.AERIAL.ordinal])
        assertEquals(-1, NicheSelection.choose(orca, EcologyCompiler.compile(
            listOf(EarthSpeciesCatalog.MAMMALS.single { it.id == "orca" }),
        ), land))
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

        assertTrue(saguaro.temperatureOuterLow > 0.0)
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
                species.nicheFit.any { it > 0.0 },
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

        assertEquals(0.0, baobab.habitatSupport[Habitat.SUNLIT_WATER.ordinal])
        assertEquals(0.0, baobab.habitatSupport[Habitat.DARK_WATER.ordinal])
        assertEquals(0.0, kelp.habitatSupport[Habitat.LAND_SURFACE.ordinal])
        assertEquals(0.0, kelp.habitatSupport[Habitat.CANOPY.ordinal])
        assertTrue(kelp.habitatSupport[Habitat.SUNLIT_WATER.ordinal] > 0.0)
    }
}
