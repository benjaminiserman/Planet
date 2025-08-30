package dev.biserman.planet.gui

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.CheckButton

@RegisterClass
class ToggleButton(val default: Boolean = false, val onClick: ((Boolean) -> Any)? = null) : CheckButton() {
    constructor() : this(false, null)

    @RegisterFunction
    override fun _ready() {
        this.toggleMode = true
        this.buttonPressed = default
        this.focusMode = FocusMode.NONE
    }

    @RegisterFunction
    override fun _pressed() {
        onClick?.invoke(this.buttonPressed)
    }
}