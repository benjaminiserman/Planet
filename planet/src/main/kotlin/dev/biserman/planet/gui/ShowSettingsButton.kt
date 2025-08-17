package dev.biserman.planet.gui

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.CheckButton

@RegisterClass
class ShowSettingsButton() : CheckButton() {
    @RegisterFunction
    override fun _pressed() {
        Gui.settingsVisible = !Gui.settingsVisible
    }
}