package dev.biserman.planet

import dev.biserman.planet.geometry.intersectRaySphere
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.gui.Gui
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.Camera3D
import godot.api.Input
import godot.api.InputEvent
import godot.api.InputEventMouse
import godot.api.InputEventMouseButton
import godot.api.InputEventMouseMotion
import godot.api.Node3D
import godot.api.RayCast3D
import godot.core.MouseButton
import godot.core.Vector2
import godot.core.Vector3
import godot.global.GD
import kotlin.math.PI
import kotlin.math.sqrt

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
	val camera by lazy { innerGimbal.findChild("Camera3D") as Camera3D }

	lateinit var clickStartPosition: Vector2

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

		if (event is InputEventMouseButton && Input.isActionPressed("click")) {
			clickStartPosition = event.position
		}

		if (mouseControl && event is InputEventMouseMotion && Input.isMouseButtonPressed(MouseButton.LEFT)) {
			if (event.relative.x != 0.0) {
				rotateObjectLocal(Vector3.UP, -event.relative.x.toFloat() * mouseSensitivity)
			}

			if (event.relative.y != 0.0) {
				innerGimbal.rotateObjectLocal(Vector3.RIGHT, -event.relative.y.toFloat() * mouseSensitivity)
			}
		}

		if (event is InputEventMouse &&
			Input.isActionJustReleased("click") &&
			event.position.distanceTo(clickStartPosition) == 0.0
		) {
			val origin = camera.projectRayOrigin(event.position)
			val normal = camera.projectRayNormal(event.position)

			val t = intersectRaySphere(origin, normal, Vector3.ZERO, 1.0)
			if (t != null) {
				val hitPoint = origin + normal * t
				target = hitPoint
				val planet = Main.instance.planet
				val selectedTile =
					planet.topology.rTree
						.nearest(hitPoint.toPoint(), planet.topology.averageRadius * 2, 1)
						.first()
						.value()
				Gui.instance.selectedTile = selectedTile
			} else {
				Gui.instance.selectedTile = null
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
