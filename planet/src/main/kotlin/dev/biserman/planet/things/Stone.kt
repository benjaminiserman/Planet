package dev.biserman.planet.things

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.utils.weightedBagOf
import godot.core.Color
import kotlin.random.Random

enum class StonePlacementType(val stoneType: StoneType, val concepts: List<Concept> = emptyList()) {
    AlluvialDeposition(StoneType.Sedimentary, listOf(Concept.RIVER)),
    OceanicDeposition(StoneType.Sedimentary, listOf(Concept.OCEAN)),
    MetamorphicAlluvial(StoneType.Metamorphic, listOf(Concept.MOUNTAIN)),
    MetamorphicOceanic(StoneType.Metamorphic, listOf(Concept.MOUNTAIN)),
    MetamorphicMantle(StoneType.Metamorphic, listOf(Concept.MOUNTAIN)),
    MetamorphicSubduction(StoneType.Metamorphic, listOf(Concept.MOUNTAIN)),
    MetamorphicPrimordial(StoneType.Metamorphic, listOf(Concept.ANCIENT, Concept.DEEP, Concept.MOUNTAIN)),
    MantleVolcanic(StoneType.Igneous, listOf(Concept.MAGMA)),
    SubductionVolcanic(StoneType.Igneous, listOf(Concept.MAGMA)),
    Primordial(StoneType.Igneous, listOf(Concept.ANCIENT, Concept.DEEP)),
    Meteoric(StoneType.Meteoric, listOf(Concept.COMET));

    val metamorphicForm get() = metamorphicMap[this]

    companion object {
        val metamorphicMap = mapOf(
            AlluvialDeposition to MetamorphicAlluvial,
            OceanicDeposition to MetamorphicOceanic,
            MantleVolcanic to MetamorphicMantle,
            SubductionVolcanic to MetamorphicSubduction,
            Primordial to MetamorphicPrimordial
        )
    }
}

enum class StoneType {
    Sedimentary,
    Metamorphic,
    Igneous,
    Meteoric
}

interface StonePlacementCondition {
    fun canPlace(planetTile: PlanetTile): Boolean

    class MantleConvectionMagnitudeAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) =
            planetTile.planet.noise.mantleConvection.sampleAt(planetTile).length() > threshold
    }

    class MantleConvectionMagnitudeBelow(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) =
            planetTile.planet.noise.mantleConvection.sampleAt(planetTile).length() < threshold
    }

    class EssenceAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) =
            planetTile.planet.noise.essence.sample3d(planetTile.tile.position) < threshold
    }

    class LocalHotspotActivityAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile): Boolean =
            planetTile.planet.noise.hotspots.sampleAt(planetTile) > threshold
    }

    class GlobalHotspotActivityAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile): Boolean = planetTile.planet.hotspotActivity > threshold
    }

    class GlobalHotspotActivityBelow(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile): Boolean = planetTile.planet.hotspotActivity < threshold
    }

    class WaterCoverageAbove(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.planet.waterCoverage > threshold
    }

    class WaterCoverageBelow(val threshold: Double) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.planet.waterCoverage < threshold
    }

    class ContinentialityAbove(val threshold: Int) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.continentiality > threshold
    }

    class ContinentialityBelow(val threshold: Int) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) = planetTile.continentiality < threshold
    }

    class ContinentialityAround(val center: Int, val distance: Int) : StonePlacementCondition {
        override fun canPlace(planetTile: PlanetTile) =
            planetTile.continentiality in (center - distance)..(center + distance)
    }

    companion object {
        val conditionBag = weightedBagOf<(Random) -> StonePlacementCondition>(
            { random: Random -> MantleConvectionMagnitudeAbove(random.nextDouble()) } to 1,
            { random: Random -> MantleConvectionMagnitudeBelow(random.nextDouble()) } to 1,
            { random: Random -> EssenceAbove(random.nextDouble()) } to 1,
            { random: Random -> LocalHotspotActivityAbove(random.nextDouble()) } to 1,
            { random: Random -> GlobalHotspotActivityAbove(random.nextDouble()) } to 1,
            { random: Random -> GlobalHotspotActivityBelow(random.nextDouble()) } to 1,
            { random: Random -> WaterCoverageAbove(random.nextDouble()) } to 1,
            { random: Random -> WaterCoverageBelow(random.nextDouble()) } to 1,
            { random: Random -> ContinentialityAbove(random.nextInt(-10, 10)) } to 1,
            { random: Random -> ContinentialityBelow(random.nextInt(-10, 10)) } to 1,
            { random: Random -> ContinentialityAround(random.nextInt(-10, 10), random.nextInt(1, 5)) } to 1
        )
    }
}

data class StoneComponent(
    var debugName: String,
    var acidityModifier: Double,
    var fertilityModifier: Double,
    var moistureCapacityMultiplier: Double,
    var placementType: StonePlacementType,
) : ResourceComponent {
    val type get() = placementType.stoneType
}

class Stone(
    var stoneComponent: StoneComponent,
    components: MutableComponentSet<ResourceComponent>,
    colors: List<Color>,
    concepts: List<Concept>
) : Resource(
    components.also { it.add(stoneComponent) }, colors, concepts
) {
    override fun toString() =
        "Stone(stoneComponent=$stoneComponent,colors=${colors.map { it.toHtml(false) }},concepts=$concepts)"
}
