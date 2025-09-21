package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeamReplayViewModel(
    val pokemonImageService: PokemonImageService,
    private val handleReplaysUseCase: HandleTeamReplaysUseCase,
    val team: Teamlytics,
) {

    private companion object {
        val REPLAYS_SEPARATOR_REGEX = Regex("[,\\s]+")
    }
    var isLoading by mutableStateOf(false)
        private set

    var showAddReplayDialog by mutableStateOf(false)
        private set

    var replayUrlsText by mutableStateOf("")
        private set

    var replayToRemove by mutableStateOf<ReplayAnalytics?>(null)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)

    fun showAddReplayDialog() {
        showAddReplayDialog = true
        replayUrlsText = ""
    }

    fun hideAddReplayDialog() {
        showAddReplayDialog = false
        replayUrlsText = ""
    }

    fun showRemoveReplayDialog(replay: ReplayAnalytics) {
        replayToRemove = replay
    }

    fun removeReplay() {
        replayToRemove?.let {
            scope.launch {
                handleReplaysUseCase.removeReplay(it)
            }
        }
        replayToRemove = null
    }

    fun hideRemoveReplayDialog() {
        replayToRemove = null
    }

    fun updateReplayUrlsText(text: String) {
        replayUrlsText = text
    }

    fun addReplays(snackBar: SnackBar) {
        if (isLoading) {
            return
        }
        isLoading = true
        scope.launch {
            val urls = parseReplayUrls(replayUrlsText)
            val results = urls.map { url ->
                async {
                    try {
                        handleReplaysUseCase.parseReplay(url)
                    } catch (e: Exception) {
                        // TODO handle and return null or use Result or arrow Either
                        throw RuntimeException(e)
                    }
                }
            }.awaitAll()
            val duplicates = results.intersect(team.replays)
            if (duplicates.size < results.size) {
                handleReplaysUseCase.addReplays(results - duplicates)
            }
            withContext(Dispatchers.Main) {
                isLoading = false
                when {
                    duplicates.size == results.size -> snackBar.show(if (duplicates.size == 1) "The replay was already added" else "The Replays were already added")
                    duplicates.isNotEmpty() -> snackBar.show("Some replay(s) were already added")
                }
            }
        }
        hideAddReplayDialog()
    }

    private fun parseReplayUrls(text: String): List<String> {
        return text
            .split(REPLAYS_SEPARATOR_REGEX)
            .map { it.trim() }
            .filter { it.isNotBlank() && isValidReplayUrl(it) }
    }

    private fun isValidReplayUrl(url: String): Boolean {
        return isValidUrl(url) && url.contains("replay.pokemonshowdown.com")
    }

    private fun isValidUrl(s: String) = try {
        Url(s) // will throw IllegalArgumentException if invalid
        true
    } catch (_: IllegalArgumentException) {
        false
    }
    fun getValidationMessage(): String? {
        if (replayUrlsText.isBlank()) return null

        val urls = parseReplayUrls(replayUrlsText)
        val totalUrls = replayUrlsText
            .split(REPLAYS_SEPARATOR_REGEX)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .size

        return when {
            urls.isEmpty() && totalUrls > 0 -> "No valid replay URLs found"
            urls.size < totalUrls -> "${urls.size}/${totalUrls} valid URLs found"
            urls.isNotEmpty() -> "${urls.size} valid URLs found"
            else -> null
        }
    }
}