package dev.biserman.planet.language

enum class SegmentType { CONSONANT, VOWEL, AFFRICATE }
enum class Manner { NASAL, PLOSIVE, FRICATIVE, SEMIVOWEL, LIQUID, TRILL, TAP, IMPLOSIVE, CLICK }
enum class Height { CLOSE, NEAR_CLOSE, CLOSE_MID, MID, OPEN_MID, NEAR_OPEN, OPEN }
enum class Depth { FRONT, NEAR_FRONT, CENTRAL, NEAR_BACK, BACK }

data class ConsonantSegment(
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

object SyllableConstructor {
    val topLevelSymbols = mapOf<String, (ConsonantSegment) -> Boolean>(
        "C" to { it.type == SegmentType.CONSONANT },
        "V" to { it.type == SegmentType.VOWEL },
        "A" to { it.type == SegmentType.AFFRICATE },

        "N" to { it.manner == Manner.NASAL },
        "P" to { it.manner == Manner.PLOSIVE },
        "F" to { it.manner == Manner.FRICATIVE },
        "W" to { it.manner == Manner.SEMIVOWEL},
        "L" to { it.manner == Manner.LIQUID },
        "T" to { it.manner == Manner.TRILL },

        "X" to { it.manner == Manner.TAP },
        "I" to { it.manner == Manner.IMPLOSIVE },
        "Q" to { it.manner == Manner.CLICK },
    )

    val clarifiers = mapOf<String, (ConsonantSegment) -> Boolean>(
        "V" to { it.voiced == true },
        "A" to { it.isAspirated == true },
        "E" to { it.isEjective == true },

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

        "R" to { it.rounded == true }
    )

    // () for optional, [] for required that needs clarifiers or for match groups, {} for optional match groups, all uppercase are top level symbols or clarifiers, all lowercase are literals. . for positive clarifiers, ^ for negative
}