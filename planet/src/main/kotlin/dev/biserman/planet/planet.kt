package dev.biserman.planet

import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.ArrayMesh
import godot.api.Mesh
import godot.api.MeshInstance3D
import godot.api.Node
import godot.core.VariantArray
import godot.core.Vector3
import godot.core.toVariantArray
import godot.global.GD
import kotlin.math.sqrt

// Adapted from Andy Gainey, original license below:
// Copyright © 2014 Andy Gainey <andy@experilous.com>
//
// Usage of the works is permitted provided that this instrument
// is retained with the works, so that any entity that uses the
// works is notified of this instrument.
//
// DISCLAIMER: THE WORKS ARE WITHOUT WARRANTY.


@RegisterClass
class Planet : MeshInstance3D() {
	@RegisterFunction
	override fun _process(delta: Double) {
		rotation += delta / 10
	}
}
