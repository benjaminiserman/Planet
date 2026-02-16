package dev.biserman.planet.planet.tectonics

import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.things.Concept
import dev.biserman.planet.things.Stone
import dev.biserman.planet.utils.weightedBagOf
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

data class StonePlacement(
    val type: StonePlacementType,
    val specialOptions: List<SpecialOption>,
    val defaultOption: Stone
) {
    data class SpecialOption(val condition: StonePlacementCondition, val stone: Stone)
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
            { random: Random -> MantleConvectionMagnitudeAbove(random.nextDouble()) } to 0
        )
    }
}

data class StoneColumn(var surface: Stone, var middle: Stone, var deep: Stone) {
    fun getLayer(planetTile: PlanetTile, stonePlacementType: StonePlacementType): Stone {
        val stonePlacement = planetTile.planet.worldKinds.stonePlacements[stonePlacementType]
            ?: return planetTile.planet.worldKinds.defaultSurfaceStone
        val firstSpecial = stonePlacement.specialOptions.firstOrNull { it.condition.canPlace(planetTile) }
        return firstSpecial?.stone ?: stonePlacement.defaultOption
    }

    fun accreteLayer(planetTile: PlanetTile, stonePlacementType: StonePlacementType) {
        val layer = getLayer(planetTile, stonePlacementType)
        deep = middle
        middle = surface
        surface = layer
    }

    fun erodeLayer(planetTile: PlanetTile) {
        surface = middle
        middle = deep
        deep = planetTile.planet.worldKinds.defaultDeepStone
    }

    fun tryTransmuteDeep(planetTile: PlanetTile) {
        val metamorphicForm = deep.placementType.metamorphicForm ?: return
        deep = getLayer(planetTile, metamorphicForm)
    }
}