package dev.biserman.planet.planet.ecology

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class EcologyExperimentsTest {
    @Test
    fun `similar resident and invader can partition a broad niche`() {
        val ecology = EcologyCompiler.compile(
            listOf(
                producer("producer"),
                grazer("resident"),
                grazer("water-storing invader", extraTraits = listOf(CommonTrait.WATER_STORAGE_TISSUE)),
            ),
        )
        val environment = environment(precipitationMm = 95.0)
        val runtime = EcologyRuntime(ecology)
        val community = TileCommunity().also {
            addEstablished(it, ecology, environment, speciesIndex = 0, biomass = 2_000_000.0)
            addEstablished(it, ecology, environment, speciesIndex = 1, biomass = 700_000.0)
        }

        repeat(250) { runtime.advanceSeason(community, environment) }
        addEstablished(community, ecology, environment, speciesIndex = 2, biomass = 180_000.0)
        repeat(600) { runtime.advanceSeason(community, environment) }

        assertTrue(community.find(1) >= 0)
        assertTrue(community.find(2) >= 0)
    }

    @Test
    fun `unchanged ecosystem settles into bounded variation`() {
        val ecology = EcologyCompiler.compile(
            listOf(producer("producer"), grazer("grazer"), predator("predator"), scavenger("scavenger")),
        )
        val environment = environment()
        val community = seededCommunity(ecology, environment, listOf(2_000_000.0, 700_000.0, 220_000.0, 90_000.0))
        val runtime = experimentalRuntime(ecology)
        val tail = DoubleArray(160)

        repeat(900) { turn ->
            runtime.advanceSeason(community, environment)
            if (turn >= 740) tail[turn - 740] = community.totalBiomass()
        }
        val mean = tail.average()
        val coefficientOfVariation = standardDeviation(tail, mean) / mean

        println(
            "ECOLOGY_STABILITY final_biomass=${"%.3f".format(community.totalBiomass())} " +
                "tail_cv=${"%.6f".format(coefficientOfVariation)} populations=${community.size}",
        )
        assertTrue(mean > 0.0)
        assertTrue(mean < terrestrialBiomassGuardrail(environment))
        assertTrue(coefficientOfVariation < 0.12)
    }

    @Test
    fun `invasion perturbs residents and then remains bounded`() {
        val ecology = EcologyCompiler.compile(
            listOf(
                producer("producer"),
                grazer("resident"),
                grazer("invader", extraTraits = listOf(CommonTrait.WATER_STORAGE_TISSUE)),
            ),
        )
        val environment = environment(precipitationMm = 95.0)
        val runtime = experimentalRuntime(ecology)
        val community = TileCommunity().also {
            addEstablished(it, ecology, environment, speciesIndex = 0, biomass = 2_000_000.0)
            addEstablished(it, ecology, environment, speciesIndex = 1, biomass = 700_000.0)
        }
        repeat(300) { runtime.advanceSeason(community, environment) }
        val residentBefore = biomassOf(community, 1)
        assertTrue(residentBefore > 0.0)
        addEstablished(community, ecology, environment, speciesIndex = 2, biomass = 180_000.0)
        repeat(240) { runtime.advanceSeason(community, environment) }
        val residentAfter = biomassOf(community, 1)
        val invaderAfter = biomassOf(community, 2)

        println(
            "ECOLOGY_INVASION resident_before=${"%.3f".format(residentBefore)} " +
                "resident_after=${"%.3f".format(residentAfter)} " +
                "invader_after=${"%.3f".format(invaderAfter)} " +
                "total_after=${"%.3f".format(community.totalBiomass())}",
        )
        assertTrue(community.totalBiomass().isFinite())
        assertTrue(community.totalBiomass() < terrestrialBiomassGuardrail(environment))
        assertTrue(residentAfter != residentBefore || invaderAfter > 0.0)
    }

    @Test
    fun `temporary climate shock uses dormancy and permits recovery`() {
        val ecology = EcologyCompiler.compile(
            listOf(
                producer(
                    "dormant-producer",
                    extraTraits = listOf(CommonTrait.WHOLE_BODY_ANHYDROBIOSIS),
                ),
                grazer(
                    "torpid-grazer",
                    extraTraits = listOf(CommonTrait.SEASONAL_TORPOR),
                ),
            ),
        )
        val normal = environment()
        val shock = environment(temperatureC = 68.0, precipitationMm = 2.0)
        val runtime = experimentalRuntime(ecology)
        val community = seededCommunity(ecology, normal, listOf(2_000_000.0, 650_000.0))
        repeat(220) { runtime.advanceSeason(community, normal) }
        val baseline = community.totalBiomass()
        var dormantPeak = 0.0
        repeat(12) {
            runtime.advanceSeason(community, shock)
            dormantPeak = maxOf(dormantPeak, dormantTotal(community))
        }
        val afterShock = community.totalBiomass()
        repeat(180) { runtime.advanceSeason(community, normal) }
        val recovered = community.totalBiomass()

        println(
            "ECOLOGY_CLIMATE baseline=${"%.3f".format(baseline)} " +
                "after_shock=${"%.3f".format(afterShock)} " +
                "dormant_peak=${"%.3f".format(dormantPeak)} " +
                "recovered=${"%.3f".format(recovered)}",
        )
        assertTrue(dormantPeak > 0.0)
        assertTrue(afterShock > 0.0)
        assertTrue(recovered > afterShock)
        assertTrue(recovered < terrestrialBiomassGuardrail(normal))
    }

    private fun experimentalRuntime(ecology: CompiledEcology) = EcologyRuntime(
        ecology,
        EcologyRuntimeConfig(minimumViableIndividuals = 0.0),
    )

    private fun seededCommunity(
        ecology: CompiledEcology,
        environment: SeasonalCellEnvironment,
        biomasses: List<Double>,
    ) = TileCommunity().also { community ->
        biomasses.forEachIndexed { speciesIndex, biomass ->
            addEstablished(community, ecology, environment, speciesIndex, biomass)
        }
    }

    private fun addEstablished(
        community: TileCommunity,
        ecology: CompiledEcology,
        environment: SeasonalCellEnvironment,
        speciesIndex: Int,
        biomass: Double,
    ) {
        val niche = NicheSelection.choose(ecology.species[speciesIndex], ecology, environment)
        require(niche >= 0)
        community.add(speciesIndex, niche, biomass, reserves = biomass * 0.12)
    }

    private fun biomassOf(community: TileCommunity, speciesIndex: Int): Double {
        val index = community.find(speciesIndex)
        return if (index < 0) {
            0.0
        } else {
            community.activeBiomass[index] + community.dormantBiomass[index]
        }
    }

    private fun dormantTotal(community: TileCommunity): Double {
        var total = 0.0
        for (index in 0 until community.size) total += community.dormantBiomass[index]
        return total
    }

    private fun standardDeviation(values: DoubleArray, mean: Double): Double =
        sqrt(values.sumOf { (it - mean).pow(2) } / values.size)

    /**
     * Emergency runaway check scaled to tile area and the largest authored
     * terrestrial producer density. This is intentionally much looser than an
     * ecological target; trophic-proportion tests provide the meaningful bounds.
     */
    private fun terrestrialBiomassGuardrail(environment: SeasonalCellEnvironment): Double =
        environment.areaKm2 *
            EcologyBiomass.terrestrialProducerDensityKgKm2.values.max() *
            10.0

    private fun producer(
        id: String,
        extraTraits: List<SpeciesTrait> = emptyList(),
    ) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = SizeClass.SMALL,
        motile = false,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
        ) + extraTraits,
        photosyntheticColor = BiologicalColor.GREEN,
    )

    private fun grazer(
        id: String,
        extraTraits: List<SpeciesTrait> = emptyList(),
    ) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = SizeClass.MEDIUM,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.HETEROTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.FAT_RESERVES,
        ) + extraTraits,
        camouflageColor = BiologicalColor.BROWN,
    )

    private fun predator(id: String) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = SizeClass.LARGE,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.CAMOUFLAGE_PATTERN,
        ),
        camouflageColor = BiologicalColor.BROWN,
    )

    private fun scavenger(id: String) = SpeciesDefinition(
        id = id,
        displayName = id,
        sizeClass = SizeClass.MEDIUM,
        motile = true,
        traits = listOf(
            CommonTrait.TEMPERATE_BIOCHEMISTRY,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.SCAVENGING_SENSES,
        ),
        camouflageColor = BiologicalColor.BLACK,
    )

    private fun environment(
        temperatureC: Double = 21.0,
        precipitationMm: Double = 85.0,
    ) = SeasonalCellEnvironment.create(
        areaKm2 = 40_000.0,
        temperatureC = temperatureC,
        annualAverageTemperatureC = 17.0,
        insolation = 0.8,
        precipitationMm = precipitationMm,
        surfaceFertilityModifier = 0.5,
        isLand = true,
        resources = FunctionalResources(
            carrion = 0.42,
        ),
    )
}