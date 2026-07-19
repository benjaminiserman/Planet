package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateClassification

val speciesBlueprints = listOf(
    speciesBlueprint("grass", SizeClass.MINUSCULE, Trait.SESSILE_PRODUCER, Trait.GRASS_LIKE, Trait.LOW_FOLIAGE, Trait.COARSE_GRASS, Trait.FAST_GROWING, Trait.WAXY_CUTICLE, Trait.UNDERGROUND_MERISTEMS),
    speciesBlueprint("bamboo", SizeClass.MEDIUM, Trait.SESSILE_PRODUCER, Trait.GRASS_LIKE, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.BAMBOO_CULMS, Trait.FAST_GROWING, Trait.RHIZOMATOUS_GROWTH),
    speciesBlueprint("clover", SizeClass.MINUSCULE, Trait.SESSILE_PRODUCER, Trait.GRASS_LIKE, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.FAST_GROWING, Trait.SEED_BEARING),
    speciesBlueprint("wildflowers", SizeClass.MINUSCULE, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.SEED_BEARING, Trait.FAST_GROWING),
    speciesBlueprint("shrub", SizeClass.MEDIUM, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.WOODY, Trait.SLOW_GROWING, Trait.DEEP_ROOTS),
    speciesBlueprint("berry_bush", SizeClass.MEDIUM, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.WOODY, Trait.FRUITING, Trait.SEED_BEARING, Trait.WATERLOGGING_SENSITIVE_ROOTS),
    speciesBlueprint("apple_tree", SizeClass.GIANT, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.CANOPY_FOLIAGE, Trait.WOODY, Trait.FRUITING, Trait.SEED_BEARING, Trait.DORMANT_BUDS),
    speciesBlueprint("mango_tree", SizeClass.GIANT, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.CANOPY_FOLIAGE, Trait.LARGE_CANOPY, Trait.WOODY, Trait.FRUITING, Trait.SEED_BEARING, Trait.DRIP_TIP_LEAVES),
    speciesBlueprint("fig_tree", SizeClass.GIANT, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.CANOPY_FOLIAGE, Trait.LARGE_CANOPY, Trait.WOODY, Trait.FRUITING, Trait.SEED_BEARING, Trait.FAST_GROWING, Trait.DRIP_TIP_LEAVES),
    speciesBlueprint("date_palm", SizeClass.GIANT, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.CANOPY_FOLIAGE, Trait.WOODY, Trait.FRUITING, Trait.SEED_BEARING, Trait.DEEP_ROOTS, Trait.WAXY_CUTICLE),
    speciesBlueprint("oak", SizeClass.GIANT, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.CANOPY_FOLIAGE, Trait.LARGE_CANOPY, Trait.WOODY, Trait.SEED_BEARING, Trait.SLOW_GROWING, Trait.THICK_BARK, Trait.DORMANT_BUDS),
    speciesBlueprint("mesquite", SizeClass.MEDIUM, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.WOODY, Trait.SEED_BEARING, Trait.DEEP_ROOTS, Trait.WAXY_CUTICLE, Trait.DROUGHT_DECIDUOUS_FOLIAGE),
    speciesBlueprint("acacia", SizeClass.GIANT, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.CANOPY_FOLIAGE, Trait.WOODY, Trait.SEED_BEARING, Trait.DEEP_ROOTS, Trait.THICK_BARK, Trait.DROUGHT_DECIDUOUS_FOLIAGE),
    speciesBlueprint("kapok_tree", SizeClass.GIANT, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.CANOPY_FOLIAGE, Trait.LARGE_CANOPY, Trait.WOODY, Trait.SEED_BEARING, Trait.FAST_GROWING),
    speciesBlueprint("spruce", SizeClass.GIANT, Trait.SESSILE_PRODUCER, Trait.CANOPY_FOLIAGE, Trait.LARGE_CANOPY, Trait.WOODY, Trait.SEED_BEARING, Trait.SLOW_GROWING, Trait.EVERGREEN, Trait.NEEDLE_LEAVES, Trait.THICK_BARK, Trait.DORMANT_BUDS),
    speciesBlueprint("dwarf_willow", SizeClass.MEDIUM, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.WOODY, Trait.DEEP_ROOTS, Trait.DORMANT_BUDS, Trait.PROSTRATE_WOODY_GROWTH),
    speciesBlueprint("edelweiss", SizeClass.MINUSCULE, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.SEED_BEARING, Trait.ALPINE_CUSHION_GROWTH),
    speciesBlueprint("cactus", SizeClass.SMALL, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.WAXY_CUTICLE, Trait.WATER_STORAGE_TISSUE, Trait.CAM_PHOTOSYNTHESIS),
    speciesBlueprint("reeds", SizeClass.SMALL, Trait.SESSILE_PRODUCER, Trait.GRASS_LIKE, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.COARSE_GRASS, Trait.AMPHIBIOUS, Trait.FAST_GROWING),

    speciesBlueprint("sundew", SizeClass.TINY, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.SLOW_GROWING, Trait.INSECTIVORE, Trait.AMBUSH_PREDATOR, Trait.BOG_ROOTS),
    speciesBlueprint("solar_slug", SizeClass.TINY, Trait.PHOTOSYNTHETIC, Trait.MOTILE, Trait.AQUATIC, Trait.LEAF_EATER, Trait.GROUND_FORAGING),
    speciesBlueprint("phytoplankton", SizeClass.MINUSCULE, Trait.PHOTOSYNTHETIC, Trait.MOTILE, Trait.AQUATIC),
    speciesBlueprint("krill", SizeClass.TINY, Trait.MOTILE, Trait.FILTER_FEEDER, Trait.AQUATIC, Trait.ANTIFREEZE_PROTEINS),
    speciesBlueprint("kelp", SizeClass.MEDIUM, Trait.SESSILE_PRODUCER, Trait.LEAFY, Trait.CANOPY_FOLIAGE, Trait.LARGE_CANOPY, Trait.AQUATIC, Trait.FAST_GROWING, Trait.MARINE_HOLDFAST, Trait.COLD_WATER_ENZYMES),
    speciesBlueprint("seagrass", SizeClass.MINUSCULE, Trait.SESSILE_PRODUCER, Trait.GRASS_LIKE, Trait.LEAFY, Trait.LOW_FOLIAGE, Trait.AQUATIC, Trait.RHIZOMATOUS_GROWTH),
    speciesBlueprint("diatoms", SizeClass.MINUSCULE, Trait.PHOTOSYNTHETIC, Trait.MOTILE, Trait.AQUATIC, Trait.SILICA_FRUSTULE, Trait.COLD_WATER_ENZYMES),
    speciesBlueprint("zooplankton", SizeClass.MINUSCULE, Trait.MOTILE, Trait.FILTER_FEEDER, Trait.AQUATIC, Trait.ANTIFREEZE_PROTEINS),
    speciesBlueprint("jellyfish", SizeClass.SMALL, Trait.MOTILE, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.AQUATIC),
    speciesBlueprint("sea_urchin", SizeClass.TINY, Trait.MOTILE, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.AQUATIC, Trait.ARMORED),
    speciesBlueprint("mussel", SizeClass.TINY, Trait.FILTER_FEEDER, Trait.ATTACHED_FILTER_FEEDER, Trait.AQUATIC, Trait.ARMORED),
    speciesBlueprint("crab", SizeClass.SMALL, Trait.MOTILE, Trait.OMNIVORE, Trait.BENTHIC_FORAGER, Trait.AQUATIC, Trait.ARMORED, Trait.GILLS),
    speciesBlueprint("shrimp", SizeClass.TINY, Trait.MOTILE, Trait.FILTER_FEEDER, Trait.BENTHIC_FORAGER, Trait.AQUATIC, Trait.ARMORED, Trait.GILLS),
    speciesBlueprint("squid", SizeClass.SMALL, Trait.MOTILE, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.AQUATIC, Trait.JET_PROPULSION, Trait.SCHOOLING),
    speciesBlueprint("octopus", SizeClass.SMALL, Trait.MOTILE, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.AQUATIC, Trait.JET_PROPULSION, Trait.ADAPTIVE_CAMOUFLAGE),
    speciesBlueprint("sardine", SizeClass.TINY, Trait.MOTILE, Trait.FILTER_FEEDER, Trait.AQUATIC, Trait.GILLS, Trait.SCHOOLING, Trait.STREAMLINED_BODY),
    speciesBlueprint("anchovy", SizeClass.TINY, Trait.MOTILE, Trait.FILTER_FEEDER, Trait.AQUATIC, Trait.GILLS, Trait.SCHOOLING, Trait.STREAMLINED_BODY),
    speciesBlueprint("mackerel", SizeClass.SMALL, Trait.MOTILE, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.AQUATIC, Trait.GILLS, Trait.SCHOOLING, Trait.STREAMLINED_BODY),
    speciesBlueprint("cod", SizeClass.MEDIUM, Trait.MOTILE, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.AQUATIC, Trait.GILLS, Trait.COLD_WATER_ENZYMES),
    speciesBlueprint("salmon", SizeClass.SMALL, Trait.MOTILE, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.AQUATIC, Trait.GILLS, Trait.COLD_WATER_ENZYMES, Trait.SEASONAL_MIGRATION),
    speciesBlueprint("parrotfish", SizeClass.SMALL, Trait.MOTILE, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.AQUATIC, Trait.GILLS, Trait.SCALES, Trait.HEAT_SHOCK_PROTEINS),
    speciesBlueprint("reef_coral", SizeClass.SMALL, Trait.PHOTOSYNTHETIC, Trait.FILTER_FEEDER, Trait.ATTACHED_FILTER_FEEDER, Trait.AQUATIC, Trait.REEF_BUILDING, Trait.HEAT_SHOCK_PROTEINS),
    speciesBlueprint("sea_sponge", SizeClass.SMALL, Trait.FILTER_FEEDER, Trait.ATTACHED_FILTER_FEEDER, Trait.AQUATIC),

    speciesBlueprint("rabbit", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.BURROWING),
    speciesBlueprint("hare", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.GROUND_FORAGING),
    speciesBlueprint("mouse", SizeClass.TINY, Trait.MOTILE, Trait.WARM_BLOODED, Trait.FRUGIVORE, Trait.SEED_EATER, Trait.BURROWING, Trait.NOCTURNAL, Trait.CONSTRICTING_PUPILS, Trait.CONCENTRATED_URINE),
    speciesBlueprint("vole", SizeClass.TINY, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.SEED_EATER, Trait.GROUND_FORAGING, Trait.BURROWING),
    speciesBlueprint("deer", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.HIGH_BROWSING, Trait.RUMINANT_STOMACH, Trait.WOODLAND_FORAGING),
    speciesBlueprint("beaver", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.DENDROVORE, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.AMPHIBIOUS, Trait.BLUBBER),
    speciesBlueprint("grasshopper", SizeClass.MINUSCULE, Trait.MOTILE, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.FLIGHT),
    speciesBlueprint("caterpillar", SizeClass.MINUSCULE, Trait.MOTILE, Trait.LEAF_EATER, Trait.GROUND_FORAGING),
    speciesBlueprint("cricket", SizeClass.MINUSCULE, Trait.MOTILE, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.BURROWING, Trait.NOCTURNAL),
    speciesBlueprint("beetle", SizeClass.MINUSCULE, Trait.MOTILE, Trait.LEAF_EATER, Trait.SEED_EATER, Trait.GROUND_FORAGING, Trait.ARMORED),
    speciesBlueprint("fruit_fly", SizeClass.MINUSCULE, Trait.MOTILE, Trait.FRUGIVORE, Trait.FLIGHT),
    speciesBlueprint("sparrow", SizeClass.TINY, Trait.MOTILE, Trait.WARM_BLOODED, Trait.SEED_EATER, Trait.INSECTIVORE, Trait.FLIGHT, Trait.GRASSLAND_CAMOUFLAGE),
    speciesBlueprint("pigeon", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.SEED_EATER, Trait.FRUGIVORE, Trait.FLIGHT, Trait.BRIGHT_MATING_PLUMAGE),
    speciesBlueprint("quail", SizeClass.TINY, Trait.MOTILE, Trait.WARM_BLOODED, Trait.SEED_EATER, Trait.INSECTIVORE, Trait.FLIGHT, Trait.GRASSLAND_CAMOUFLAGE),
    speciesBlueprint("boar", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.OMNIVORE, Trait.GRASS_GRAZER, Trait.FRUGIVORE, Trait.GROUND_FORAGING, Trait.ARMORED, Trait.WOODLAND_FORAGING),
    speciesBlueprint("frog", SizeClass.TINY, Trait.MOTILE, Trait.INSECTIVORE, Trait.AMBUSH_PREDATOR, Trait.PROJECTILE_TONGUE, Trait.LARGE_EYES, Trait.PERMEABLE_SKIN, Trait.WETLAND_BREEDING, Trait.AQUATIC_LARVAE),
    speciesBlueprint("snake", SizeClass.SMALL, Trait.MOTILE, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.VENOMOUS),
    speciesBlueprint("weasel", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.BURROWING),
    speciesBlueprint("fox", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.CHASE_PREDATOR),
    speciesBlueprint("owl", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.INSECTIVORE, Trait.AMBUSH_PREDATOR, Trait.FLIGHT, Trait.NOCTURNAL, Trait.REFLECTIVE_RETINA, Trait.CONSTRICTING_PUPILS),
    speciesBlueprint("hawk", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.INSECTIVORE, Trait.CHASE_PREDATOR, Trait.FLIGHT, Trait.UV_FILTERING_LENSES, Trait.CONSTRICTING_PUPILS),
    speciesBlueprint("coyote", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.PACK_HUNTER, Trait.CONCENTRATED_URINE),
    speciesBlueprint("lynx", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.THICK_FUR, Trait.REFLECTIVE_RETINA),
    speciesBlueprint("wolf", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.PACK_HUNTER, Trait.THICK_FUR),
    speciesBlueprint("eagle", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.FLIGHT, Trait.UV_FILTERING_LENSES, Trait.CONSTRICTING_PUPILS),
    speciesBlueprint("bear", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.OMNIVORE, Trait.CARNIVORE, Trait.FRUGIVORE, Trait.ARMORED, Trait.THICK_FUR),

    speciesBlueprint("dromedary", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.HIGH_BROWSING, Trait.RUMINANT_STOMACH, Trait.HUMP_FAT, Trait.CONCENTRATED_URINE, Trait.LARGE_HEAT_RADIATING_EARS),
    speciesBlueprint("bactrian_camel", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.HIGH_BROWSING, Trait.RUMINANT_STOMACH, Trait.HUMP_FAT, Trait.CONCENTRATED_URINE, Trait.THICK_FUR, Trait.OPEN_PLAINS_LOCOMOTION),
    speciesBlueprint("fennec_fox", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.BURROWING, Trait.NOCTURNAL, Trait.LARGE_HEAT_RADIATING_EARS, Trait.SPARSE_COAT, Trait.CONCENTRATED_URINE, Trait.DESERT_CAMOUFLAGE),
    speciesBlueprint("jerboa", SizeClass.TINY, Trait.MOTILE, Trait.WARM_BLOODED, Trait.SEED_EATER, Trait.BURROWING, Trait.NOCTURNAL, Trait.LARGE_HEAT_RADIATING_EARS, Trait.CONCENTRATED_URINE),
    speciesBlueprint("oryx", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.RUMINANT_STOMACH, Trait.CONCENTRATED_URINE, Trait.SPARSE_COAT),
    speciesBlueprint("jaguar", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.SPARSE_COAT, Trait.FOREST_CAMOUFLAGE),
    speciesBlueprint("sloth", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.LEAF_EATER, Trait.HIGH_BROWSING, Trait.LOW_BASAL_METABOLISM, Trait.SPARSE_COAT),
    speciesBlueprint("toucan", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.FRUGIVORE, Trait.FLIGHT, Trait.CONSTRICTING_PUPILS),
    speciesBlueprint("gorilla", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.OMNIVORE, Trait.LEAF_EATER, Trait.FRUGIVORE, Trait.GROUND_FORAGING, Trait.SPARSE_COAT),
    speciesBlueprint("capybara", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.AMPHIBIOUS, Trait.SPARSE_COAT),
    speciesBlueprint("anaconda", SizeClass.LARGE, Trait.MOTILE, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.AMPHIBIOUS, Trait.SCALES, Trait.HEAT_SENSING_PITS),
    speciesBlueprint("tapir", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.LEAF_EATER, Trait.FRUGIVORE, Trait.GROUND_FORAGING, Trait.SPARSE_COAT),
    speciesBlueprint("lion", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.PACK_HUNTER, Trait.SPARSE_COAT),
    speciesBlueprint("elephant", SizeClass.GIANT, Trait.MOTILE, Trait.WARM_BLOODED, Trait.LEAF_EATER, Trait.FRUGIVORE, Trait.HIGH_BROWSING, Trait.HINDGUT_FERMENTATION, Trait.PREHENSILE_TRUNK, Trait.TUSKS, Trait.LARGE_HEAT_RADIATING_EARS),
    speciesBlueprint("giraffe", SizeClass.GIANT, Trait.MOTILE, Trait.WARM_BLOODED, Trait.LEAF_EATER, Trait.HIGH_BROWSING, Trait.RUMINANT_STOMACH, Trait.LONG_NECK, Trait.SPARSE_COAT),
    speciesBlueprint("zebra", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.GROUND_FORAGING, Trait.HINDGUT_FERMENTATION, Trait.SPARSE_COAT, Trait.OPEN_PLAINS_LOCOMOTION, Trait.GRASSLAND_CAMOUFLAGE),
    speciesBlueprint("hyena", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.PACK_HUNTER, Trait.SPARSE_COAT),
    speciesBlueprint("wildebeest", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.GROUND_FORAGING, Trait.RUMINANT_STOMACH, Trait.SEASONAL_MIGRATION, Trait.OPEN_PLAINS_LOCOMOTION),
    speciesBlueprint("tiger", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.FOREST_CAMOUFLAGE),
    speciesBlueprint("giant_panda", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.BAMBOO_SPECIALIST, Trait.THICK_FUR, Trait.LOW_BASAL_METABOLISM),
    speciesBlueprint("crocodile", SizeClass.LARGE, Trait.MOTILE, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.AMPHIBIOUS, Trait.SCALES, Trait.ARMORED),
    speciesBlueprint("moose", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.LEAF_EATER, Trait.GRASS_GRAZER, Trait.HIGH_BROWSING, Trait.RUMINANT_STOMACH, Trait.THICK_FUR),
    speciesBlueprint("caribou", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.GROUND_FORAGING, Trait.RUMINANT_STOMACH, Trait.THICK_FUR, Trait.SEASONAL_MIGRATION, Trait.OPEN_PLAINS_LOCOMOTION),
    speciesBlueprint("wolverine", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.THICK_FUR),
    speciesBlueprint("ibex", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.HIGH_BROWSING, Trait.RUMINANT_STOMACH, Trait.HIGH_AFFINITY_HEMOGLOBIN),
    speciesBlueprint("bison", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.GROUND_FORAGING, Trait.RUMINANT_STOMACH, Trait.THICK_FUR, Trait.OPEN_PLAINS_LOCOMOTION),
    speciesBlueprint("saiga", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.GROUND_FORAGING, Trait.RUMINANT_STOMACH, Trait.CONCENTRATED_URINE, Trait.SEASONAL_MIGRATION, Trait.OPEN_PLAINS_LOCOMOTION),
    speciesBlueprint("musk_ox", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.GROUND_FORAGING, Trait.RUMINANT_STOMACH, Trait.THICK_FUR, Trait.SUBCUTANEOUS_FAT),
    speciesBlueprint("arctic_fox", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.THICK_FUR, Trait.REFLECTIVE_RETINA, Trait.SNOW_CAMOUFLAGE),
    speciesBlueprint("polar_bear", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.THICK_FUR, Trait.BLUBBER, Trait.AMPHIBIOUS, Trait.SNOW_CAMOUFLAGE),
    speciesBlueprint("emperor_penguin", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.AQUATIC, Trait.AMPHIBIOUS, Trait.INSULATING_FEATHERS, Trait.BLUBBER, Trait.COUNTERCURRENT_HEAT_EXCHANGE),
    speciesBlueprint("monitor_lizard", SizeClass.MEDIUM, Trait.MOTILE, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.SCALES, Trait.HEAT_SENSING_PITS),
    speciesBlueprint("scorpion", SizeClass.TINY, Trait.MOTILE, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.VENOMOUS, Trait.NOCTURNAL, Trait.ARMORED, Trait.WAXY_EXOSKELETON, Trait.DESERT_CAMOUFLAGE),
    speciesBlueprint("kangaroo", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.GROUND_FORAGING, Trait.LARGE_HEAT_RADIATING_EARS, Trait.CONCENTRATED_URINE, Trait.SPARSE_COAT, Trait.WOODLAND_FORAGING),
    speciesBlueprint("yak", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.GROUND_FORAGING, Trait.RUMINANT_STOMACH, Trait.THICK_FUR, Trait.ENLARGED_LUNGS, Trait.SUBCUTANEOUS_FAT),
    speciesBlueprint("mammoth", SizeClass.GIANT, Trait.MOTILE, Trait.WARM_BLOODED, Trait.GRASS_GRAZER, Trait.LEAF_EATER, Trait.HIGH_BROWSING, Trait.RUMINANT_STOMACH, Trait.PREHENSILE_TRUNK, Trait.TUSKS, Trait.THICK_FUR, Trait.SUBCUTANEOUS_FAT, Trait.OPEN_PLAINS_LOCOMOTION),
    speciesBlueprint("orca", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.PACK_HUNTER, Trait.AQUATIC, Trait.BLUBBER, Trait.STREAMLINED_BODY, Trait.ECHOLOCATION),
    speciesBlueprint("harbor_seal", SizeClass.MEDIUM, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.AQUATIC, Trait.BLUBBER, Trait.STREAMLINED_BODY),
    speciesBlueprint("walrus", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.BENTHIC_FORAGER, Trait.AMPHIBIOUS, Trait.BLUBBER, Trait.TUSKS),
    speciesBlueprint("great_white_shark", SizeClass.LARGE, Trait.MOTILE, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.AQUATIC, Trait.SCALES, Trait.STREAMLINED_BODY, Trait.ELECTRORECEPTION),
    speciesBlueprint("tuna", SizeClass.MEDIUM, Trait.MOTILE, Trait.CARNIVORE, Trait.CHASE_PREDATOR, Trait.AQUATIC, Trait.SCALES, Trait.STREAMLINED_BODY),
    speciesBlueprint("dolphin", SizeClass.LARGE, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.PACK_HUNTER, Trait.AQUATIC, Trait.STREAMLINED_BODY, Trait.ECHOLOCATION),
    speciesBlueprint("manta_ray", SizeClass.LARGE, Trait.MOTILE, Trait.FILTER_FEEDER, Trait.AQUATIC, Trait.STREAMLINED_BODY, Trait.ELECTRORECEPTION),
    speciesBlueprint("anglerfish", SizeClass.SMALL, Trait.MOTILE, Trait.CARNIVORE, Trait.AMBUSH_PREDATOR, Trait.AQUATIC, Trait.BIOLUMINESCENT_LURE),
    speciesBlueprint("sea_turtle", SizeClass.LARGE, Trait.MOTILE, Trait.OMNIVORE, Trait.LEAF_EATER, Trait.AQUATIC, Trait.ARMORED, Trait.SCALES, Trait.SEASONAL_MIGRATION),
    speciesBlueprint("blue_whale", SizeClass.COLOSSAL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.FILTER_FEEDER, Trait.AQUATIC, Trait.BLUBBER, Trait.STREAMLINED_BODY, Trait.SEASONAL_MIGRATION),
    speciesBlueprint("whale_shark", SizeClass.COLOSSAL, Trait.MOTILE, Trait.FILTER_FEEDER, Trait.AQUATIC, Trait.SCALES, Trait.STREAMLINED_BODY),
    speciesBlueprint("albatross", SizeClass.SMALL, Trait.MOTILE, Trait.WARM_BLOODED, Trait.CARNIVORE, Trait.FLIGHT, Trait.AQUATIC, Trait.SEASONAL_MIGRATION),
)

val speciesCatalog = deriveSpeciesCatalog(speciesBlueprints)

/** Stable-id lookup used by serialized tile ecosystem state. */
val speciesCatalogById: Map<String, SpeciesDefinition> = speciesCatalog.associateBy { it.id }

/** Compiled species pools consumed by BiotaDistribution order assignments. */
val speciesCatalogByTaxonomicOrder: Map<TaxonomicOrder, List<SpeciesDefinition>> =
    speciesCatalog.groupBy { it.taxonomicOrder }.also { byOrder ->
        require(speciesBlueprints.all { it.id in taxonomicOrderBySpeciesId }) {
            "Every fixed species blueprint must have a taxonomic order"
        }
        require(byOrder.values.flatten().map { it.id }.toSet() == speciesCatalog.map { it.id }.toSet()) {
            "The Order-to-Species mapping must cover the complete compiled catalog"
        }
    }

/** Representative catalog animals for every Hersfeldt classification id. */
val hersfeldtIconicAnimalIds: Map<String, Set<String>> = buildMap {
    fun assign(climateIds: String, vararg animalIds: String) {
        climateIds.split(' ').forEach { climateId -> put(climateId, animalIds.toSet()) }
    }

    assign("Aha Ahh", "dromedary", "fennec_fox", "scorpion")
    assign("Ahc", "bactrian_camel", "saiga")
    assign("Ahe", "bactrian_camel", "jerboa")
    assign("Ada Adh", "oryx", "kangaroo", "fennec_fox")
    assign("Adc", "bactrian_camel", "saiga")
    assign("Ade", "oryx", "jerboa")

    assign("TUr TUrp", "jaguar", "sloth", "toucan", "gorilla")
    assign("TUf TUfp", "jaguar", "tapir", "toucan")
    assign("TUs TUsp", "elephant", "capybara", "crocodile")
    assign("TUA TUAp", "lion", "giraffe", "zebra", "wildebeest")
    assign("TQf TQfp", "tiger", "giant_panda", "tapir")
    assign("TQs TQsp", "tiger", "elephant", "capybara")
    assign("TQA TQAp", "lion", "oryx", "zebra")
    assign("TF", "jaguar", "anaconda", "owl")
    assign("TG", "monitor_lizard", "scorpion")

    assign("CTf CTfp", "tiger", "giant_panda", "tapir")
    assign("CTs CTsp", "elephant", "lion", "zebra")
    assign("CDa CDap", "deer", "fox", "bear", "beaver")
    assign("CDb CDbp", "bison", "wolf", "deer", "bear")
    assign("CEa CEap", "moose", "bear", "wolverine")
    assign("CEb CEbp", "caribou", "wolf", "moose")
    assign("CEc CEcp", "caribou", "wolverine", "musk_ox")
    assign("CMa CMb", "ibex", "boar", "fox")
    assign("CAMa CAMb", "ibex", "boar", "eagle")
    assign("CAa CAap", "bison", "saiga", "wolf")
    assign("CAb CAbp", "saiga", "bison", "wolf")
    assign("CFa", "caribou", "arctic_fox", "musk_ox")
    assign("CFb", "musk_ox", "arctic_fox", "caribou")
    assign("CG", "arctic_fox", "polar_bear")
    assign("CI", "polar_bear", "emperor_penguin")

    assign("HTf HTfp", "crocodile", "anaconda", "gorilla")
    assign("HTs HTsp", "elephant", "crocodile", "lion")
    assign("HDa HDap", "crocodile", "capybara", "monitor_lizard")
    assign("HDb HDbp", "crocodile", "anaconda", "monitor_lizard")
    assign("HDc HDcp", "monitor_lizard", "scorpion", "crocodile")
    assign("HMa HMb HMc", "ibex", "oryx", "monitor_lizard")
    assign("HAMa HAMb HAMc", "dromedary", "oryx", "ibex")
    assign("HAa HAap", "lion", "elephant", "giraffe")
    assign("HAb HAbp", "oryx", "dromedary", "scorpion")
    assign("HAc HAcp", "dromedary", "monitor_lizard", "scorpion")
    assign("HFa HFb HFc", "dromedary", "scorpion", "monitor_lizard")
    assign("HG", "scorpion", "monitor_lizard")

    assign("ETf ETfp", "mammoth", "yak", "tiger")
    assign("ETs ETsp", "bison", "mammoth", "wildebeest")
    assign("EDa EDap", "mammoth", "caribou", "wolverine")
    assign("EDb EDbp", "mammoth", "musk_ox", "arctic_fox")
    assign("EMa EMb", "ibex", "yak", "boar")
    assign("EAMa EAMb", "ibex", "saiga", "bactrian_camel")
    assign("EAa EAap", "bison", "saiga", "wildebeest")
    assign("EAb EAbp", "saiga", "yak", "mammoth")
    assign("EFa EFb", "caribou", "mammoth", "arctic_fox")
    assign("EG", "mammoth", "yak")

    assign("Ofi", "emperor_penguin", "walrus", "polar_bear", "krill")
    assign("Ofd", "orca", "harbor_seal", "walrus")
    assign("Ofg", "orca", "harbor_seal", "anglerfish")
    assign("Og", "anglerfish", "orca", "blue_whale")
    assign("Oc", "blue_whale", "orca", "harbor_seal", "tuna", "albatross", "krill")
    assign("Ot", "manta_ray", "sea_turtle", "whale_shark", "dolphin")
    assign("Oh", "sea_turtle", "manta_ray", "dolphin")
    assign("Or", "whale_shark", "manta_ray", "sea_turtle")
    assign("Oe", "blue_whale", "albatross", "orca", "sea_turtle")
}

/** Resolves the representative animal candidates for an actual classified tile. */
fun iconicAnimalsFor(climate: ClimateClassification): List<SpeciesDefinition> {
    val animalIds = hersfeldtIconicAnimalIds[climate.id].orEmpty()
    return speciesCatalog.filter { it.id in animalIds }
}
