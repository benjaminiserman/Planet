package dev.biserman.planet.planet.ecology

data class CompiledEcology(
    val species: List<CompiledSpecies>,
    val niches: List<NicheDefinition>,
    val interactions: InteractionMatrix,
) {
    private val indexById = species.associate { it.id to it.index }

    fun speciesIndex(id: String): Int =
        indexById[id] ?: error("Unknown species id: $id")
}

object EcologyCompiler {
    private val biochemistryTraits = setOf(
        CommonTrait.TEMPERATE_BIOCHEMISTRY,
        CommonTrait.FRIGID_BIOCHEMISTRY,
        CommonTrait.HOT_BIOCHEMISTRY,
    )
    fun compile(
        definitions: List<SpeciesDefinition>,
        niches: List<NicheDefinition> = EcologyNiches.defaults,
    ): CompiledEcology {
        require(definitions.isNotEmpty())
        require(definitions.map { it.id }.distinct().size == definitions.size) {
            "Species ids must be unique"
        }
        require(niches.distinct().size == niches.size) {
            "Niche definitions must be unique"
        }

        val compiledSpecies = definitions.mapIndexed { index, definition ->
            compileSpecies(index, definition, niches)
        }
        return CompiledEcology(
            species = compiledSpecies,
            niches = niches,
            interactions = FoodWebCompiler.compile(definitions, compiledSpecies),
        )
    }

    private fun compileSpecies(
        index: Int,
        definition: SpeciesDefinition,
        niches: List<NicheDefinition>,
    ): CompiledSpecies {
        val commonTraits = definition.traits.filterIsInstance<CommonTrait>().toSet()
        val allEffects = definition.traits.flatMap { it.effects }
        val thermalStrategies = allEffects.filterIsInstance<TraitEffect.ThermalRegulation>()
        require(
            definition.kind == SpeciesKind.INVARIANT ||
                definition.traits.none { it.invariantOnly },
        ) {
            "${definition.displayName} uses a trait reserved for invariant aggregate guilds"
        }
        require(
            definition.kind != SpeciesKind.INVARIANT ||
                CommonTrait.INVARIANT_RESISTANCE in commonTraits,
        ) {
            "${definition.displayName} is invariant and must have invariant guild resilience"
        }
        require(commonTraits.count { it in biochemistryTraits } == 1) {
            "${definition.displayName} must have exactly one biochemistry foundation"
        }
        require(!definition.motile || thermalStrategies.size == 1) {
            "${definition.displayName} is motile and must have exactly one thermal strategy"
        }
        require(definition.motile || thermalStrategies.isEmpty()) {
            "${definition.displayName} is not motile but has a motile thermal strategy"
        }
        require(!definition.motile || CommonTrait.ROOTED_BODY !in commonTraits) {
            "${definition.displayName} is motile and cannot have a rooted body; use a locomotion trait"
        }
        require(definition.motile || CommonTrait.TERRESTRIAL_LOCOMOTION !in commonTraits) {
            "${definition.displayName} is not motile and cannot have terrestrial locomotion"
        }
        definition.traits.filterNot { it.isFoundation }.forEach { trait ->
            val cost = trait.effects.filterIsInstance<TraitEffect.MaintenanceCost>().sumOf { it.fraction }
            require(cost > 0.0) {
                "Non-foundation trait '${trait.displayName}' must have an explicit maintenance/opportunity cost"
            }
            require(trait.effects.any { it !is TraitEffect.MaintenanceCost } || trait.relationships.isNotEmpty()) {
                "Non-foundation trait '${trait.displayName}' must provide an effect"
            }
        }

        val context = SpeciesCompilationContext(
            speciesDisplayName = definition.displayName,
            sizeTemperatureTolerance = sizeTemperatureTolerance(definition.sizeClass),
        )
        definition.traits.forEach(context::apply)
        context.applyCrossTraitRules(definition.sizeClass, commonTraits)
        return context.finish(index, definition, niches, commonTraits)
    }

    private fun sizeTemperatureTolerance(sizeClass: SizeClass): Double = when (sizeClass) {
        SizeClass.MINUSCULE, SizeClass.TINY, SizeClass.SMALL -> 0.0
        SizeClass.MEDIUM -> 0.5
        SizeClass.LARGE -> 1.5
        SizeClass.HUGE -> 3.0
        SizeClass.COLOSSAL -> 4.0
    }
}