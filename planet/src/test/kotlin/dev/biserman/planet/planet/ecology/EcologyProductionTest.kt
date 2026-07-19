package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.BiotaDistributionAquatic
import dev.biserman.planet.planet.BiotaDistribution
import dev.biserman.planet.planet.BiotaDistributionTerrestrial
import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.ClimateDatumSample
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EcologyProductionTest {
    @Test
    fun `taxonomic order mapping covers every catalog species exactly once`() {
        assertEquals(speciesCatalog.size, speciesCatalogById.size)
        assertEquals(speciesCatalog.map { it.id }.toSet(), taxonomicOrderBySpeciesId.keys)
        assertEquals(speciesCatalog.map { it.id }.toSet(), speciesCatalogByTaxonomicOrder.values
            .flatten()
            .map { it.id }
            .toSet())
        speciesCatalog.forEach { species ->
            assertEquals(taxonomicOrderBySpeciesId.getValue(species.id), species.taxonomicOrder)
        }
    }

    @Test
    fun `distribution order pools respect aquatic habitat`() {
        val aquaticOrders = EcologyRuntime.ordersCompatibleWith(BiotaDistributionAquatic)
        val terrestrialOrders = EcologyRuntime.ordersCompatibleWith(BiotaDistributionTerrestrial)

        assertTrue(aquaticOrders.isNotEmpty())
        assertTrue(terrestrialOrders.isNotEmpty())
        with(EcologyRuntime) {
            assertTrue(aquaticOrders.all { order ->
                speciesCatalogByTaxonomicOrder.getValue(order)
                    .any { it.matchesHabitat(BiotaDistributionAquatic) }
            })
            assertTrue(terrestrialOrders.all { order ->
                speciesCatalogByTaxonomicOrder.getValue(order)
                    .any { it.matchesHabitat(BiotaDistributionTerrestrial) }
            })
            listOf("reeds", "beaver", "capybara", "walrus").forEach { speciesId ->
                val species = speciesCatalogById.getValue(speciesId)
                assertTrue(Trait.AMPHIBIOUS in species.traits)
                assertFalse(species.matchesHabitat(BiotaDistributionAquatic))
                assertTrue(species.matchesHabitat(BiotaDistributionTerrestrial))
            }
            val phytoplankton = speciesCatalogById.getValue("phytoplankton")
            assertTrue(phytoplankton.matchesHabitat(BiotaDistributionAquatic))
            assertFalse(phytoplankton.matchesHabitat(BiotaDistributionTerrestrial))

            val emperorPenguin = speciesCatalogById.getValue("emperor_penguin")
            assertTrue(emperorPenguin.matchesHabitat(BiotaDistributionAquatic))
            assertTrue(emperorPenguin.matchesHabitat(BiotaDistributionTerrestrial))
        }
    }

    @Test
    fun `biota distribution order assignment never reuses an order`() {
        val usedOrders = mutableSetOf<TaxonomicOrder>()
        val random = Random(1234)
        repeat(10) { index ->
            val method = if (index % 2 == 0) {
                BiotaDistributionTerrestrial
            } else {
                BiotaDistributionAquatic
            }
            val order = BiotaDistribution.randomAvailableOrder(method, usedOrders, random)
            assertTrue(usedOrders.add(order))
            assertTrue(order in EcologyRuntime.ordersCompatibleWith(method))
        }
    }

    @Test
    fun `global order configuration is explicit and stable`() {
        assertEquals(
            setOf(
                TaxonomicOrder.POALES,
                TaxonomicOrder.FABALES,
                TaxonomicOrder.ASTERALES,
                TaxonomicOrder.DINOPHYSIALES,
                TaxonomicOrder.RODENTIA,
                TaxonomicOrder.CARNIVORA,
                TaxonomicOrder.PASSERIFORMES,
                TaxonomicOrder.ACCIPITRIFORMES,
                TaxonomicOrder.COLEOPTERA,
                TaxonomicOrder.DIPTERA,
                TaxonomicOrder.LEPIDOPTERA,
                TaxonomicOrder.ORTHOPTERA,
                TaxonomicOrder.LAMINARIALES,
                TaxonomicOrder.ALISMATALES,
                TaxonomicOrder.BACILLARIALES,
                TaxonomicOrder.CALANOIDA,
                TaxonomicOrder.DECAPODA,
                TaxonomicOrder.CLUPEIFORMES,
                TaxonomicOrder.SCLERACTINIA,
            ),
            EcologyConfig.globallyDistributedOrders.toSet(),
        )
    }

    @Test
    fun `year round suitability rejects climate opposites`() {
        fun climate(samples: List<ClimateDatumSample>) = ClimateDatum(
            tileId = 1,
            months = List(12) { index -> samples[index % samples.size] },
        )
        val tundra = climate(listOf(
            ClimateDatumSample(-27.0, 20.0, 25.0),
            ClimateDatumSample(5.0, 180.0, 45.0),
        ))
        val desert = climate(listOf(
            ClimateDatumSample(18.0, 180.0, 8.0),
            ClimateDatumSample(35.0, 310.0, 2.0),
        ))

        assertFalse(isSpeciesSuitedTo(speciesCatalogById.getValue("lion"), tundra, 0.0))
        assertFalse(isSpeciesSuitedTo(speciesCatalogById.getValue("jaguar"), tundra, 0.0))
        assertFalse(isSpeciesSuitedTo(speciesCatalogById.getValue("arctic_fox"), desert, 0.0))

        val emergencyProducer = speciesCatalogById.getValue("grass")
        val fallbackSelection = selectSpeciesForEcosystem(
            preferredPool = emptyList(),
            producerPool = listOf(emergencyProducer),
            limit = 20,
            random = Random(1234),
        )
        assertEquals(listOf(emergencyProducer), fallbackSelection)
    }

    @Test
    fun `annual suitability permits cold adapted animals through boreal winters`() {
        val boreal = ClimateDatum(
            tileId = 1,
            months = List(12) { month ->
                if (month in 4..8) {
                    ClimateDatumSample(13.0, 190.0, 55.0)
                } else {
                    ClimateDatumSample(-16.0, 45.0, 35.0)
                }
            },
        )

        assertTrue(isSpeciesSuitedTo(speciesCatalogById.getValue("caribou"), boreal, 500.0))
        assertTrue(isSpeciesSuitedTo(speciesCatalogById.getValue("hare"), boreal, 500.0))
    }

    @Test
    fun `marine catalog provides producers grazers filter feeders and predators`() {
        val marineSpeciesIds = setOf(
            "kelp", "seagrass", "diatoms", "zooplankton", "jellyfish", "sea_urchin",
            "mussel", "crab", "shrimp", "squid", "octopus", "sardine", "anchovy",
            "mackerel", "cod", "salmon", "parrotfish", "reef_coral", "sea_sponge",
        )
        val marineSpecies = marineSpeciesIds.map(speciesCatalogById::getValue)

        assertTrue(marineSpecies.all { Trait.AQUATIC in it.traits })
        assertTrue(marineSpecies.any { it.autotrophy != null })
        assertTrue(marineSpecies.any { Trait.FILTER_FEEDER in it.traits })
        assertTrue(marineSpecies.any { Trait.CARNIVORE in it.traits })

        val zooplanktonDiet = speciesCatalogById.getValue("zooplankton").diet.keys
        assertTrue(zooplanktonDiet.any { it.preyId == "phytoplankton" })
        assertTrue(zooplanktonDiet.any { it.preyId == "diatoms" })
        assertTrue(speciesCatalogById.getValue("cod").diet.isNotEmpty())
    }

    @Test
    fun `seasonal noise seeds are deterministic and tile scoped`() {
        val seed = seasonalNoiseSeed(1234, 56L, 78)

        assertEquals(seed, seasonalNoiseSeed(1234, 56L, 78))
        assertFalse(seed == seasonalNoiseSeed(1234, 57L, 78))
        assertFalse(seed == seasonalNoiseSeed(1234, 56L, 79))
    }

    @Test
    fun `ecosystem selection respects its cap and guarantees a producer`() {
        val candidates = listOf(
            speciesCatalogById.getValue("grass"),
            speciesCatalogById.getValue("rabbit"),
            speciesCatalogById.getValue("wolf"),
        )
        val selected = selectSpeciesForEcosystem(
            preferredPool = candidates,
            producerPool = candidates.filter { it.autotrophy != null },
            limit = 2,
            random = Random(1234),
        )

        assertEquals(2, selected.size)
        assertTrue(selected.any { it.autotrophy != null })
    }

    @Test
    fun `one seasonal step remains finite and reports absolute time`() {
        val selected = listOf(speciesCatalogById.getValue("grass"), speciesCatalogById.getValue("rabbit"))
        val model = EcosystemModel(
            species = selected,
            seasonsEnabled = true,
            landArea = 10_000.0,
            climate = oceanicTemperateClimate,
            altitudeMeters = 500.0,
        )
        val history = simulate(
            model = model,
            initial = mapOf("grass" to 5_000.0, "rabbit" to 100.0),
            years = 0.25,
            dt = 0.25 / 16.0,
            sampleEverySteps = 16,
            noise = PopulationNoise.NONE,
            random = Random(1234),
            minimumIndividuals = 0.0,
            startYear = 7.5,
        )

        assertEquals(7.5, history.first().year, 1e-12)
        assertEquals(7.75, history.last().year, 1e-12)
        assertTrue(history.last().biomass.values.all { it.isFinite() && it >= 0.0 })
    }

    @Test
    fun `extinction threshold is based on individual count`() {
        val rabbit = speciesCatalogById.getValue("rabbit")
        val model = EcosystemModel(listOf(rabbit), seasonsEnabled = false, landArea = 1.0)

        assertEquals(
            0.0,
            applyIndividualExtinctionThreshold(
                mapOf("rabbit" to rabbit.individualBiomass * 1.999),
                model,
                minimumIndividuals = 2.0,
            ).getValue("rabbit"),
        )
        assertTrue(applyIndividualExtinctionThreshold(
            mapOf("rabbit" to rabbit.individualBiomass * 2.0),
            model,
            minimumIndividuals = 2.0,
        ).getValue("rabbit") > 0.0)
    }

    @Test
    fun `suitability accounts for elevation and explicit niche limits`() {
        val seaLevel = ClimateDatumSample(20.0, 200.0, 100.0)
        assertEquals(13.5, climateSampleAtElevation(seaLevel, 1_000.0).averageTemperature, 1e-12)

        val cactus = speciesCatalogById.getValue("cactus")
        assertFalse(isSpeciesSuitedTo(cactus, ClimateDatumSample(20.0, 200.0, 500.0), 0.0))
        assertTrue(speciesSuitabilityIssues(cactus, ClimateDatumSample(20.0, 200.0, 500.0), 0.0)
            .contains("too wet"))
    }
}
