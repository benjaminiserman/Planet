package dev.biserman.planet

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.MeshInstance3D

@RegisterClass
class Rotate : MeshInstance3D() {
    @RegisterFunction
    override fun _process(delta: Double) {
        rotation += delta / 10
    }
}