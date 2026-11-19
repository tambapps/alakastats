package com.tambapps.pokemon.alakastats

import com.tambapps.pokemon.alakastats.ui.navigation.AppShortcut
import com.tambapps.pokemon.alakastats.ui.navigation.AppShortcuts

/**
 * Called by the iOS app (Swift) for its quick actions and App Intents: goes to the screen of the shortcut
 * @param id the id of an [AppShortcut], e.g. damage-calc
 * @return whether it's one of a shortcut
 */
fun openAppShortcut(id: String): Boolean = AppShortcuts.open(id)
