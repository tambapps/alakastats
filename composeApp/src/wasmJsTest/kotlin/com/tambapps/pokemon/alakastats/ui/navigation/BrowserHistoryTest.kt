package com.tambapps.pokemon.alakastats.ui.navigation

import kotlinx.browser.window
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * With the real history of the test's browser. The test page has no base: it's the app's root here
 */
class BrowserHistoryTest {

    private val history = BrowserHistory()

    @AfterTest
    fun restoreState() = window.history.replaceState(null, "", "")

    @Test
    fun anEntryThatIsntOursHasNoIndex() {
        window.history.replaceState(null, "", "")
        assertNull(history.index)
    }

    @Test
    fun theIndexIsReadFromTheEntry() {
        history.replace(5, "")
        assertEquals(5, history.index)
    }
}
