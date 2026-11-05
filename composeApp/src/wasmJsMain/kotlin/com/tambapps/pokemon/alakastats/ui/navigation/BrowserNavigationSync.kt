package com.tambapps.pokemon.alakastats.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavKey
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL

/**
 * The browser's history of this tab. Each entry holds its index, to tell where the browser went
 */
internal interface WebHistory {
    /** The current entry's index, null if it isn't one of ours */
    val index: Int?

    /** The current URL's path, relative to the app's root */
    val path: String

    fun push(index: Int, path: String)

    fun replace(index: Int, path: String)

    fun back()
}

internal class BrowserHistory : WebHistory {
    // the document's base is the app's root
    private val rootPath = URL(document.baseURI).pathname

    override val index get() = (window.history.state as? JsNumber)?.toInt()

    override val path get() = window.location.pathname.removePrefix(rootPath)

    override fun push(index: Int, path: String) = window.history.pushState(index.toJsNumber(), "", rootPath + path)

    override fun replace(index: Int, path: String) = window.history.replaceState(index.toJsNumber(), "", rootPath + path)

    override fun back() = window.history.back()
}

/**
 * Keeps the back stack and the browser's history in sync, each going where the other one went
 */
internal class BackStackHistorySync(
    private val history: WebHistory,
    private val backStack: MutableList<NavKey>,
) {
    private var currentIndex = history.index ?: 0

    // what each history entry shows. The ones from before a reload aren't known: they're found from their URL
    private val backStackByIndex = mutableMapOf(currentIndex to backStack.toList())

    // the history goes back asynchronously: what the browser then tells about it is already taken into account
    private var pendingBacks = 0

    init {
        // the URL may be replaced: it may not be known, or not written as the app does
        history.replace(currentIndex, WebRoutes.pathOf(backStack))
    }

    /** The app navigated */
    fun onBackStackChanged(newBackStack: List<NavKey>) {
        val currentBackStack = backStackByIndex.getValue(currentIndex)
        when {
            newBackStack == currentBackStack -> Unit
            newBackStack.size > currentBackStack.size -> {
                // as the browser does, a new entry removes the next ones
                backStackByIndex.keys.removeAll { it > currentIndex }
                currentIndex++
                backStackByIndex[currentIndex] = newBackStack
                history.push(currentIndex, WebRoutes.pathOf(newBackStack))
            }
            newBackStack.size < currentBackStack.size && backStackByIndex[currentIndex - 1] == newBackStack -> {
                currentIndex--
                pendingBacks++
                history.back()
            }
            else -> {
                // going back to an entry that isn't the previous one would leave the screen or the site, e.g. when
                // the app was opened from an URL on a screen having the home screen below it
                backStackByIndex[currentIndex] = newBackStack
                history.replace(currentIndex, WebRoutes.pathOf(newBackStack))
            }
        }
    }

    /** The browser navigated, with its back or forward button */
    fun onHistoryChanged() {
        if (pendingBacks > 0) {
            pendingBacks--
            return
        }
        currentIndex = history.index ?: currentIndex
        val target = backStackByIndex.getOrPut(currentIndex) { WebRoutes.backStackOf(history.path) }
        Snapshot.withMutableSnapshot {
            // the screens in common aren't touched: they keep their state
            val common = backStack.zip(target).takeWhile { (route, targetRoute) -> route == targetRoute }.size
            while (backStack.size > common) backStack.removeLastOrNull()
            backStack.addAll(target.drop(common))
        }
    }
}

@Composable
internal fun BrowserNavigationSync(backStack: MutableList<NavKey>, history: WebHistory) {
    val sync = remember(backStack) { BackStackHistorySync(history, backStack) }
    LaunchedEffect(sync) {
        snapshotFlow { backStack.toList() }.collect(sync::onBackStackChanged)
    }
    DisposableEffect(sync) {
        val listener: (Event) -> Unit = { sync.onHistoryChanged() }
        window.addEventListener("popstate", listener)
        onDispose { window.removeEventListener("popstate", listener) }
    }
}
