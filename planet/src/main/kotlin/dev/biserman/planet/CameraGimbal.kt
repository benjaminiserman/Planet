package dev.biserman.planet

import dev.biserman.planet.geometry.intersectRaySphere
import dev.biserman.planet.geometry.toPoint
import dev.biserman.planet.gui.Gui
import dev.biserman.planet.planet.PlanetTile
import dev.biserman.planet.topology.Tile
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.MouseButton
import godot.core.Vector2
import godot.core.Vector3
import godot.global.GD
import kotlin.math.PI
import kotlin.times

@RegisterClass
class CameraGimbal : Node3D() {
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

	var clickStartPosition: Vector2? = null

	val hoveredPoint
		get(): Vector3? {
			val viewport = getViewport()!!
			val origin = camera.projectRayOrigin(viewport.getMousePosition())
			val normal = camera.projectRayNormal(viewport.getMousePosition())

			val t = intersectRaySphere(origin, normal, Vector3.ZERO, 1.0)
			return if (t == null) null
			else origin + normal * t
		}

	val hoveredTile
		get(): Tile? {
			val hitPoint = hoveredPoint ?: return null
			val planet = Main.instance.planet
			return planet.topology.rTree
				.nearest(hitPoint.toPoint(), planet.topology.averageRadius * 2, 1)
				.first()
				.value()
		}

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

		val clickStart = clickStartPosition
		if (event is InputEventMouse &&
			Input.isActionJustReleased("click") &&
			clickStart != null &&
			event.position.distanceTo(clickStart) == 0.0
		) {
			Gui.instance.selectedTile = hoveredTile
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
		globalTransform.origin = hoveredPoint ?: Vector3.ZERO
		innerGimbal.setRotation(
			Vector3(
				GD.clamp(innerGimbal.rotation.x, -PI / 2, PI / 2),
				innerGimbal.rotation.y,
				innerGimbal.rotation.z
			)
		)
	}

	@RegisterFunction
	override fun _ready() {
		instance = this
	}

	companion object {
		lateinit var instance: CameraGimbal
	}
}
