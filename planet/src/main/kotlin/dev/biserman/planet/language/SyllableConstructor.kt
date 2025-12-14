package dev.biserman.planet.language

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.biserman.planet.utils.memoize
import java.io.File
import kotlin.text.indexOf

typealias InventoryTransformation = (Set<Segment>) -> Set<Segment>

enum class SegmentType { CONSONANT, VOWEL }
enum class Manner { SEMIVOWEL, GLIDE, LIQUID, NASAL, FRICATIVE, TAP, TRILL, PLOSIVE, IMPLOSIVE, CLICK }
enum class Height { CLOSE, NEAR_CLOSE, CLOSE_MID, MID, OPEN_MID, NEAR_OPEN, OPEN }
enum class Depth { FRONT, NEAR_FRONT, CENTRAL, NEAR_BACK, BACK }
enum class Place { BILABIAL, DENTAL, LABIODENTAL, ALVEOLAR, POSTALVEOLAR, PALATAL, LABIOVELAR, VELAR, UVULAR, GLOTTAL, RETROFLEX }

data class Glide(
    val place: Place,
    val manner: Manner
) {
    fun display(voiced: Boolean?) =
        SyllableConstructor.glideMap[this to voiced] ?: SyllableConstructor.glideMap[this to null]

    companion object {
        fun from(data: SegmentData): Glide = Glide(data.place, data.manner!!)
    }
}

data class SegmentData(
    val type: SegmentType,
    val place: Place?,
    val manner: Manner?,
    val voiced: Boolean?,
    val isAspirated: Boolean?,
    val isEjective: Boolean?,
    val height: Height?,
    val depth: Depth?,
    val rounded: Boolean?,
    val consonantGlide: Glide?,
    val onGlide: Glide?,
    val offGlide: Glide?,
) {
    fun strip() = this.copy(consonantGlide = null, onGlide = null, offGlide = null)
}

data class Segment(
    val symbol: String,
    val data: SegmentData,
    val prevalence: Double
) {
    fun copyData(transform: (SegmentData) -> SegmentData) =
        transform(data).let { transformed ->
            val found = SyllableConstructor.segments.values.find {
                it.data == transformed.strip()
            }!!
            Segment(found.symbol, transformed, found.prevalence)
        }

    val display by lazy {
        val onGlide = data.onGlide?.display(data.voiced) ?: ""
        val affricateGlide = data.consonantGlide?.display(data.voiced) ?: ""
        val offGlide = data.offGlide?.display(data.voiced) ?: ""
        "$onGlide$symbol$affricateGlide$offGlide"
    }
}

data class PhonemeData(
    val place: String? = null,
    val manner: String? = null,
    val voiced: Boolean? = null,
    val aspirated: Boolean? = null,
    val ejective: Boolean? = null,
    val height: String? = null,
    val depth: String? = null,
    val rounded: Boolean? = null,
    val prevalence: Double? = null
)

data class PhonemeJson(
    val consonants: Map<String, PhonemeData>,
    val vowels: Map<String, PhonemeData>
)

data class RomanizationData(
    val consonants: Map<String, String>
)

data class ComplexityTier(
    val allowedConsonants: String,
    val allowedVowels: String,
    val affricates: String,
    val rewrite_rules: Map<String, String>,
    val forbidden_cluster_rules: List<String>,
    val syllables: Map<String, Int>
)

data class LanguageSettingsJson(
    val romanization: RomanizationData,
    val complexity_tiers: List<ComplexityTier>
)

class Language(
    val availableConsonants: Set<Segment>,
    val availableVowels: Set<Segment>,
    val syllablePatterns: List<String>
) {
    val generateSyllables: (String) -> List<String> = memoize { pattern: String ->
        return@memoize when {
            pattern.isEmpty() -> listOf("")
            pattern[0] == '(' -> {
                val optional = pattern.substring(1, pattern.indexOf(')'))
                generateSyllables(optional).plus("").flatMap { option ->
                    generateSyllables(
                        pattern.substring(pattern.indexOf(')') + 1)
                    ).map { option + it }
                }
            }
            pattern[0] == '[' -> {
                val options = pattern.substring(1, pattern.indexOf(']'))
                options.flatMap { option ->
                    generateSyllables(
                        pattern.substring(
                            pattern.indexOf(']') + 1
                        )
                    ).map { option + it }
                }
            }
            pattern[0] in SyllableConstructor.topLevelSymbolsFilters -> SyllableConstructor.symbolResults(pattern[0].toString())
            pattern[0].isLowerCase() -> generateSyllables(pattern.substring(1)).map { pattern[0] + it }
            else -> listOf("")
        }
    }
}

object SyllableConstructor {
    var languageFile = "english.json"
    var phonemeFile = "phonemes.json"

    val languageSettings: LanguageSettingsJson by lazy {
        val mapper = jacksonObjectMapper()
        mapper.readValue(File(languageFile))
    }

    val segments: Map<String, Segment> by lazy {
        val mapper = jacksonObjectMapper()
        val phonemeJson: PhonemeJson = mapper.readValue(File(phonemeFile))

        val consonants = phonemeJson.consonants.map { (symbol, data) ->
            Segment(
                symbol = symbol,
                SegmentData(
                    type = SegmentType.CONSONANT,
                    place = data.place?.uppercase()?.let { Place.valueOf(it) },
                    manner = data.manner?.uppercase()?.let { Manner.valueOf(it) },
                    voiced = data.voiced,
                    isAspirated = data.aspirated ?: false,
                    isEjective = data.ejective ?: false,
                    height = null,
                    depth = null,
                    rounded = null,
                    consonantGlide = null,
                    onGlide = null,
                    offGlide = null
                ),
                prevalence = data.prevalence ?: 0.0
            )
        }.filter { it.symbol in languageSettings.complexity_tiers[0].allowedConsonants }

        val vowels = phonemeJson.vowels.map { (symbol, data) ->
            Segment(
                symbol = symbol,
                SegmentData(
                    type = SegmentType.VOWEL,
                    place = null,
                    manner = null,
                    voiced = null,
                    isAspirated = null,
                    isEjective = null,
                    height = data.height?.replace("-", "_")?.uppercase()?.let { Height.valueOf(it) },
                    depth = data.depth?.replace("-", "_")?.uppercase()?.let { Depth.valueOf(it) },
                    rounded = data.rounded,
                    consonantGlide = null,
                    onGlide = null,
                    offGlide = null,
                ),
                prevalence = data.prevalence ?: 0.0
            )
        }.filter { it.symbol in languageSettings.complexity_tiers[0].allowedVowels }

        (consonants + vowels).associateBy { it.symbol }
    }

    val glideMap: Map<Pair<Glide, Boolean?>, String> by lazy {
        segments
            .filterValues { it.data.manner == Manner.FRICATIVE || it.data.manner == Manner.SEMIVOWEL || it.data.manner == Manner.LIQUID }
            .filterValues { it.data.place != Place.GLOTTAL }
            .entries
            .associate { Pair(Glide.from(it.value.data), it.value.data.voiced) to it.key }
    }

    val topLevelSymbolsFilters = mapOf<Char, (Segment) -> Boolean>(
        'C' to { it.data.type == SegmentType.CONSONANT },
        'V' to { it.data.type == SegmentType.VOWEL },
        'N' to { it.data.manner == Manner.NASAL },
        'P' to { it.data.manner == Manner.PLOSIVE },
        'F' to { it.data.manner == Manner.FRICATIVE },
        'W' to { it.data.manner == Manner.SEMIVOWEL },
        'L' to { it.data.manner == Manner.LIQUID },
        'T' to { it.data.manner == Manner.TRILL },
        'X' to { it.data.manner == Manner.TAP },
        'I' to { it.data.manner == Manner.IMPLOSIVE },
        'Q' to { it.data.manner == Manner.CLICK },
    )

    val clarifiers = mapOf<String, (Segment) -> Boolean>(
        "V" to { it.data.voiced == true },
        "NV" to { it.data.voiced == false },
        "A" to { it.data.isAspirated == true },
        "NA" to { it.data.isAspirated == false },
        "E" to { it.data.isEjective == true },
        "NE" to { it.data.isEjective == false },
        "CL" to { it.data.height == Height.CLOSE },
        "NC" to { it.data.height == Height.NEAR_CLOSE },
        "CM" to { it.data.height == Height.CLOSE_MID },
        "MI" to { it.data.height == Height.MID },
        "OM" to { it.data.height == Height.OPEN_MID },
        "NO" to { it.data.height == Height.NEAR_OPEN },
        "OP" to { it.data.height == Height.OPEN },
        "FR" to { it.data.depth == Depth.FRONT },
        "NF" to { it.data.depth == Depth.NEAR_FRONT },
        "CE" to { it.data.depth == Depth.CENTRAL },
        "NB" to { it.data.depth == Depth.NEAR_BACK },
        "BA" to { it.data.depth == Depth.BACK },
        "R" to { it.data.rounded == true },
        "NR" to { it.data.rounded == false })

    val symbolResults: (String) -> List<String> = memoize { symbol ->
        segments.values.filter { topLevelSymbolsFilters[symbol[0]]!!(it) }.map { it.symbol }
    }
}