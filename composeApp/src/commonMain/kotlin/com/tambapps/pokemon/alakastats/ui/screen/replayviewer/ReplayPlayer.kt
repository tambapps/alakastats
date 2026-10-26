package com.tambapps.pokemon.alakastats.ui.screen.replayviewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.ui.composables.HtmlBridge
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Where the battle of the page built by [replayHtml] is at, as the page posts it
 */
@Serializable
data class ReplayPlaybackState(
    val turn: Int,
    val paused: Boolean,
    val atEnd: Boolean,
    val started: Boolean,
    // in CSS pixels, which are dp on every platform
    val contentHeight: Int,
)

enum class ReplaySpeed(val label: String, internal val showdownValue: String) {
    HYPERFAST("Hyperfast", "hyperfast"),
    FAST("Fast", "fast"),
    NORMAL("Normal", "normal"),
    SLOW("Slow", "slow"),
    REALLY_SLOW("Really slow", "reallyslow"),
}

/**
 * Drives the battle of the page built by [replayHtml], through its battle and setSpeed globals
 * @param showLog whether the battle's log is shown at first
 */
class ReplayPlayer(showLog: Boolean) {
    /**
     * Null until the battle is loaded
     */
    var state by mutableStateOf<ReplayPlaybackState?>(null)
        private set
    var speed by mutableStateOf(ReplaySpeed.NORMAL)
        private set
    var showLog by mutableStateOf(showLog)
        private set

    val bridge = HtmlBridge { message ->
        try {
            state = json.decodeFromString<ReplayPlaybackState>(message)
        } catch (_: Exception) {
            // not a message of ours
        }
    }

    fun play() = run("battle.play()")
    fun pause() = run("battle.pause()")
    fun replay() = run("battle.reset(); battle.play()")
    fun reset() = run("battle.reset()")
    fun previousTurn() = run("battle.seekBy(-1)")
    fun nextTurn() = run("battle.seekBy(1)")
    fun goToTurn(turn: Int) = run("battle.seekTurn($turn)")
    fun goToEnd() = run("battle.seekTurn(Infinity)")
    fun switchSides() = run("battle.switchViewpoint()")

    fun changeSpeed(speed: ReplaySpeed) {
        this.speed = speed
        run("setSpeed('${speed.showdownValue}')")
    }

    fun toggleLog() {
        showLog = !showLog
        run("document.body.classList.toggle('$HIDE_LOG_CLASS', ${!showLog})")
    }

    private fun run(script: String) {
        // the battle doesn't post its state on every command (switching sides), so the page does it after it
        bridge.evaluate("if (window.battle) { $script; postState(); }")
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
