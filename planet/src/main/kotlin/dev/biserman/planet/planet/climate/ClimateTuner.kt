package dev.biserman.planet.planet.climate

import com.fasterxml.jackson.databind.node.ObjectNode
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.utils.Serialization
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant

private data class ClimateTunerOptions(
    val planetFile: File,
    val referenceFile: File,
    val configFile: File,
    val tuningSpaceFile: File,
    val outputFile: File,
    val reportFile: File,
    val maxEvaluations: Int,
    val selectedParameters: Set<String>?,
    val interactionPairs: List<Pair<String, String>>,
    val apply: Boolean,
    val resume: Boolean,
)

private data class ClimateTunerEvaluationRecord(
    val index: Int,
    val changedParameter: String?,
    val values: Map<String, Double>,
    val loss: Double,
    val score: HersfeldtReference.Score?,
    val deltaFromBaseline: HersfeldtReference.ScoreDelta?,
    val error: String?,
)

private data class ClimateTunerArtifacts(
    val baselineSimulatedMap: String? = null,
    val baselineDifferenceMap: String? = null,
    val bestSimulatedMap: String? = null,
    val bestDifferenceMap: String? = null,
)

private data class ClimateTunerReport(
    val startedAt: String,
    val finishedAt: String?,
    val planetFile: String,
    val referenceFile: String,
    val inputConfigFile: String,
    val bestConfigFile: String,
    val maxEvaluations: Int,
    val selectedParameters: List<String>,
    val interactionPairs: List<String>,
    val initialLoss: Double?,
    val bestLoss: Double?,
    val improved: Boolean?,
    val appliedToInputConfig: Boolean,
    val artifacts: ClimateTunerArtifacts,
    val evaluations: List<ClimateTunerEvaluationRecord>,
)

private data class EvaluationOutcome(
    val loss: Double,
    val score: HersfeldtReference.Score?,
    val planet: Planet?,
    val error: String?,
)

fun main(args: Array<String>) = runClimateTuner(args)

fun runClimateTuner(args: Array<String>) {
    if (args.any { it == "--help" || it == "-h" }) {
        printClimateTunerHelp()
        return
    }

    val options = parseClimateTunerOptions(args)
    require(options.maxEvaluations > 0) { "--max-evaluations must be positive" }
    listOf(
        options.planetFile,
        options.referenceFile,
        options.configFile,
        options.tuningSpaceFile,
    ).forEach { require(it.isFile) { "File not found: ${it.absolutePath}" } }

    val mapper = Serialization.configMapper
    val startingConfigFile = if (options.resume && options.outputFile.isFile) {
        options.outputFile
    } else {
        options.configFile
    }
    val baseConfig = mapper.readTree(startingConfigFile) as? ObjectNode
        ?: error("Climate config must be a JSON object: $startingConfigFile")
    val tuningSpace = mapper.readValue(options.tuningSpaceFile, ClimateTuningSpace::class.java)
    val parameters = tuningSpace.parameters.filter { parameter ->
        options.selectedParameters == null || parameter.name in options.selectedParameters
    }
    require(parameters.isNotEmpty()) { "No tuning parameters were selected" }
    options.selectedParameters?.let { selected ->
        val missing = selected - parameters.map { it.name }.toSet()
        require(missing.isEmpty()) { "Unknown tuning parameters: ${missing.sorted().joinToString()}" }
    }

    val initialValues = parameters.associate { parameter ->
        val node = baseConfig.get(parameter.name)
            ?: error("${parameter.name} is missing from ${options.configFile}")
        require(node.isNumber) { "${parameter.name} must be numeric" }
        parameter.name to node.doubleValue()
    }

    options.outputFile.parentFile?.mkdirs()
    options.reportFile.parentFile?.mkdirs()
    val startedAt = Instant.now().toString()
    val records = mutableListOf<ClimateTunerEvaluationRecord>()
    var pendingOutcome: EvaluationOutcome? = null
    var bestLoss = Double.POSITIVE_INFINITY
    var bestConfig = baseConfig.deepCopy()
    var baselineScore: HersfeldtReference.Score? = null
    var bestScore: HersfeldtReference.Score? = null
    var bestPlanet: Planet? = null
    var artifacts = ClimateTunerArtifacts()
    val artifactsDirectory = File(options.reportFile.parentFile ?: File("."), "artifacts")

    fun candidateConfig(values: Map<String, Double>) = baseConfig.deepCopy().also { config ->
        values.forEach { (name, value) -> config.put(name, value) }
    }

    fun writeReport(finishedAt: String? = null, initialLoss: Double? = null, applied: Boolean = false) {
        mapper.writeValue(
            options.reportFile,
            ClimateTunerReport(
                startedAt = startedAt,
                finishedAt = finishedAt,
                planetFile = options.planetFile.absolutePath,
                referenceFile = options.referenceFile.absolutePath,
                inputConfigFile = options.configFile.absolutePath,
                bestConfigFile = options.outputFile.absolutePath,
                maxEvaluations = options.maxEvaluations,
                selectedParameters = parameters.map { it.name },
                interactionPairs = options.interactionPairs.map { "${it.first}+${it.second}" },
                initialLoss = initialLoss,
                bestLoss = bestLoss.takeIf { it.isFinite() },
                improved = initialLoss?.let { bestLoss < it - IMPROVEMENT_EPSILON },
                appliedToInputConfig = applied,
                artifacts = artifacts,
                evaluations = records,
            ),
        )
    }

    println("Climate tuner")
    println("  planet: ${options.planetFile}")
    println("  reference: ${options.referenceFile}")
    println("  starting config: $startingConfigFile")
    println("  parameters: ${parameters.joinToString { it.name }}")
    if (options.interactionPairs.isNotEmpty()) {
        println("  interactions: ${options.interactionPairs.joinToString { "${it.first}+${it.second}" }}")
    }
    println("  evaluation budget: ${options.maxEvaluations}")

    val search = ClimateTuningSearch(
        parameters = parameters,
        initialValues = initialValues,
        maxEvaluations = options.maxEvaluations,
        interactionPairs = options.interactionPairs,
        evaluate = { values ->
            val config = candidateConfig(values)
            pendingOutcome = try {
                applyClimateConfig(config)
                val planet = Serialization.load(options.planetFile.absolutePath)
                planet.climateMap = ClimateSimulation.calculateClimate(planet).mapKeys { it.key.tileId }
                val score = HersfeldtReference.score(planet, options.referenceFile.absolutePath)
                    ?: error("Reference scoring produced no land samples")
                EvaluationOutcome(score.loss, score, planet, null)
            } catch (error: Throwable) {
                EvaluationOutcome(
                    loss = FAILED_EVALUATION_LOSS,
                    score = null,
                    planet = null,
                    error = "${error::class.simpleName}: ${error.message}",
                )
            }
            pendingOutcome!!.loss
        },
        afterEvaluation = { evaluation ->
            val outcome = checkNotNull(pendingOutcome)
            val score = outcome.score
            if (evaluation.index == 1) {
                baselineScore = score
                val rendered = outcome.planet?.let { planet ->
                    runCatching {
                        HersfeldtReference.renderMaps(
                            planet,
                            options.referenceFile.absolutePath,
                            artifactsDirectory,
                            "baseline",
                        )
                    }.onFailure { println("Warning: baseline maps were not rendered: ${it.message}") }.getOrNull()
                }
                if (rendered != null) {
                    artifacts = artifacts.copy(
                        baselineSimulatedMap = rendered.simulatedMap,
                        baselineDifferenceMap = rendered.differenceMap,
                    )
                }
            }
            records += ClimateTunerEvaluationRecord(
                index = evaluation.index,
                changedParameter = evaluation.changedParameter,
                values = evaluation.values,
                loss = evaluation.loss,
                score = score,
                deltaFromBaseline = score?.let { current -> baselineScore?.let(current::deltaFrom) },
                error = outcome.error,
            )

            val status = score?.let {
                "loss %.5f, match %.2f%%".format(it.loss, it.matchPercent)
            } ?: "FAILED: ${outcome.error}"
            val change = evaluation.changedParameter?.let { changed ->
                changed.split('+').joinToString(prefix = ", ") { name ->
                    "$name=${evaluation.values.getValue(name)}"
                }
            } ?: ", baseline"
            val delta = records.last().deltaFromBaseline?.let {
                ", corrected ${it.correctedTiles}, regressed ${it.regressedTiles}"
            }.orEmpty()
            println("[${evaluation.index}/${options.maxEvaluations}] $status$change$delta")

            if (score != null && evaluation.loss < bestLoss) {
                bestLoss = evaluation.loss
                bestConfig = candidateConfig(evaluation.values)
                bestScore = score
                bestPlanet = outcome.planet
                writeJsonAtomically(options.outputFile, bestConfig)
            }
            writeReport()
        },
    )

    val result = search.run()
    val improved = result.bestLoss < result.initialLoss - IMPROVEMENT_EPSILON
    bestPlanet?.let { planet ->
        val rendered = runCatching {
            HersfeldtReference.renderMaps(
                planet,
                options.referenceFile.absolutePath,
                artifactsDirectory,
                "best",
            )
        }.onFailure { println("Warning: best maps were not rendered: ${it.message}") }.getOrNull()
        if (rendered != null) {
            artifacts = artifacts.copy(
                bestSimulatedMap = rendered.simulatedMap,
                bestDifferenceMap = rendered.differenceMap,
            )
        }
    }
    var applied = false
    if (options.apply && improved) {
        val backup = File(options.reportFile.parentFile, "original-${options.configFile.name}")
        if (!backup.exists()) {
            Files.copy(options.configFile.toPath(), backup.toPath())
        }
        writeJsonAtomically(options.configFile, bestConfig)
        applied = true
    }

    writeReport(
        finishedAt = Instant.now().toString(),
        initialLoss = result.initialLoss,
        applied = applied,
    )

    println()
    println("Finished ${result.evaluations} evaluation(s).")
    println("Initial loss: %.6f".format(result.initialLoss))
    println("Best loss:    %.6f".format(result.bestLoss))
    println("Best config:  ${options.outputFile}")
    println("Report:       ${options.reportFile}")
    println("Artifacts:    $artifactsDirectory")
    bestScore?.largestMismatches?.take(5)?.takeIf { it.isNotEmpty() }?.let { mismatches ->
        println("Largest remaining confusions:")
        mismatches.forEach { mismatch ->
            println(
                "  ${mismatch.reference.id} -> ${mismatch.simulated.id}: " +
                    "${mismatch.count} (${mismatch.conditionDistance} condition hop(s))"
            )
        }
    }
    when {
        applied -> println("Applied improved config to ${options.configFile}")
        options.apply -> println("No improvement; ${options.configFile.name} was left unchanged")
        else -> println("Input config was left unchanged; pass --apply to apply an improving winner")
    }

}

private fun applyClimateConfig(config: ObjectNode) {
    Serialization.configMapper.readValue(config.toString(), ClimateSimulationGlobals::class.java)
    ClimateRuntimeConfig.resetToDefaults()
}

private fun writeJsonAtomically(file: File, value: Any) {
    file.parentFile?.mkdirs()
    val temporary = File(file.parentFile ?: File("."), ".${file.name}.tmp")
    Serialization.configMapper.writeValue(temporary, value)
    try {
        Files.move(
            temporary.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    } catch (_: Exception) {
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

private fun parseClimateTunerOptions(args: Array<String>): ClimateTunerOptions {
    val values = mutableMapOf<String, String>()
    var apply = false
    var resume = false
    var index = 0
    while (index < args.size) {
        val argument = args[index]
        if (argument == "--apply") {
            apply = true
            index++
            continue
        }
        if (argument == "--resume") {
            resume = true
            index++
            continue
        }
        require(argument.startsWith("--")) { "Unexpected argument: $argument" }
        require(index + 1 < args.size) { "Missing value after $argument" }
        values[argument.removePrefix("--")] = args[index + 1]
        index += 2
    }

    val output = File(values["output"] ?: "build/climate-tuning/best-climate_config.json")
    return ClimateTunerOptions(
        planetFile = File(values["planet"] ?: "save/earth.json"),
        referenceFile = File(values["reference"] ?: HersfeldtReference.defaultFilename),
        configFile = File(values["config"] ?: "climate_config.json"),
        tuningSpaceFile = File(values["space"] ?: "climate_tuning.json"),
        outputFile = output,
        reportFile = File(values["report"] ?: File(output.parentFile ?: File("."), "report.json").path),
        maxEvaluations = values["max-evaluations"]?.toInt()
            ?: DEFAULT_MAX_EVALUATIONS,
        selectedParameters = values["parameters"]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet(),
        interactionPairs = parseInteractionPairs(values["interactions"]),
        apply = apply,
        resume = resume,
    )
}

private fun parseInteractionPairs(value: String?): List<Pair<String, String>> = value
    ?.split(',')
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?.map { pair ->
        val names = pair.split('+').map { it.trim() }.filter { it.isNotEmpty() }
        require(names.size == 2) { "Interaction must be written A+B: $pair" }
        names[0] to names[1]
    }
    ?: emptyList()

private fun printClimateTunerHelp() {
    println(
        """
        Tune ClimateSimulationGlobals against the Hersfeldt Earth reference.

        Usage:
          .\tune_climate.ps1 [options]

        Options:
          --planet FILE          Imported Earth planet save (default: save/earth.json)
          --reference FILE       Hersfeldt reference PNG
          --config FILE          Starting climate config
          --space FILE           Parameter bounds and step sizes
          --parameters A,B       Tune only the named parameters
          --interactions A+B,C+D Test paired +/- moves after coordinate trials
          --max-evaluations N    Total simulation budget (default: $DEFAULT_MAX_EVALUATIONS)
          --output FILE          Best candidate config
          --report FILE          Machine-readable evaluation report
          --resume               Start from an existing --output config
          --apply                Replace --config only when the score improves
          --help                 Show this help
        """.trimIndent(),
    )
}

private const val DEFAULT_MAX_EVALUATIONS = 9
private const val FAILED_EVALUATION_LOSS = 10.0
private const val IMPROVEMENT_EPSILON = 1e-12
