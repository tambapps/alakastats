package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A browser history, whose back is asynchronous as in a real one: [deliverPendingBack] tells when it happens
 */
private class FakeHistory(path: String = "", initialIndex: Int? = null) : WebHistory {
    val entries = mutableListOf(initialIndex to path)
    var position = 0
    var pendingBacks = 0

    override val index get() = entries[position].first
    override val path get() = entries[position].second

    override fun push(index: Int, path: String) {
        while (entries.size > position + 1) entries.removeLast()
        entries += index to path
        position++
    }

    override fun replace(index: Int, path: String) {
        entries[position] = index to path
    }

    override fun back() {
        pendingBacks++
    }

    fun deliverPendingBack(sync: BackStackHistorySync) {
        pendingBacks--
        position--
        sync.onHistoryChanged()
    }

    // the user's buttons
    fun pressBack(sync: BackStackHistorySync) {
        position--
        sync.onHistoryChanged()
    }

    fun pressForward(sync: BackStackHistorySync) {
        position++
        sync.onHistoryChanged()
    }
}

class BackStackHistorySyncTest {

    private val home = AppRoute.Home
    private val pokedex = AppRoute.Pokedex()
    private val metagross = AppRoute.Pokedex("metagross")
    // a screen without URL
    private val cloudSync = AppRoute.CloudSync

    private fun sync(history: WebHistory, vararg routes: NavKey): Pair<BackStackHistorySync, MutableList<NavKey>> {
        val backStack = routes.toMutableList()
        return BackStackHistorySync(history, backStack) to backStack
    }

    private fun BackStackHistorySync.navigate(backStack: MutableList<NavKey>, change: MutableList<NavKey>.() -> Unit) {
        backStack.change()
        onBackStackChanged(backStack.toList())
    }

    @Test
    fun theUrlIsRewrittenAtStart() {
        val history = FakeHistory("unknown")
        sync(history, home)
        assertEquals("", history.path)
        assertEquals(0, history.index)
    }

    @Test
    fun aPushAddsAnEntry() {
        val history = FakeHistory()
        val (sync, backStack) = sync(history, home)
        sync.navigate(backStack) { add(pokedex) }
        assertEquals("pokedex", history.path)
        assertEquals(1, history.index)
        assertEquals(2, history.entries.size)
    }

    @Test
    fun changingThePokemonReplacesTheEntry() {
        val history = FakeHistory()
        val (sync, backStack) = sync(history, home)
        sync.navigate(backStack) { add(pokedex) }
        sync.navigate(backStack) { this[lastIndex] = metagross }
        assertEquals("pokedex/metagross", history.path)
        assertEquals(2, history.entries.size)
    }

    @Test
    fun aPopInTheAppGoesBackInTheHistory() {
        val history = FakeHistory()
        val (sync, backStack) = sync(history, home)
        sync.navigate(backStack) { add(pokedex) }
        sync.navigate(backStack) { removeLast() }
        assertEquals(1, history.pendingBacks)
        history.deliverPendingBack(sync)
        assertEquals("", history.path)
        assertEquals(listOf<NavKey>(home), backStack)
    }

    @Test
    fun twoQuickPopsDontBringTheScreensBack() {
        val history = FakeHistory()
        val (sync, backStack) = sync(history, home)
        sync.navigate(backStack) { add(cloudSync) }
        sync.navigate(backStack) { add(pokedex) }
        sync.navigate(backStack) { removeLast() }
        sync.navigate(backStack) { removeLast() }
        assertEquals(2, history.pendingBacks)
        history.deliverPendingBack(sync)
        assertEquals(listOf<NavKey>(home), backStack)
        history.deliverPendingBack(sync)
        assertEquals(listOf<NavKey>(home), backStack)
        assertEquals("", history.path)
    }

    @Test
    fun theBrowsersBackAndForwardFollow() {
        val history = FakeHistory()
        val (sync, backStack) = sync(history, home)
        sync.navigate(backStack) { add(metagross) }

        history.pressBack(sync)
        assertEquals(listOf<NavKey>(home), backStack)
        sync.onBackStackChanged(backStack.toList())
        assertEquals(2, history.entries.size)

        history.pressForward(sync)
        assertEquals(listOf<NavKey>(home, metagross), backStack)
        sync.onBackStackChanged(backStack.toList())
        assertEquals(1, history.index)
    }

    @Test
    fun aScreenWithoutUrlIsAnEntryWithItsParentsUrlAndComesBackForward() {
        val history = FakeHistory()
        val (sync, backStack) = sync(history, home)
        sync.navigate(backStack) { add(cloudSync) }
        assertEquals("", history.path)
        assertEquals(2, history.entries.size)

        history.pressBack(sync)
        assertEquals(listOf<NavKey>(home), backStack)

        history.pressForward(sync)
        assertEquals(listOf<NavKey>(home, cloudSync), backStack)
    }

    @Test
    fun poppingASetupOpenedFromItsUrlGoesToTheQuizzes() {
        val history = FakeHistory("quizzes/natures")
        val (sync, backStack) = sync(history, home, AppRoute.Quizzes, AppRoute.NatureQuizSetup)
        sync.navigate(backStack) { removeLast() }
        assertEquals(0, history.pendingBacks)
        assertEquals("quizzes", history.path)
        sync.navigate(backStack) { removeLast() }
        assertEquals(0, history.pendingBacks)
        assertEquals("", history.path)
        assertEquals(listOf<NavKey>(home), backStack)
    }

    @Test
    fun poppingAScreenOpenedFromItsUrlStaysOnTheSite() {
        val history = FakeHistory("pokedex/metagross")
        val (sync, backStack) = sync(history, home, metagross)
        sync.navigate(backStack) { removeLast() }
        // no previous entry to go back to: it would leave the site
        assertEquals(0, history.pendingBacks)
        assertEquals("", history.path)
        assertEquals(listOf<NavKey>(home), backStack)
    }

    @Test
    fun aPushRemovesTheForwardEntries() {
        val history = FakeHistory()
        val (sync, backStack) = sync(history, home)
        sync.navigate(backStack) { add(pokedex) }
        history.pressBack(sync)
        sync.onBackStackChanged(backStack.toList())

        sync.navigate(backStack) { add(cloudSync) }
        assertEquals(2, history.entries.size)
        assertEquals(listOf<NavKey>(home, cloudSync), backStack)
    }

    @Test
    fun anEntryFromBeforeAReloadIsFoundFromItsUrl() {
        val history = FakeHistory("pokedex/metagross", initialIndex = 3)
        history.entries.add(0, 2 to "pokedex")
        history.position = 1
        val (sync, backStack) = sync(history, home, metagross)
        assertEquals(3, history.index)

        history.pressBack(sync)
        assertEquals(listOf<NavKey>(home, pokedex), backStack)
    }
}
