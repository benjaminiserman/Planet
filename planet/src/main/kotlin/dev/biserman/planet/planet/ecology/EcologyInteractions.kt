package dev.biserman.planet.planet.ecology

enum class InteractionKind {
    NONE,
    PREDATION,
    FILTER_FEEDING,
    GRAZING,
    SUPPLEMENTAL_FEEDING,
    PARASITISM,
}

data class CompiledInteraction(
    val kind: InteractionKind,
    val consumerGainRate: Double,
    val targetLossRate: Double,
    val targetBenefitRate: Double = 0.0,
    val targetRequired: Boolean = false,
) {
    companion object {
        val NONE = CompiledInteraction(InteractionKind.NONE, 0.0, 0.0)
    }
}

class InteractionMatrix internal constructor(
    val speciesCount: Int,
    private val kinds: ByteArray,
    private val consumerGainRates: DoubleArray,
    private val targetLossRates: DoubleArray,
    private val targetBenefitRates: DoubleArray,
    private val requiredTargets: ByteArray,
) {
    fun get(consumerIndex: Int, targetIndex: Int): CompiledInteraction {
        val offset = consumerIndex * speciesCount + targetIndex
        return CompiledInteraction(
            kind = InteractionKind.entries[kinds[offset].toInt()],
            consumerGainRate = consumerGainRates[offset],
            targetLossRate = targetLossRates[offset],
            targetBenefitRate = targetBenefitRates[offset],
            targetRequired = requiredTargets[offset].toInt() != 0,
        )
    }

    internal fun kindAt(offset: Int): Int = kinds[offset].toInt()
    internal fun consumerGainAt(offset: Int): Double = consumerGainRates[offset]
    internal fun targetLossAt(offset: Int): Double = targetLossRates[offset]
    internal fun targetBenefitAt(offset: Int): Double = targetBenefitRates[offset]
    internal fun targetRequiredAt(offset: Int): Boolean =
        requiredTargets[offset].toInt() != 0
}