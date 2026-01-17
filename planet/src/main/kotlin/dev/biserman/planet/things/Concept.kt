package dev.biserman.planet.things

enum class Concept(val body: (Concept).() -> Unit = {}) {
    ELEMENT({ parent of listOf(WATER, EARTH, FIRE, AIR) }),
    WATER({ opposite of FIRE }),
    EARTH({ opposite of AIR }),
    FIRE({ opposite of WATER }),
    AIR({ opposite of EARTH }),

    NORTH({ opposite of SOUTH }),
    EAST({ opposite of WEST }),
    SOUTH({ opposite of NORTH }),
    WEST({ opposite of EAST }),

    DEEP({ opposite of SHALLOW }),
    SHALLOW({ opposite of DEEP }),

    STONE({ related with EARTH }),
    MAGMA({
        made of STONE
        related with FIRE
    }),
    WOOD,
    LEAF({ related with listOf(VEGETABLE, HERB) }),
    ROOT({ related with HERB }),
    FLESH,

    FRUIT,
    NUT,
    VEGETABLE,
    GRAIN({ related with GRASS }),
    SPICE({ related with HERB }),
    HERB({ type of PLANT }),
    MEAT({
        made of FLESH
        related with ANIMAL
    }),
    MILK({ related with ANIMAL }),

    PLANT({
        related with EARTH
        consumer of WATER
        related with listOf(FRUIT, NUT, VEGETABLE, GRAIN, HERB)
    }),
    BUSH({
        made of listOf(LEAF, WOOD, ROOT)
        producer of listOf(NUT, FRUIT)
        type of PLANT
    }),
    TREE({
        made of listOf(LEAF, WOOD, ROOT)
        producer of listOf(NUT, FRUIT)
        type of PLANT
    }),
    GRASS({ type of PLANT }),

    FUNGUS,
    ANIMAL({ made of FLESH }),

    LIGHT({ opposite of DARK }),
    DARK({ opposite of LIGHT }),
    DAY({
        opposite of NIGHT
        related with LIGHT
    }),
    NIGHT({
        opposite of DAY
        related with DARK
    }),

    // Geography
    LAND({
        related with EARTH
        downwards of SKY
        opposite of COSMOS
        has feature listOf(MOUNTAIN, RIVER)
    }),
    COSMOS({
        related with listOf(NIGHT, DARK)
        has feature listOf(STAR, COMET)
    }),
    SKY({
        related with listOf(COSMOS, AIR)
        has feature COMET
    }),
    OCEAN({
        made of WATER
        downwards of SKY
    }),
    STAR({ related with listOf(COSMOS, LIGHT) }),
    COMET({ related with listOf(STAR, FIRE) }),

    ABYSS({ related with listOf(OCEAN, DARK, DEEP) }),
    RIVER({ made of WATER }),
    MOUNTAIN({
        related with EARTH
        made of STONE
    }),

    ANCIENT({ related with EARTH }),

    LUST({ cultural with FIRE }),
    GLUTTONY,
    INDULGENCE,
    TEMPERANCE({
        related with WATER
        opposite of listOf(LUST, GLUTTONY, INDULGENCE)
    });

    data class Relationship(val type: RelationshipType, val origin: Concept, val other: Concept, val via: Concept?) {
        fun addReverse() = type.addReverse(this)
    }

    val relationships: MutableSet<Relationship> = mutableSetOf()

    fun init() {
        body()
    }

    fun prettyPrint(): String =
        "$name: [${relationships.joinToString { "${it.type.name} ${it.type.keywords.first()} ${it.other.name}" }}]"

    // Relationship DSL
    open class RelationshipType(val name: String, vararg keywords: String, val addReverse: Relationship.() -> Unit) {
        val keywords = keywords.toSet()
    }

    class WithRelationship(name: String, addReverse: Relationship.() -> Unit) :
        RelationshipType(name, "with", addReverse = addReverse)

    class OfRelationship(name: String, addReverse: Relationship.() -> Unit) :
        RelationshipType(name, "of", addReverse = addReverse)

    class ByRelationship(name: String, addReverse: Relationship.() -> Unit) :
        RelationshipType(name, "by", addReverse = addReverse)

    class FeatureRelationship(name: String, addReverse: Relationship.() -> Unit) :
        RelationshipType(name, "feature", addReverse = addReverse)


    private val child: OfRelationship =
        OfRelationship("child") { other += Relationship(parent, other, origin, via) }
    private val parent: OfRelationship =
        OfRelationship("parent") { other += Relationship(child, other, origin, via) }
    private val related: WithRelationship =
        WithRelationship("related") { other += Relationship(related, other, origin, via) }
    private val cultural: WithRelationship =
        WithRelationship("cultural") { other += Relationship(cultural, other, origin, via) }
    private val opposite: OfRelationship =
        OfRelationship("opposite") { other += Relationship(opposite, other, origin, via) }
    private val upwards: OfRelationship =
        OfRelationship("upwards") { other += Relationship(downwards, other, origin, via) }
    private val downwards: OfRelationship =
        OfRelationship("downwards") { other += Relationship(upwards, other, origin, via) }
    private val inside: OfRelationship =
        OfRelationship("inside") { other += Relationship(contains, other, origin, via) }
    private val contains: OfRelationship =
        OfRelationship("contains") { other += Relationship(inside, other, origin, via) }
    private val made: OfRelationship =
        OfRelationship("made") { other += Relationship(substance, other, origin, via) }
    private val substance: OfRelationship =
        OfRelationship("substance") { other += Relationship(made, other, origin, via) }
    private val has: FeatureRelationship =
        FeatureRelationship("has") { other += Relationship(feature, other, origin, via) }
    private val feature: OfRelationship =
        OfRelationship("feature") { other += Relationship(has, other, origin, via) }
    private val type: OfRelationship =
        OfRelationship("type") { other += Relationship(supertype, other, origin, via) }
    private val supertype: OfRelationship =
        OfRelationship("supertype") { other += Relationship(type, other, origin, via) }
    private val product: OfRelationship =
        OfRelationship("product") { other += Relationship(producer, other, origin, via) }
    private val producer: OfRelationship =
        OfRelationship("producer") { other += Relationship(product, other, origin, via) }
    private val consumer: OfRelationship =
        OfRelationship("consumer") { other += Relationship(consumed, other, origin, via) }
    private val consumed: ByRelationship =
        ByRelationship("consumed") { other += Relationship(consumer, other, origin, via) }


    private fun makeRelationship(type: RelationshipType, keyword: String, vararg others: Concept) {
        val concept = this@Concept
        if (keyword !in type.keywords) {
            throw Error("Invalid syntax: ${type.name} $keyword ${others.joinToString { it.name }}")
        }
        others.forEach { other ->
            relationships.add(Relationship(type, concept, other, concept))
        }
    }

    private infix fun (WithRelationship).with(other: Concept) = makeRelationship(this, "with", other)
    private infix fun (WithRelationship).with(others: List<Concept>) =
        makeRelationship(this, "with", *others.toTypedArray())

    private infix fun (OfRelationship).of(other: Concept) = makeRelationship(this, "of", other)
    private infix fun (OfRelationship).of(others: List<Concept>) =
        makeRelationship(this, "of", *others.toTypedArray())

    private infix fun (ByRelationship).by(other: Concept) = makeRelationship(this, "by", other)
    private infix fun (ByRelationship).by(others: List<Concept>) =
        makeRelationship(this, "by", *others.toTypedArray())

    private infix fun (FeatureRelationship).feature(other: Concept) = makeRelationship(this, "feature", other)
    private infix fun (FeatureRelationship).feature(others: List<Concept>) =
        makeRelationship(this, "feature", *others.toTypedArray())

    @Deprecated(
        message = "Don't use `to` with relationships",
        level = DeprecationLevel.ERROR
    )
    private infix fun RelationshipType.to(other: Concept): Nothing = throw Error("Invalid syntax: $this to $other")

    @Deprecated(
        message = "Don't use `to` with relationships",
        level = DeprecationLevel.ERROR
    )
    private infix fun RelationshipType.to(others: List<Concept>): Nothing =
        throw Error("Invalid syntax: $this to $others")


    private operator fun plusAssign(relationship: Relationship): Unit =
        run { relationships.add(relationship) }

    companion object {
        init {
            entries.forEach { it.init() }
            entries.forEach { entry ->
                entry.relationships.forEach {
                    it.addReverse()
                }
                entry.relationships.groupBy { it.copy(via = null) }.forEach { (prototype, group) ->
                    if (group.size > 1) {
                        entry.relationships.removeAll(group.toSet())
                        entry.relationships.add(prototype)
                    }
                }
            }
        }
    }
}