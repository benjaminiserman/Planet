package dev.biserman.planet.rendering

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.MutEdge
import dev.biserman.planet.geometry.MutMesh
import dev.biserman.planet.geometry.MutVertex
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.geometry.scaleAndCoerce01
import dev.biserman.planet.geometry.scaleAndCoerceIn
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.colormodes.BiomeColorMode
import dev.biserman.planet.rendering.colormodes.SimpleColorMode
import dev.biserman.planet.rendering.colormodes.SimpleDoubleColorMode
import dev.biserman.planet.rendering.colormodes.SimpleDoubleColorMode.Companion.redOutsideRange
import dev.biserman.planet.rendering.colormodes.SimpleDoubleColorMode.Companion.redWhenNull
import dev.biserman.planet.rendering.renderers.*
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.global.GD
import kotlin.math.absoluteValue
import kotlin.time.measureTime

class PlanetRenderer(parent: Node, var planet: Planet) {
    val planetDebugRenders = listOf(
        CellWireframeRenderer(parent, lift = 1.005, visibleByDefault = false),
        TectonicForcesRenderer(parent, lift = 1.005, visibleByDefault = false),
        TectonicPlateBoundaryRenderer(parent, lift = 1.005, visibleByDefault = false),
        TileMovementRenderer(parent, lift = 1.005, visibleByDefault = false),
        TileVectorRenderer(
            parent,
            "mantle_convection",
            lift = 1.005,
            getFn = { Main.noise.mantleConvection.sample4d(it.tile.position, planet.tectonicAge.toDouble()) },
            color = Color(1.0, 0.5, 0.0, 1.0),
            visibleByDefault = false
        ),
        TileVectorRenderer(
            parent,
            "spring_displacement",
            lift = 1.005,
            getFn = { it.springDisplacement },
            visibleByDefault = false
        ),
        SimpleDebugRenderer(parent, "rivers") { planet ->
            val pointElevations = planet.planetTiles.values
                .flatMap { it.tile.corners }
                .distinctBy { it.position }
                .associateWith { it.tiles.map { tile -> planet.planetTiles[tile]!!.elevation }.average() }

            val riverSegments = pointElevations.keys
                .map { it to it.corners.minBy { corner -> pointElevations[corner]!! } }
                .filter { pointElevations[it.first]!! > 0 || pointElevations[it.second]!! > 0 }

            val verts = mutableListOf<MutVertex>()
            val edges = mutableListOf<MutEdge>()

            val lift = 1.001
            for (segment in riverSegments) {
                val length = verts.size
                verts.add(MutVertex(segment.first.position * lift))
                verts.add(MutVertex(segment.second.position * lift))
                edges.add(MutEdge(mutableListOf(length, length + 1)))
            }

            listOf(
                MeshData(
                    MutMesh(verts, edges).toWireframe(),
                    StandardMaterial3D().apply { this.albedoColor = Color.blue })
            )
        }
    )

    val planetColorModes = listOf(
        BiomeColorMode(this, visibleByDefault = true),
        SimpleDoubleColorMode(
            this, "elevation", visibleByDefault = false,
        ) {
            it.elevation
                .scaleAndCoerceIn(-5000.0..5000.0, 0.0..1.0)
        },
        SimpleDoubleColorMode(
            this, "density", visibleByDefault = false,
            colorFn = redOutsideRange(-1.0..1.0)
        ) { it.density.adjustRange(-1.0..1.0, 0.0..1.0) },
        SimpleDoubleColorMode(
            this, "mountain_elevation", visibleByDefault = false,
            colorFn = redOutsideRange(-1.0..1.0)
        ) { it.elevation.scaleAndCoerceIn(2000.0..12000.0, 0.0..1.0) },
        SimpleDoubleColorMode(
            this, "plate_density", visibleByDefault = false,
            colorFn = redOutsideRange(-1.0..1.0)
        ) { it.tectonicPlate?.density?.adjustRange(-1.0..1.0, 0.0..1.0) },
        SimpleDoubleColorMode(
            this, "temperature", visibleByDefault = false,
            colorFn = redWhenNull { Color(it, 0.0, 0.0, 1.0) }
        ) { it.temperature },
        SimpleDoubleColorMode(
            this, "hotspots", visibleByDefault = false,
            colorFn = redWhenNull { Color(it, it / 2.0, 0.0, 1.0) }
        ) { Main.noise.hotspots.sample4d(it.tile.position, planet.tectonicAge.toDouble()) },
        SimpleColorMode(
            this, "subduction_zones", visibleByDefault = false,
        ) { if (it.tile in planet.subductionZones) Color.blue * planet.subductionZones[it.tile]!!.speed else null },
        SimpleColorMode(
            this, "divergence_zones", visibleByDefault = false,
        ) { if (it.tile in planet.divergenceZones) Color.red * planet.divergenceZones[it.tile]!!.strength else null },
        SimpleColorMode(
            this, "tectonic_plates", visibleByDefault = false,
        ) { it.tectonicPlate?.debugColor ?: Color.black },
        SimpleColorMode(
            this, "slope", visibleByDefault = false,
        ) { Color.white * it.slope.scaleAndCoerceIn(0.0..500.0, 0.0..1.0) },
        SimpleColorMode(
            this, "prominence", visibleByDefault = false,
        ) { Color.white * it.prominence.scaleAndCoerceIn(0.0..500.0, 0.0..1.0) },
        SimpleColorMode(
            this, "erosion", visibleByDefault = false,
        ) {
            val scaled = it.erosionDelta
                .scaleAndCoerceIn(-50.0..50.0, -1.0..1.0)
                .absoluteValue
            when {
                it.erosionDelta < 0 -> Color.red * scaled
                it.erosionDelta > 0 -> Color.blue * scaled
                else -> Color.gray
            }
        },
        SimpleColorMode(
            this, "crust_age", visibleByDefault = false,
        ) {
            Color.white * it.formationTime.toDouble()
                .scaleAndCoerceIn(planet.oldestCrust.toDouble()..planet.youngestCrust.toDouble(), 0.0..1.0)
        },

        )

    val meshInstance = MeshInstance3D().also { it.setName("Planet") }

    init {
        parent.addChild(meshInstance, forceReadableName = true)
        planetDebugRenders.forEach { it.init() }
        planetColorModes.forEach { it.init() }
    }

    fun update(planet: Planet) {
        this.planet = planet
        updateMesh()

        val timeTaken = measureTime {
            planetDebugRenders.forEach {
                it.update(planet)
            }
        }

        GD.print("Updating renderers took ${timeTaken.inWholeMilliseconds}ms")
    }

    fun updateMesh() {
        val colorModeResults = planetColorModes.filter { it.visible }.map { mode ->
            planet.topology.tiles.flatMap { tile -> mode.colorsFor(planet.planetTiles[tile]!!) }
        }

        val colors = if (colorModeResults.isNotEmpty()) {
            val resultsSize = colorModeResults.first().size
            if (colorModeResults.any { it.size != resultsSize }) {
                throw IllegalStateException("Color mode results don't match")
            }

            (0..<resultsSize).map { i ->
                colorModeResults.filter { it[i] != null }
                    .fold(Color.black) { acc, list -> acc + list[i]!! } /
                        colorModeResults.filter { it[i] != null }.size.toDouble()
            }
        } else listOf()

        meshInstance.setMesh(planet.topology.makeMesh().apply {
            this.recalculateNormals()
            this.colors.addAll(colors)
        }.toArrayMesh())
        meshInstance.setSurfaceOverrideMaterial(0, GD.load<StandardMaterial3D>("res://planet_mat.tres"))
    }
}