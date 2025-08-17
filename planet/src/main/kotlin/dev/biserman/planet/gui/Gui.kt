package dev.biserman.planet.gui

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.CheckButton
import godot.api.Node
import godot.core.Vector2

@RegisterClass
class Gui : Node() {

    @RegisterFunction
    override fun _ready() {
        instance = this
        buttons.forEach { addChild(it) }
    }

    companion object {
        var instance: Gui? = null; private set
        val buttons = mutableListOf<CheckButton>()

        var settingsVisible = false
            set(value) {
                field = value
                buttons.forEach { it.setVisible(value) }
            }

        private val toggles = mutableMapOf<String, Boolean>()
        fun addToggle(toggle: String, defaultValue: Boolean = false, onClick: (Boolean) -> Any) {
            toggles[toggle] = defaultValue
            buttons += ToggleButton(default = defaultValue, onClick = {
                toggles[toggle] = it
                onClick(it)
            }).also {
                it.text = toggle
                it.setPosition(Vector2(0, 40 * (buttons.size + 1)))
                it.setVisible(settingsVisible)
                instance?.addChild(it)
            }
        }
    }
}