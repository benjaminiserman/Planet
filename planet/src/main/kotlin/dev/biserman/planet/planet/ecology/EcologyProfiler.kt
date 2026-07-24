package dev.biserman.planet.planet.ecology

import godot.global.GD
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.LongAdder

internal class EcologyTurnProfile(
    private val historyTurn: Long,
    private val totalTiles: Int,
) {
    val activeTiles = LongAdder()
    val emptyTiles = LongAdder()
    val totalSpecies = LongAdder()
    val modelCacheHits = LongAdder()
    val modelCacheMisses = LongAdder()
    val modelNanos = LongAdder()
    val solverNanos = LongAdder()
    val finalizationNanos = LongAdder()
    val tileNanos = LongAdder()
    val eulerSteps = LongAdder()
    val rk2Steps = LongAdder()
    val rk4Steps = LongAdder()
    val derivativeCalls = LongAdder()
    val derivativeNanos = LongAdder()
    val habitatNanos = LongAdder()
    val feedingCalls = LongAdder()
    val feedingNanos = LongAdder()
    val feedingOpportunities = LongAdder()
    val realizedFluxes = LongAdder()
    val allocationIterations = LongAdder()

    private var maximumTileNanos = 0L
    private var maximumTileId = -1
    private var maximumTileSpecies = 0
    private val memoryBean = ManagementFactory.getMemoryMXBean()
    private val garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans()
    private val startingHeapBytes = memoryBean.heapMemoryUsage.used
    private val startingGcCount = garbageCollectors.sumOf { it.collectionCount.coerceAtLeast(0L) }
    private val startingGcMillis = garbageCollectors.sumOf { it.collectionTime.coerceAtLeast(0L) }

    @Synchronized
    fun recordTile(tileId: Int, speciesCount: Int, elapsedNanos: Long) {
        tileNanos.add(elapsedNanos)
        if (elapsedNanos > maximumTileNanos) {
            maximumTileNanos = elapsedNanos
            maximumTileId = tileId
            maximumTileSpecies = speciesCount
        }
    }

    fun recordFeeding(
        elapsedNanos: Long,
        opportunityCount: Int,
        fluxCount: Int,
        iterations: Int,
    ) {
        feedingCalls.increment()
        feedingNanos.add(elapsedNanos)
        feedingOpportunities.add(opportunityCount.toLong())
        realizedFluxes.add(fluxCount.toLong())
        allocationIterations.add(iterations.toLong())
    }

    fun report(wallNanos: Long): String {
        val active = activeTiles.sum()
        val derivatives = derivativeCalls.sum()
        val feedings = feedingCalls.sum()
        val endingHeapBytes = memoryBean.heapMemoryUsage.used
        val gcCount = garbageCollectors.sumOf { it.collectionCount.coerceAtLeast(0L) } - startingGcCount
        val gcMillis = garbageCollectors.sumOf { it.collectionTime.coerceAtLeast(0L) } - startingGcMillis
        fun milliseconds(nanos: Long): String = "%.2f".format(nanos / 1_000_000.0)
        fun average(total: Long, count: Long): String =
            if (count == 0L) "0.00" else "%.2f".format(total.toDouble() / count)
        val solverOverheadNanos = (solverNanos.sum() - derivativeNanos.sum()).coerceAtLeast(0L)
        val derivativeOtherNanos = (
            derivativeNanos.sum() - feedingNanos.sum() - habitatNanos.sum()
        ).coerceAtLeast(0L)

        return buildString {
            appendLine("ECOLOGY PROFILE turn=$historyTurn wall=${milliseconds(wallNanos)}ms")
            appendLine(
                "  tiles: total=$totalTiles active=$active empty=${emptyTiles.sum()} " +
                    "avgSpecies=${average(totalSpecies.sum(), active)} " +
                    "tileCpu=${milliseconds(tileNanos.sum())}ms"
            )
            appendLine(
                    "  tile phases: model=${milliseconds(modelNanos.sum())}ms " +
                    "solver=${milliseconds(solverNanos.sum())}ms " +
                    "solverOverhead=${milliseconds(solverOverheadNanos)}ms " +
                    "finalize=${milliseconds(finalizationNanos.sum())}ms " +
                    "slowestTile=$maximumTileId/${maximumTileSpecies}sp/${milliseconds(maximumTileNanos)}ms"
            )
            appendLine(
                "  model cache: hits=${modelCacheHits.sum()} misses=${modelCacheMisses.sum()}"
            )
            appendLine(
                    "  integration: euler=${eulerSteps.sum()} rk2=${rk2Steps.sum()} " +
                    "rk4=${rk4Steps.sum()} derivatives=$derivatives " +
                    "derivativeCpu=${milliseconds(derivativeNanos.sum())}ms " +
                    "habitatCpu=${milliseconds(habitatNanos.sum())}ms " +
                    "otherDerivativeCpu=${milliseconds(derivativeOtherNanos)}ms"
            )
            appendLine(
                "  feeding: calls=$feedings cpu=${milliseconds(feedingNanos.sum())}ms " +
                    "avgOpportunities=${average(feedingOpportunities.sum(), feedings)} " +
                    "avgFluxes=${average(realizedFluxes.sum(), feedings)} " +
                    "avgAllocationIterations=${average(allocationIterations.sum(), feedings)}"
            )
            append(
                "  memory: heapDelta=${"%.2f".format((endingHeapBytes - startingHeapBytes) / 1_048_576.0)}MiB " +
                    "gcCollections=$gcCount gcTime=${gcMillis}ms"
            )
        }
    }
}

internal object EcologyProfiler {
    @Volatile
    var current: EcologyTurnProfile? = null
        private set

    fun begin(historyTurn: Long, totalTiles: Int): EcologyTurnProfile? {
        if (!EcologyConfig.debugProfiling) return null
        check(current == null) { "An ecology profile is already active" }
        return EcologyTurnProfile(historyTurn, totalTiles).also { current = it }
    }

    fun finish(profile: EcologyTurnProfile?, wallNanos: Long) {
        if (profile == null) return
        if (current === profile) current = null
        GD.print(profile.report(wallNanos))
    }
}
