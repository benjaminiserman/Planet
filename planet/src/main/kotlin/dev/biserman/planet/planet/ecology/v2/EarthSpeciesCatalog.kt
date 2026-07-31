package dev.biserman.planet.planet.ecology.v2

/**
 * Broad authoring and stress-test catalog of recognizable Earth organisms.
 *
 * These are ecological prototypes, not taxonomic rules or calibrated claims
 * about exact adult mass. Size classes intentionally use the nearest useful
 * simulation foundation, and every species remains an ordinary combination of
 * descriptive traits.
 */
object EarthSpeciesCatalog {
    val MAMMALS: List<SpeciesDefinition> = listOf(
        animal(
            "african-elephant",
            "African elephant",
            SizeClass.HUGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.PREHENSILE_TRUNK,
            CommonTrait.MASSIVE_EARS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.EXTENDED_BROOD_CARE
        ),
        animal(
            "giraffe",
            "Giraffe",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.LONG_NECK,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.BARE_HEAT_DISSIPATING_SKIN,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "plains-zebra",
            "Plains zebra",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.SWEAT_GLANDS,
            CommonTrait.OPEN_COUNTRY_HERDING,
            CommonTrait.REGIONAL_MIGRATION,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "blue-wildebeest",
            "Blue wildebeest",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.OPEN_COUNTRY_HERDING,
            CommonTrait.REGIONAL_MIGRATION,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "thomsons-gazelle",
            "Thomson's gazelle",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.BARE_HEAT_DISSIPATING_SKIN,
            CommonTrait.OPEN_COUNTRY_HERDING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "african-lion",
            "African lion",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.COOPERATIVE_HUNTING,
            CommonTrait.PREY_DERIVED_WATER,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "cheetah",
            "Cheetah",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.PREY_DERIVED_WATER,
            CommonTrait.BARE_HEAT_DISSIPATING_SKIN,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "spotted-hyena",
            "Spotted hyena",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.COOPERATIVE_HUNTING,
            CommonTrait.SCAVENGING_SENSES,
            CommonTrait.PREY_DERIVED_WATER,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "hippopotamus",
            "Hippopotamus",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.BARE_HEAT_DISSIPATING_SKIN,
            CommonTrait.ARMORED_HIDE
        ),
        animal(
            "white-rhinoceros",
            "White rhinoceros",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.ARMORED_HIDE,
            CommonTrait.BARE_HEAT_DISSIPATING_SKIN
        ),
        animal(
            "western-gorilla",
            "Western gorilla",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.EXTENDED_BROOD_CARE,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "chimpanzee",
            "Chimpanzee",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.TOOL_USING_FORELIMBS,
            CommonTrait.EXTENDED_BROOD_CARE,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "bornean-orangutan",
            "Bornean orangutan",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.TOOL_USING_FORELIMBS,
            CommonTrait.EXTENDED_BROOD_CARE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "polar-bear",
            "Polar bear",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.SEA_ICE_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.DENSE_FUR,
            CommonTrait.BLUBBER,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "brown-bear",
            "Brown bear",
            SizeClass.LARGE,
            CommonTrait.HETEROTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.DENSE_FUR,
            CommonTrait.FAT_RESERVES,
            CommonTrait.SEASONAL_TORPOR,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "gray-wolf",
            "Gray wolf",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.COOPERATIVE_HUNTING,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "red-fox",
            "Red fox",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.DENSE_FUR,
            CommonTrait.CACHED_FOOD,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "snowshoe-hare",
            "Snowshoe hare",
            SizeClass.MEDIUM,
            CommonTrait.HETEROTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.LEAPING_LEGS,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "north-american-beaver",
            "North American beaver",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.DENSE_FUR,
            CommonTrait.CACHED_FOOD,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "red-squirrel",
            "Red squirrel",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.SEED_CRACKING_MOUTHPARTS,
            CommonTrait.DENSE_FUR,
            CommonTrait.CACHED_FOOD,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "house-mouse",
            "House mouse",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.DIGGING_CLAWS,
            CommonTrait.CACHED_FOOD,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "norway-rat",
            "Norway rat",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.DIGGING_CLAWS,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "red-kangaroo",
            "Red kangaroo",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.LEAPING_LEGS,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.FOOD_DERIVED_WATER,
            CommonTrait.OPEN_COUNTRY_HERDING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "koala",
            "Koala",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.EXTENDED_BROOD_CARE,
            obligateBrowser(
                foodSpeciesId = "eucalyptus",
                displayName = "eucalyptus leaf specialization",
            ),
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "giant-panda",
            "Giant panda",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.DENSE_FUR,
            CommonTrait.FERMENTING_HINDGUT,
            obligateBrowser(
                foodSpeciesId = "giant-bamboo",
                displayName = "bamboo feeding specialization",
            ),
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "bengal-tiger",
            "Bengal tiger",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.CAMOUFLAGE_PATTERN,
            CommonTrait.PREY_DERIVED_WATER,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "snow-leopard",
            "Snow leopard",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.DENSE_FUR,
            CommonTrait.WOOLLY_UNDERCOAT,
            CommonTrait.SEASONAL_WINTER_COAT,
            CommonTrait.HYPOXIA_RESPONSIVE_METABOLISM,
            CommonTrait.FOOD_DERIVED_WATER,
            CommonTrait.CAMOUFLAGE_PATTERN,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "white-tailed-deer",
            "White-tailed deer",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.SEASONAL_WINTER_COAT,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "american-bison",
            "American bison",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.DENSE_FUR,
            CommonTrait.OPEN_COUNTRY_HERDING,
            CommonTrait.REGIONAL_MIGRATION,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "dromedary-camel",
            "Dromedary camel",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.WATER_STORAGE_TISSUE,
            CommonTrait.BARE_HEAT_DISSIPATING_SKIN,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "blue-whale",
            "Blue whale",
            SizeClass.COLOSSAL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.PROLONGED_BREATH_HOLDING,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.BALEEN,
            CommonTrait.BLUBBER,
            CommonTrait.LONG_MIGRATION,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.BLUE_GREEN
        ),
        animal(
            "humpback-whale",
            "Humpback whale",
            SizeClass.COLOSSAL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.PROLONGED_BREATH_HOLDING,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.BALEEN,
            CommonTrait.BLUBBER,
            CommonTrait.LONG_MIGRATION,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "orca",
            "Orca",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.PROLONGED_BREATH_HOLDING,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.COOPERATIVE_HUNTING,
            CommonTrait.BLUBBER,
            CommonTrait.ECHOLOCATION,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "bottlenose-dolphin",
            "Bottlenose dolphin",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.PROLONGED_BREATH_HOLDING,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.COOPERATIVE_HUNTING,
            CommonTrait.ECHOLOCATION,
            CommonTrait.BLUBBER,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "harbor-seal",
            "Harbor seal",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.PROLONGED_BREATH_HOLDING,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.BLUBBER,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "sea-otter",
            "Sea otter",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.TOOL_USING_FORELIMBS,
            CommonTrait.DENSE_FUR,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "walrus",
            "Walrus",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.PROLONGED_BREATH_HOLDING,
            CommonTrait.BENTHIC_SUCTION_FEEDING,
            CommonTrait.BLUBBER,
            CommonTrait.COASTAL_CLINGING_FEET,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "west-indian-manatee",
            "West Indian manatee",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.GRAZING_MOUTHPARTS,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "little-brown-bat",
            "Little brown bat",
            SizeClass.SMALL,
            CommonTrait.HETEROTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.ECHOLOCATION,
            CommonTrait.SEASONAL_TORPOR,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "duck-billed-platypus",
            "Duck-billed platypus",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.ELECTRORECEPTION,
            CommonTrait.DENSE_FUR,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "three-toed-sloth",
            "Three-toed sloth",
            SizeClass.MEDIUM,
            CommonTrait.HETEROTHERMY,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "giant-anteater",
            "Giant anteater",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.DIGGING_CLAWS,
            camouflage = BiologicalColor.BROWN
        ),
        // Siberian boreal forest and taiga
        animal(
            "siberian-musk-deer",
            "Siberian musk deer",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "sable",
            "Sable",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.DENSE_FUR,
            CommonTrait.CACHED_FOOD,
            camouflage = BiologicalColor.BROWN
        ),
        // Himalayan and Tibetan alpine plateau
        animal(
            "wild-yak",
            "Wild yak",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            CommonTrait.ENLARGED_CARDIOPULMONARY_SYSTEM,
            CommonTrait.SNOW_AND_ICE_LICKING,
            CommonTrait.OPEN_COUNTRY_HERDING,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "himalayan-pika",
            "Himalayan pika",
            SizeClass.SMALL,
            CommonTrait.HETEROTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            CommonTrait.FOOD_DERIVED_WATER,
            CommonTrait.HYPOXIA_RESPONSIVE_METABOLISM,
            CommonTrait.INSULATED_BURROW_REFUGE,
            CommonTrait.CACHED_FOOD,
            camouflage = BiologicalColor.BROWN
        ),
        // Rocky Mountains
        animal(
            "rocky-mountain-elk",
            "Rocky Mountain elk",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.SEASONAL_WINTER_COAT,
            CommonTrait.OPEN_COUNTRY_HERDING,
            CommonTrait.SHORT_MIGRATION,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "mountain-goat",
            "Mountain goat",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.LEAPING_LEGS,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            camouflage = BiologicalColor.PALE
        ),
        // High Andes
        animal(
            "vicuna",
            "Vicuña",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.DENSE_FUR,
            CommonTrait.HIGH_AFFINITY_HEMOGLOBIN,
            CommonTrait.OPEN_COUNTRY_HERDING,
            camouflage = BiologicalColor.BROWN
        ),
        // Sahara
        animal(
            "addax",
            "Addax",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.FOOD_DERIVED_WATER,
            CommonTrait.HEAT_STABLE_ENZYMES,
            CommonTrait.OPEN_COUNTRY_HERDING,
            CommonTrait.REGIONAL_MIGRATION,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "fennec-fox",
            "Fennec fox",
            SizeClass.SMALL,
            CommonTrait.HETEROTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.MASSIVE_EARS,
            CommonTrait.PREY_DERIVED_WATER,
            CommonTrait.DRY_BURROW_NEST,
            camouflage = BiologicalColor.PALE
        ),
        // Canadian Shield boreal forest
        animal(
            "woodland-caribou",
            "Woodland caribou",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.RUMINANT_STOMACH,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            CommonTrait.OPEN_COUNTRY_HERDING,
            CommonTrait.REGIONAL_MIGRATION,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "canada-lynx",
            "Canada lynx",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            CommonTrait.CAMOUFLAGE_PATTERN,
            camouflage = BiologicalColor.PALE
        ),
    )

    val EXTINCT_SPECIES: List<SpeciesDefinition> = listOf(
        animal(
            "tyrannosaurus-rex",
            "Tyrannosaurus rex",
            SizeClass.HUGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "velociraptor",
            "Velociraptor",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.HOOKED_TALONS,
            CommonTrait.INSULATING_PLUMAGE,
            CommonTrait.COOPERATIVE_HUNTING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "triceratops",
            "Triceratops",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.ARMORED_HIDE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "stegosaurus",
            "Stegosaurus",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            CommonTrait.ARMORED_HIDE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "ankylosaurus",
            "Ankylosaurus",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.ARMORED_HIDE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "brachiosaurus",
            "Brachiosaurus",
            SizeClass.HUGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.LONG_NECK,
            CommonTrait.FERMENTING_HINDGUT,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "pteranodon",
            "Pteranodon",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "woolly-mammoth",
            "Woolly mammoth",
            SizeClass.HUGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.PREHENSILE_TRUNK,
            CommonTrait.DENSE_FUR,
            CommonTrait.SEASONAL_WINTER_COAT,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "saber-toothed-cat",
            "Saber-toothed cat",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.COOPERATIVE_HUNTING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "dodo",
            "Dodo",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.SEED_CRACKING_MOUTHPARTS,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "megalodon",
            "Megalodon",
            SizeClass.HUGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.GILLS,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.ELECTRORECEPTION,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "trilobite",
            "Trilobite",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.COASTAL_CLINGING_FEET,
            CommonTrait.GILLS,
            CommonTrait.MARINE_SNOW_PALPS,
            CommonTrait.ARMORED_HIDE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "ammonite",
            "Ammonite",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.GILLS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.PROTECTIVE_SHELL,
            camouflage = BiologicalColor.PALE
        ),
    )

    val BIRDS: List<SpeciesDefinition> = listOf(
        animal(
            "bald-eagle",
            "Bald eagle",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.HOOKED_TALONS,
            CommonTrait.INSULATING_PLUMAGE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "great-horned-owl",
            "Great horned owl",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.HOOKED_TALONS,
            CommonTrait.INSULATING_PLUMAGE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "peregrine-falcon",
            "Peregrine falcon",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.HOOKED_TALONS,
            CommonTrait.INSULATING_PLUMAGE,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "ruby-throated-hummingbird",
            "Ruby-throated hummingbird",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.NECTAR_SIPPING_TONGUE,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.GREEN
        ),
        animal(
            "scarlet-macaw",
            "Scarlet macaw",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.SEED_CRACKING_MOUTHPARTS,
            CommonTrait.EXTENDED_BROOD_CARE,
            camouflage = BiologicalColor.RED
        ),
        animal(
            "common-raven",
            "Common raven",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.SCAVENGING_SENSES,
            CommonTrait.TOOL_USING_FORELIMBS,
            CommonTrait.INSULATING_PLUMAGE,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "emperor-penguin",
            "Emperor penguin",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.PROLONGED_BREATH_HOLDING,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.SEA_ICE_ROOKERY,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.INSULATING_PLUMAGE,
            CommonTrait.WATERPROOF_PLUMAGE,
            CommonTrait.COLD_ACTIVE_ENZYMES,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "common-ostrich",
            "Common ostrich",
            SizeClass.LARGE,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.LEAPING_LEGS,
            CommonTrait.BARE_HEAT_DISSIPATING_SKIN,
            CommonTrait.OPEN_COUNTRY_HERDING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "greater-flamingo",
            "Greater flamingo",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.EURYHALINE_OSMOREGULATION,
            CommonTrait.SIEVE_TEETH,
            CommonTrait.WATERPROOF_PLUMAGE,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "brown-pelican",
            "Brown pelican",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.EURYHALINE_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.WATERPROOF_PLUMAGE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "mallard",
            "Mallard duck",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.WATERPROOF_PLUMAGE,
            CommonTrait.REGIONAL_MIGRATION,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "canada-goose",
            "Canada goose",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.WATERPROOF_PLUMAGE,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "mute-swan",
            "Mute swan",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.WATERPROOF_PLUMAGE,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "red-junglefowl",
            "Red junglefowl",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.SEED_CRACKING_MOUTHPARTS,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "indian-peafowl",
            "Indian peafowl",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.SEED_CRACKING_MOUTHPARTS,
            camouflage = BiologicalColor.BLUE_GREEN
        ),
        animal(
            "pileated-woodpecker",
            "Pileated woodpecker",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.INSULATING_PLUMAGE,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "wandering-albatross",
            "Wandering albatross",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.EURYHALINE_OSMOREGULATION,
            CommonTrait.PELAGIC_SOARING_WINGS,
            CommonTrait.SCAVENGING_SENSES,
            CommonTrait.WATERPROOF_PLUMAGE,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "turkey-vulture",
            "Turkey vulture",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.SCAVENGING_SENSES,
            CommonTrait.EXPANDABLE_CROP,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "african-grey-parrot",
            "African grey parrot",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.SEED_CRACKING_MOUTHPARTS,
            CommonTrait.EXTENDED_BROOD_CARE,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "common-kingfisher",
            "Common kingfisher",
            SizeClass.SMALL,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.WATERPROOF_PLUMAGE,
            camouflage = BiologicalColor.BLUE_GREEN
        ),
        animal(
            "andean-condor",
            "Andean condor",
            SizeClass.MEDIUM,
            CommonTrait.ENDOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.SCAVENGING_SENSES,
            CommonTrait.EXPANDABLE_CROP,
            CommonTrait.INSULATING_PLUMAGE,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.BLACK
        ),
    )

    val REPTILES_AND_AMPHIBIANS: List<SpeciesDefinition> = listOf(
        animal(
            "nile-crocodile",
            "Nile crocodile",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.ARMORED_HIDE,
            CommonTrait.SEASONAL_TORPOR,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "american-alligator",
            "American alligator",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AMPHIBIOUS_LIMBS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.ARMORED_HIDE,
            CommonTrait.SEASONAL_TORPOR,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "komodo-dragon",
            "Komodo dragon",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.VENOM_DELIVERY,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "green-iguana",
            "Green iguana",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.BROWSING_MOUTHPARTS,
            CommonTrait.FERMENTING_HINDGUT,
            camouflage = BiologicalColor.GREEN
        ),
        animal(
            "veiled-chameleon",
            "Veiled chameleon",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.CAMOUFLAGE_PATTERN,
            camouflage = BiologicalColor.GREEN
        ),
        animal(
            "reticulated-python",
            "Reticulated python",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.CONSTRICTING_BODY,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "king-cobra",
            "King cobra",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.VENOM_DELIVERY,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "western-diamondback",
            "Western diamondback rattlesnake",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.SUBTERRANEAN_BURROWING,
            CommonTrait.HEAT_STABLE_ENZYMES,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.VENOM_DELIVERY,
            CommonTrait.FOOD_DERIVED_WATER,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "green-sea-turtle",
            "Green sea turtle",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.WARM_WATER_ENZYMES,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.PROLONGED_BREATH_HOLDING,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.PROTECTIVE_SHELL,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.GREEN
        ),
        animal(
            "galapagos-tortoise",
            "Galapagos tortoise",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.PROTECTIVE_SHELL,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "red-eyed-tree-frog",
            "Red-eyed tree frog",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.LEAPING_LEGS,
            CommonTrait.TOXIC_SKIN,
            camouflage = BiologicalColor.GREEN
        ),
        animal(
            "poison-dart-frog",
            "Poison dart frog",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.TOXIC_SKIN,
            camouflage = BiologicalColor.RED
        ),
        animal(
            "axolotl",
            "Axolotl",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.GILLS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "common-mudpuppy",
            "Common mudpuppy",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.GILLS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "tuatara",
            "Tuatara",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.SUBTERRANEAN_BURROWING,
            CommonTrait.SEASONAL_TORPOR,
            camouflage = BiologicalColor.BROWN
        ),
    )

    val FISH: List<SpeciesDefinition> = listOf(
        animal(
            "great-white-shark",
            "Great white shark",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.ELECTRORECEPTION,
            CommonTrait.FAT_RESERVES,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "whale-shark",
            "Whale shark",
            SizeClass.HUGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.WARM_WATER_ENZYMES,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.GILL_PADS,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.BLUE_GREEN
        ),
        animal(
            "giant-oceanic-manta",
            "Giant oceanic manta ray",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.GILL_PADS,
            CommonTrait.ELECTRORECEPTION,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "atlantic-bluefin-tuna",
            "Atlantic bluefin tuna",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.BLUE_GREEN
        ),
        animal(
            "atlantic-salmon",
            "Atlantic salmon",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.EURYHALINE_OSMOREGULATION,
            CommonTrait.COLD_ACTIVE_ENZYMES,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "ocellaris-clownfish",
            "Ocellaris clownfish",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.WARM_WATER_ENZYMES,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.GILL_PADS,
            CommonTrait.REEF_NESTING,
            camouflage = BiologicalColor.RED
        ),
        animal(
            "lined-seahorse",
            "Lined seahorse",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.REEF_CAMOUFLAGE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "electric-eel",
            "Electric eel",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.ELECTRORECEPTION,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "deep-sea-anglerfish",
            "Deep-sea anglerfish",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.COLD_ACTIVE_ENZYMES,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.BIOLUMINESCENT_LURE,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "porcupinefish",
            "Porcupinefish",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.QUILLS,
            CommonTrait.REEF_NESTING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "atlantic-swordfish",
            "Atlantic swordfish",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.BLUE_GREEN
        ),
        animal(
            "channel-catfish",
            "Channel catfish",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "common-carp",
            "Common carp",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.BENTHIC_SUCTION_FEEDING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "peruvian-anchoveta",
            "Peruvian anchoveta",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.GILL_PADS,
            camouflage = BiologicalColor.BLUE_GREEN
        ),
        animal(
            "coral-grouper",
            "Coral grouper",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.WARM_WATER_ENZYMES,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.REEF_CAMOUFLAGE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "bumphead-parrotfish",
            "Bumphead parrotfish",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.WARM_WATER_ENZYMES,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.REEF_BORING,
            camouflage = BiologicalColor.GREEN
        ),
        animal(
            "arapaima",
            "Arapaima",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.FRESHWATER_OSMOREGULATION,
            CommonTrait.AMBUSH_MUSCULATURE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "antarctic-silverfish",
            "Antarctic silverfish",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.AQUATIC_FLIPPERS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.ANTIFREEZE_PROTEINS,
            CommonTrait.COLD_ACTIVE_ENZYMES,
            camouflage = BiologicalColor.PALE
        ),
    ).map { definition ->
        if (CommonTrait.GILLS in definition.traits) {
            definition
        } else {
            definition.copy(traits = definition.traits + CommonTrait.GILLS)
        }
    }

    val INVERTEBRATES: List<SpeciesDefinition> = listOf(
        animal(
            "common-octopus",
            "Common octopus",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.WARM_WATER_ENZYMES,
            CommonTrait.GILLS,
            CommonTrait.JET_PROPULSION,
            CommonTrait.GRASPING_TENTACLES,
            CommonTrait.SUCTION_CUPS,
            CommonTrait.INK_CLOUD,
            CommonTrait.TOOL_USING_FORELIMBS,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "giant-squid",
            "Giant squid",
            SizeClass.LARGE,
            CommonTrait.ECTOTHERMY,
            CommonTrait.GILLS,
            CommonTrait.JET_PROPULSION,
            CommonTrait.DEEP_DIVING_PHYSIOLOGY,
            CommonTrait.GRASPING_TENTACLES,
            CommonTrait.SUCTION_CUPS,
            CommonTrait.INK_CLOUD,
            camouflage = BiologicalColor.RED
        ),
        animal(
            "moon-jelly",
            "Moon jellyfish",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.DIFFUSIVE_AQUATIC_GAS_EXCHANGE,
            CommonTrait.BUOYANCY_BLADDER,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.VENOM_DELIVERY,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "crown-of-thorns-starfish",
            "Crown-of-thorns starfish",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.WARM_WATER_ENZYMES,
            CommonTrait.DIFFUSIVE_AQUATIC_GAS_EXCHANGE,
            CommonTrait.COASTAL_CLINGING_FEET,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.TOXIC_SKIN,
            CommonTrait.REEF_BORING,
            camouflage = BiologicalColor.RED
        ),
        animal(
            "blue-crab",
            "Blue crab",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.WARM_WATER_ENZYMES,
            CommonTrait.GILLS,
            CommonTrait.COASTAL_CLINGING_FEET,
            CommonTrait.CRUSHING_CLAWS,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.ARMORED_HIDE,
            camouflage = BiologicalColor.BLUE_GREEN
        ),
        animal(
            "american-lobster",
            "American lobster",
            SizeClass.MEDIUM,
            CommonTrait.ECTOTHERMY,
            CommonTrait.GILLS,
            CommonTrait.COASTAL_CLINGING_FEET,
            CommonTrait.CRUSHING_CLAWS,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.ARMORED_HIDE,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "cleaner-shrimp",
            "Cleaner shrimp",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.GILLS,
            CommonTrait.COASTAL_CLINGING_FEET,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.REEF_NESTING,
            camouflage = BiologicalColor.RED
        ),
        sessile(
            "staghorn-coral",
            "Staghorn coral",
            SizeClass.SMALL,
            BiologicalColor.BROWN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.DIFFUSIVE_AQUATIC_GAS_EXCHANGE,
            CommonTrait.SUBSTRATE_HOLDFAST,
            CommonTrait.REEF_BUILDING,
            CommonTrait.SHALLOW_WATER_PHOTOSYMBIOSIS,
            CommonTrait.WARM_WATER_ENZYMES
        ),
        sessile(
            "eastern-oyster",
            "Eastern oyster",
            SizeClass.SMALL,
            null,
            CommonTrait.SUBSTRATE_HOLDFAST,
            CommonTrait.GILL_PADS,
            CommonTrait.WARM_WATER_ENZYMES
        ),
        sessile(
            "blue-mussel",
            "Blue mussel",
            SizeClass.SMALL,
            null,
            CommonTrait.SUBSTRATE_HOLDFAST,
            CommonTrait.GILL_PADS,
            CommonTrait.PROTECTIVE_SHELL
        ),
        animal(
            "garden-snail",
            "Garden snail",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.PROTECTIVE_SHELL,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "banana-slug",
            "Banana slug",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "common-earthworm",
            "Common earthworm",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.DECOMPOSING_ENZYMES,
            CommonTrait.SUBTERRANEAN_BURROWING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "monarch-butterfly",
            "Monarch butterfly",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.NECTAR_SIPPING_TONGUE,
            CommonTrait.TOXIC_SKIN,
            CommonTrait.LONG_MIGRATION,
            camouflage = BiologicalColor.RED
        ),
        animal(
            "western-honey-bee",
            "Western honey bee",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.NECTAR_SIPPING_TONGUE,
            CommonTrait.COLONY_LIVING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "leafcutter-ant",
            "Leafcutter ant",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.COLONY_LIVING,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "termite",
            "Termite",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.DECOMPOSING_ENZYMES,
            CommonTrait.COLONY_LIVING,
            CommonTrait.SUBTERRANEAN_BURROWING,
            camouflage = BiologicalColor.PALE
        ),
        animal(
            "desert-locust",
            "Desert locust",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.GRAZING_MOUTHPARTS,
            CommonTrait.HEAT_STABLE_ENZYMES,
            CommonTrait.REGIONAL_MIGRATION,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "seven-spot-ladybird",
            "Seven-spot ladybird",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.AMBUSH_MUSCULATURE,
            camouflage = BiologicalColor.RED
        ),
        animal(
            "orb-weaver-spider",
            "Orb-weaver spider",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.CLIMBING_LIMBS,
            CommonTrait.WEB_SILK,
            CommonTrait.VENOM_DELIVERY,
            camouflage = BiologicalColor.BROWN
        ),
        animal(
            "emperor-scorpion",
            "Emperor scorpion",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.VENOM_DELIVERY,
            CommonTrait.ARMORED_HIDE,
            CommonTrait.HEAT_STABLE_ENZYMES,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "common-green-darner",
            "Common green darner dragonfly",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.PURSUIT_LIMBS,
            CommonTrait.REGIONAL_MIGRATION,
            camouflage = BiologicalColor.GREEN
        ),
        animal(
            "common-mosquito",
            "Common mosquito",
            SizeClass.TINY,
            CommonTrait.ECTOTHERMY,
            CommonTrait.POWERED_FLIGHT,
            CommonTrait.SAP_SUCKING_PROBOSCIS,
            CommonTrait.BURROWING_EGGS,
            camouflage = BiologicalColor.BLACK
        ),
        animal(
            "giant-centipede",
            "Giant centipede",
            SizeClass.SMALL,
            CommonTrait.ECTOTHERMY,
            CommonTrait.TERRESTRIAL_LOCOMOTION,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.VENOM_DELIVERY,
            camouflage = BiologicalColor.BROWN
        ),
    )

    val PRODUCERS_AND_FUNGI: List<SpeciesDefinition> = listOf(
        sessile(
            "coast-redwood",
            "Coast redwood",
            SizeClass.COLOSSAL,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.DEEP_ROOT_SYSTEM,
            CommonTrait.PERENNIAL_STORAGE_TISSUE
        ),
        sessile(
            "english-oak",
            "English oak",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.SEASONAL_LEAF_DORMANCY,
            CommonTrait.FROST_HARDENED_TISSUES,
            CommonTrait.DEEP_ROOT_SYSTEM
        ),
        sessile(
            "scots-pine",
            "Scots pine",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.FROST_HARDENED_TISSUES,
            CommonTrait.SEASONAL_LEAF_DORMANCY
        ),
        sessile(
            "african-baobab",
            "African baobab",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.SUCCULENT_STEM,
            CommonTrait.DROUGHT_DECIDUOUS_LEAVES
        ),
        sessile(
            "umbrella-thorn-acacia",
            "Umbrella thorn acacia",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.DEEP_ROOT_SYSTEM,
            CommonTrait.WAXY_CUTICLE,
            CommonTrait.DROUGHT_DECIDUOUS_LEAVES
        ),
        sessile(
            "red-mangrove",
            "Red mangrove",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.SALT_EXCLUDING_ROOTS,
            CommonTrait.WAXY_CUTICLE
        ),
        sessile(
            "saguaro",
            "Saguaro cactus",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.SUCCULENT_STEM,
            CommonTrait.WAXY_CUTICLE,
            CommonTrait.FROST_SENSITIVE_SUCCULENT_TISSUES,
            CommonTrait.DESICCATION_RESISTANT_PROPAGULES
        ),
        sessile(
            "common-sunflower",
            "Common sunflower",
            SizeClass.SMALL,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.DEEP_ROOT_SYSTEM,
            CommonTrait.DESICCATION_RESISTANT_PROPAGULES
        ),
        sessile(
            "perennial-ryegrass",
            "Perennial ryegrass",
            SizeClass.TINY,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.SEASONAL_LEAF_DORMANCY
        ),
        sessile(
            "common-wheat",
            "Common wheat",
            SizeClass.TINY,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.DESICCATION_RESISTANT_PROPAGULES
        ),
        sessile(
            "eucalyptus",
            "Eucalyptus tree",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.DEEP_ROOT_SYSTEM
        ),
        sessile(
            "giant-bamboo",
            "Giant bamboo",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.PERENNIAL_STORAGE_TISSUE
        ),
        sessile(
            "bracken-fern",
            "Bracken fern",
            SizeClass.SMALL,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.SHADE_FRONDS,
            CommonTrait.PERENNIAL_STORAGE_TISSUE,
            CommonTrait.SEASONAL_LEAF_DORMANCY
        ),
        sessile(
            "sphagnum-moss",
            "Sphagnum moss",
            SizeClass.TINY,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.SHADE_FRONDS
        ),
        sessile(
            "white-water-lily",
            "White water lily",
            SizeClass.SMALL,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.SUBSTRATE_HOLDFAST,
            CommonTrait.FRESHWATER_OSMOREGULATION
        ),
        sessile(
            "eelgrass",
            "Eelgrass",
            SizeClass.SMALL,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.DIFFUSIVE_AQUATIC_GAS_EXCHANGE,
            CommonTrait.SUBSTRATE_HOLDFAST,
            CommonTrait.SALT_EXCLUDING_ROOTS
        ),
        sessile(
            "giant-kelp",
            "Giant kelp",
            SizeClass.LARGE,
            BiologicalColor.BROWN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.DIFFUSIVE_AQUATIC_GAS_EXCHANGE,
            CommonTrait.SUBSTRATE_HOLDFAST,
            CommonTrait.FLOATING_FRONDS,
            CommonTrait.PERENNIAL_STORAGE_TISSUE
        ),
        sessile(
            "field-mushroom",
            "Field mushroom",
            SizeClass.SMALL,
            null,
            CommonTrait.ROOTED_BODY,
            CommonTrait.ABSORPTIVE_FILAMENTS,
            CommonTrait.DECOMPOSING_ENZYMES,
            CommonTrait.DESICCATION_RESISTANT_PROPAGULES
        ),
        sessile(
            "bread-mold",
            "Bread mold",
            SizeClass.TINY,
            null,
            CommonTrait.ROOTED_BODY,
            CommonTrait.ABSORPTIVE_FILAMENTS,
            CommonTrait.DECOMPOSING_ENZYMES,
            CommonTrait.DESICCATION_RESISTANT_PROPAGULES
        ),
        sessile(
            "reindeer-lichen",
            "Reindeer lichen",
            SizeClass.TINY,
            // Its thallus is pale, but its photobiont still captures light with
            // chlorophyll rather than a pale photosynthetic pigment.
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.ABSORPTIVE_FILAMENTS,
            CommonTrait.FROST_HARDENED_TISSUES,
            CommonTrait.WHOLE_BODY_ANHYDROBIOSIS
        ),
        sessile(
            "venus-flytrap",
            "Venus flytrap",
            SizeClass.SMALL,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.AMBUSH_MUSCULATURE,
            CommonTrait.WATER_STORAGE_TISSUE
        ),
        sessile(
            "siberian-larch",
            "Siberian larch",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.FROST_HARDENED_TISSUES,
            CommonTrait.SEASONAL_LEAF_DORMANCY,
            CommonTrait.DEEP_ROOT_SYSTEM
        ),
        sessile(
            "himalayan-juniper",
            "Himalayan juniper",
            SizeClass.MEDIUM,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.FROST_HARDENED_TISSUES,
            CommonTrait.WAXY_CUTICLE,
            CommonTrait.DEEP_ROOT_SYSTEM
        ),
        sessile(
            "lodgepole-pine",
            "Lodgepole pine",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.FROST_HARDENED_TISSUES,
            CommonTrait.SEASONAL_LEAF_DORMANCY,
            CommonTrait.PERENNIAL_STORAGE_TISSUE
        ),
        sessile(
            "ichu-grass",
            "Ichu grass",
            SizeClass.TINY,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.FROST_HARDENED_TISSUES,
            CommonTrait.SEASONAL_LEAF_DORMANCY,
            CommonTrait.WAXY_CUTICLE
        ),
        sessile(
            "saharan-cypress",
            "Saharan cypress",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.DEEP_ROOT_SYSTEM,
            CommonTrait.WAXY_CUTICLE,
            CommonTrait.DROUGHT_DECIDUOUS_LEAVES
        ),
        sessile(
            "black-spruce",
            "Black spruce",
            SizeClass.LARGE,
            BiologicalColor.GREEN,
            CommonTrait.PHOTOSYNTHETIC_SURFACE,
            CommonTrait.ROOTED_BODY,
            CommonTrait.CANOPY_GROWTH,
            CommonTrait.FROST_HARDENED_TISSUES,
            CommonTrait.SEASONAL_LEAF_DORMANCY,
            CommonTrait.PERENNIAL_STORAGE_TISSUE
        ),
    )

    val ALL: List<SpeciesDefinition> =
        MAMMALS /*+ EXTINCT_SPECIES*/ + BIRDS + REPTILES_AND_AMPHIBIANS + FISH +
                INVERTEBRATES + PRODUCERS_AND_FUNGI

    private fun animal(
        id: String,
        name: String,
        sizeClass: SizeClass,
        thermalStrategy: CommonTrait,
        vararg adaptations: SpeciesTrait,
        camouflage: BiologicalColor? = null,
        biochemistry: CommonTrait = CommonTrait.TEMPERATE_BIOCHEMISTRY,
    ) = SpeciesDefinition(
        id = id,
        displayName = name,
        sizeClass = sizeClass,
        motile = true,
        traits = listOf(biochemistry, thermalStrategy) + adaptations,
        camouflageColor = camouflage,
    )

    private fun sessile(
        id: String,
        name: String,
        sizeClass: SizeClass,
        photosyntheticColor: BiologicalColor?,
        vararg adaptations: SpeciesTrait,
    ) = SpeciesDefinition(
        id = id,
        displayName = name,
        sizeClass = sizeClass,
        motile = false,
        traits = listOf(CommonTrait.TEMPERATE_BIOCHEMISTRY) + adaptations,
        photosyntheticColor = photosyntheticColor,
    )

    private fun obligateBrowser(
        foodSpeciesId: String,
        displayName: String,
    ) = TargetedRelationshipTrait(
        displayName = displayName,
        description =
            "Digestive and feeding anatomy is specialized around one locally available producer lineage.",
        relationships = listOf(
            RelationshipEffect.ObligateFood(
                target = SpeciesSelector.ExactSpecies(foodSpeciesId),
                attackRate = 0.0015,
                assimilationEfficiency = 0.65,
            ),
        ),
        maintenanceCost = 0.04,
    )
}
