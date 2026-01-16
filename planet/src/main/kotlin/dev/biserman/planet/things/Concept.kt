package dev.biserman.planet.things

enum class Concept(val body: (Concept).() -> Unit = {}) {
    ELEMENT({ parent of listOf(WATER, EARTH, FIRE, AIR) }),
    WATER,
    EARTH,
    FIRE,
    AIR,
    STONE({ related with EARTH }),
    COSMOS,
    STAR({ related with COSMOS }),
    SKY({ related with COSMOS }),
    ANCIENT({ related with EARTH }),
    ABYSSAL({ related with OCEAN }),
    OCEAN({ related with WATER }),
    RIVER({ related with WATER }),
    MOUNTAIN({ related with EARTH }),
    COMET({ related with listOf(COSMOS, STAR, SKY, FIRE) }),
    LUST({ associated with FIRE }),
    GLUTTONY,
    INDULGENCE,
    TEMPERANCE({
        related with WATER
        opposite of listOf(LUST, GLUTTONY, INDULGENCE)
    });

    data class Relationship(val type: RelationshipType, val origin: Concept, val other: Concept, val via: Concept?) {
        fun addReverse() = type.addReverse(this)
    }

    private var innerRelationships: MutableSet<Relationship>? = null
    val relationships: Set<Relationship>
        get() = innerRelationships ?: throw Error("Concept $name used before initialization!")

    fun init() {
        innerRelationships = mutableSetOf()
        body()
    }

    fun prettyPrint(): String =
        "$name: [${relationships.joinToString { "${it.type.name} ${it.type.keywords.first()} ${it.other.name}" }}]"

    // Relationship DSL
    class RelationshipType(val name: String, vararg keywords: String, val addReverse: Relationship.() -> Unit) {
        val keywords = keywords.toSet()
    }

    private val child: RelationshipType =
        RelationshipType("child", "of") { other += Relationship(parent, other, origin, via) }
    private val parent: RelationshipType =
        RelationshipType("parent", "of") { other += Relationship(child, other, origin, via) }
    private val related: RelationshipType =
        RelationshipType("related", "with") { other += Relationship(related, other, origin, via) }
    private val associated: RelationshipType =
        RelationshipType("associated", "with") { other += Relationship(associated, other, origin, via) }
    private val opposite: RelationshipType =
        RelationshipType("opposite", "of") { other += Relationship(opposite, other, origin, via) }

    private fun makeRelationship(type: RelationshipType, keyword: String, vararg others: Concept) {
        val concept = this@Concept
        if (keyword !in type.keywords) {
            throw Error("Invalid syntax: ${type.name} $keyword ${others.joinToString { it.name }}")
        }
        others.forEach { other ->
            innerRelationships!!.add(Relationship(type, concept, other, concept))
        }
    }

    private infix fun (RelationshipType).with(other: Concept) = makeRelationship(this, "with", other)
    private infix fun (RelationshipType).with(others: List<Concept>) =
        makeRelationship(this, "with", *others.toTypedArray())

    private infix fun (RelationshipType).of(other: Concept) = makeRelationship(this, "of", other)
    private infix fun (RelationshipType).of(others: List<Concept>) =
        makeRelationship(this, "of", *others.toTypedArray())


    private operator fun plusAssign(relationship: Relationship): Unit =
        run { innerRelationships!!.add(relationship) }

    companion object {
        init {
            entries.forEach { it.init() }
            entries.forEach { entry ->
                entry.relationships.forEach {
                    it.addReverse()
                }
                entry.relationships.groupBy { it.copy(via = null) }.forEach { (prototype, group) ->
                    if (group.size > 1) {
                        entry.innerRelationships!!.removeAll(group.toSet())
                        entry.innerRelationships!!.add(prototype)
                    }
                }
            }
        }
    }
}