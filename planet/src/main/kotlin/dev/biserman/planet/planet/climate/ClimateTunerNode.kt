package dev.biserman.planet.planet.climate

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Node
import godot.api.OS

/** Runs the tuner inside Godot, where Kotlin/JVM native core types are valid. */
@RegisterClass
class ClimateTunerNode : Node() {
    @RegisterFunction
    override fun _ready() {
        val exitCode = try {
            runClimateTuner(OS.getCmdlineUserArgs().toList().toTypedArray())
            0
        } catch (error: Throwable) {
            System.err.println("Climate tuner failed: ${error.message}")
            error.printStackTrace()
            1
        }
        getTree()?.quit(exitCode)
    }
}
