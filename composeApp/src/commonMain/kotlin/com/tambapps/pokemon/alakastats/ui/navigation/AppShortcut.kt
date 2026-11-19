package com.tambapps.pokemon.alakastats.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The parts of the app the platforms' shortcuts open: Android's launcher shortcuts, iOS's quick actions and App Intents
 * @param id how the platforms name it, in shortcuts.xml on Android and in Info.plist and the App Intents on iOS
 */
enum class AppShortcut(val id: String, val route: AppRoute) {
    DAMAGE_CALC("damage-calc", AppRoute.DamageCalc),
    SHOWDOWN_REPLAYS("showdown-replays", AppRoute.ShowdownReplays),
    POKEDEX("pokedex", AppRoute.Pokedex);

    companion object {
        fun fromId(id: String?): AppShortcut? = entries.find { it.id == id }
    }
}

/**
 * The shortcut used to open the app, until it went to its screen. A state rather than an event: the shortcut can come
 * before the app is shown (when it's launched by it), or while it's running
 */
object AppShortcuts {
    private val _pending = MutableStateFlow<AppShortcut?>(null)
    val pending: StateFlow<AppShortcut?> = _pending.asStateFlow()

    /** @return whether the id is one of a shortcut */
    fun open(id: String?): Boolean {
        val shortcut = AppShortcut.fromId(id) ?: return false
        _pending.value = shortcut
        return true
    }

    /** Once the app went to its screen. Leaves another shortcut used meanwhile */
    fun consume(shortcut: AppShortcut) {
        _pending.compareAndSet(shortcut, null)
    }
}
