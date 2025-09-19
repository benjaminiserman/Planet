package dev.biserman.planet.planet

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.biserman.planet.geometry.*
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.topology.toTopology
import dev.biserman.planet.utils.memo
import kotlin.random.Random

class Planet(val seed: Int, val size: Int) {
    @JsonIgnore
    val random = Random(seed)
    @JsonIgnore
    val noise = NoiseMaps(seed, random)
    @JsonIgnore
    val topology = makeTopology(size)

    var planetTiles = topology.tiles.associate { tile -> tile.id to PlanetTile(this, tile.id) }

    fun getTile(tile: Tile) = planetTiles[tile.id]!!

    val planetStats = PlanetStats()

    @Suppress("JoinDeclarationAndAssignment")
    var tectonicPlates: MutableList<TectonicPlate>
    var convergenceZones: MutableMap<Int, ConvergenceZone> = mutableMapOf()
    var divergenceZones: MutableMap<Int, DivergenceZone> = mutableMapOf()

    var tectonicAge = 0
    var nextPlateId = 0

    val seaLevel: Double = 0.0

    val oldestCrust by memo({ tectonicAge }) { planetTiles.values.minOf { it.formationTime } }
    val youngestCrust by memo({ tectonicAge }) { planetTiles.values.maxOf { it.formationTime } }

    init {
        planetTiles.values.forEach {
            it.planetInit()
        }
        tectonicPlates = Tectonics.seedPlates(this, random.nextInt(10, 15))
        Tectonics.voronoiPlates(this)
        Tectonics.assignDensities(this)
        tectonicPlates.forEach { plate ->
            plate.torque = noise.mantleConvection.sample4d(plate.region.tiles.first().tile.position, 0.0)
        }
    }

    fun makeTopology(degree: Int): Topology {
        val icosahedron = makeIcosahedron()
        val sub = icosahedron.subdivideIcosahedron(degree)
        sub.distortTriangles(0.5)
        sub.relaxRepeatedly(500)
        sub.reorderVerts()
        return sub.toTopology()
    }
}

class PlanetDeserializer(val mapper: ObjectMapper) : JsonDeserializer<Planet>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Planet {
        val node = p.codec.readTree<JsonNode>(p)

        // Step 1: partially deserialize to get seed and size
        val seed = node["seed"].asInt()
        val size = node["size"].asInt()
        val planet = Planet(seed, size) // create instance manually

        // Step 2: deserialize the rest into the existing instance
        val injectables = InjectableValues.Std()
        injectables.addValue(Planet::class.java, planet)  // inject the planet instance
        mapper.setInjectableValues(injectables)

        mapper.readerForUpdating(planet).readValue<JsonNode>(node)

        return planet
    }
}