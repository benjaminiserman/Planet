package dev.biserman.planet.rendering.renderers

import dev.biserman.planet.geometry.DebugVector
import dev.biserman.planet.geometry.tangent
import dev.biserman.planet.planet.Planet
import dev.biserman.planet.rendering.MeshData
import dev.biserman.planet.rendering.DebugRenderer
import dev.biserman.planet.rendering.rotVectorMesh
import godot.api.Node

class TectonicForcesRenderer(parent: Node, val lift: Double, override val visibleByDefault: Boolean = false) :
    DebugRenderer<Planet>(parent) {
    override val name = "tectonic_forces"

    override fun generateMeshes(input: Planet): List<MeshData> =
        input.tectonicPlates.flatMap { plate ->
            val edgeVectors = plate.tiles.filter { it.plateBoundaryForces.length() > 0.005 }.flatMap {
                listOf(
                    Pair(
                        DebugVector(
                            it.tile.position * lift,
                            it.plateBoundaryForces
                        ), it.rotationalForce * 0.3
                    )
                )
            }

            val overallMovementVectors = listOf(plate.averageForce).map {
                Pair(
                    DebugVector(
                        plate.region.center * lift,
                        plate.averageForce.tangent(plate.region.center * lift) * 100,
                    ), plate.averageRotation * 10
                )
            }

            rotVectorMesh(edgeVectors, plate.debugColor) + rotVectorMesh(overallMovementVectors, plate.debugColor)
        }
}