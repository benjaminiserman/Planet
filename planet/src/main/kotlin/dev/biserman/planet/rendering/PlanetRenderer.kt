package dev.biserman.planet.rendering

import dev.biserman.planet.geometry.adjustRange
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.DebugDraw.drawMesh
import godot.api.Node
import godot.api.StandardMaterial3D
import godot.core.Color
import godot.global.GD

class PlanetRenderer(val parent: Node) {
    fun levelIt(level: Float) = when {
        level < 0.8f -> level * 0.5f
        level >= 1.0f -> 1.0f
        else -> level - 0.25f
    }

    fun saturation(level: Float) = when {
        level >= 1.0f -> 0.0f
        else -> 0.9f
    }

    fun update(planet: Planet) {
        parent.drawMesh("Planet", planet.topology.makeMesh(enrich = { mesh, tile ->
            val planetTile = planet.planetTiles[tile] ?: return@makeMesh
            val level = planetTile.elevation.adjustRange(-0.5f..0.5f, 0.0f..1.0f)
            val hue = planetTile.tectonicPlate?.biomeColor?.h ?: 0.0
//			val hue = noise.getNoise3dv(planetTile.tile.position * 100).toDouble()
            var color = Color.fromHsv(hue, saturation(level).toDouble(), levelIt(level).toDouble(), level.toDouble())
            if (level < 0.7) {
                color = Color.fromHsv(0.7, saturation(level).toDouble(), levelIt(level).toDouble(), level.toDouble())
            }
            mesh.colors.add(color)

            mesh.colors.addAll((0..<tile.corners.size).map {
                val level =
                    (tile.corners[it].tiles.map { tile -> planet.planetTiles[tile]!!.elevation }.toFloatArray()
                        .sum() / tile.corners[it].tiles.size)
                        .adjustRange(-0.5f..0.5f, 0.0f..1.0f)
                val color = Color.fromHsv(
                    color.h,
                    color.s,
                    levelIt(level).toDouble(),
                    level.toDouble()
                )

                color
            })
        }).apply {
            this.recalculateNormals()
        }.toArrayMesh(), GD.load<StandardMaterial3D>("res://planet_mat.tres"))
    }
}