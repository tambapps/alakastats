package com.tambapps.pokemon.alakastats.ui.navigation

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppShortcutsTest {

    @AfterTest
    fun clear() {
        AppShortcuts.pending.value?.let(AppShortcuts::consume)
    }

    @Test
    fun theIdsAreThoseOfThePlatforms() {
        // the ids of androidApp's res/xml/shortcuts.xml and of the iOS app's Info.plist and App Intents
        assertEquals(AppShortcut.DAMAGE_CALC, AppShortcut.fromId("damage-calc"))
        assertEquals(AppShortcut.SHOWDOWN_REPLAYS, AppShortcut.fromId("showdown-replays"))
        assertEquals(AppShortcut.POKEDEX, AppShortcut.fromId("pokedex"))
    }

    @Test
    fun anUnknownIdOpensNothing() {
        assertFalse(AppShortcuts.open("teams"))
        assertFalse(AppShortcuts.open(null))
        assertNull(AppShortcuts.pending.value)
    }

    @Test
    fun theShortcutIsPendingUntilConsumed() {
        assertTrue(AppShortcuts.open("pokedex"))
        assertEquals(AppShortcut.POKEDEX, AppShortcuts.pending.value)

        AppShortcuts.consume(AppShortcut.POKEDEX)
        assertNull(AppShortcuts.pending.value)
    }

    @Test
    fun consumingAShortcutLeavesOneUsedMeanwhile() {
        AppShortcuts.open("pokedex")
        AppShortcuts.open("damage-calc")

        AppShortcuts.consume(AppShortcut.POKEDEX)
        assertEquals(AppShortcut.DAMAGE_CALC, AppShortcuts.pending.value)
    }
}
