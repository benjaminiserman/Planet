package dev.biserman.planet.planet.ecology

import dev.biserman.planet.utils.Serialization
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EcologyProductionTest {
    private val ecology = EcologyCompiler.compile(
        EarthSpeciesCatalog.ALL + EarthSpeciesCatalog.EXTINCT_SPECIES + InvariantSpecies.ALL,
    )

    @Test
    fun `every current habitat has a distinct byte cache bit`() {
        HabitatCacheMask.validateCapacity()
        val roundTrippedBits = Habitat.entries.map { habitat ->
            HabitatCacheMask.bit(habitat).toByte().toInt() and 0xFF
        }

        assertEquals(Habitat.entries.size, roundTrippedBits.toSet().size)
        assertTrue(roundTrippedBits.none { it == 0 })
    }

    @Test
    fun `colossal Earth species are redwoods and baleen whales`() {
        assertEquals(
            setOf("coast-redwood", "blue-whale", "humpback-whale"),
            EarthSpeciesCatalog.ALL
                .filter { it.sizeClass == SizeClass.COLOSSAL }
                .mapTo(linkedSetOf()) { it.id },
        )
    }

    @Test
    fun `every authored species has a credible Hersfeldt climate`() {
        val fixtures = climateFixtures()
        val unmatched = mutableListOf<String>()

        ecology.species
            .filter { it.kind == SpeciesKind.EVOLVING }
            .forEach { species ->
                val results = fixtures.map { (_, environment) ->
                    EcologySuitability.evaluate(species, ecology, environment)
                }
                results.forEach { result ->
                    assertTrue(result.score.isFinite(), species.displayName)
                    assertTrue(result.score in 0.0..1.0, species.displayName)
                }
                if (results.none { it.suitable }) {
                    val best = results.maxBy { it.score }
                    val fixture = fixtures[results.indexOf(best)].first
                    unmatched += "${species.displayName} (best $fixture: $best)"
                }
            }

        assertTrue(
            unmatched.isEmpty(),
            "No suitable authored climate for: ${unmatched.joinToString()}",
        )
    }

    @Test
    fun `climate specialists are accepted and rejected in expected places`() {
        assertSuitable("saguaro", HersfeldtClimatePresets.DESERT)
        assertUnsuitable("saguaro", HersfeldtClimatePresets.TUNDRA)
        assertSuitable("dromedary-camel", HersfeldtClimatePresets.DESERT)
        assertUnsuitable("dromedary-camel", HersfeldtClimatePresets.ICE_CAP)
        assertSuitable("poison-dart-frog", HersfeldtClimatePresets.JUNGLE)
        assertUnsuitable("poison-dart-frog", HersfeldtClimatePresets.DESERT)
        assertSuitable("emperor-penguin", HersfeldtClimatePresets.PERMANENT_SEA_ICE)
        assertUnsuitable("emperor-penguin", HersfeldtClimatePresets.TROPICAL_REEF)
        assertSuitable("ocellaris-clownfish", HersfeldtClimatePresets.TROPICAL_REEF)
        assertSuitable("porcupinefish", HersfeldtClimatePresets.TROPICAL_REEF)
        assertSuitable("cleaner-shrimp", HersfeldtClimatePresets.TROPICAL_REEF)
        assertUnsuitable("ocellaris-clownfish", HersfeldtClimatePresets.POLAR_SEA)
        assertSuitable("deep-sea-anglerfish", HersfeldtClimatePresets.DEEP_OCEAN)
        assertUnsuitable("deep-sea-anglerfish", HersfeldtClimatePresets.TROPICAL_REEF)
        assertSuitable("western-honey-bee", HersfeldtClimatePresets.OCEANIC_TEMPERATE)
        assertUnsuitable("western-honey-bee", HersfeldtClimatePresets.ICE_CAP)
    }

    @Test
    fun `carpet plants require a thawed growing season`() {
        assertSuitable(InvariantSpecies.CARPET_PLANTS.id, HersfeldtClimatePresets.TUNDRA)
        assertUnsuitable(InvariantSpecies.CARPET_PLANTS.id, HersfeldtClimatePresets.ICE_CAP)
    }

    @Test
    fun `random ecosystem initialization is independent of hemisphere season`() {
        val northernYear = environment(HersfeldtClimatePresets.PERMANENT_SEA_ICE)
        val southernYear = northernYear.drop(6) + northernYear.take(6)

        val northernInvariants =
            PlanetEcology.relevantInvariantSpecies(northernYear).map { it.id }.toSet()
        val southernInvariants =
            PlanetEcology.relevantInvariantSpecies(southernYear).map { it.id }.toSet()

        assertEquals(northernInvariants, southernInvariants)
        assertTrue(InvariantSpecies.PLANKTON.id in northernInvariants)

        val plankton = ecology.species.single { it.id == InvariantSpecies.PLANKTON.id }
        val nicheIndex =
            EcologySuitability.evaluate(plankton, ecology, northernYear).nicheIndex
        val niche = ecology.niches[nicheIndex]
        val northernCapacity =
            PlanetEcology.initialCarryingCapacityKg(plankton, niche, northernYear)
        val southernCapacity =
            PlanetEcology.initialCarryingCapacityKg(plankton, niche, southernYear)

        assertEquals(northernCapacity, southernCapacity, northernCapacity * 1e-12)
    }

    @Test
    fun `catalog animals do not gain broad climate ranges from generic physiology`() {
        assertUnsuitable("bengal-tiger", HersfeldtClimatePresets.DESERT)
        assertUnsuitable("red-kangaroo", HersfeldtClimatePresets.DESERT)

        listOf(
            "nile-crocodile",
            "american-alligator",
            "bornean-orangutan",
            "cheetah",
        ).forEach { speciesId ->
            assertUnsuitable(speciesId, HersfeldtClimatePresets.BOREAL)
        }

        val tiger = ecology.species.single { it.id == "bengal-tiger" }
        val kangaroo = ecology.species.single { it.id == "red-kangaroo" }
        assertTrue(tiger.physiology.hydration.minimumWater > 0.0)
        assertTrue(kangaroo.physiology.hydration.minimumWater > 0.0)
    }

    @Test
    fun `deep diving whales span polar temperate and tropical seas`() {
        listOf("blue-whale", "humpback-whale").forEach { speciesId ->
            assertSuitable(speciesId, HersfeldtClimatePresets.POLAR_SEA)
            assertSuitable(speciesId, HersfeldtClimatePresets.TEMPERATE_SHELF)
            assertSuitable(speciesId, HersfeldtClimatePresets.TROPICAL_REEF)
        }
    }

    @Test
    fun `open grass grazer fits savanna better than dense jungle`() {
        val savanna = evaluate("plains-zebra", HersfeldtClimatePresets.SAVANNA)
        val jungle = evaluate("plains-zebra", HersfeldtClimatePresets.JUNGLE)

        assertTrue(
            savanna.score > jungle.score * 1.5,
            "savanna=$savanna jungle=$jungle",
        )
    }

    @Test
    fun `suitability can target one habitat compartment`() {
        val species = ecology.species.single { it.id == "saguaro" }
        val desert = environment(HersfeldtClimatePresets.DESERT, majorRiver = true)

        assertTrue(
            EcologySuitability.evaluate(
                species,
                ecology,
                desert,
                Habitat.LAND_SURFACE,
            ).suitable,
        )
        assertFalse(
            EcologySuitability.evaluate(
                species,
                ecology,
                desert,
                Habitat.FRESHWATER,
            ).suitable,
        )
    }

    @Test
    fun `tile ecosystem survives a compressed save round trip`() {
        val original = TileEcosystem(
            populations = mutableListOf(
                EcosystemPopulation(
                    speciesId = "dromedary-camel",
                    habitat = Habitat.LAND_SURFACE,
                    strategy = EcoStrategy.GRAZING,
                    activeBiomassKg = 12_500.0,
                    reservesKg = 800.0,
                    dormantBiomassKg = 20.0,
                ),
            ),
            resources = FunctionalResources(
                carrion = 15.0,
                detritus = 22.0,
                waste = 9.0,
                marineSnow = 0.0,
            ),
            reefCover = 0.0,
        )
        val saveFile = Files.createTempFile("tile-ecosystem", ".json.gz")
        try {
            Serialization.save(saveFile.toString(), original)
            val restored = Serialization.load(saveFile.toString(), TileEcosystem::class.java)

            assertEquals(original, restored)
            assertEquals(1, restored.community(ecology).size)
            assertEquals("dromedary-camel", restored.populations.single().speciesId)
        } finally {
            saveFile.deleteIfExists()
        }
    }

    private fun assertSuitable(
        speciesId: String,
        preset: HersfeldtClimatePreset,
    ) {
        val result = evaluate(speciesId, preset)
        assertTrue(
            result.suitable,
            "$speciesId should suit ${preset.displayName}: $result",
        )
    }

    private fun assertUnsuitable(
        speciesId: String,
        preset: HersfeldtClimatePreset,
    ) {
        val result = evaluate(speciesId, preset)
        assertFalse(
            result.suitable,
            "$speciesId should not suit ${preset.displayName}: $result",
        )
    }

    private fun evaluate(
        speciesId: String,
        preset: HersfeldtClimatePreset,
    ): SpeciesSuitability {
        val species = ecology.species.single { it.id == speciesId }
        return EcologySuitability.evaluate(species, ecology, environment(preset))
    }

    private fun climateFixtures(): List<Pair<String, List<SeasonalCellEnvironment>>> =
        buildList {
            HersfeldtClimatePresets.ALL.forEach { preset ->
                add(preset.id to environment(preset))
                if (!preset.ocean) {
                    add("${preset.id}-river" to environment(preset, majorRiver = true))
                    add("${preset.id}-coast" to environment(preset, coastal = true))
                    add("${preset.id}-highland" to environment(preset, elevationM = 3_500.0))
                }
            }
        }

    private fun environment(
        preset: HersfeldtClimatePreset,
        majorRiver: Boolean = false,
        coastal: Boolean = false,
        elevationM: Double = 0.0,
    ): List<SeasonalCellEnvironment> {
        val annualTemperature = preset.months.map { it.averageTemperature }.average()
        val canopy = when (preset) {
            HersfeldtClimatePresets.JUNGLE -> 0.90
            HersfeldtClimatePresets.BOREAL -> 0.72
            HersfeldtClimatePresets.OCEANIC_TEMPERATE -> 0.58
            HersfeldtClimatePresets.SAVANNA -> 0.18
            else -> 0.03
        }
        val waterDepth = when (preset) {
            HersfeldtClimatePresets.TROPICAL_REEF -> 35.0
            HersfeldtClimatePresets.TEMPERATE_SHELF -> 120.0
            HersfeldtClimatePresets.POLAR_SEA -> 250.0
            HersfeldtClimatePresets.PERMANENT_SEA_ICE -> 250.0
            HersfeldtClimatePresets.DEEP_OCEAN -> 2_500.0
            else -> 0.0
        }
        return preset.months.map { month ->
            SeasonalCellEnvironment.create(
                areaKm2 = 40_000.0,
                temperatureC = month.averageTemperature,
                annualAverageTemperatureC = annualTemperature,
                insolation = (month.insolation / 345.0).coerceIn(0.0, 1.0),
                precipitationMm = month.precipitation,
                isLand = !preset.ocean,
                adjacentToOcean = if (coastal) 1.0 else 0.0,
                adjacentToLand =
                if (preset == HersfeldtClimatePresets.PERMANENT_SEA_ICE) 1.0 else 0.0,
                adjacentToMajorRiver = if (majorRiver) 1.0 else 0.0,
                elevationM = elevationM,
                waterDepthM = waterDepth,
                permanentSeaIce =
                preset.ocean &&
                    PlanetEcologyEnvironment.supportsSeaIceHabitat(
                        preset.climateDatum(tileId = 1),
                    ),
                usefulSunlightReachesWater =
                preset != HersfeldtClimatePresets.DEEP_OCEAN,
                canopyCover = canopy,
                reefCover =
                if (preset == HersfeldtClimatePresets.TROPICAL_REEF) 0.75 else 0.0,
            )
        }
    }
}