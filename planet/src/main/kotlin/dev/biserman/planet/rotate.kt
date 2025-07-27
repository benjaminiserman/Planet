package dev.biserman.planet

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.MeshInstance3D

// Adapted from Andy Gainey, original license below:
// Copyright © 2014 Andy Gainey <andy@experilous.com>
//
// Usage of the works is permitted provided that this instrument
// is retained with the works, so that any entity that uses the
// works is notified of this instrument.
//
// DISCLAIMER: THE WORKS ARE WITHOUT WARRANTY.


@RegisterClass
class Rotate : MeshInstance3D() {
	@RegisterFunction
	override fun _process(delta: Double) {
		rotation += delta / 10
	}
}
