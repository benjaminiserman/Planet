package dev.biserman.planet.planet.ecology

@Suppress("MayBeConstant")
object EcologyConfig {
    var maxSpeciesPerEcosystem = 20
    var integrationSubstepsPerSeason = 16
    var minimumViableIndividuals = 2.0
    var environmentalVolatility = 0.20
    var speciesVolatility = 0.30
    var maximumAverageEstablishmentStress = 0.85
    var maximumPeakEstablishmentStress = 2.75
    var minimumEnvironmentAffinity = -0.35
    var environmentAffinitySelectionStrength = 1.5
    var climateStressSelectionStrength = 2.0
    var debugProfiling = false
    var parallelTileSimulation = false
    var parallelTileWorkers = 2
    var simulationAreaPerSquareMeter = 1.0e-6
    var initialProducerCapacityFractionMin = 0.10
    var initialProducerCapacityFractionMax = 0.55
    var initialConsumerDensityMin = 0.002
    var initialConsumerDensityMax = 0.060

    var globallyDistributedOrders = mutableListOf(
        TaxonomicOrder.POALES,
        TaxonomicOrder.FABALES,
        TaxonomicOrder.ASTERALES,
        TaxonomicOrder.DINOPHYSIALES,
        TaxonomicOrder.RODENTIA,
        TaxonomicOrder.CARNIVORA,
        TaxonomicOrder.PASSERIFORMES,
        TaxonomicOrder.ACCIPITRIFORMES,
        TaxonomicOrder.COLEOPTERA,
        TaxonomicOrder.DIPTERA,
        TaxonomicOrder.LEPIDOPTERA,
        TaxonomicOrder.ORTHOPTERA,
        TaxonomicOrder.LAMINARIALES,
        TaxonomicOrder.ALISMATALES,
        TaxonomicOrder.BACILLARIALES,
        TaxonomicOrder.CALANOIDA,
        TaxonomicOrder.DECAPODA,
        TaxonomicOrder.CLUPEIFORMES,
        TaxonomicOrder.SCLERACTINIA,
    )

    fun validate() {
        require(maxSpeciesPerEcosystem >= 1) { "maxSpeciesPerEcosystem must be positive" }
        require(integrationSubstepsPerSeason >= 13) {
            "integrationSubstepsPerSeason must preserve weekly-or-smaller integration steps"
        }
        require(minimumViableIndividuals >= 0.0)
        require(environmentalVolatility >= 0.0)
        require(speciesVolatility >= 0.0)
        require(maximumAverageEstablishmentStress >= 0.0)
        require(maximumPeakEstablishmentStress >= maximumAverageEstablishmentStress)
        require(minimumEnvironmentAffinity.isFinite())
        require(environmentAffinitySelectionStrength >= 0.0)
        require(climateStressSelectionStrength >= 0.0)
        require(parallelTileWorkers >= 1)
        require(simulationAreaPerSquareMeter > 0.0)
        require(initialProducerCapacityFractionMin in 0.0..1.0)
        require(initialProducerCapacityFractionMax in initialProducerCapacityFractionMin..1.0)
        require(initialConsumerDensityMin >= 0.0)
        require(initialConsumerDensityMax >= initialConsumerDensityMin)
        require(globallyDistributedOrders.isNotEmpty())
        val globalSpecies = globallyDistributedOrders.flatMap {
            speciesCatalogByTaxonomicOrder[it].orEmpty()
        }
        require(globalSpecies.any { it.autotrophy != null && Trait.AQUATIC !in it.traits }) {
            "globallyDistributedOrders must supply a terrestrial producer"
        }
        require(globalSpecies.any { it.autotrophy != null && Trait.AQUATIC in it.traits }) {
            "globallyDistributedOrders must supply an aquatic producer"
        }
    }
}
