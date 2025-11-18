package dev.biserman.planet.rendering

import dev.biserman.planet.geometry.*
import dev.biserman.planet.planet.climate.ClimateSimulation
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.rendering.colormodes.BiomeColorMode
import dev.biserman.planet.rendering.colormodes.SimpleColorMode
import dev.biserman.planet.rendering.colormodes.SimpleDoubleColorMode
import dev.biserman.planet.rendering.colormodes.SimpleDoubleColorMode.Companion.redOutsideRange
import dev.biserman.planet.rendering.colormodes.SimpleDoubleColorMode.Companion.redWhenNull
import dev.biserman.planet.rendering.renderers.CellWireframeRenderer
import dev.biserman.planet.rendering.renderers.TectonicPlateBoundaryRenderer
import dev.biserman.planet.rendering.renderers.TileMovementRenderer
import dev.biserman.planet.rendering.renderers.TileVectorRenderer
import dev.biserman.planet.utils.UtilityExtensions.degToRad
import dev.biserman.planet.utils.alphaAverage
import dev.biserman.planet.utils.sum
import dev.biserman.planet.utils.transparent
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.core.Vector3
import godot.global.GD
import kotlin.collections.average
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.measureTime

class PlanetRenderer(parent: Node, var planet: Planet) {
    val planetDebugRenders = listOf(
        CellWireframeRenderer(parent, lift = 1.005, visibleByDefault = false),
        SimpleDebugRenderer(parent, "tectonic_boundary_movement") { planet ->
            planet.tectonicPlates.flatMap { plate ->
                val edgeVectors = plate.tiles.filter { it.isTectonicBoundary }
                    .filter { it.movement.length() > 0.00005 }
                    .map { DebugVector(it.tile.position * 1.005, it.movement) }
                vectorMesh(edgeVectors, plate.debugColor)
            }
        },
        TectonicPlateBoundaryRenderer(parent, lift = 1.005, visibleByDefault = false),
        TileMovementRenderer(parent, lift = 1.005, visibleByDefault = false),
        TileVectorRenderer(
            parent,
            "mantle_convection",
            lift = 1.005,
            getFn = { planet.noise.mantleConvection.sample4d(it.tile.position, planet.tectonicAge.toDouble()) },
            color = Color(1.0, 0.5, 0.0, 1.0),
            visibleByDefault = false
        ),
        TileVectorRenderer(
            parent, "spring_displacement", lift = 1.005, getFn = { it.springDisplacement }, visibleByDefault = false
        ),
        TileVectorRenderer(
            parent,
            "edge_interaction",
            lift = 1.005,
            getFn = { it.getEdgeForces().sum() * 10 },
            visibleByDefault = false
        ),
        SimpleDebugRenderer(parent, "rivers") { planet ->
            val pointElevations = planet.planetTiles.values.flatMap { it.tile.corners }
                .distinctBy { it.position }
                .associateWith { it.tiles.map { tile -> planet.getTile(tile).elevation }.average() }

            val riverSegments =
                pointElevations.keys.map { it to it.corners.minBy { corner -> pointElevations[corner]!! } }
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
                    MutMesh(verts, edges).toWireframe(), StandardMaterial3D().apply { this.albedoColor = Color.blue })
            )
        },
        SimpleDebugRenderer(parent, "climate_cells") { planet ->
            val bands = listOf(-60, -30, 0, +30, +60).map { it.toDouble() }
            val verts = mutableListOf<MutVertex>()
            val edges = mutableListOf<MutEdge>()
            val lift = 1.001

            bands.forEach { band ->
                val y = sin(band.degToRad())
                val radius = cos(band.degToRad()) * lift
                val circumference = 2 * radius * PI
                val lineSegments = (circumference / planet.topology.averageRadius).toInt()
                val firstVert = verts.size

                for (i in 0..<lineSegments) {
                    val angle = i * 2 * PI / lineSegments
                    val x = radius * cos(angle)
                    val z = radius * sin(angle)
                    if (i != 0) {
                        edges.add(MutEdge(mutableListOf(verts.size - 1, verts.size)))
                    }
                    verts.add(MutVertex(Vector3(x, y, z)))
                }
                edges.add(MutEdge(mutableListOf(verts.size - 1, firstVert)))
            }

            listOf(
                MeshData(
                    MutMesh(verts, edges).toWireframe(), StandardMaterial3D().apply { this.albedoColor = Color.white }
                )
            )
        },
        TileVectorRenderer(
            parent,
            "wind",
            lift = 1.005,
            getFn = { it.prevailingWind * 2.5 },
            visibleByDefault = false
        ),
    )

    val planetColorModes = listOf(
        BiomeColorMode(this, visibleByDefault = true),
        SimpleColorMode(
            this, "fast_biome", visibleByDefault = false
        ) { if (it.isAboveWater) Color.darkGreen else Color.darkBlue },
        SimpleDoubleColorMode(
            this, "elevation", visibleByDefault = false,
        ) {
            it.elevation.scaleAndCoerceIn(-10000.0..10000.0, 0.0..1.0)
        },
        SimpleDoubleColorMode(
            this, "density", visibleByDefault = false, colorFn = redOutsideRange(-1.0..1.0)
        ) { it.density.adjustRange(-1.0..1.0, 0.0..1.0) },
        SimpleDoubleColorMode(
            this, "mountain_elevation", visibleByDefault = false, colorFn = redOutsideRange(-1.0..1.0)
        ) { it.elevation.scaleAndCoerceIn(2000.0..12000.0, 0.0..1.0) },
        SimpleDoubleColorMode(
            this, "plate_density", visibleByDefault = false, colorFn = redOutsideRange(-1.0..1.0)
        ) { it.tectonicPlate?.density?.adjustRange(-1.0..1.0, 0.0..1.0) },
        SimpleDoubleColorMode(
            this,
            "temperature",
            visibleByDefault = false,
            colorFn = redWhenNull { Color(it, 0.0, 0.0, 1.0) }) { it.temperature },
        SimpleDoubleColorMode(
            this,
            "moisture",
            visibleByDefault = false,
            colorFn = redWhenNull { Color(0.0, 0.0, it, 1.0) }) { it.moisture },
        SimpleDoubleColorMode(
            this,
            "hotspots",
            visibleByDefault = false,
            colorFn = redWhenNull { Color(it, it / 2.0, 0.0, 1.0) }) {
            planet.noise.hotspots.sample4d(
                it.tile.position,
                planet.tectonicAge.toDouble()
            )
        },
        SimpleColorMode(
            this, "convergence_zones", visibleByDefault = false,
        ) {
            val convergenceZone = planet.convergenceZones[it.tile.id] ?: return@SimpleColorMode null
            val subductionStrength =
                convergenceZone.subductionStrengths[it.tectonicPlate?.id ?: return@SimpleColorMode null]
                    ?: return@SimpleColorMode null
            val strengthFactor = (convergenceZone.speed * subductionStrength.absoluteValue).pow(0.5)
            (if (subductionStrength > 0) Color.blue else Color.green) * strengthFactor
        },
        SimpleColorMode(
            this, "divergence_zones", visibleByDefault = false,
        ) { if (it.tile.id in planet.divergenceZones) Color.red * planet.divergenceZones[it.tile.id]!!.strength else null },
        SimpleColorMode(
            this, "tectonic_plates", visibleByDefault = false,
        ) { it.tectonicPlate?.debugColor ?: Color.black },
        SimpleColorMode(
            this, "slope", visibleByDefault = false,
        ) { Color.white * it.slope.scaleAndCoerceIn(0.0..2000.0, 0.0..1.0) },
        SimpleColorMode(
            this, "contiguous_slope", visibleByDefault = false,
        ) { Color.white * it.contiguousSlope.scaleAndCoerceIn(0.0..2000.0, 0.0..1.0) },
        SimpleColorMode(
            this, "non_contiguous_slope", visibleByDefault = false,
        ) { Color.white * it.nonContiguousSlope.scaleAndCoerceIn(0.0..2000.0, 0.0..1.0) },
        SimpleColorMode(
            this, "prominence", visibleByDefault = false,
        ) { Color.white * it.prominence.scaleAndCoerceIn(0.0..2000.0, 0.0..1.0) },
        SimpleColorMode(
            this, "erosion", visibleByDefault = false,
        ) {
            val scaled = it.erosionDelta.scaleAndCoerceIn(-50.0..50.0, -1.0..1.0).absoluteValue
            when {
                it.erosionDelta < 0 -> Color.red * scaled
                it.erosionDelta > 0 -> Color.blue * scaled
                else -> Color.gray
            }
        },
        SimpleColorMode(
            this, "deposit_flow", visibleByDefault = false,
        ) { Color.green * it.depositFlow.scaleAndCoerceIn(0.0..250.0, 0.0..1.0) },
        SimpleColorMode(
            this, "water_flow", visibleByDefault = false,
        ) { Color.blue * it.waterFlow.scaleAndCoerceIn(0.0..50.0, 0.0..1.0) },
        SimpleColorMode(
            this, "crust_age", visibleByDefault = false,
        ) {
            Color.white * it.formationTime.toDouble()
                .scaleAndCoerceIn(planet.oldestCrust.toDouble()..planet.youngestCrust.toDouble(), 0.0..1.0)
        },
        SimpleColorMode(
            this, "insolation", visibleByDefault = false,
        ) { planetTile ->
            Color.black.transparent.lerp(Color.orange, planetTile.insolation)
        },
        SimpleColorMode(
            this, "edge_depth", visibleByDefault = false,
        ) { planetTile ->
            Color.white * (planetTile.edgeDepth / 40.0)
        },
        SimpleColorMode(
            this, "continentiality", visibleByDefault = false,
        ) { planetTile ->
            if (planetTile.continentiality >= 0) Color.red * (planetTile.continentiality / 40.0)
            else Color.blue * (-planetTile.continentiality / 40.0)
        },
        SimpleColorMode(this, "air_pressure", visibleByDefault = false) { planetTile ->
            val airPressure = planetTile.airPressure
            if (airPressure <= ClimateSimulation.basePressure) {
                Color.black.lerp(Color.blue, ((airPressure - ClimateSimulation.basePressure) / 25).pow(2))
            } else {
                Color.black.lerp(
                    Color.darkorange,
                    ((ClimateSimulation.basePressure - airPressure) / 25).pow(2)
                )
            }
        },
        SimpleColorMode(this, "ocean_currents", visibleByDefault = false) {
            val current = planet.oceanCurrents[it.tile.id] ?: return@SimpleColorMode null
            if (current.temperature > 0) Color.red * current.temperature
            else Color.blue * current.temperature
        },
        SimpleColorMode(this, "debug_color", visibleByDefault = false) { it.debugColor },
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

    fun getColor(planetTile: PlanetTile): Color {
        val colors = planetColorModes.filter { it.visible }
            .mapNotNull { mode -> mode.colorsFor(planetTile).first() }
        return colors.alphaAverage()
    }

    fun updateMesh() {
        val colorModeResults = planetColorModes.filter { it.visible }.map { mode ->
            planet.topology.tiles.flatMap { tile -> mode.colorsFor(planet.getTile(tile)) }
        }

        val colors = if (colorModeResults.isNotEmpty()) {
            val resultsSize = colorModeResults.first().size
            if (colorModeResults.any { it.size != resultsSize }) {
                throw IllegalStateException("Color mode results don't match")
            }

            (0..<resultsSize).map { i ->
                colorModeResults.mapNotNull { it[i] }.alphaAverage()
            }
        } else listOf()

        meshInstance.setMesh(planet.topology.makeMesh().apply {
            this.recalculateNormals()
            this.colors.addAll(colors)
        }.toArrayMesh())
        meshInstance.setSurfaceOverrideMaterial(0, GD.load<StandardMaterial3D>("res://planet_mat.tres"))
    }
}