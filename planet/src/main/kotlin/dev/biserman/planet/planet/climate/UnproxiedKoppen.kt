package dev.biserman.planet.planet.climate

import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.climate.Hersfeldt.GrowthLevel
import dev.biserman.planet.planet.climate.Hersfeldt.WinterType
import dev.biserman.planet.planet.climate.Hersfeldt.aridityFactor
import dev.biserman.planet.planet.climate.Hersfeldt.estimateAet
import dev.biserman.planet.planet.climate.Hersfeldt.evaporationRatio
import dev.biserman.planet.planet.climate.Hersfeldt.gdd
import dev.biserman.planet.planet.climate.Hersfeldt.growthAridityFactor
import dev.biserman.planet.planet.climate.Hersfeldt.growthSupply
import dev.biserman.planet.planet.climate.Hersfeldt.hargreavesPet
import dev.biserman.planet.planet.climate.Hersfeldt.koppenlikePet
import dev.biserman.planet.planet.climate.Hersfeldt.maxIce
import dev.biserman.planet.planet.climate.Hersfeldt.minIce
import dev.biserman.planet.planet.climate.Hersfeldt.summerType
import dev.biserman.planet.planet.climate.Hersfeldt.winterType
import dev.biserman.planet.planet.climate.Koppen.COLD_DESERT
import dev.biserman.planet.planet.climate.Koppen.COLD_SEMIARID
import dev.biserman.planet.planet.climate.Koppen.COOL_SUMMER_MEDITERRANEAN
import dev.biserman.planet.planet.climate.Koppen.EXTREMELY_COLD_SUBARCTIC_MONSOON
import dev.biserman.planet.planet.climate.Koppen.HOT_DESERT
import dev.biserman.planet.planet.climate.Koppen.HOT_SEMIARID
import dev.biserman.planet.planet.climate.Koppen.HOT_SUMMER_HUMID_CONTINENTAL
import dev.biserman.planet.planet.climate.Koppen.HOT_SUMMER_HUMID_CONTINENTAL_MONSOON
import dev.biserman.planet.planet.climate.Koppen.HOT_SUMMER_MEDITERRANEAN
import dev.biserman.planet.planet.climate.Koppen.HUMID_SUBTROPICAL
import dev.biserman.planet.planet.climate.Koppen.HUMID_SUBTROPICAL_MONSOON
import dev.biserman.planet.planet.climate.Koppen.ICE_CAP
import dev.biserman.planet.planet.climate.Koppen.MEDITERRANEAN_EXTREMELY_COLD_SUBARCTIC
import dev.biserman.planet.planet.climate.Koppen.MEDITERRANEAN_HOT_SUMMER_HUMID_CONTINENTAL
import dev.biserman.planet.planet.climate.Koppen.MEDITERRANEAN_SUBARCTIC
import dev.biserman.planet.planet.climate.Koppen.MEDITERRANEAN_WARM_SUMMER_HUMID_CONTINENTAL
import dev.biserman.planet.planet.climate.Koppen.OCEAN
import dev.biserman.planet.planet.climate.Koppen.SUBARCTIC
import dev.biserman.planet.planet.climate.Koppen.SUBARCTIC_MONSOON
import dev.biserman.planet.planet.climate.Koppen.SUBPOLAR_OCEANIC
import dev.biserman.planet.planet.climate.Koppen.SUBPOLAR_OCEANIC_MONSOON
import dev.biserman.planet.planet.climate.Koppen.SUBTROPICAL_HIGHLAND
import dev.biserman.planet.planet.climate.Koppen.SUBTROPICAL_HIGHLAND_MONSOON
import dev.biserman.planet.planet.climate.Koppen.TEMPERATE_OCEANIC
import dev.biserman.planet.planet.climate.Koppen.TEMPERATE_OCEANIC_MONSOON
import dev.biserman.planet.planet.climate.Koppen.TROPICAL_MONSOON
import dev.biserman.planet.planet.climate.Koppen.TROPICAL_RAINFOREST
import dev.biserman.planet.planet.climate.Koppen.TROPICAL_SAVANNA_SUMMER
import dev.biserman.planet.planet.climate.Koppen.TROPICAL_SAVANNA_WINTER
import dev.biserman.planet.planet.climate.Koppen.TUNDRA
import dev.biserman.planet.planet.climate.Koppen.WARM_SUMMER_HUMID_CONTINENTAL
import dev.biserman.planet.planet.climate.Koppen.WARM_SUMMER_HUMID_CONTINENTAL_MONSOON
import dev.biserman.planet.planet.climate.Koppen.WARM_SUMMER_MEDITERRANEAN
import dev.biserman.planet.planet.climate.Koppen.highlandElevationThreshold

object UnproxiedKoppen : ClimateClassifier {
    override fun classify(
        planet: Planet,
        datum: ClimateDatum
    ): ClimateClassification {
        val tile = planet.planetTiles[datum.tileId]!!

        // climate parameters
        val pet = datum.months.map { koppenlikePet(it.averageTemperature) }
        val aet = estimateAet(datum, pet)
        val aridityFactor = aridityFactor(pet, aet)
        val evaporationRatio = evaporationRatio(datum, aet)
        val minIce = minIce(planet, datum)
        val maxIce = maxIce(planet, datum)

        val gddResults = gdd(datum)

        val growthSupplyValue = growthSupply(datum, gddResults.monthlyGdd, aet)
        val growthAridityFactor = growthAridityFactor(pet, gddResults.monthlyGdd, aet)

        // tuned thresholds
        val winterType = winterType(datum.months.minOf { it.averageTemperature })
        val summerType = summerType(datum.months.maxOf { it.averageTemperature })
        // using tweaked growthSupplyThreshold of 1.0 instead of 1.15
        val growthSupply = if (growthSupplyValue < 1.0) GrowthLevel.LOW else GrowthLevel.HIGH

        // koppen-specific thresholds
        val lowGrowth = growthSupply == GrowthLevel.LOW
        val highGrowthAridity = growthAridityFactor / aridityFactor > 1.02
        val longGrowingSeason = gddResults.totalGdd > 2300
        val mediumGrowingSeason = gddResults.totalGdd > 1300

        if (!tile.isAboveWater) {
            return OCEAN
        }

        return when {
            gddResults.totalGdd < 250 ->
                if (minIce > 0.1) ICE_CAP
                else TUNDRA
            aridityFactor < 0.32 -> when {
                aridityFactor < 0.14 ->
                    if (winterType == WinterType.MILD || winterType == WinterType.COOL) HOT_DESERT
                    else COLD_DESERT
                else ->
                    if (winterType == WinterType.MILD || winterType == WinterType.COOL) HOT_SEMIARID
                    else COLD_SEMIARID
            }
            winterType == WinterType.MILD -> when {
                aridityFactor > 0.92 -> TROPICAL_RAINFOREST
                aridityFactor > 0.85 || evaporationRatio < 0.45 -> TROPICAL_MONSOON
                growthAridityFactor > aridityFactor -> TROPICAL_SAVANNA_WINTER
                else -> TROPICAL_SAVANNA_SUMMER
            }
            winterType == WinterType.COOL -> when {
                lowGrowth -> when {
                    longGrowingSeason -> HOT_SUMMER_MEDITERRANEAN
                    mediumGrowingSeason -> WARM_SUMMER_MEDITERRANEAN
                    else -> COOL_SUMMER_MEDITERRANEAN
                }
                highGrowthAridity -> when {
                    longGrowingSeason -> HUMID_SUBTROPICAL_MONSOON
                    mediumGrowingSeason ->
                        if (tile.elevation >= highlandElevationThreshold) SUBTROPICAL_HIGHLAND_MONSOON
                        else TEMPERATE_OCEANIC_MONSOON
                    else -> SUBPOLAR_OCEANIC_MONSOON
                }
                else -> when {
                    longGrowingSeason -> HUMID_SUBTROPICAL
                    mediumGrowingSeason ->
                        if (tile.elevation >= highlandElevationThreshold) SUBTROPICAL_HIGHLAND
                        else TEMPERATE_OCEANIC
                    else -> SUBPOLAR_OCEANIC
                }
            }
            winterType == WinterType.COLD || winterType == WinterType.FRIGID -> when {
                lowGrowth -> when {
                    longGrowingSeason -> MEDITERRANEAN_HOT_SUMMER_HUMID_CONTINENTAL
                    mediumGrowingSeason -> MEDITERRANEAN_WARM_SUMMER_HUMID_CONTINENTAL
                    winterType == WinterType.COLD -> MEDITERRANEAN_SUBARCTIC
                    else -> MEDITERRANEAN_EXTREMELY_COLD_SUBARCTIC
                }
                highGrowthAridity -> when {
                    longGrowingSeason -> HOT_SUMMER_HUMID_CONTINENTAL_MONSOON
                    mediumGrowingSeason -> WARM_SUMMER_HUMID_CONTINENTAL_MONSOON
                    winterType == WinterType.COLD -> SUBARCTIC_MONSOON
                    else -> EXTREMELY_COLD_SUBARCTIC_MONSOON
                }
                else -> when {
                    longGrowingSeason -> HOT_SUMMER_HUMID_CONTINENTAL
                    mediumGrowingSeason -> WARM_SUMMER_HUMID_CONTINENTAL
                    winterType == WinterType.COLD -> SUBARCTIC
                    else -> EXTREMELY_COLD_SUBARCTIC_MONSOON
                }
            }
            else -> UNKNOWN_CLIMATE
        }
    }
}