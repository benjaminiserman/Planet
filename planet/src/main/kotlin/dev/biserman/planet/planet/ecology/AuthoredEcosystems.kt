package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.ClimateDatumSample

data class AuthoredEcosystemScenario(
    val name: String,
    val climate: ClimateDatum,
    val tile: AuthoredEcosystemTile,
    val species: List<SpeciesDefinition>,
    val introductions: List<AuthoredSpeciesIntroduction> = emptyList(),
    val climateShifts: List<AuthoredClimateShift> = emptyList(),
    val habitatShifts: List<AuthoredHabitatShift> = emptyList(),
    val populationRemovals: List<AuthoredPopulationRemoval> = emptyList(),
    val expectedExtinctions: Set<String> = emptySet(),
    val intendedStable: Boolean = true,
)

data class AuthoredEcosystemTile(
    val isLand: Boolean,
    val adjacentToOcean: Double = 0.0,
    val adjacentToLand: Double = 0.0,
    val adjacentToMajorRiver: Double = 0.0,
    val elevationM: Double = 0.0,
    val waterDepthM: Double = 0.0,
    val usefulSunlightReachesWater: Boolean = true,
    val canopyCover: Double = 0.0,
    val reefCover: Double = 0.0,
    val fertilityModifier: Double = 0.0,
    val includeAeroplankton: Boolean = false,
)

data class AuthoredSpeciesIntroduction(
    val speciesId: String,
    val year: Int,
    val biomassKg: Double? = null,
)

data class AuthoredClimateShift(val year: Int, val climate: ClimateDatum)

data class AuthoredHabitatShift(
    val year: Int,
    val canopyCover: Double? = null,
    val reefCover: Double? = null,
)

data class AuthoredPopulationRemoval(
    val speciesId: String,
    val year: Int,
    val fraction: Double = 1.0,
)

/** Shared, strongly typed source of truth for the notebook and health tests. */
object AuthoredEcosystems {
    private val speciesById = EarthSpeciesCatalog.ALL.associateBy { it.id }

    val ARCTIC_TUNDRA = scenario(
        "Arctic tundra",
        climate(1, q(-12, -6, 8, -2), q(15, 150, 280, 75), q(20, 25, 45, 30)),
        AuthoredEcosystemTile(isLand = true),
        "snowshoe-hare",
        "gray-wolf",
    )

    val SOUTHERN_OCEAN = scenario(
        "Southern Ocean",
        climate(2, q(1.2, -10.2, -6.8, -4.2), q(290, 95, 8, 125), q(28, 24, 18, 24)),
        AuthoredEcosystemTile(isLand = false, waterDepthM = 3_000.0),
        "antarctic-silverfish",
        "weddell-seal",
        "crabeater-seal",
        "orca",
    )

    val AMAZON_RAINFOREST = scenario(
        "Amazon rainforest",
        climate(3, q(27, 27, 26, 27.5), q(220, 235, 210, 230), q(260, 240, 150, 210)),
        AuthoredEcosystemTile(isLand = true, canopyCover = 0.92, fertilityModifier = 0.15),
        "giant-bamboo", "eucalyptus", "bracken-fern", "three-toed-sloth",
        "scarlet-macaw", "bengal-tiger", "field-mushroom", "termite",
    )

    val SERENGETI_SAVANNA = scenario(
        "Serengeti savanna",
        climate(4, q(25, 24, 21, 27), q(275, 260, 240, 295), q(85, 120, 18, 55)),
        AuthoredEcosystemTile(
            isLand = true,
            adjacentToMajorRiver = 1.0,
            fertilityModifier = 0.25,
        ),
        "perennial-ryegrass", "common-sunflower", "blue-wildebeest", "plains-zebra",
        "african-lion", "field-mushroom", "termite",
    )

    val SAHEL = scenario(
        "Sahel",
        climate(5, q(22, 30, 29, 26), q(260, 320, 270, 300), q(10, 15, 105, 20)),
        AuthoredEcosystemTile(isLand = true, fertilityModifier = -0.15),
        "umbrella-thorn-acacia",
        "dromedary-camel",
        "african-lion",
    )

    val SONORAN_DESERT = scenario(
        "Sonoran Desert",
        climate(6, q(13, 27, 35, 24), q(180, 310, 315, 235), q(25, 5, 45, 18)),
        AuthoredEcosystemTile(isLand = true, fertilityModifier = -0.30),
        "saguaro",
    )

    val BOREAL_FOREST = scenario(
        "Boreal forest",
        climate(7, q(-15, 3, 18, 4), q(25, 170, 285, 90), q(20, 35, 65, 45)),
        AuthoredEcosystemTile(isLand = true, canopyCover = 0.72, fertilityModifier = -0.05),
        "scots-pine",
        "snowshoe-hare",
        "gray-wolf",
    )

    val APPALACHIAN_TEMPERATE_FOREST = scenario(
        "Appalachian temperate forest",
        climate(8, q(2, 14, 25, 13), q(85, 225, 285, 145), q(90, 105, 115, 95)),
        AuthoredEcosystemTile(isLand = true, canopyCover = 0.86, fertilityModifier = 0.30),
        "english-oak",
        "house-mouse",
        "white-tailed-deer",
        "great-horned-owl",
        "red-fox",
    )

    val HIMALAYAN_ALPINE_MEADOW = scenario(
        "Himalayan alpine meadow",
        climate(9, q(-8, 0, 10, 1), q(100, 230, 305, 170), q(8, 30, 105, 22)),
        AuthoredEcosystemTile(isLand = true, elevationM = 4_500.0, fertilityModifier = -0.20),
        "himalayan-pika",
        "wild-yak",
        "snow-leopard",
    )

    val SUNDARBANS_MANGROVE = scenario(
        "Sundarbans mangrove",
        climate(10, q(20, 29, 31, 27), q(190, 285, 250, 235), q(18, 70, 315, 120)),
        AuthoredEcosystemTile(
            isLand = true,
            adjacentToOcean = 1.0,
            adjacentToMajorRiver = 1.0,
            canopyCover = 0.70,
            fertilityModifier = 0.25,
        ),
        "red-mangrove",
        "blue-crab",
        "common-octopus",
    )

    val OKAVANGO_DELTA = scenario(
        "Okavango Delta",
        climate(11, q(27, 23, 17, 29), q(300, 245, 205, 310), q(105, 25, 2, 35)),
        AuthoredEcosystemTile(
            isLand = true,
            adjacentToMajorRiver = 1.0,
            fertilityModifier = 0.40,
        ),
        "white-water-lily",
        "hippopotamus",
        "nile-crocodile",
    )

    val LAKE_BAIKAL = scenario(
        "Lake Baikal",
        climate(12, q(0, 2, 12, 5), q(30, 175, 285, 100), q(15, 32, 65, 35)),
        AuthoredEcosystemTile(
            isLand = true,
            adjacentToMajorRiver = 1.0,
            fertilityModifier = -0.10,
        ),
        "atlantic-salmon",
    )

    val TROPICAL_CORAL_REEF = scenario(
        "Tropical coral reef",
        climate(13, q(26, 27, 29, 28), q(230, 295, 280, 250), q(90, 135, 180, 120)),
        AuthoredEcosystemTile(
            isLand = false,
            waterDepthM = 35.0,
            reefCover = 0.78,
            fertilityModifier = 0.10,
        ),
        "staghorn-coral",
        "bumphead-parrotfish",
        "ocellaris-clownfish",
        "coral-grouper",
    )

    val CALIFORNIA_KELP_FOREST = scenario(
        "California kelp forest",
        climate(14, q(13, 14, 17, 15), q(135, 250, 280, 180), q(85, 30, 4, 25)),
        AuthoredEcosystemTile(
            isLand = false,
            adjacentToLand = 1.0,
            waterDepthM = 45.0,
            fertilityModifier = 0.35,
        ),
        "giant-kelp",
        "eelgrass",
        "sea-otter",
        "great-white-shark",
    )

    val HUMBOLDT_CURRENT = scenario(
        "Humboldt Current",
        climate(15, q(19, 17, 14, 16), q(290, 220, 165, 255), q(1, 1, 2, 1)),
        AuthoredEcosystemTile(isLand = false, waterDepthM = 500.0, fertilityModifier = 0.45),
        "peruvian-anchoveta",
        "giant-squid",
        "atlantic-bluefin-tuna",
        "harbor-seal",
    )

    val AEROPLANKTON_SKIES = scenario(
        "Aeroplankton skies",
        climate(16, q(18, 23, 26, 20), q(190, 285, 320, 225), q(70, 110, 85, 95)),
        AuthoredEcosystemTile(
            isLand = true,
            canopyCover = 0.20,
            fertilityModifier = 0.10,
            includeAeroplankton = true,
        ),
        "common-green-darner",
    )

    val CLIMATE_MALADAPTATION = scenario(
        "CONTROL - climate maladaptation",
        climate(101, q(-32, -24, -16, -26), q(20, 75, 110, 45), q(2, 3, 5, 2)),
        AuthoredEcosystemTile(isLand = true, canopyCover = 0.80, fertilityModifier = -0.35),
        "giant-bamboo",
        "three-toed-sloth",
        "bengal-tiger",
        expectedExtinctions = setOf(
            "giant-bamboo",
            "three-toed-sloth",
            "bengal-tiger",
            "invariant-bugs",
            "invariant-carpet-plants",
        ),
        intendedStable = false,
    )

    val COMPETITIVE_DROUGHT = scenario(
        "EXTINCTION - competitive drought",
        climate(201, q(18, 24, 27, 20), q(180, 270, 300, 210), q(85, 105, 75, 95)),
        AuthoredEcosystemTile(isLand = true, fertilityModifier = 0.10),
        "common-sunflower",
        "saguaro",
        climateShifts = listOf(
            AuthoredClimateShift(
                35,
                climate(211, q(20, 29, 33, 24), q(200, 295, 325, 230), q(4, 7, 3, 5)),
            ),
        ),
        expectedExtinctions = setOf("common-sunflower"),
    )

    val ISLAND_PREY_COLLAPSE = scenario(
        "EXTINCTION - island prey collapse after predator introduction",
        climate(202, q(8, 16, 22, 13), q(170, 245, 275, 195), q(95, 120, 80, 110)),
        AuthoredEcosystemTile(isLand = true, adjacentToOcean = 1.0, fertilityModifier = 0.05),
        "giant-bamboo", "giant-panda", "bengal-tiger",
        introductions = listOf(AuthoredSpeciesIntroduction("bengal-tiger", 45, 240_000.0)),
        populationRemovals = listOf(AuthoredPopulationRemoval("giant-panda", 60)),
        expectedExtinctions = setOf("giant-panda", "bengal-tiger"),
    )

    val HOST_SPECIALIST_COEXTINCTION = scenario(
        "EXTINCTION - host-specialist coextinction",
        climate(203, q(16, 22, 26, 19), q(175, 255, 290, 205), q(70, 95, 65, 85)),
        AuthoredEcosystemTile(isLand = true, canopyCover = 0.60, fertilityModifier = 0.08),
        "eucalyptus",
        "giant-bamboo",
        "koala",
        populationRemovals = listOf(AuthoredPopulationRemoval("eucalyptus", 40)),
        expectedExtinctions = setOf("eucalyptus", "koala"),
    )

    val CANOPY_COLLAPSE = scenario(
        "EXTINCTION - canopy collapse",
        climate(204, q(24, 27, 28, 25), q(195, 245, 270, 210), q(190, 230, 145, 205)),
        AuthoredEcosystemTile(isLand = true, canopyCover = 0.92, fertilityModifier = 0.14),
        "coast-redwood", "bracken-fern", "three-toed-sloth", "white-tailed-deer",
        habitatShifts = listOf(AuthoredHabitatShift(35, canopyCover = 0.0)),
        expectedExtinctions = setOf("coast-redwood", "three-toed-sloth"),
    )

    val REEF_BUILDER_LOSS = scenario(
        "EXTINCTION - reef-builder loss",
        climate(205, q(18, 21, 23, 20), q(220, 285, 300, 240), q(95, 125, 145, 110)),
        AuthoredEcosystemTile(
            isLand = false,
            waterDepthM = 30.0,
            reefCover = 0.82,
            fertilityModifier = 0.10,
        ),
        "staghorn-coral",
        climateShifts = listOf(
            AuthoredClimateShift(
                30,
                climate(215, q(39, 41, 42, 40), q(225, 295, 315, 245), q(85, 115, 130, 95)),
            ),
        ),
        habitatShifts = listOf(AuthoredHabitatShift(35, reefCover = 0.02)),
        expectedExtinctions = setOf("staghorn-coral"),
    )

    val ALL: List<AuthoredEcosystemScenario> = listOf(
        ARCTIC_TUNDRA,
        SOUTHERN_OCEAN,
        AMAZON_RAINFOREST,
        SERENGETI_SAVANNA,
        SAHEL,
        SONORAN_DESERT,
        BOREAL_FOREST,
        APPALACHIAN_TEMPERATE_FOREST,
        HIMALAYAN_ALPINE_MEADOW,
        SUNDARBANS_MANGROVE,
        OKAVANGO_DELTA,
        LAKE_BAIKAL,
        TROPICAL_CORAL_REEF,
        CALIFORNIA_KELP_FOREST,
        HUMBOLDT_CURRENT,
        AEROPLANKTON_SKIES,
        CLIMATE_MALADAPTATION,
        COMPETITIVE_DROUGHT,
        ISLAND_PREY_COLLAPSE,
        HOST_SPECIALIST_COEXTINCTION,
        CANOPY_COLLAPSE,
        REEF_BUILDER_LOSS,
    )

    private fun scenario(
        name: String,
        climate: ClimateDatum,
        tile: AuthoredEcosystemTile,
        vararg speciesIds: String,
        introductions: List<AuthoredSpeciesIntroduction> = emptyList(),
        climateShifts: List<AuthoredClimateShift> = emptyList(),
        habitatShifts: List<AuthoredHabitatShift> = emptyList(),
        populationRemovals: List<AuthoredPopulationRemoval> = emptyList(),
        expectedExtinctions: Set<String> = emptySet(),
        intendedStable: Boolean = true,
    ) = AuthoredEcosystemScenario(
        name = name,
        climate = climate,
        tile = tile,
        species = speciesIds.map { id ->
            requireNotNull(speciesById[id]) { "$name references unknown Earth species $id" }
        },
        introductions = introductions,
        climateShifts = climateShifts,
        habitatShifts = habitatShifts,
        populationRemovals = populationRemovals,
        expectedExtinctions = expectedExtinctions,
        intendedStable = intendedStable,
    )

    private fun climate(
        tileId: Int,
        temperatures: List<Double>,
        insolations: List<Double>,
        precipitation: List<Double>,
    ): ClimateDatum {
        require(temperatures.size == 4 && insolations.size == 4 && precipitation.size == 4)
        fun interpolate(values: List<Double>, month: Int): Double {
            val quarter = month / 3
            val next = (quarter + 1) % 4
            val fraction = (month % 3) / 3.0
            return values[quarter] + (values[next] - values[quarter]) * fraction
        }
        return ClimateDatum(
            tileId,
            (0 until 12).map { month ->
                ClimateDatumSample(
                    interpolate(temperatures, month),
                    interpolate(insolations, month),
                    interpolate(precipitation, month),
                )
            },
        )
    }

    private fun q(first: Number, second: Number, third: Number, fourth: Number) =
        listOf(first, second, third, fourth).map(Number::toDouble)
}