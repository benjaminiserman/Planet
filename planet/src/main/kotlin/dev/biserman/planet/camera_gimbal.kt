package dev.biserman.planet

import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.Input
import godot.api.InputEvent
import godot.api.InputEventMouseMotion
import godot.api.Node3D
import godot.core.MouseButton
import godot.core.Vector3
import godot.global.GD
import kotlin.math.PI

@RegisterClass
class CameraGimbal : Node3D() {
	@Export
	@RegisterProperty
	var target = Vector3.ZERO

	@Export
	@RegisterProperty
	var rotationSpeed = PI.toFloat() / 3

	@Export
	@RegisterProperty
	var mouseControl = true

	@Export
	@RegisterProperty
	var mouseSensitivity = 0.005f

	@Export
	@RegisterProperty
	var maxZoom = 3.0f

	@Export
	@RegisterProperty
	var minZoom = 0.5f

	@Export
	@RegisterProperty
	var zoomSpeed = 0.09f

	var zoom = 1.5f

	val innerGimbal by lazy { findChild("InnerGimbal") as Node3D }

	@RegisterFunction
	override fun _unhandledInput(event: InputEvent?) {
		if (event == null) {
			return
		}

		zoom = GD.clamp(
			zoom + when {
				event.isActionPressed("cam_zoom_in") -> -zoomSpeed
				event.isActionPressed("cam_zoom_out") -> +zoomSpeed
				else -> 0f
			}, minZoom, maxZoom
		)

		if (mouseControl && event is InputEventMouseMotion && Input.isMouseButtonPressed(MouseButton.LEFT)) {
			if (event.relative.x != 0.0) {
				rotateObjectLocal(Vector3.UP, -event.relative.x.toFloat() * mouseSensitivity)
			}

			if (event.relative.y != 0.0) {
				innerGimbal.rotateObjectLocal(Vector3.RIGHT, -event.relative.y.toFloat() * mouseSensitivity)
			}
		}
	}

	fun handleKeyboardInput(delta: Double) {
		val yRotation = when {
			Input.isActionPressed("cam_right") -> +1f
			Input.isActionPressed("cam_left") -> -1f
			else -> 0f
		}
		rotateObjectLocal(Vector3.UP, yRotation * rotationSpeed * delta.toFloat())

		val xRotation = when {
			Input.isActionPressed("cam_down") -> +1f
			Input.isActionPressed("cam_up") -> -1f
			else -> 0f
		}
		innerGimbal.rotateObjectLocal(Vector3.RIGHT, xRotation * rotationSpeed * delta.toFloat())
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		handleKeyboardInput(delta)
		scale = GD.lerp(scale, Vector3.ONE * zoom, zoomSpeed)
		globalTransform.origin = target
		innerGimbal.setRotation(
			Vector3(
				GD.clamp(innerGimbal.rotation.x, -PI.toDouble() / 2, PI.toDouble() / 2),
				innerGimbal.rotation.y,
				innerGimbal.rotation.z
			)
		)
	}
}
