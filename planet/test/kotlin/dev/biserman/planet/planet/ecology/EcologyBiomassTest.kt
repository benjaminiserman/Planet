package dev.biserman.planet.planet.ecology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcologyBiomassTest {
    @Test
    fun `authored biomass tables cover every size class`() {
        val allSizes = SizeClass.entries.toSet()

        assertEquals(allSizes, EcologyBiomass.terrestrialProducerDensityKgKm2.keys)
        assertEquals(allSizes, EcologyBiomass.aquaticProducerDensityKgKm2.keys)
        assertEquals(allSizes, EcologyBiomass.terrestrialGrazingAccessibilityBySize.keys)
        assertEquals(allSizes, EcologyBiomass.filterFeedingEfficiencyBySize.keys)
        assertEquals(allSizes, EcologyBiomass.aquaticFilterFeederProducerFractionBySize.keys)
    }

    @Test
    fun `large terrestrial producers store more standing biomass but expose less to grazers`() {
        val producerDensity = EcologyBiomass.terrestrialProducerDensityKgKm2
        val accessibility = EcologyBiomass.terrestrialGrazingAccessibilityBySize

        assertTrue(producerDensity.getValue(SizeClass.HUGE) > producerDensity.getValue(SizeClass.TINY))
        assertTrue(accessibility.getValue(SizeClass.HUGE) < accessibility.getValue(SizeClass.TINY))
        assertTrue(
            EcologyBiomass.filterFeedingEfficiency(SizeClass.HUGE) <
                EcologyBiomass.filterFeedingEfficiency(SizeClass.MEDIUM),
        )
    }

    @Test
    fun `ordinary ocean tile supports an order of magnitude plausible plankton stock`() {
        val ecology = EcologyCompiler.compile(listOf(InvariantSpecies.PLANKTON))
        val plankton = ecology.species.single()
        val environment = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 18.0,
            insolation = 0.8,
            precipitationMm = 0.0,
            isLand = false,
            waterDepthM = 1_000.0,
        )
        val nicheIndex = NicheSelection.choose(plankton, ecology, environment)
        val carryingCapacity = EcologyBiomass.carryingCapacityKg(
            plankton,
            ecology.niches[nicheIndex],
            environment,
        )

        assertTrue(carryingCapacity in 1.0e9..1.0e11)
    }

    @Test
    fun `dry climates support much less terrestrial producer biomass`() {
        val ecology = EcologyCompiler.compile(listOf(InvariantSpecies.CARPET_PLANTS))
        val producer = ecology.species.single()
        fun environment(precipitationMm: Double) = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 24.0,
            insolation = 0.8,
            precipitationMm = precipitationMm,
            isLand = true,
        )
        val dry = environment(10.0)
        val temperate = environment(80.0)
        val nicheIndex = NicheSelection.choose(producer, ecology, temperate)
        val niche = ecology.niches[nicheIndex]
        val dryCapacity = EcologyBiomass.carryingCapacityKg(producer, niche, dry)
        val temperateCapacity = EcologyBiomass.carryingCapacityKg(producer, niche, temperate)

        assertTrue(dry.waterAvailability < temperate.waterAvailability)
        assertTrue(
            dryCapacity < temperateCapacity * 0.20,
            "dry=${"%.3e".format(dryCapacity)} temperate=${"%.3e".format(temperateCapacity)}",
        )
    }

    @Test
    fun `modeled prey consumers receive a trophic ceiling without background food`() {
        val ecology = EcologyCompiler.compile(listOf(InvariantSpecies.BUGS))
        val bugs = ecology.species.single()
        val environment = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 20.0,
            insolation = 0.8,
            precipitationMm = 800.0,
            isLand = true,
        )
        val nicheIndex = NicheSelection.choose(bugs, ecology, environment)
        val niche = ecology.niches[nicheIndex]
        val carryingCapacity =
            EcologyBiomass.carryingCapacityKg(bugs, niche, environment)
        val expectedCapacity =
            environment.areaKm2 *
                220.0 *
                bugs.sizeClass.densityScale *
                environment.fertility *
                environment.habitatAvailability(niche.habitat)

        assertEquals(expectedCapacity, carryingCapacity)
        assertEquals(0.0, environment.resourceSupport(niche, bugs.sizeClass))

        val community = TileCommunity()
        community.add(bugs.index, nicheIndex, carryingCapacity * 0.50)
        val runtime = EcologyRuntime(ecology)
        repeat(20) {
            runtime.advanceSeason(community, environment)
        }

        assertTrue(
            community.find(bugs.index) < 0 ||
                community.activeBiomass[community.find(bugs.index)] < carryingCapacity * 0.50,
            "A higher trophic ceiling must not create food where no producer exists",
        )
    }

    @Test
    fun `huge filter feeder remains far below its plankton stock over one thousand years`() {
        val whaleShark = EarthSpeciesCatalog.FISH.single { it.id == "whale-shark" }
        val ecology = EcologyCompiler.compile(listOf(InvariantSpecies.PLANKTON, whaleShark))
        val environment = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 27.0,
            annualAverageTemperatureC = 27.0,
            insolation = 0.80,
            precipitationMm = 1_500.0,
            surfaceFertilityModifier = 0.40,
            isLand = false,
            waterDepthM = 80.0,
        )
        val community = TileCommunity()
        ecology.species.forEach { species ->
            val nicheIndex = NicheSelection.choose(species, ecology, environment)
            val niche = ecology.niches[nicheIndex]
            val carryingCapacity = EcologyBiomass.carryingCapacityKg(species, niche, environment)
            val biomass = if (species.id == InvariantSpecies.PLANKTON.id) {
                carryingCapacity * 0.65
            } else {
                maxOf(species.physiology.massKg * 20.0, carryingCapacity * 0.10)
            }
            community.add(species.index, nicheIndex, biomass, biomass * 0.10)
        }

        val runtime = EcologyRuntime(ecology)
        repeat(4_000) {
            runtime.advanceSeason(community, environment)
        }

        val planktonPopulation = community.find(ecology.speciesIndex(InvariantSpecies.PLANKTON.id))
        val whalePopulation = community.find(ecology.speciesIndex("whale-shark"))
        assertTrue(planktonPopulation >= 0)
        assertTrue(whalePopulation >= 0)
        val planktonBiomass =
            community.activeBiomass[planktonPopulation] + community.dormantBiomass[planktonPopulation]
        val whaleBiomass =
            community.activeBiomass[whalePopulation] + community.dormantBiomass[whalePopulation]

        assertTrue(
            whaleBiomass < planktonBiomass * 0.02,
            "whale=${"%.3e".format(whaleBiomass)} plankton=${"%.3e".format(planktonBiomass)} " +
                "ratio=${"%.5f".format(whaleBiomass / planktonBiomass)}",
        )
    }

    @Test
    fun `coastal producer guilds persist for one thousand years without animal recycling`() {
        val ecology = EcologyCompiler.compile(
            listOf(InvariantSpecies.CARPET_PLANTS, InvariantSpecies.PLANKTON),
        )
        val environment = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 12.0,
            annualAverageTemperatureC = 12.0,
            insolation = 0.72,
            precipitationMm = 900.0,
            surfaceFertilityModifier = 0.10,
            isLand = true,
            adjacentToOcean = 1.0,
            waterDepthM = 40.0,
        )
        val community = TileCommunity()
        ecology.species.forEach { species ->
            val nicheIndex = NicheSelection.choose(species, ecology, environment)
            val carryingCapacity = EcologyBiomass.carryingCapacityKg(
                species,
                ecology.niches[nicheIndex],
                environment,
            )
            community.add(species.index, nicheIndex, carryingCapacity * 0.35)
        }

        val runtime = EcologyRuntime(ecology)
        repeat(4_000) {
            runtime.advanceSeason(community, environment)
        }

        ecology.species.forEach { species ->
            val population = community.find(species.index)
            assertTrue(population >= 0, "${species.displayName} disappeared without consumers")
            val biomass =
                community.activeBiomass[population] + community.dormantBiomass[population]
            assertTrue(
                biomass > EcologyBiomass.carryingCapacityKg(
                    species,
                    ecology.niches[community.nicheIndices[population]],
                    environment,
                ) * 0.05,
                "${species.displayName} continued toward functional extinction",
            )
        }
    }
}