package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.Clipboard
import arrow.core.getOrElse
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.usecase.HandleTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.util.copyToClipboard
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

    var replayNotesText by mutableStateOf("")

    var replayToRemove by mutableStateOf<ReplayAnalytics?>(null)
        private set

    var replayToNote by mutableStateOf<ReplayAnalytics?>(null)
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

    fun hideNoteReplayDialog() {
        replayToNote = null
        replayNotesText = ""
    }


    fun showRemoveReplayDialog(replay: ReplayAnalytics) {
        replayToRemove = replay
    }

    fun showNoteReplayDialog(replay: ReplayAnalytics) {
        replayToNote = replay
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

    fun editNotes(notes: String?) {
        val replayToNote = this.replayToNote ?: return
        editNotes(replayToNote, notes)
    }

    fun editNotes(replayToNote: ReplayAnalytics, notes: String?) {
        scope.launch {
            handleReplaysUseCase.replaceReplay(replayToNote, replayToNote.copy(notes = notes))
        }
        hideNoteReplayDialog()
    }

    fun addReplays(snackBar: SnackBar) {
        if (isLoading) {
            return
        }
        isLoading = true
        // need to be here because after we're clearing the text
        val urls = parseReplayUrls(replayUrlsText)
        scope.launch {
            val results = urls.map { url ->
                async {
                    handleReplaysUseCase.parseReplay(url).getOrElse { error ->
                        withContext(Dispatchers.Main) {
                            snackBar.show("Failed to fetch replay: ${error.message}", SnackBar.Severity.ERROR)
                        }
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            val duplicates = results.filter { resultReplay -> team.replays.any { it.reference == resultReplay.reference } }
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
            .splitToSequence(REPLAYS_SEPARATOR_REGEX)
            .map { it.trim() }
            .map { if (it.contains("?")) it.substringBefore("?") else it }
            .filter { it.isNotBlank() && isValidReplayUrl(it) }
            .toList()
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

    fun copyToClipboard(
        clipboardManager: Clipboard,
        snackbar: SnackBar,
        label: String,
        text: String
    ) {
        scope.launch {
            if (copyToClipboard(clipboardManager, label, text)) {
                snackbar.show("Copied to clipboard")
            } else {
                snackbar.show("Copy to clipboard not supported")
            }
        }
    }
}