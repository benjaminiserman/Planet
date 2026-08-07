package dev.biserman.planet.planet.ecology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcologyEnvironmentTest {
    @Test
    fun `planet adapter preserves signed elevation`() {
        assertEquals(-750.0, PlanetEcologyEnvironment.signedElevationM(-750.0))
        assertEquals(4_500.0, PlanetEcologyEnvironment.signedElevationM(4_500.0))
    }

    @Test
    fun `major rivers expose freshwater and increase land water availability`() {
        val dry = land(adjacentToMajorRiver = 0.0)
        val partialRiver = land(adjacentToMajorRiver = 0.25)
        val river = land(adjacentToMajorRiver = 1.0)

        assertEquals(0.0, dry.habitatAvailability(Habitat.FRESHWATER))
        assertEquals(0.105, partialRiver.habitatAvailability(Habitat.FRESHWATER), 1e-12)
        assertEquals(0.42, river.habitatAvailability(Habitat.FRESHWATER), 1e-12)
        assertTrue(river.waterAvailability > dry.waterAvailability)
        assertEquals(
            (river.waterAvailability - dry.waterAvailability) * 0.25,
            partialRiver.waterAvailability - dry.waterAvailability,
            1e-12,
        )
    }

    @Test
    fun `shared coastline fraction scales coastal habitat`() {
        val land = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 20.0,
            insolation = 0.8,
            precipitationMm = 35.0,
            isLand = true,
            adjacentToOcean = 0.25,
        )
        val ocean = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 20.0,
            insolation = 0.8,
            precipitationMm = 35.0,
            isLand = false,
            adjacentToLand = 0.25,
        )

        assertEquals(0.12, land.habitatAvailability(Habitat.COASTAL), 1e-12)
        assertEquals(0.25, ocean.habitatAvailability(Habitat.COASTAL), 1e-12)
        assertEquals(0.25, land.adjacentToOcean)
        assertEquals(0.25, ocean.adjacentToLand)
    }

    @Test
    fun `water depth and useful light select aquatic compartments`() {
        val shallow = ocean(waterDepthM = 40.0, usefulSunlightReachesWater = true)
        val deep = ocean(waterDepthM = 900.0, usefulSunlightReachesWater = true)
        val darkSurface = ocean(waterDepthM = 40.0, usefulSunlightReachesWater = false)

        assertTrue(shallow.habitatAvailability(Habitat.SUNLIT_WATER) > 0.0)
        assertEquals(0.0, shallow.habitatAvailability(Habitat.DARK_WATER))
        assertTrue(deep.habitatAvailability(Habitat.DARK_WATER) > 0.0)
        assertEquals(0.0, darkSurface.habitatAvailability(Habitat.SUNLIT_WATER))
        assertTrue(darkSurface.habitatAvailability(Habitat.DARK_WATER) > 0.0)
    }

    @Test
    fun `hard-coded pigments favor different star colors`() {
        val greenAtYellow = LightColorModel.photosyntheticMatch(StarLight.YELLOW, BiologicalColor.GREEN)
        val redAtYellow = LightColorModel.photosyntheticMatch(StarLight.YELLOW, BiologicalColor.RED)
        val greenAtRed = LightColorModel.photosyntheticMatch(StarLight.RED, BiologicalColor.GREEN)
        val redAtRed = LightColorModel.photosyntheticMatch(StarLight.RED, BiologicalColor.RED)

        assertTrue(greenAtYellow > redAtYellow)
        assertTrue(redAtRed > greenAtRed)
    }

    @Test
    fun `authored light table covers every strongly typed star`() {
        assertEquals(
            StarLight.entries.toSet(),
            LightColorModel.authoredCompatibility.keys,
            "Every StarLight needs an authored photosynthetic compatibility table",
        )
    }

    @Test
    fun `adding a biological color requires an explicit light compatibility`() {
        val expectedColors = BiologicalColor.entries.toSet()
        LightColorModel.authoredCompatibility.forEach { (starLight, compatibility) ->
            assertEquals(
                expectedColors,
                compatibility.byPigment.keys,
                "$starLight is missing an authored BiologicalColor compatibility",
            )
        }
    }

    @Test
    fun `pale matches open ground while white matches snow`() {
        val paleOpen = Habitat.LAND_SURFACE.camouflageMatch(
            BiologicalColor.PALE,
            snowOrIce = false,
            canopyCover = 0.1,
            reefCover = 0.0,
        )
        val paleForest = Habitat.LAND_SURFACE.camouflageMatch(
            BiologicalColor.PALE,
            snowOrIce = false,
            canopyCover = 0.8,
            reefCover = 0.0,
        )
        val paleSnow = Habitat.LAND_SURFACE.camouflageMatch(
            BiologicalColor.PALE,
            snowOrIce = true,
            canopyCover = 0.1,
            reefCover = 0.0,
        )
        val whiteSnow = Habitat.LAND_SURFACE.camouflageMatch(
            BiologicalColor.WHITE,
            snowOrIce = true,
            canopyCover = 0.1,
            reefCover = 0.0,
        )
        val whiteOpen = Habitat.LAND_SURFACE.camouflageMatch(
            BiologicalColor.WHITE,
            snowOrIce = false,
            canopyCover = 0.1,
            reefCover = 0.0,
        )

        assertTrue(paleOpen > paleForest)
        assertTrue(whiteSnow > paleSnow)
        assertTrue(whiteSnow > whiteOpen)
    }

    @Test
    fun `countershading matches sunlit water but not dark water`() {
        val countershadedSunlit = Habitat.SUNLIT_WATER.camouflageMatch(
            BiologicalColor.COUNTERSHADE,
            snowOrIce = false,
            canopyCover = 0.0,
            reefCover = 0.0,
        )
        val blueGreenSunlit = Habitat.SUNLIT_WATER.camouflageMatch(
            BiologicalColor.BLUE,
            snowOrIce = false,
            canopyCover = 0.0,
            reefCover = 0.0,
        )
        val countershadedDark = Habitat.DARK_WATER.camouflageMatch(
            BiologicalColor.COUNTERSHADE,
            snowOrIce = false,
            canopyCover = 0.0,
            reefCover = 0.0,
        )

        assertTrue(countershadedSunlit > blueGreenSunlit)
        assertTrue(countershadedSunlit > countershadedDark)
    }

    @Test
    fun `grazing obtains food only from modeled producer populations`() {
        assertEquals(
            0.0,
            EcoStrategy.GRAZING.resourceSupport(
                land(),
                Habitat.LAND_SURFACE,
                SizeClass.MEDIUM,
            ),
        )
    }

    @Test
    fun `functional organic resources start empty`() {
        assertEquals(FunctionalResources(), land().resources)
    }

    @Test
    fun `decomposition and coprophagy use their dynamic organic resources`() {
        val environment = land().withResources(
            FunctionalResources(detritus = 0.62, waste = 0.37),
        )

        assertEquals(
            0.62,
            EcoStrategy.DECOMPOSITION.resourceSupport(
                environment,
                Habitat.LAND_SURFACE,
                SizeClass.SMALL,
            ),
        )
        assertEquals(
            0.37,
            EcoStrategy.COPROPHAGY.resourceSupport(
                environment,
                Habitat.LAND_SURFACE,
                SizeClass.SMALL,
            ),
        )
    }

    @Test
    fun `dry burrow nests have an upper water limit but generic burrowing does not`() {
        fun burrower(id: String, dryNest: Boolean) = SpeciesDefinition(
            id = id,
            displayName = id,
            sizeClass = SizeClass.SMALL,
            motile = true,
            traits = listOfNotNull(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ECTOTHERMY,
                CommonTrait.SUBTERRANEAN_BURROWING,
                CommonTrait.AMBUSH_MUSCULATURE,
                CommonTrait.DRY_BURROW_NEST.takeIf { dryNest },
            ),
        )
        val ecology = EcologyCompiler.compile(
            listOf(burrower("generic-burrower", false), burrower("dry-nester", true)),
        )
        val saturated = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 20.0,
            insolation = 0.7,
            precipitationMm = 10_000.0,
            isLand = true,
        )
        val genericFit = EcologyFitness.water(ecology.species[0], saturated, Habitat.LAND_SURFACE)
        val dryNestFit = EcologyFitness.water(ecology.species[1], saturated, Habitat.LAND_SURFACE)

        assertEquals(1.0, genericFit)
        assertTrue(dryNestFit < genericFit)
    }

    @Test
    fun `thermal strategies affect activity fitness`() {
        fun grazer(id: String, thermalTrait: CommonTrait) = SpeciesDefinition(
            id = id,
            displayName = id,
            sizeClass = SizeClass.MEDIUM,
            motile = true,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                thermalTrait,
                CommonTrait.TERRESTRIAL_LOCOMOTION,
                CommonTrait.GRAZING_MOUTHPARTS,
            ),
        )
        val ecology = EcologyCompiler.compile(
            listOf(
                grazer("ectotherm", CommonTrait.ECTOTHERMY),
                grazer("endotherm", CommonTrait.ENDOTHERMY),
            ),
        )
        val cold = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 4.0,
            insolation = 0.5,
            precipitationMm = 60.0,
            isLand = true,
        )

        assertTrue(
            EcologyFitness.thermal(ecology.species[1], cold) >
                EcologyFitness.thermal(ecology.species[0], cold),
        )
    }

    @Test
    fun `seasonal coat responds to low insolation rather than cold alone`() {
        val coated = SpeciesDefinition(
            id = "coated",
            displayName = "Coated grazer",
            sizeClass = SizeClass.MEDIUM,
            motile = true,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ENDOTHERMY,
                CommonTrait.TERRESTRIAL_LOCOMOTION,
                CommonTrait.GRAZING_MOUTHPARTS,
                CommonTrait.SEASONAL_WINTER_COAT,
            ),
        )
        val species = EcologyCompiler.compile(listOf(coated)).species.single()
        val winterFitness = EcologyFitness.seasonalTemperature(species, -5.0, insolation = 0.18)
        val brightColdFitness = EcologyFitness.seasonalTemperature(species, -5.0, insolation = 0.85)

        assertTrue(winterFitness > brightColdFitness)
    }

    @Test
    fun `colony thermoregulation buffers dim winters and hot summers`() {
        val bee = EarthSpeciesCatalog.ALL.single { it.id == "western-honey-bee" }
        val unregulated = bee.copy(
            id = "unregulated-honey-bee",
            traits = bee.traits - CommonTrait.COLONY_THERMOREGULATION,
        )
        val compiled = EcologyCompiler.compile(listOf(bee, unregulated)).species
        val regulated = compiled[0]
        val ordinary = compiled[1]

        assertTrue(
            EcologyFitness.seasonalTemperature(regulated, 0.0, insolation = 0.15) >
                EcologyFitness.seasonalTemperature(ordinary, 0.0, insolation = 0.15),
        )
        assertTrue(
            EcologyFitness.temperature(regulated, 31.0) >
                EcologyFitness.temperature(ordinary, 31.0),
        )
        assertTrue(regulated.physiology.maintenanceDemand > ordinary.physiology.maintenanceDemand)
        assertTrue(regulated.physiology.hydration.minimumWater > ordinary.physiology.hydration.minimumWater)
    }

    @Test
    fun `terrestrial motile elevation fitness declines outside a shifted optimal band`() {
        fun organism(
            id: String,
            motile: Boolean,
            locomotion: CommonTrait,
            altitudeTrait: CommonTrait? = null,
        ) = SpeciesDefinition(
            id = id,
            displayName = id,
            sizeClass = SizeClass.MEDIUM,
            motile = motile,
            traits = listOfNotNull(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ENDOTHERMY.takeIf { motile },
                locomotion,
                CommonTrait.GRAZING_MOUTHPARTS.takeIf { motile },
                CommonTrait.PHOTOSYNTHETIC_SURFACE.takeIf { !motile },
                altitudeTrait,
            ),
            photosyntheticColor = BiologicalColor.GREEN.takeIf { !motile },
        )
        val ecology = EcologyCompiler.compile(
            listOf(
                organism("lowland-grazer", true, CommonTrait.TERRESTRIAL_LOCOMOTION),
                organism(
                    "highland-grazer",
                    true,
                    CommonTrait.TERRESTRIAL_LOCOMOTION,
                    CommonTrait.HIGH_AFFINITY_HEMOGLOBIN,
                ),
                organism("flying-grazer", true, CommonTrait.POWERED_FLIGHT),
                organism("rooted-producer", false, CommonTrait.ROOTED_BODY),
            ),
        )
        val lowland = ecology.species[0]
        val highland = ecology.species[1]
        val flying = ecology.species[2]
        val rooted = ecology.species[3]

        assertEquals(0.0, EcologyFitness.elevation(lowland, land(elevationM = -1_000.0), Habitat.LAND_SURFACE))
        assertEquals(0.5, EcologyFitness.elevation(lowland, land(elevationM = -500.0), Habitat.LAND_SURFACE))
        assertEquals(1.0, EcologyFitness.elevation(lowland, land(elevationM = 0.0), Habitat.LAND_SURFACE))
        assertEquals(1.0, EcologyFitness.elevation(lowland, land(elevationM = 2_000.0), Habitat.LAND_SURFACE))
        assertEquals(0.5, EcologyFitness.elevation(lowland, land(elevationM = 2_500.0), Habitat.LAND_SURFACE))
        assertEquals(0.0, EcologyFitness.elevation(lowland, land(elevationM = 3_000.0), Habitat.LAND_SURFACE))
        assertEquals(0.0, EcologyFitness.elevation(highland, land(elevationM = 1_500.0), Habitat.LAND_SURFACE))
        assertEquals(0.5, EcologyFitness.elevation(highland, land(elevationM = 2_000.0), Habitat.LAND_SURFACE))
        assertEquals(1.0, EcologyFitness.elevation(highland, land(elevationM = 2_500.0), Habitat.LAND_SURFACE))
        assertEquals(1.0, EcologyFitness.elevation(highland, land(elevationM = 4_500.0), Habitat.LAND_SURFACE))
        assertEquals(0.5, EcologyFitness.elevation(highland, land(elevationM = 5_000.0), Habitat.LAND_SURFACE))
        assertEquals(0.0, EcologyFitness.elevation(highland, land(elevationM = 5_500.0), Habitat.LAND_SURFACE))
        assertEquals(1.0, EcologyFitness.elevation(flying, land(elevationM = 6_000.0), Habitat.AERIAL))
        assertEquals(1.0, EcologyFitness.elevation(rooted, land(elevationM = 6_000.0), Habitat.LAND_SURFACE))
    }

    @Test
    fun `snow licking supplies water only when snow or ice is present`() {
        val yak = EcologyCompiler.compile(
            listOf(EarthSpeciesCatalog.MAMMALS.single { it.id == "wild-yak" }),
        ).species.single()
        val frozen = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = -8.0,
            annualAverageTemperatureC = -2.0,
            insolation = 0.5,
            precipitationMm = 2.0,
            isLand = true,
        )
        val thawed = SeasonalCellEnvironment.create(
            areaKm2 = 40_000.0,
            temperatureC = 8.0,
            annualAverageTemperatureC = 8.0,
            insolation = 0.7,
            precipitationMm = 2.0,
            isLand = true,
        )

        assertEquals(1.0, EcologyFitness.water(yak, frozen, Habitat.LAND_SURFACE))
        assertTrue(EcologyFitness.water(yak, thawed, Habitat.LAND_SURFACE) < 1.0)
    }

    @Test
    fun `competition can make a locally secondary niche the best establishment choice`() {
        val definition = SpeciesDefinition(
            id = "branch-mat",
            displayName = "Branch mat",
            sizeClass = SizeClass.SMALL,
            motile = false,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.PHOTOSYNTHETIC_SURFACE,
                CommonTrait.ROOTED_BODY,
                CommonTrait.CANOPY_GROWTH,
            ),
            photosyntheticColor = BiologicalColor.GREEN,
        )
        val ecology = EcologyCompiler.compile(listOf(definition))
        val species = ecology.species.single()
        val environment = land(canopyCover = 0.9)
        val unopposed = NicheSelection.choose(species, ecology, environment)
        val competition = DoubleArray(ecology.niches.size)
        competition[unopposed] = 1_000.0
        val diverted = NicheSelection.choose(species, ecology, environment, competition)

        assertTrue(unopposed >= 0)
        assertTrue(diverted >= 0)
        assertTrue(diverted != unopposed)
    }

    @Test
    fun `radiation selects an intrinsic niche instead of escaping into an empty fallback`() {
        val definition = SpeciesDefinition(
            id = "branch-mat",
            displayName = "Branch mat",
            sizeClass = SizeClass.SMALL,
            motile = false,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.PHOTOSYNTHETIC_SURFACE,
                CommonTrait.ROOTED_BODY,
                CommonTrait.CANOPY_GROWTH,
            ),
            photosyntheticColor = BiologicalColor.GREEN,
        )
        val ecology = EcologyCompiler.compile(listOf(definition))
        val species = ecology.species.single()
        val environment = land(canopyCover = 0.9)
        val intrinsicBest = NicheSelection.choose(species, ecology, environment)
        val competition = DoubleArray(ecology.niches.size)
        competition[intrinsicBest] = 1_000_000.0

        val radiationChoice = NicheSelection.choose(
            species = species,
            ecology = ecology,
            environment = environment,
            competitionByNiche = competition,
            minimumRelativeIntrinsicFit = 0.80,
            competitionAffectsSelection = false,
        )

        assertEquals(intrinsicBest, radiationChoice)
    }

    @Test
    fun `a valid scavenging niche can establish before the first carrion flux`() {
        val definition = SpeciesDefinition(
            id = "obligate-scavenger",
            displayName = "Obligate scavenger",
            sizeClass = SizeClass.MEDIUM,
            motile = true,
            traits = listOf(
                CommonTrait.TEMPERATE_BIOCHEMISTRY,
                CommonTrait.ENDOTHERMY,
                CommonTrait.POWERED_FLIGHT,
                CommonTrait.SCAVENGING_SENSES,
            ),
        )
        val ecology = EcologyCompiler.compile(listOf(definition))
        val environment = land().withResources(
            FunctionalResources(carrion = 0.0),
        )

        val nicheIndex = NicheSelection.choose(ecology.species.single(), ecology, environment)

        assertTrue(nicheIndex >= 0)
        assertEquals(EcoStrategy.SCAVENGING, ecology.niches[nicheIndex].strategy)
        assertEquals(Habitat.AERIAL, ecology.niches[nicheIndex].habitat)
    }

    @Test
    fun `ordinary flight does not establish over open ocean`() {
        val ecology = EcologyCompiler.compile(
            listOf(
                EarthSpeciesCatalog.ALL.first { it.id == "bald-eagle" },
                EarthSpeciesCatalog.ALL.first { it.id == "brown-pelican" },
                EarthSpeciesCatalog.ALL.first { it.id == "wandering-albatross" },
            ),
        )
        val openOcean = ocean(waterDepthM = 80.0, usefulSunlightReachesWater = true)

        assertEquals(-1, NicheSelection.choose(ecology.species[0], ecology, openOcean))
        assertEquals(-1, NicheSelection.choose(ecology.species[1], ecology, openOcean))
        val albatrossNiche = NicheSelection.choose(ecology.species[2], ecology, openOcean)
        assertTrue(albatrossNiche >= 0)
        assertEquals(Habitat.AERIAL, ecology.niches[albatrossNiche].habitat)
        assertTrue(ecology.species[2].environment.pelagicAerialResident)
    }

    @Test
    fun `dark water requires an explicit depth adaptation`() {
        val ecology = EcologyCompiler.compile(
            listOf(
                EarthSpeciesCatalog.ALL.first { it.id == "ocellaris-clownfish" },
                EarthSpeciesCatalog.ALL.first { it.id == "deep-sea-anglerfish" },
            ),
        )
        val darkOcean = ocean(waterDepthM = 900.0, usefulSunlightReachesWater = false)

        assertEquals(-1, NicheSelection.choose(ecology.species[0], ecology, darkOcean))
        val anglerfishNiche = NicheSelection.choose(ecology.species[1], ecology, darkOcean)
        assertTrue(anglerfishNiche >= 0)
        assertEquals(Habitat.DARK_WATER, ecology.niches[anglerfishNiche].habitat)
        assertTrue(ecology.species[1].environment.darkWaterAdapted)
    }

    private fun land(
        adjacentToMajorRiver: Double = 0.0,
        canopyCover: Double = 0.0,
        elevationM: Double = 0.0,
    ) = SeasonalCellEnvironment.create(
        areaKm2 = 40_000.0,
        temperatureC = 22.0,
        insolation = 0.8,
        precipitationMm = 35.0,
        isLand = true,
        adjacentToMajorRiver = adjacentToMajorRiver,
        canopyCover = canopyCover,
        elevationM = elevationM,
        resources = FunctionalResources(),
    )

    private fun ocean(
        waterDepthM: Double,
        usefulSunlightReachesWater: Boolean,
    ) = SeasonalCellEnvironment.create(
        areaKm2 = 40_000.0,
        temperatureC = 18.0,
        insolation = 0.8,
        precipitationMm = 60.0,
        isLand = false,
        waterDepthM = waterDepthM,
        usefulSunlightReachesWater = usefulSunlightReachesWater,
    )
}