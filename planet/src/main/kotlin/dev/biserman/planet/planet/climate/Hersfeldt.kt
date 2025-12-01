package dev.biserman.planet.planet.climate

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.utils.UtilityExtensions.formatDigits
import dev.biserman.planet.utils.UtilityExtensions.weightedAverage
import godot.common.util.lerp
import godot.core.Color
import godot.global.GD
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

// a climate classification scheme developed by Nikolai Hersfeldt
// see: https://worldbuildingpasta.blogspot.com/2025/03/beyond-koppen-geiger-climate.html#climateparameters
// included with permission
object Hersfeldt : ClimateClassifier {
    var enableAlternativeNames = true
    val alternativeNames = mapOf(
        "hyperpluvial_tropical_rainforest" to "tropical_monsoon_rainforest",

        "quasitropical_forest" to "semitropical_forest",
        "quasitropical_monsoon_forest" to "semitropical_monsoon_forest",
        "quasitropical_moist_savanna" to "semitropical_moist_savanna",
        "quasitropical_moist_monsoon_savanna" to "semitropical_moist_monsoon_savanna",
        "quasitropical_dry_savanna" to "semitropical_dry_savanna",
        "quasitropical_dry_monsoon_savanna" to "semitropical_dry_monsoon_savanna",

        "tropical_barren" to "tropical_permanent_night",

        "percontinental_boreal" to "frigid_boreal",
        "percontinental_boreal_rainforest" to "frigid_boreal_rainforest",

        "cold_pluvial_steppe" to "cold_monsoon_steppe",
        "supertropical_forest" to "hot_tropical_forest",
        "supertropical_monsoon_forest" to "hot_tropical_monsoon_forest",
        "supertropical_moist_savanna" to "hot_moist_savanna",
        "supertropical_moist_monsoon_savanna" to "hot_moist_monsoon_savanna",

        "hot_pluvial_swelter" to "hot_monsoon_swelter",
        "torrid_pluvial_swelter" to "torrid_monsoon_swelter",
        "boiling_pluvial_swelter" to "boiling_monsoon_swelter",

        "hot_subparamediterranean" to "hot_subcalditerranean",
        "torrid_subparamediterranean" to "torrid_subcalditerranean",
        "boiling_subparamediterranean" to "boiling_subcalditerranean",
        "hot_paramediterranean" to "hot_calditerranean",
        "torrid_paramediterranean" to "torrid_calditerranean",
        "boiling_paramediterranean" to "boiling_calditerranean",

        "torrid_pluvial_steppe" to "torrid_monsoon_steppe",
        "boiling_pluvial_steppe" to "boiling_monsoon_steppe",

        "extratropical_forest" to "superseasonal_rainforest",
        "extratropical_monsoon_forest" to "superseasonal_monsoon_rainforest",
        "extratropical_moist_savanna" to "superseasonal_moist_savanna",
        "extratropical_moist_monsoon_savanna" to "superseasonal_moist_monsoon_savanna",
        "superseasonal_extracontinental" to "superseasonal_continental",
        "superseasonal_extracontinental_rainforest" to "superseasonal_continental_monsoon",
        "hyperseasonal_extracontinental" to "hyperseasonal_continental",
        "hyperseasonal_extracontinental_rainforest" to "hyperseasonal_continental_monsoon",
        "superseasonal_subextramediterranean" to "superseasonal_submediterranean",
        "hyperseasonal_subextramediterranean" to "hyperseasonal_submediterranean",
        "superseasonal_extramediterranean" to "superseasonal_mediterranean",
        "hyperseasonal_extramediterranean" to "hyperseasonal_mediterranean",
        "hyperseasonal_pluvial_steppe" to "hyperseasonal_monsoon_steppe",
    )

    fun (ClimateClassification).altName() =
        this.copy(name = if (enableAlternativeNames) alternativeNames[this.name] ?: this.name else this.name)

    // Arid (A) - Hyperarid Desert
    private val WARM_DESERT =
        ClimateClassification("Aha", "warm_desert", Color.html("FFFBCC"), Color.html("insert_here")).altName()
    private val COLD_DESERT =
        ClimateClassification("Ahc", "cold_desert", Color.html("ECF2E6"), Color.html("insert_here")).altName()
    private val HOT_DESERT =
        ClimateClassification("Ahh", "hot_desert", Color.html("FFE0E0"), Color.html("insert_here")).altName()
    private val HYPERSEASONAL_DESERT =
        ClimateClassification("Ahe", "hyperseasonal_desert", Color.html("EFD2EE"), Color.html("insert_here")).altName()

    // Arid (A) - Semidesert
    private val WARM_SEMIDESERT =
        ClimateClassification("Ada", "warm_semidesert", Color.html("E8E6A2"), Color.html("insert_here")).altName()
    private val COLD_SEMIDESERT =
        ClimateClassification("Adc", "cold_semidesert", Color.html("CDDDBA"), Color.html("insert_here")).altName()
    private val HOT_SEMIDESERT =
        ClimateClassification("Adh", "hot_semidesert", Color.html("EAB8B8"), Color.html("insert_here")).altName()
    private val HYPERSEASONAL_SEMIDESERT =
        ClimateClassification(
            "Ade",
            "hyperseasonal_semidesert",
            Color.html("E3BAE3"),
            Color.html("insert_here")
        ).altName()

    // Tropical (T) - Eutropical
    private val TROPICAL_RAINFOREST =
        ClimateClassification("TUr", "tropical_rainforest", Color.html("0000FF"), Color.html("insert_here")).altName()
    private val HYPERPLUVIAL_TROPICAL_RAINFOREST = ClimateClassification(
        "TUrp",
        "hyperpluvial_tropical_rainforest",
        Color.html("0400BF"),
        Color.html("insert_here")
    ).altName()
    private val TROPICAL_FOREST =
        ClimateClassification("TUf", "tropical_forest", Color.html("2970FF"), Color.html("insert_here")).altName()
    private val TROPICAL_MONSOON_FOREST =
        ClimateClassification(
            "TUfp",
            "tropical_monsoon_forest",
            Color.html("1A50BC"),
            Color.html("insert_here")
        ).altName()
    private val TROPICAL_MOIST_SAVANNA =
        ClimateClassification(
            "TUs",
            "tropical_moist_savanna",
            Color.html("91B4FF"),
            Color.html("insert_here")
        ).altName()
    private val TROPICAL_MOIST_MONSOON_SAVANNA = ClimateClassification(
        "TUsp",
        "tropical_moist_monsoon_savanna",
        Color.html("5F7DC4"),
        Color.html("insert_here")
    ).altName()
    private val TROPICAL_DRY_SAVANNA =
        ClimateClassification("TUA", "tropical_dry_savanna", Color.html("C7D8FF"), Color.html("insert_here")).altName()
    private val TROPICAL_DRY_MONSOON_SAVANNA = ClimateClassification(
        "TUAp",
        "tropical_dry_monsoon_savanna",
        Color.html("889DCE"),
        Color.html("insert_here")
    ).altName()

    // Tropical (T) - Quasitropical
    private val QUASITROPICAL_FOREST =
        ClimateClassification("TQf", "quasitropical_forest", Color.html("37D2C0"), Color.html("insert_here")).altName()
    private val QUASITROPICAL_MONSOON_FOREST = ClimateClassification(
        "TQfp",
        "quasitropical_monsoon_forest",
        Color.html("308D82"),
        Color.html("insert_here")
    ).altName()
    private val QUASITROPICAL_MOIST_SAVANNA = ClimateClassification(
        "TQs",
        "quasitropical_moist_savanna",
        Color.html("75F5E6"),
        Color.html("insert_here")
    ).altName()
    private val QUASITROPICAL_MOIST_MONSOON_SAVANNA = ClimateClassification(
        "TQsp",
        "quasitropical_moist_monsoon_savanna",
        Color.html("72C5BC"),
        Color.html("insert_here")
    ).altName()
    private val QUASITROPICAL_DRY_SAVANNA =
        ClimateClassification(
            "TQA",
            "quasitropical_dry_savanna",
            Color.html("BAFDF5"),
            Color.html("insert_here")
        ).altName()
    private val QUASITROPICAL_DRY_MONSOON_SAVANNA = ClimateClassification(
        "TQAp",
        "quasitropical_dry_monsoon_savanna",
        Color.html("AEDBD5"),
        Color.html("insert_here")
    ).altName()

    // Tropical (T) - Marginal
    private val TROPICAL_TWILIGHT =
        ClimateClassification("TF", "tropical_twilight", Color.html("535393"), Color.html("insert_here")).altName()
    private val TROPICAL_BARREN =
        ClimateClassification("TG", "tropical_barren", Color.html("1E1C6D"), Color.html("insert_here")).altName()

    // Cold (C) - Subtropical
    private val SUBTROPICAL_FOREST =
        ClimateClassification("CTf", "subtropical_forest", Color.html("54DA22"), Color.html("insert_here")).altName()
    private val SUBTROPICAL_MONSOON_FOREST = ClimateClassification(
        "CTfp",
        "subtropical_monsoon_forest",
        Color.html("369E10"),
        Color.html("insert_here")
    ).altName()
    private val SUBTROPICAL_MOIST_SAVANNA =
        ClimateClassification(
            "CTs",
            "subtropical_moist_savanna",
            Color.html("A7FD81"),
            Color.html("insert_here")
        ).altName()
    private val SUBTROPICAL_MOIST_MONSOON_SAVANNA = ClimateClassification(
        "CTsp",
        "subtropical_moist_monsoon_savanna",
        Color.html("78C059"),
        Color.html("insert_here")
    ).altName()

    // Cold (C) - Temperate
    private val OCEANIC_TEMPERATE =
        ClimateClassification("CDa", "oceanic_temperate", Color.html("0EFB5D"), Color.html("insert_here")).altName()
    private val OCEANIC_TEMPERATE_RAINFOREST = ClimateClassification(
        "CDap",
        "oceanic_temperate_rainforest",
        Color.html("00C241"),
        Color.html("insert_here")
    ).altName()
    private val CONTINENTAL_TEMPERATE =
        ClimateClassification("CDb", "continental_temperate", Color.html("00DB75"), Color.html("insert_here")).altName()
    private val CONTINENTAL_TEMPERATE_RAINFOREST = ClimateClassification(
        "CDbp",
        "continental_temperate_rainforest",
        Color.html("059E42"),
        Color.html("insert_here")
    ).altName()

    // Cold (C) - Boreal
    private val OCEANIC_BOREAL =
        ClimateClassification("CEa", "oceanic_boreal", Color.html("ACFBD6"), Color.html("insert_here")).altName()
    private val OCEANIC_BOREAL_RAINFOREST =
        ClimateClassification(
            "CEap",
            "oceanic_boreal_rainforest",
            Color.html("85D6B0"),
            Color.html("insert_here")
        ).altName()
    private val CONTINENTAL_BOREAL =
        ClimateClassification("CEb", "continental_boreal", Color.html("70F0BA"), Color.html("insert_here")).altName()
    private val CONTINENTAL_BOREAL_RAINFOREST = ClimateClassification(
        "CEbp",
        "continental_boreal_rainforest",
        Color.html("36AB78"),
        Color.html("insert_here")
    ).altName()
    private val PERCONTINENTAL_BOREAL =
        ClimateClassification("CEc", "percontinental_boreal", Color.html("41FBFB"), Color.html("insert_here")).altName()
    private val PERCONTINENTAL_BOREAL_RAINFOREST = ClimateClassification(
        "CEcp",
        "percontinental_boreal_rainforest",
        Color.html("04B6B9"),
        Color.html("insert_here")
    ).altName()

    // Cold (C) - Submediterranean
    private val OCEANIC_SUBMEDITERRANEAN =
        ClimateClassification(
            "CMa",
            "oceanic_submediterranean",
            Color.html("B4F033"),
            Color.html("insert_here")
        ).altName()
    private val CONTINENTAL_SUBMEDITERRANEAN = ClimateClassification(
        "CMb",
        "continental_submediterranean",
        Color.html("ACD12C"),
        Color.html("insert_here")
    ).altName()

    // Cold (C) - Semiarid
    private val OCEANIC_MEDITERRANEAN =
        ClimateClassification(
            "CAMa",
            "oceanic_mediterranean",
            Color.html("FBFF00"),
            Color.html("insert_here")
        ).altName()
    private val CONTINENTAL_MEDITERRANEAN =
        ClimateClassification(
            "CAMb",
            "continental_mediterranean",
            Color.html("A2AC1B"),
            Color.html("insert_here")
        ).altName()
    private val COOL_DRY_SAVANNA =
        ClimateClassification("CAa", "cool_dry_savanna", Color.html("D7C275"), Color.html("insert_here")).altName()
    private val COOL_DRY_MONSOON_SAVANNA =
        ClimateClassification(
            "CAap",
            "cool_dry_monsoon_savanna",
            Color.html("A18B36"),
            Color.html("insert_here")
        ).altName()
    private val COLD_STEPPE =
        ClimateClassification("CAb", "cold_steppe", Color.html("C5DB76"), Color.html("insert_here")).altName()
    private val COLD_PLUVIAL_STEPPE =
        ClimateClassification("CAbp", "cold_pluvial_steppe", Color.html("84AB54"), Color.html("insert_here")).altName()

    // Cold (C) - Marginal
    private val OCEANIC_TUNDRA =
        ClimateClassification("CFa", "oceanic_tundra", Color.html("ACCBD2"), Color.html("insert_here")).altName()
    private val CONTINENTAL_TUNDRA =
        ClimateClassification("CFb", "continental_tundra", Color.html("B4BCC0"), Color.html("insert_here")).altName()
    private val COLD_BARREN =
        ClimateClassification("CG", "cold_barren", Color.html("999999"), Color.html("insert_here")).altName()
    private val ICE = ClimateClassification("CI", "ice", Color.html("5E5E5E"), Color.html("insert_here")).altName()

    // Hot (H) - Supertropical
    private val SUPERTROPICAL_FOREST =
        ClimateClassification("HTf", "supertropical_forest", Color.html("FF6600"), Color.html("insert_here")).altName()
    private val SUPERTROPICAL_MONSOON_FOREST = ClimateClassification(
        "HTfp",
        "supertropical_monsoon_forest",
        Color.html("933B01"),
        Color.html("insert_here")
    ).altName()
    private val SUPERTROPICAL_MOIST_SAVANNA = ClimateClassification(
        "HTs",
        "supertropical_moist_savanna",
        Color.html("FD9753"),
        Color.html("insert_here")
    ).altName()
    private val SUPERTROPICAL_MOIST_MONSOON_SAVANNA = ClimateClassification(
        "HTsp",
        "supertropical_moist_monsoon_savanna",
        Color.html("C65910"),
        Color.html("insert_here")
    ).altName()

    // Hot (H) - Swelter
    private val HOT_SWELTER =
        ClimateClassification("HDa", "hot_swelter", Color.html("FF4242"), Color.html("insert_here")).altName()
    private val HOT_PLUVIAL_SWELTER =
        ClimateClassification("HDap", "hot_pluvial_swelter", Color.html("DF3030"), Color.html("insert_here")).altName()
    private val TORRID_SWELTER =
        ClimateClassification("HDb", "torrid_swelter", Color.html("FF0000"), Color.html("insert_here")).altName()
    private val TORRID_PLUVIAL_SWELTER =
        ClimateClassification(
            "HDbp",
            "torrid_pluvial_swelter",
            Color.html("C70000"),
            Color.html("insert_here")
        ).altName()
    private val BOILING_SWELTER =
        ClimateClassification("HDc", "boiling_swelter", Color.html("B52130"), Color.html("insert_here")).altName()
    private val BOILING_PLUVIAL_SWELTER =
        ClimateClassification(
            "HDcp",
            "boiling_pluvial_swelter",
            Color.html("890B1A"),
            Color.html("insert_here")
        ).altName()

    // Hot (H) - Subparamediterranean
    private val HOT_SUBPARAMEDITERRANEAN =
        ClimateClassification(
            "HMa",
            "hot_subparamediterranean",
            Color.html("FD9D1E"),
            Color.html("insert_here")
        ).altName()
    private val TORRID_SUBPARAMEDITERRANEAN = ClimateClassification(
        "HMb",
        "torrid_subparamediterranean",
        Color.html("D98912"),
        Color.html("insert_here")
    ).altName()
    private val BOILING_SUBPARAMEDITERRANEAN = ClimateClassification(
        "HMc",
        "boiling_subparamediterranean",
        Color.html("B86B00"),
        Color.html("insert_here")
    ).altName()

    // Hot (H) - Semiarid
    private val HOT_PARAMEDITERRANEAN =
        ClimateClassification(
            "HAMa",
            "hot_paramediterranean",
            Color.html("FFBB00"),
            Color.html("insert_here")
        ).altName()
    private val TORRID_PARAMEDITERRANEAN =
        ClimateClassification(
            "HAMb",
            "torrid_paramediterranean",
            Color.html("D4A011"),
            Color.html("insert_here")
        ).altName()
    private val BOILING_PARAMEDITERRANEAN =
        ClimateClassification(
            "HAMc",
            "boiling_paramediterranean",
            Color.html("B1891B"),
            Color.html("insert_here")
        ).altName()
    private val HOT_DRY_SAVANNA =
        ClimateClassification("HAa", "hot_dry_savanna", Color.html("F5C8A3"), Color.html("insert_here")).altName()
    private val HOT_DRY_MONSOON_SAVANNA =
        ClimateClassification(
            "HAap",
            "hot_dry_monsoon_savanna",
            Color.html("D1A17A"),
            Color.html("insert_here")
        ).altName()
    private val TORRID_STEPPE =
        ClimateClassification("HAb", "torrid_steppe", Color.html("E6A494"), Color.html("insert_here")).altName()
    private val TORRID_PLUVIAL_STEPPE =
        ClimateClassification(
            "HAbp",
            "torrid_pluvial_steppe",
            Color.html("CA816D"),
            Color.html("insert_here")
        ).altName()
    private val BOILING_STEPPE =
        ClimateClassification("HAc", "boiling_steppe", Color.html("D27979"), Color.html("insert_here")).altName()
    private val BOILING_PLUVIAL_STEPPE =
        ClimateClassification(
            "HAcp",
            "boiling_pluvial_steppe",
            Color.html("B25353"),
            Color.html("insert_here")
        ).altName()

    // Hot (H) - Marginal
    private val HOT_PARCH =
        ClimateClassification("HFa", "hot_parch", Color.html("9A6A6A"), Color.html("insert_here")).altName()
    private val TORRID_PARCH =
        ClimateClassification("HFb", "torrid_parch", Color.html("885959"), Color.html("insert_here")).altName()
    private val BOILING_PARCH =
        ClimateClassification("HFc", "boiling_parch", Color.html("773C3C"), Color.html("insert_here")).altName()
    private val HOT_BARREN =
        ClimateClassification("HG", "hot_barren", Color.html("471F1F"), Color.html("insert_here")).altName()

    // Extraseasonal (E) - Extratropical
    private val EXTRATROPICAL_FOREST =
        ClimateClassification("ETf", "extratropical_forest", Color.html("8000FF"), Color.html("insert_here")).altName()
    private val EXTRATROPICAL_MONSOON_FOREST = ClimateClassification(
        "ETfp",
        "extratropical_monsoon_forest",
        Color.html("6300C7"),
        Color.html("insert_here")
    ).altName()
    private val EXTRATROPICAL_MOIST_SAVANNA = ClimateClassification(
        "ETs",
        "extratropical_moist_savanna",
        Color.html("B573F7"),
        Color.html("insert_here")
    ).altName()
    private val EXTRATROPICAL_MOIST_MONSOON_SAVANNA = ClimateClassification(
        "ETsp",
        "extratropical_moist_monsoon_savanna",
        Color.html("8F5AC4"),
        Color.html("insert_here")
    ).altName()

    // Extraseasonal (E) - Extracontinental
    private val SUPERSEASONAL_EXTRACONTINENTAL = ClimateClassification(
        "EDa",
        "superseasonal_extracontinental",
        Color.html("E100FF"),
        Color.html("insert_here")
    ).altName()
    private val SUPERSEASONAL_EXTRACONTINENTAL_RAINFOREST = ClimateClassification(
        "EDap",
        "superseasonal_extracontinental_rainforest",
        Color.html("9E00B3"),
        Color.html("insert_here")
    ).altName()
    private val HYPERSEASONAL_EXTRACONTINENTAL = ClimateClassification(
        "EDb",
        "hyperseasonal_extracontinental",
        Color.html("F96CDA"),
        Color.html("insert_here")
    ).altName()
    private val HYPERSEASONAL_EXTRACONTINENTAL_RAINFOREST = ClimateClassification(
        "EDbp",
        "hyperseasonal_extracontinental_rainforest",
        Color.html("B54F9F"),
        Color.html("insert_here")
    ).altName()

    // Extraseasonal (E) - Subextramediterranean
    private val SUPERSEASONAL_SUBEXTRAMEDITERRANEAN = ClimateClassification(
        "EMa",
        "superseasonal_subextramediterranean",
        Color.html("FF1ACD"),
        Color.html("insert_here")
    ).altName()
    private val HYPERSEASONAL_SUBEXTRAMEDITERRANEAN = ClimateClassification(
        "EMb",
        "hyperseasonal_subextramediterranean",
        Color.html("D10AA6"),
        Color.html("insert_here")
    ).altName()

    // Extraseasonal (E) - Semiarid
    private val SUPERSEASONAL_EXTRAMEDITERRANEAN = ClimateClassification(
        "EAMa",
        "superseasonal_extramediterranean",
        Color.html("FF007B"),
        Color.html("insert_here")
    ).altName()
    private val HYPERSEASONAL_EXTRAMEDITERRANEAN = ClimateClassification(
        "EAMb",
        "hyperseasonal_extramediterranean",
        Color.html("B21F66"),
        Color.html("insert_here")
    ).altName()
    private val SUPERSEASONAL_DRY_SAVANNA =
        ClimateClassification(
            "EAa",
            "superseasonal_dry_savanna",
            Color.html("C18B9F"),
            Color.html("insert_here")
        ).altName()
    private val SUPERSEASONAL_DRY_MONSOON_SAVANNA = ClimateClassification(
        "EAap",
        "superseasonal_dry_monsoon_savanna",
        Color.html("A06A7D"),
        Color.html("insert_here")
    ).altName()
    private val HYPERSEASONAL_STEPPE =
        ClimateClassification("EAb", "hyperseasonal_steppe", Color.html("FFB8F8"), Color.html("insert_here")).altName()
    private val HYPERSEASONAL_PLUVIAL_STEPPE = ClimateClassification(
        "EAbp",
        "hyperseasonal_pluvial_steppe",
        Color.html("C874C1"),
        Color.html("insert_here")
    ).altName()

    // Extraseasonal (E) - Marginal
    private val SUPERSEASONAL_PULSE =
        ClimateClassification("EFa", "superseasonal_pulse", Color.html("BD94C2"), Color.html("insert_here")).altName()
    private val HYPERSEASONAL_PULSE =
        ClimateClassification("EFb", "hyperseasonal_pulse", Color.html("9D76A2"), Color.html("insert_here")).altName()
    private val EXTRASEASONAL_BARREN =
        ClimateClassification("EG", "extraseasonal_barren", Color.html("59345B"), Color.html("insert_here")).altName()

    // Ocean (O)
    private val PERMANENT_FROZEN_OCEAN =
        ClimateClassification(
            "Ofi",
            "permanent_frozen_ocean",
            Color.html("E2F8FF"),
            Color.html("insert_here")
        ).altName()
    private val SEASONAL_FROZEN_OCEAN =
        ClimateClassification("Ofd", "seasonal_frozen_ocean", Color.html("B9E3FF"), Color.html("insert_here")).altName()
    private val DARK_SEASONAL_FROZEN_OCEAN =
        ClimateClassification(
            "Ofg",
            "dark_seasonal_frozen_ocean",
            Color.html("89A9BE"),
            Color.html("insert_here")
        ).altName()
    private val DARK_OCEAN =
        ClimateClassification("Og", "dark_ocean", Color.html("4F7796"), Color.html("insert_here")).altName()
    private val COOL_OCEAN =
        ClimateClassification("Oc", "cool_ocean", Color.html("71ABD8"), Color.html("insert_here")).altName()
    private val TROPICAL_OCEAN =
        ClimateClassification("Ot", "tropical_ocean", Color.html("0978AB"), Color.html("insert_here")).altName()
    private val HOT_OCEAN =
        ClimateClassification("Oh", "hot_ocean", Color.html("064F93"), Color.html("insert_here")).altName()
    private val TORRID_OCEAN =
        ClimateClassification("Or", "torrid_ocean", Color.html("012D56"), Color.html("insert_here")).altName()
    private val EXTRASEASONAL_OCEAN =
        ClimateClassification("Oe", "extraseasonal_ocean", Color.html("5109AA"), Color.html("insert_here")).altName()

    enum class WinterType { FRIGID, COLD, COOL, MILD }
    enum class SummerType { WARM, HOT, TORRID, BOILING }
    enum class GrowthLevel { LOW, HIGH }

    // I've provided simplified alternatives to some of Hersfeldt's climate names for accessibility
    var alternateNames = false

    fun winterType(averageTemperature: Double) = when {
        averageTemperature < -30 -> WinterType.FRIGID
        averageTemperature < 0 -> WinterType.COLD
        averageTemperature < 17 -> WinterType.COOL
        else -> WinterType.MILD
    }

    fun summerType(averageTemperature: Double) = when {
        averageTemperature < 40 -> SummerType.WARM
        averageTemperature < 60 -> SummerType.HOT
        averageTemperature < 90 -> SummerType.TORRID
        else -> SummerType.BOILING
    }

    fun linearGraph(vararg points: Pair<Double, Double>): (Double) -> Double {
        val sorted = points.sortedBy { it.first }

        return ({ value ->
            when {
                value <= sorted.first().first -> sorted.first().second
                value >= sorted.last().first -> sorted.last().second
                else -> {
                    val index = sorted.indexOfFirst { it.first > value }
                    val (x1, y1) = sorted[index - 1]
                    val (x2, y2) = sorted[index]
                    lerp(y1, y2, (value - x1) / (x2 - x1))
                }
            }
        })
    }

    val gddGraph = linearGraph(5.0 to 0.0, 25.0 to 20.0, 40.0 to 20.0, 50.0 to 0.0)
    val gddzGraph = linearGraph(0.0 to 0.0, 20.0 to 20.0, 40.0 to 20.0, 60.0 to 0.0)
    val gddiGraph = linearGraph(20.0 to 0.0, 220.0 to 20.0)
    val gddizGraph = linearGraph(0.0 to 0.0, 200.0 to 20.0)
    val monthLength = 30
    val gintThreshold = 1250

    data class GddResults(
        val monthlyGdd: List<Double>,
        val monthlyGddl: List<Double>,
        val monthlyGddz: List<Double>,
        val monthlyGint: List<Double>,
        val totalGdd: Double,
        val totalGddl: Double,
        val totalGddz: Double,
        val totalGint: Double,
    )

    fun gdd(datum: ClimateDatum): GddResults {
        val monthlyGdd =
            datum.months.map { minOf(gddGraph(it.averageTemperature), gddiGraph(it.insolation)) * monthLength }
        val monthlyGddl =
            datum.months.map { gddiGraph(it.insolation) * monthLength }
        val monthlyGddz =
            datum.months.map { minOf(gddzGraph(it.averageTemperature), gddizGraph(it.insolation)) * monthLength }
        val monthlyGint = monthlyGddz.map { max(0.0, 15 * monthLength - it) }

        var totalGdd = 0.0
        var totalGddl = 0.0
        var totalGddz = 0.0
        var totalGint = 0.0

        var accumulatedGdd = 0.0
        var accumulatedGddl = 0.0
        var accumulatedGddz = 0.0
        var accumulatedGint = 0.0
        for (i in 0..<(datum.months.size * 3)) {
            accumulatedGdd += monthlyGdd[i % datum.months.size]
            accumulatedGddl += monthlyGddl[i % datum.months.size]
            accumulatedGddz += monthlyGddz[i % datum.months.size]
            accumulatedGint += monthlyGint[i % datum.months.size]

            if (monthlyGdd[i % datum.months.size] <= 0.0 && accumulatedGint >= gintThreshold) {
                accumulatedGdd = 0.0
            }

            if (monthlyGddl[i % datum.months.size] <= 0.0) {
                accumulatedGddl = 0.0
            }

            if (monthlyGddz[i % datum.months.size] <= 0.0) {
                accumulatedGddz = 0.0
            }

            if (monthlyGint[i % datum.months.size] <= 0.0) {
                accumulatedGint = 0.0
            }

            if (i >= datum.months.size * 2) {
                totalGdd = max(totalGdd, accumulatedGdd)
                totalGddl = max(totalGddl, accumulatedGddl)
                totalGddz = max(totalGddz, accumulatedGddz)
                totalGint = max(totalGint, accumulatedGint)
            }
        }

        return GddResults(monthlyGdd, monthlyGddl, monthlyGddz, monthlyGint, totalGdd, totalGddl, totalGddz, totalGint)
    }

    // Hargreaves method for calculating potential evapotranspiration
    fun hargreavesPet(
        averageTemperature: Double, // °C
        insolation: Double // W/m²
    ): Double {
        val insolationMegajoules = insolation * 0.0864
        return 0.0135 *
                (averageTemperature + 17.8) *
                insolationMegajoules *
                (238.8 / (595.5 - 0.55 * averageTemperature))
    }

    fun koppenlikePet(averageTemperature: Double) = max(0.0, 7 * averageTemperature)

    // heuristic for estimating actual evapotranspiration
    fun estimateAet(
        datum: ClimateDatum,
        pet: List<Double>
    ): List<Double> {
        var soilMoisture = 0.0
        val aet = datum.months.map { 0.0 }.toMutableList()

        while (true) {
            val startSoilMoisture = soilMoisture
            for (i in 0..<datum.months.size) {
                val precipitation = datum.months[i].precipitation
                if (precipitation > pet[i]) {
                    soilMoisture += precipitation - pet[i]
                    soilMoisture = min(soilMoisture, 500.0)
                    aet[i] = pet[i]
                } else {
                    val soilEvaporation =
                        if (soilMoisture < 250) min(soilMoisture, (pet[i] - precipitation) * soilMoisture / 250.0)
                        else pet[i] - precipitation
                    soilMoisture = max(soilMoisture - soilEvaporation, 0.0)
                    aet[i] = precipitation + soilEvaporation
                }
            }

            if ((soilMoisture - startSoilMoisture).absoluteValue <= 10.0) {
                break
            }
        }

        if (aet.zip(pet).any { it.first > it.second + 0.01 }) {
            GD.print("temperature: ${datum.months.map { it.averageTemperature.formatDigits() }}")
            GD.print("precipitation: ${datum.months.map { it.precipitation.formatDigits() }}")
            GD.print("AET: ${aet.map { it.formatDigits() }}")
            GD.print("PET: ${pet.map { it.formatDigits() }}")
            throw Error("AET is greater than PET")
        }

        return aet
    }

    fun aridityFactor(
        pet: List<Double>,
        aet: List<Double>
    ) = aet.sum() / pet.sum()

    fun growthAridityFactor(
        pet: List<Double>,
        gdd: List<Double>,
        aet: List<Double>,
    ) = aet.zip(gdd).weightedAverage() / pet.zip(gdd).weightedAverage()

    fun growthSupply(
        datum: ClimateDatum,
        gdd: List<Double>,
        aet: List<Double>,
    ) = datum.months.map { it.precipitation }.zip(gdd).weightedAverage() / aet.average()

    fun evaporationRatio(
        datum: ClimateDatum,
        aet: List<Double>
    ) = aet.sum() / datum.annualPrecipitation

    fun minIce(planet: Planet, datum: ClimateDatum): Double {
        val threshold = if (planet.planetTiles[datum.tileId]!!.isAboveWater) 0.0 else -2.0
        return if (datum.months.maxOf { it.averageTemperature } <= threshold) 1.0 else 0.0
    }

    fun maxIce(planet: Planet, datum: ClimateDatum): Double {
        val threshold = if (planet.planetTiles[datum.tileId]!!.isAboveWater) 0.0 else -2.0
        return if (datum.months.minOf { it.averageTemperature } <= threshold) 1.0 else 0.0
    }

    override fun classify(
        planet: Planet,
        datum: ClimateDatum
    ): ClimateClassification {
        // climate parameters
        val pet = datum.months.map { koppenlikePet(it.averageTemperature) }
        val aet = estimateAet(datum, pet)
        val aridityFactor = aridityFactor(pet, aet)
        val evaporationRatio = evaporationRatio(datum, aet)
        val minIce = minIce(planet, datum)
        val maxIce = maxIce(planet, datum)

        val gddResults = gdd(datum)

        val growthSupply = growthSupply(datum, gddResults.monthlyGdd, aet)
        val growthAridityFactor = growthAridityFactor(pet, gddResults.monthlyGdd, aet)

        // tuned thresholds
        val winterType = winterType(datum.months.minOf { it.averageTemperature })
        val summerType = summerType(datum.months.maxOf { it.averageTemperature })

        // classification
        val tile = planet.planetTiles[datum.tileId]!!

        // Ocean classification
        if (!tile.isAboveWater) {
            val minTemp = datum.months.minOf { it.averageTemperature }
            val maxTemp = datum.months.maxOf { it.averageTemperature }

            return when {
                // Permanent ice
                minIce >= 0.8 -> PERMANENT_FROZEN_OCEAN
                // Seasonal ice with sufficient light
                maxIce >= 0.2 && gddResults.totalGddl >= 50 -> SEASONAL_FROZEN_OCEAN
                // Dark seasonal frozen (low light)
                maxIce >= 0.2 && gddResults.totalGddl < 50 -> DARK_SEASONAL_FROZEN_OCEAN
                // Dark ocean (warm but low light)
                gddResults.totalGddl < 50 -> DARK_OCEAN
                // Temperature-based ocean types
                maxTemp >= 60 -> TORRID_OCEAN
                maxTemp >= 40 -> HOT_OCEAN
                minTemp < 18 && maxTemp >= 40 -> EXTRASEASONAL_OCEAN
                minTemp >= 18 -> TROPICAL_OCEAN
                else -> COOL_OCEAN
            }
        }

        if (minIce >= 0.8) {
            return ICE
        }

        // Arid climates (very low aridity factor)
        if (aridityFactor < 0.2) {
            // Hyperarid desert
            if (aridityFactor < 0.06) {
                return when {
                    winterType == WinterType.COLD || winterType == WinterType.FRIGID -> {
                        when (summerType) {
                            SummerType.TORRID, SummerType.BOILING -> HYPERSEASONAL_DESERT
                            SummerType.HOT, SummerType.WARM -> COLD_DESERT
                        }
                    }
                    summerType == SummerType.TORRID || summerType == SummerType.BOILING -> HOT_DESERT
                    else -> WARM_DESERT
                }
            }
            // Semidesert
            else {
                return when {
                    winterType == WinterType.COLD || winterType == WinterType.FRIGID -> {
                        when (summerType) {
                            SummerType.TORRID, SummerType.BOILING -> HYPERSEASONAL_SEMIDESERT
                            SummerType.HOT, SummerType.WARM -> COLD_SEMIDESERT
                        }
                    }
                    summerType == SummerType.TORRID || summerType == SummerType.BOILING -> HOT_SEMIDESERT
                    else -> WARM_SEMIDESERT
                }
            }
        }

        // Tropical climates (warm summers, mild winters)
        if (winterType == WinterType.MILD && summerType == SummerType.WARM) {
            // Check for marginal tropical
            if (gddResults.totalGddz < 50) return TROPICAL_BARREN
            if (gddResults.totalGdd < 350) return TROPICAL_TWILIGHT

            // Eutropical vs Quasitropical
            val isEutropical = gddResults.totalGint < gintThreshold

            return when {
                // Rainforest
                aridityFactor >= 0.9 -> {
                    if (isEutropical) {
                        if (evaporationRatio < 0.4) HYPERPLUVIAL_TROPICAL_RAINFOREST
                        else TROPICAL_RAINFOREST
                    } else {
                        if (evaporationRatio < 0.45) QUASITROPICAL_MONSOON_FOREST
                        else QUASITROPICAL_FOREST
                    }
                }
                // Forest
                aridityFactor >= 0.75 -> {
                    if (isEutropical) {
                        if (evaporationRatio < 0.45) TROPICAL_MONSOON_FOREST
                        else TROPICAL_FOREST
                    } else {
                        if (evaporationRatio < 0.45) QUASITROPICAL_MONSOON_FOREST
                        else QUASITROPICAL_FOREST
                    }
                }
                // Moist savanna
                growthAridityFactor >= 0.5 -> {
                    if (isEutropical) {
                        if (evaporationRatio < 0.45) TROPICAL_MOIST_MONSOON_SAVANNA
                        else TROPICAL_MOIST_SAVANNA
                    } else {
                        if (evaporationRatio < 0.45) QUASITROPICAL_MOIST_MONSOON_SAVANNA
                        else QUASITROPICAL_MOIST_SAVANNA
                    }
                }
                // Dry savanna
                else -> {
                    if (isEutropical) {
                        if (evaporationRatio < 0.45) TROPICAL_DRY_MONSOON_SAVANNA
                        else TROPICAL_DRY_SAVANNA
                    } else {
                        if (evaporationRatio < 0.45) QUASITROPICAL_DRY_MONSOON_SAVANNA
                        else QUASITROPICAL_DRY_SAVANNA
                    }
                }
            }
        }

        // Hot climates (hot/torrid/boiling summers, mild winters)
        if (winterType == WinterType.MILD && summerType != SummerType.WARM) {
            if (gddResults.totalGddz < 50) return HOT_BARREN
            // Check for marginal hot
            if (gddResults.totalGdd < 350) {
                return when (summerType) {
                    SummerType.HOT -> HOT_PARCH
                    SummerType.TORRID -> TORRID_PARCH
                    SummerType.BOILING -> BOILING_PARCH
                    else -> throw Error("Invalid summer type")
                }
            }

            // Semiarid hot climates
            if (growthAridityFactor < 0.5) {
                // Paramediterranean
                if (growthSupply < 0.8) {
                    return when (summerType) {
                        SummerType.HOT -> HOT_PARAMEDITERRANEAN
                        SummerType.TORRID -> TORRID_PARAMEDITERRANEAN
                        SummerType.BOILING -> BOILING_PARAMEDITERRANEAN
                        else -> throw Error("Invalid summer type")
                    }
                }

                // Dry savanna / Steppe
                return when (summerType) {
                    SummerType.HOT ->
                        if (evaporationRatio < 0.45) HOT_DRY_MONSOON_SAVANNA
                        else HOT_DRY_SAVANNA
                    SummerType.TORRID ->
                        if (evaporationRatio < 0.45) TORRID_PLUVIAL_STEPPE
                        else TORRID_STEPPE
                    SummerType.BOILING ->
                        if (evaporationRatio < 0.45) BOILING_PLUVIAL_STEPPE
                        else BOILING_STEPPE
                    else -> throw Error("Invalid summer type")
                }
            }

            // Subparamediterranean
            if (growthSupply < 0.8) {
                return when (summerType) {
                    SummerType.HOT -> HOT_SUBPARAMEDITERRANEAN
                    SummerType.TORRID -> TORRID_SUBPARAMEDITERRANEAN
                    SummerType.BOILING -> BOILING_SUBPARAMEDITERRANEAN
                    else -> throw Error("Invalid summer type")
                }
            }

            // Supertropical
            if (gddResults.totalGint >= 1250 && growthSupply >= 0.8) {
                return when {
                    summerType == SummerType.HOT && aridityFactor >= 0.75 ->
                        if (evaporationRatio < 0.45) SUPERTROPICAL_MONSOON_FOREST
                        else SUPERTROPICAL_FOREST
                    aridityFactor <= 0.75 ->
                        if (evaporationRatio < 0.45) SUPERTROPICAL_MOIST_MONSOON_SAVANNA
                        else SUPERTROPICAL_MOIST_SAVANNA
                    else -> throw Error("Invalid summer type")
                }
            }

            // Swelter
            return when (summerType) {
                SummerType.HOT ->
                    if (evaporationRatio < 0.45) HOT_PLUVIAL_SWELTER
                    else HOT_SWELTER
                SummerType.TORRID ->
                    if (evaporationRatio < 0.45) TORRID_PLUVIAL_SWELTER
                    else TORRID_SWELTER
                SummerType.BOILING ->
                    if (evaporationRatio < 0.45) BOILING_PLUVIAL_SWELTER
                    else BOILING_SWELTER
                else -> throw Error("Invalid summer type")
            }
        }

        // Extraseasonal (hot/torrid/boiling summers with cool/cold/frigid winters)
        if ((winterType == WinterType.COOL || winterType == WinterType.COLD || winterType == WinterType.FRIGID) &&
            (summerType == SummerType.HOT || summerType == SummerType.TORRID || summerType == SummerType.BOILING)
        ) {
            if (gddResults.totalGddz < 50) return EXTRASEASONAL_BARREN

            val isHyperseasonal = (summerType == SummerType.TORRID || summerType == SummerType.BOILING ||
                    winterType == WinterType.COLD || winterType == WinterType.FRIGID)

            if (gddResults.totalGdd < 350) {
                return if (isHyperseasonal) HYPERSEASONAL_PULSE else SUPERSEASONAL_PULSE
            }

            // Semiarid extraseasonal
            if (growthAridityFactor < 0.5) {
                // Extramediterranean
                if (growthSupply < 0.8) {
                    return if (isHyperseasonal) HYPERSEASONAL_EXTRAMEDITERRANEAN
                    else SUPERSEASONAL_EXTRAMEDITERRANEAN
                }

                // Dry savanna / Steppe
                return if (isHyperseasonal) {
                    if (evaporationRatio < 0.45) HYPERSEASONAL_PLUVIAL_STEPPE
                    else HYPERSEASONAL_STEPPE
                } else {
                    if (evaporationRatio < 0.45) SUPERSEASONAL_DRY_MONSOON_SAVANNA
                    else SUPERSEASONAL_DRY_SAVANNA
                }
            }

            // Subextramediterranean
            if (growthSupply < 0.8 && growthAridityFactor >= 0.5) {
                return if (isHyperseasonal) HYPERSEASONAL_SUBEXTRAMEDITERRANEAN
                else SUPERSEASONAL_SUBEXTRAMEDITERRANEAN
            }

            // Extratropical
            if (gddResults.totalGint < gintThreshold && growthAridityFactor >= 0.5 && !isHyperseasonal) {
                return if (aridityFactor >= 0.75) {
                    if (evaporationRatio < 0.45) EXTRATROPICAL_MONSOON_FOREST
                    else EXTRATROPICAL_FOREST
                } else {
                    if (evaporationRatio < 0.45) EXTRATROPICAL_MOIST_MONSOON_SAVANNA
                    else EXTRATROPICAL_MOIST_SAVANNA
                }
            }

            // Extracontinental
            if (growthAridityFactor >= 0.5) {
                return if (isHyperseasonal) {
                    if (evaporationRatio < 0.45) HYPERSEASONAL_EXTRACONTINENTAL_RAINFOREST
                    else HYPERSEASONAL_EXTRACONTINENTAL
                } else {
                    if (evaporationRatio < 0.45) SUPERSEASONAL_EXTRACONTINENTAL_RAINFOREST
                    else SUPERSEASONAL_EXTRACONTINENTAL
                }
            }
        }

        // Cold climates (cool/cold/frigid winters, warm summers)
        if ((winterType == WinterType.COOL || winterType == WinterType.COLD || winterType == WinterType.FRIGID) &&
            summerType == SummerType.WARM
        ) {
            if (gddResults.totalGddz < 50) {
                return COLD_BARREN
            }
            // Check for marginal cold
            if (gddResults.totalGdd < 350) {
                val isOceanic = winterType == WinterType.COOL
                return if (isOceanic) OCEANIC_TUNDRA else CONTINENTAL_TUNDRA
            }

            val isOceanic = winterType == WinterType.COOL

            // Cold semiarid
            if (growthAridityFactor < 0.5) {
                // Mediterranean
                if (growthSupply < 0.8) {
                    return if (isOceanic) OCEANIC_MEDITERRANEAN else CONTINENTAL_MEDITERRANEAN
                }

                // Dry savanna / Steppe
                return if (isOceanic) {
                    if (evaporationRatio < 0.45) COOL_DRY_MONSOON_SAVANNA else COOL_DRY_SAVANNA
                } else {
                    if (evaporationRatio < 0.45) COLD_PLUVIAL_STEPPE else COLD_STEPPE
                }
            }

            // Submediterranean
            if (growthSupply < 0.8) {
                return if (isOceanic) OCEANIC_SUBMEDITERRANEAN else CONTINENTAL_SUBMEDITERRANEAN
            }

            // Subtropical
            if (gddResults.totalGint < gintThreshold && isOceanic) {
                return if (aridityFactor >= 0.75) {
                    if (evaporationRatio < 0.45) SUBTROPICAL_MONSOON_FOREST
                    else SUBTROPICAL_FOREST
                } else {
                    if (evaporationRatio < 0.45) SUBTROPICAL_MOIST_MONSOON_SAVANNA
                    else SUBTROPICAL_MOIST_SAVANNA
                }
            }

            // Temperate
            if (gddResults.totalGint >= gintThreshold && gddResults.totalGdd >= 1300) {
                return if (isOceanic) {
                    if (evaporationRatio < 0.45) OCEANIC_TEMPERATE_RAINFOREST
                    else OCEANIC_TEMPERATE
                } else {
                    if (evaporationRatio < 0.45) CONTINENTAL_TEMPERATE_RAINFOREST
                    else CONTINENTAL_TEMPERATE
                }
            }

            // Boreal
            return when (winterType) {
                WinterType.COOL ->
                    if (evaporationRatio < 0.45) OCEANIC_BOREAL_RAINFOREST
                    else OCEANIC_BOREAL
                WinterType.COLD ->
                    if (evaporationRatio < 0.45) CONTINENTAL_BOREAL_RAINFOREST
                    else CONTINENTAL_BOREAL
                WinterType.FRIGID ->
                    if (evaporationRatio < 0.45) PERCONTINENTAL_BOREAL_RAINFOREST
                    else PERCONTINENTAL_BOREAL
                else -> throw Error("Invalid winter type")
            }
        }

        return UNKNOWN_CLIMATE
    }
}