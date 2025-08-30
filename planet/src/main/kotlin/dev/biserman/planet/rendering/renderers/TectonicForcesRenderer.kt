package dev.biserman.planet.rendering.renderers

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.DebugRenderer
import dev.biserman.planet.rendering.MeshData
import dev.biserman.planet.rendering.vectorMesh
import godot.api.Node

class TectonicForcesRenderer(parent: Node, val lift: Double, override val visibleByDefault: Boolean = false) :
    DebugRenderer<Planet>(parent) {
    override val name = "tectonic_forces"

    override fun generateMeshes(input: Planet): List<MeshData> =
        input.tectonicPlates.flatMap { plate ->
            val edgeVectors = plate.tiles.filter { it.plateBoundaryForces.length() > 0.00005 }.map {
                DebugVector(
                    it.tile.position * lift,
                    it.plateBoundaryForces * 100
                )
            }

            vectorMesh(edgeVectors, plate.debugColor)
        }
}