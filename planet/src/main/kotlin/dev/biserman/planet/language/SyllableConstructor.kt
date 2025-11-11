package dev.biserman.planet.language

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.biserman.planet.language.Language.SyllableConstructor.symbolResults
import dev.biserman.planet.language.Language.SyllableConstructor.topLevelSymbolsFilters
import dev.biserman.planet.utils.memoize
import java.io.File
import kotlin.text.indexOf

enum class SegmentType { CONSONANT, VOWEL, AFFRICATE }
enum class Manner { NASAL, PLOSIVE, FRICATIVE, SEMIVOWEL, LIQUID, TRILL, TAP, IMPLOSIVE, CLICK }
enum class Height { CLOSE, NEAR_CLOSE, CLOSE_MID, MID, OPEN_MID, NEAR_OPEN, OPEN }
enum class Depth { FRONT, NEAR_FRONT, CENTRAL, NEAR_BACK, BACK }
data class Segment(
    val symbol: String,
    val type: SegmentType,
    val place: String?,
    val manner: Manner?,
    val voiced: Boolean?,
    val isAspirated: Boolean?,
    val isEjective: Boolean?,
    val height: Height?,
    val depth: Depth?,
    val rounded: Boolean?
)

data class PhonemeData(
    val place: String? = null,
    val manner: String? = null,
    val voiced: Boolean? = null,
    val aspirated: Boolean? = null,
    val ejective: Boolean? = null,
    val height: String? = null,
    val depth: String? = null,
    val rounded: Boolean? = null
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
            pattern[0] in topLevelSymbolsFilters -> symbolResults(pattern[0].toString())
            pattern[0].isLowerCase() -> generateSyllables(pattern.substring(1)).map { pattern[0] + it }
            else -> listOf("")
        }
    }

    object SyllableConstructor {
        val languageSettings: LanguageSettingsJson by lazy {
            val mapper = jacksonObjectMapper()
            mapper.readValue(File("english.json"))
        }

        val segments: List<Segment> by lazy {
            val mapper = jacksonObjectMapper()
            val phonemeJson: PhonemeJson = mapper.readValue(File("phonemes.json"))

            val consonants = phonemeJson.consonants.map { (symbol, data) ->
                Segment(
                    symbol = symbol,
                    type = SegmentType.CONSONANT,
                    place = data.place,
                    manner = data.manner?.uppercase()?.let { Manner.valueOf(it) },
                    voiced = data.voiced,
                    isAspirated = data.aspirated,
                    isEjective = data.ejective,
                    height = null,
                    depth = null,
                    rounded = null
                )
            }.filter { it.symbol in languageSettings.complexity_tiers[0].allowedConsonants }

            val vowels = phonemeJson.vowels.map { (symbol, data) ->
                Segment(
                    symbol = symbol,
                    type = SegmentType.VOWEL,
                    place = null,
                    manner = null,
                    voiced = null,
                    isAspirated = null,
                    isEjective = null,
                    height = data.height?.replace("-", "_")?.uppercase()?.let { Height.valueOf(it) },
                    depth = data.depth?.replace("-", "_")?.uppercase()?.let { Depth.valueOf(it) },
                    rounded = data.rounded
                )
            }.filter { it.symbol in languageSettings.complexity_tiers[0].allowedVowels }

            consonants + vowels
        }

        val topLevelSymbolsFilters = mapOf<Char, (Segment) -> Boolean>(
            'C' to { it.type == SegmentType.CONSONANT },
            'V' to { it.type == SegmentType.VOWEL },
            'A' to { it.type == SegmentType.AFFRICATE },
            'N' to { it.manner == Manner.NASAL },
            'P' to { it.manner == Manner.PLOSIVE },
            'F' to { it.manner == Manner.FRICATIVE },
            'W' to { it.manner == Manner.SEMIVOWEL },
            'L' to { it.manner == Manner.LIQUID },
            'T' to { it.manner == Manner.TRILL },
            'X' to { it.manner == Manner.TAP },
            'I' to { it.manner == Manner.IMPLOSIVE },
            'Q' to { it.manner == Manner.CLICK },
        )

        val clarifiers = mapOf<String, (Segment) -> Boolean>(
            "V" to { it.voiced == true },
            "NV" to { it.voiced == false },
            "A" to { it.isAspirated == true },
            "NA" to { it.isAspirated == false },
            "E" to { it.isEjective == true },
            "NE" to { it.isEjective == false },
            "CL" to { it.height == Height.CLOSE },
            "NC" to { it.height == Height.NEAR_CLOSE },
            "CM" to { it.height == Height.CLOSE_MID },
            "MI" to { it.height == Height.MID },
            "OM" to { it.height == Height.OPEN_MID },
            "NO" to { it.height == Height.NEAR_OPEN },
            "OP" to { it.height == Height.OPEN },
            "FR" to { it.depth == Depth.FRONT },
            "NF" to { it.depth == Depth.NEAR_FRONT },
            "CE" to { it.depth == Depth.CENTRAL },
            "NB" to { it.depth == Depth.NEAR_BACK },
            "BA" to { it.depth == Depth.BACK },
            "R" to { it.rounded == true },
            "NR" to { it.rounded == false })

        val symbolResults: (String) -> List<String> = memoize { symbol ->
            segments.filter { topLevelSymbolsFilters[symbol[0]]!!(it) }.map { it.symbol }
        }
    }
}