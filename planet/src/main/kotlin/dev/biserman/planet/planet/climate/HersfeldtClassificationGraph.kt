package dev.biserman.planet.planet.climate

/**
 * Adjacency graph for the leaf classifications in [Hersfeldt].
 *
 * An edge means that the two leaves can be separated by one classifier decision:
 * an aridity/growth threshold, a temperature regime, a growing-degree threshold,
 * continentality, or the monsoon/pluvial evaporation-ratio branch. The graph is
 * deliberately about classifier semantics rather than the display palette.
 */
object HersfeldtClassificationGraph {
    const val MAX_SCORED_DISTANCE = 5

    data class AdjacencyMatrix(
        val ids: List<String>,
        val rows: List<List<Boolean>>,
    )

    val classificationIds: Set<String> = setOf(
        "Aha", "Ahc", "Ahh", "Ahe", "Ada", "Adc", "Adh", "Ade",
        "TUr", "TUrp", "TUf", "TUfp", "TUs", "TUsp", "TUA", "TUAp",
        "TQf", "TQfp", "TQs", "TQsp", "TQA", "TQAp", "TF", "TG",
        "CTf", "CTfp", "CTs", "CTsp",
        "CDa", "CDap", "CDb", "CDbp",
        "CEa", "CEap", "CEb", "CEbp", "CEc", "CEcp",
        "CMa", "CMb", "CAMa", "CAMb", "CAa", "CAap", "CAb", "CAbp",
        "CFa", "CFb", "CG", "CI",
        "HTf", "HTfp", "HTs", "HTsp",
        "HDa", "HDap", "HDb", "HDbp", "HDc", "HDcp",
        "HMa", "HMb", "HMc", "HAMa", "HAMb", "HAMc",
        "HAa", "HAap", "HAb", "HAbp", "HAc", "HAcp",
        "HFa", "HFb", "HFc", "HG",
        "ETf", "ETfp", "ETs", "ETsp",
        "EDa", "EDap", "EDb", "EDbp",
        "EMa", "EMb", "EAMa", "EAMb",
        "EAa", "EAap", "EAb", "EAbp", "EFa", "EFb", "EG",
        "Ofi", "Ofd", "Ofg", "Og", "Oc", "Ot", "Oh", "Or", "Oe",
    )

    val adjacency: Map<String, Set<String>> = GraphBuilder(classificationIds).apply {
        // The evaporation-ratio branch is the final decision for every *p class.
        classificationIds.filter { it.endsWith("p") }.forEach { seasonal ->
            val ordinary = seasonal.dropLast(1)
            if (ordinary in classificationIds) edge(ordinary, seasonal)
        }

        // Arid: the 0.06 aridity boundary and the temperature-regime branches.
        listOf("a", "c", "h", "e").forEach { suffix -> edge("Ah$suffix", "Ad$suffix") }
        listOf("Ah", "Ad").forEach { prefix ->
            edge("${prefix}a", "${prefix}c")
            edge("${prefix}a", "${prefix}h")
            edge("${prefix}c", "${prefix}e")
            edge("${prefix}h", "${prefix}e")
        }

        // Tropical: GDD marginality, moisture tier, eutropical/quasitropical,
        // and evaporation-ratio variants.
        edge("TG", "TF")
        chain("TUA", "TUs", "TUf", "TUr")
        chain("TUAp", "TUsp", "TUfp", "TUrp")
        chain("TQA", "TQs", "TQf")
        chain("TQAp", "TQsp", "TQfp")
        listOf("A", "s", "f").forEach { tier ->
            edge("TU$tier", "TQ$tier")
            edge("TU${tier}p", "TQ${tier}p")
        }
        connect("TF", "TUA", "TUAp", "TUs", "TUsp", "TUf", "TUfp", "TUr", "TUrp")
        connect("TF", "TQA", "TQAp", "TQs", "TQsp", "TQf", "TQfp")

        // Hot: summer heat, marginal GDD, growth aridity, and growth supply.
        listOf("HF", "HD", "HM", "HAM", "HA").forEach { prefix ->
            chain("${prefix}a", "${prefix}b", "${prefix}c")
        }
        chain("HDap", "HDbp", "HDcp")
        chain("HAap", "HAbp", "HAcp")
        connect("HG", "HFa", "HFb", "HFc")
        listOf("a", "b", "c").forEach { heat ->
            val parch = "HF$heat"
            val paramediterranean = "HAM$heat"
            val submediterranean = "HM$heat"
            val dry = "HA$heat"
            val swelter = "HD$heat"
            connect(parch, paramediterranean, submediterranean, dry, swelter)
            edge(paramediterranean, dry) // growth supply
            edge(paramediterranean, submediterranean) // growth aridity
            connect(dry, swelter, "HTs")
            connect(submediterranean, swelter, "HTs")
        }
        edge("HTs", "HTf")
        edge("HTsp", "HTfp")
        connect("HAap", "HDap", "HTsp")
        connect("HAbp", "HDbp", "HTsp")
        connect("HAcp", "HDcp", "HTsp")

        // Extraseasonal: super/hyperseasonality and the same growth decisions.
        listOf("EF", "ED", "EM", "EAM", "EA").forEach { prefix ->
            edge("${prefix}a", "${prefix}b")
        }
        edge("EDap", "EDbp")
        edge("EAap", "EAbp")
        connect("EG", "EFa", "EFb")
        connect("EFa", "EAMa", "EMa", "EAa", "EDa", "ETs", "ETf")
        connect("EFb", "EAMb", "EMb", "EAb", "EDb")
        edge("EAMa", "EAa")
        edge("EAMa", "EMa")
        connect("EAa", "EDa", "ETs")
        connect("EMa", "EDa", "ETs")
        edge("ETs", "ETf")
        edge("ETsp", "ETfp")
        edge("EAMb", "EAb")
        edge("EAMb", "EMb")
        edge("EAb", "EDb")
        edge("EMb", "EDb")
        connect("EAap", "EDap", "ETsp")
        edge("EAbp", "EDbp")

        // Cold: ice/GDD marginality, winter continentality, growth decisions,
        // and the subtropical -> temperate -> boreal GDD/GINT sequence.
        edge("CI", "CG")
        connect("CG", "CFa", "CFb")
        edge("CFa", "CFb")
        listOf("CM", "CAM", "CA", "CD", "CF").forEach { prefix ->
            edge("${prefix}a", "${prefix}b")
        }
        chain("CEa", "CEb", "CEc")
        chain("CEap", "CEbp", "CEcp")
        connect("CFa", "CAMa", "CMa", "CAa", "CAap", "CTs", "CTsp", "CTf", "CTfp", "CDa", "CDap", "CEa", "CEap")
        connect("CFb", "CAMb", "CMb", "CAb", "CAbp", "CDb", "CDbp", "CEb", "CEbp", "CEc", "CEcp")
        edge("CAMa", "CAa")
        edge("CAMb", "CAb")
        edge("CAMa", "CMa")
        edge("CAMb", "CMb")
        connect("CAa", "CTs", "CDa", "CEa")
        connect("CAb", "CDb", "CEb", "CEc")
        connect("CMa", "CTs", "CDa", "CEa")
        connect("CMb", "CDb", "CEb", "CEc")
        edge("CTs", "CTf")
        edge("CTsp", "CTfp")
        connect("CDa", "CTs", "CTf", "CEa")
        connect("CDap", "CTsp", "CTfp", "CEap")
        edge("CDb", "CEb")
        edge("CDbp", "CEbp")

        // Crossing the global aridity < 0.2 decision into the appropriate
        // non-arid thermal branch.
        connect("Ada", "TUA", "TUAp", "TQA", "TQAp", "CAMa", "CAa", "CAap")
        connect("Aha", "TUA", "TUAp", "TQA", "TQAp", "CAMa", "CAa", "CAap")
        connect("Adc", "CAMb", "CAb", "CAbp", "EAMa", "EAMb", "EAa", "EAap", "EAb", "EAbp")
        connect("Ahc", "CAMb", "CAb", "CAbp", "EAMa", "EAMb", "EAa", "EAap", "EAb", "EAbp")
        connect("Adh", "HAMb", "HAMc", "HAb", "HAbp", "HAc", "HAcp")
        connect("Ahh", "HAMb", "HAMc", "HAb", "HAbp", "HAc", "HAcp")
        connect("Ade", "EAMb", "EAb", "EAbp")
        connect("Ahe", "EAMb", "EAb", "EAbp")

        // Temperature-regime transitions between the major land branches.
        connect("TUr", "HTf", "HDa")
        connect("TUf", "HTf", "HDa")
        connect("TUs", "HTs", "HDa")
        connect("TUA", "HAa", "HAMa")
        connect("TQf", "HDa")
        connect("TQs", "HDa")
        connect("TQA", "HAa", "HAMa")
        connect("TUrp", "HTfp", "HDap")
        connect("TUfp", "HTfp", "HDap")
        connect("TUsp", "HTsp", "HDap")
        connect("TUAp", "HAap")
        connect("TUr", "CTf", "CDa")
        connect("TUf", "CTf", "CDa")
        connect("TUs", "CTs")
        connect("TUA", "CAMa", "CAa")
        connect("TQf", "CDa")
        connect("TQs", "CDa")
        connect("TQA", "CAMa", "CAa")

        listOf("a", "b").forEach { seasonality ->
            val hot = if (seasonality == "a") "a" else "b"
            edge("HAM$hot", "EAM$seasonality")
            edge("HM$hot", "EM$seasonality")
            edge("HA$hot", "EA$seasonality")
            edge("HD$hot", "ED$seasonality")
        }
        connect("HTf", "ETf", "EDa")
        connect("HTs", "ETs", "EDa")
        edge("EAMa", "CAMa")
        edge("EAMb", "CAMb")
        edge("EMa", "CMa")
        edge("EMb", "CMb")
        edge("EAa", "CAa")
        edge("EAb", "CAb")
        connect("ETf", "CTf", "CDa", "CEa")
        connect("ETs", "CTs", "CDa", "CEa")
        connect("EDa", "CDa", "CEa")
        connect("EDb", "CDb", "CEb", "CEc")
        connect("EFa", "CFa")
        connect("EFb", "CFb")
        edge("EG", "CG")

        // Ocean classification is a separate classifier branch. These edges
        // follow its ice/light and min/max-temperature conditions.
        connect("Ofi", "Ofd", "Ofg")
        edge("Ofd", "Ofg")
        edge("Ofg", "Og")
        connect("Ofd", "Oc", "Ot", "Oh", "Or", "Oe")
        connect("Og", "Oc", "Ot", "Oh", "Or", "Oe")
        edge("Oc", "Ot")
        edge("Oc", "Oe")
        connect("Oe", "Ot", "Oh", "Or")
        edge("Ot", "Oh")
        edge("Oh", "Or")

        // Natural land/sea ice boundary keeps the complete matrix connected
        // without making arbitrary land and ocean climates adjacent.
        edge("CI", "Ofi")
    }.build()

    val adjacencyMatrix: AdjacencyMatrix by lazy {
        val ids = classificationIds.sorted()
        AdjacencyMatrix(
            ids = ids,
            rows = ids.map { from -> ids.map { to -> to in adjacency.getValue(from) } },
        )
    }

    fun areAdjacent(firstId: String, secondId: String): Boolean =
        secondId in adjacency[firstId].orEmpty()

    fun distance(firstId: String, secondId: String): Int {
        if (firstId == secondId) return 0
        if (firstId !in adjacency || secondId !in adjacency) return MAX_SCORED_DISTANCE

        val visited = mutableSetOf(firstId)
        var frontier = setOf(firstId)
        var distance = 0
        while (frontier.isNotEmpty() && distance < MAX_SCORED_DISTANCE) {
            distance++
            val next = frontier.flatMapTo(mutableSetOf()) { adjacency.getValue(it) } - visited
            if (secondId in next) return distance
            visited += next
            frontier = next
        }
        return MAX_SCORED_DISTANCE
    }

    fun validate(classificationIds: Collection<String>) {
        val discovered = classificationIds.toSet()
        require(discovered == this.classificationIds) {
            "Hersfeldt classification graph mismatch: missing=${this.classificationIds - discovered}, " +
                "stale=${discovered - this.classificationIds}"
        }
        require(adjacency.all { (_, neighbors) -> neighbors.isNotEmpty() }) {
            "Every Hersfeldt classification must have at least one graph neighbor"
        }
    }

    private class GraphBuilder(ids: Set<String>) {
        private val edges = ids.associateWith { mutableSetOf<String>() }

        fun edge(first: String, second: String) {
            require(first in edges) { "Unknown Hersfeldt classification: $first" }
            require(second in edges) { "Unknown Hersfeldt classification: $second" }
            require(first != second)
            edges.getValue(first) += second
            edges.getValue(second) += first
        }

        fun chain(vararg ids: String) {
            for (index in 0 until ids.lastIndex) {
                edge(ids[index], ids[index + 1])
            }
        }

        fun connect(first: String, vararg others: String) {
            others.forEach { edge(first, it) }
        }

        fun build(): Map<String, Set<String>> = edges.mapValues { (_, neighbors) -> neighbors.toSortedSet() }
    }
}
