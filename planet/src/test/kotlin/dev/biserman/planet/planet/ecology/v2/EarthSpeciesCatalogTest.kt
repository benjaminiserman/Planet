package dev.biserman.planet.planet.ecology.v2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EarthSpeciesCatalogTest {
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
    fun `high plateau species use evidence-supported water coat and oxygen adaptations`() {
        val definitions = EarthSpeciesCatalog.ALL.associateBy { it.id }

        assertTrue(
            CommonTrait.FOOD_DERIVED_WATER in
                definitions.getValue("himalayan-pika").traits,
        )
        assertTrue(
            CommonTrait.INSULATED_BURROW_REFUGE in
                definitions.getValue("himalayan-pika").traits,
        )
        assertTrue(
            CommonTrait.WOOLLY_UNDERCOAT in
                definitions.getValue("snow-leopard").traits,
        )
        assertTrue(
            CommonTrait.FOOD_DERIVED_WATER in
                definitions.getValue("snow-leopard").traits,
        )
        assertTrue(
            CommonTrait.ENLARGED_CARDIOPULMONARY_SYSTEM in
                definitions.getValue("wild-yak").traits,
        )
        assertTrue(
            CommonTrait.SNOW_AND_ICE_LICKING in
                definitions.getValue("wild-yak").traits,
        )
        assertTrue(
            CommonTrait.HIGH_AFFINITY_HEMOGLOBIN in
                definitions.getValue("vicuna").traits,
        )
        listOf("wild-yak", "himalayan-pika", "mountain-goat").forEach { speciesId ->
            assertTrue(
                CommonTrait.SEASONAL_WINTER_COAT in definitions.getValue(speciesId).traits,
                speciesId,
            )
        }

        val ecology = EcologyCompiler.compile(EarthSpeciesCatalog.ALL)
        listOf("wild-yak", "himalayan-pika", "snow-leopard").forEach { speciesId ->
            val species = ecology.species.single { it.id == speciesId }
            assertTrue(species.elevationToleranceShiftM >= 2_500.0, speciesId)
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
