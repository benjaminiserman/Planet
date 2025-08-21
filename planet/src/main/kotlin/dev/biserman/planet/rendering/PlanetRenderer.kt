package dev.biserman.planet.rendering

import dev.biserman.planet.Main
import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.colormodes.BiomeColorMode
import dev.biserman.planet.rendering.colormodes.SimpleColorMode
import dev.biserman.planet.rendering.colormodes.SimpleColorMode.Companion.redOutsideRange
import dev.biserman.planet.rendering.colormodes.SimpleColorMode.Companion.redWhenNull
import dev.biserman.planet.rendering.renderers.CellWireframeRenderer
import dev.biserman.planet.rendering.renderers.TectonicForcesRenderer
import dev.biserman.planet.rendering.renderers.TectonicPlateBoundaryRenderer
import dev.biserman.planet.rendering.renderers.TileMovementRenderer
import godot.api.MeshInstance3D
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.global.GD

class PlanetRenderer(parent: Node, var planet: Planet? = null) {
    val planetDebugRenders = listOf(
        CellWireframeRenderer(parent, lift = 1.005, visibleByDefault = false),
        TectonicForcesRenderer(parent, lift = 1.005, visibleByDefault = true),
        TectonicPlateBoundaryRenderer(parent, lift = 1.005, visibleByDefault = true),
        TileMovementRenderer(parent, lift = 1.005, visibleByDefault = false)
    )

    val planetColorModes = listOf(
        BiomeColorMode(this, visibleByDefault = true),
        SimpleColorMode(
            this, "elevation", visibleByDefault = false,
//        ) { 1.0 / (1 + E.pow((-it.elevation.toDouble() + 0.25) * 10)) },
        ) { it.elevation.toDouble().adjustRange(-1000.0..1000.0, 0.0..1.0) },
        SimpleColorMode(
            this, "plate_density", visibleByDefault = false,
            colorFn = redOutsideRange(0.0..1.0)
        )  { it.tectonicPlate?.density?.toDouble()?.adjustRange(-1.0..1.0, 0.0..1.0) },
        SimpleColorMode(
            this, "temperature", visibleByDefault = false,
            colorFn = redWhenNull { Color(it, 0.0, 0.0, 1.0) }
        ) { it.temperature },
        SimpleColorMode(
            this, "hotspots", visibleByDefault = false,
            colorFn = redWhenNull { Color(it, it / 2.0, 0.0, 1.0) }
        ) { Main.noise.hotspots.sample4d(it.tile.position, 0.0) }
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

        planetDebugRenders.forEach {
            it.update(planet)
        }
    }

    fun updateMesh() {
        val planet = planet ?: return

        val colorModeResults = planetColorModes.filter { it.visible }.map { mode ->
            planet.topology.tiles.flatMap { tile -> mode.colorsFor(planet.planetTiles[tile]!!) }
        }

        val colors = if (colorModeResults.isNotEmpty()) {
            val resultsSize = colorModeResults.first().size
            if (colorModeResults.any { it.size != resultsSize }) {
                throw IllegalStateException("Color mode results don't match")
            }

            (0..<resultsSize).map { i ->
                colorModeResults.fold(
                    Color(
                        0.0, 0.0, 0.0, 0.0
                    )
                ) { acc, list -> acc + list[i] } / colorModeResults.size.toDouble()
            }
        } else listOf()

        meshInstance.setMesh(planet.topology.makeMesh().apply {
            this.recalculateNormals()
            this.colors.addAll(colors)
        }.toArrayMesh())
        meshInstance.setSurfaceOverrideMaterial(0, GD.load<StandardMaterial3D>("res://planet_mat.tres"))
    }
}