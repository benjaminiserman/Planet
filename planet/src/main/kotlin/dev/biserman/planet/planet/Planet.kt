package dev.biserman.planet.planet

import com.esotericsoftware.kryo.DefaultSerializer
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.KryoSerializable
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import dev.biserman.planet.Main
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.NoArg
import dev.biserman.planet.utils.memo

@NoArg
@DefaultSerializer(CompatibleFieldSerializer::class)
class Planet(@Transient val topology: Topology) {
    @Transient
    val random = Main.random
    @Transient
    val noise = Main.noise
    var planetTiles = topology.tiles.associateWith { PlanetTile(this, it) }
    val planetStats = PlanetStats()

    @Suppress("JoinDeclarationAndAssignment")
    var tectonicPlates: MutableList<TectonicPlate>
    var convergenceZones: MutableMap<Tile, ConvergenceZone> = mutableMapOf()
    var divergenceZones: MutableMap<Tile, DivergenceZone> = mutableMapOf()

    var tectonicAge = 0

    val seaLevel: Double = 0.0

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
}