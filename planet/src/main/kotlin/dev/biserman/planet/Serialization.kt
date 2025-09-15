package dev.biserman.planet

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import dev.biserman.planet.planet.*
import dev.biserman.planet.topology.Border
import dev.biserman.planet.topology.Corner
import dev.biserman.planet.topology.MutBorder
import dev.biserman.planet.topology.MutCorner
import dev.biserman.planet.topology.MutTile
import dev.biserman.planet.topology.Tile
import dev.biserman.planet.topology.Topology
import dev.biserman.planet.utils.TrackedMutableSet
import godot.api.FastNoiseLite
import godot.core.Color
import godot.core.Vector2
import godot.core.Vector3
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import kotlin.jvm.java

object Serialization {
    val kryo = Kryo()

    init {
        kryo.references = true
        kryo.setMaxDepth(500)

        kryo.register(Planet::class.java)
        kryo.register(PlanetTile::class.java)
        kryo.register(TectonicPlate::class.java)
        kryo.register(ConvergenceZone::class.java)
        kryo.register(DivergenceZone::class.java)
        kryo.register(PlanetRegion::class.java)
        kryo.register(PlanetStats::class.java)

        kryo.register(NoiseMaps::class.java)

        kryo.register(Topology::class.java)
        kryo.register(MutTile::class.java)
        kryo.register(MutBorder::class.java)
        kryo.register(MutCorner::class.java)

        kryo.register(TrackedMutableSet::class.java)

        kryo.register(Vector2::class.java)
        kryo.register(Vector3::class.java)
        kryo.register(Color::class.java)

        kryo.register(Arrays.asList("").javaClass)
        kryo.register(ArrayList::class.java)
        kryo.register(LinkedHashMap::class.java)
        kryo.register(HashMap::class.java)

//        kryo.isRegistrationRequired = false
//        kryo.setWarnUnregisteredClasses(true)

//        kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())
    }

    fun save(planet: Planet) {
        val output = Output(FileOutputStream("planet.bin"))
        kryo.writeObject(output, planet)
        output.close()
    }

    fun load(): Planet {
        val input = Input(FileInputStream("planet.bin"))
        val planet = kryo.readObject(input, Planet::class.java)
        input.close()

        return planet
    }
}