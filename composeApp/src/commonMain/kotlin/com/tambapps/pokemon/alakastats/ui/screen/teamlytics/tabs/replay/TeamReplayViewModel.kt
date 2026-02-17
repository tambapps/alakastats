package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.flatMap
import arrow.core.getOrElse
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.usecase.ManageTeamReplaysUseCase
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.TeamlyticsFiltersTabViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeamReplayViewModel(
    override val useCase: ManageTeamReplaysUseCase,
    private val replayService: ReplayAnalyticsService,
    override val pokemonImageService: PokemonImageService,
): TeamlyticsFiltersTabViewModel() {
    val team get() = useCase.filteredTeam

    private companion object {
        val REPLAYS_SEPARATOR_REGEX = Regex("[,\\s]+")
    }
    override var isTabLoading by mutableStateOf(false)
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

    val hasNoReplaysToShow get() = team.replays.isEmpty() && !isLoading

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

    fun reloadReplay(snackBar: SnackBar, replay: ReplayAnalytics, url: String) {
        if (isLoading) {
            return
        }
        isTabLoading = true
        scope.launch {
            val replayEither = replayService.fetch(url)
            withContext(Dispatchers.Main) {
                isTabLoading = false
                replayEither.flatMap { reloadedReplay ->
                    useCase.replaceReplay(replay, reloadedReplay.completedWith(replay))
                }.fold(
                    ifLeft = {
                        snackBar.show("Failed to reload replay: ${it.message}", SnackBar.Severity.ERROR)
                    },
                    ifRight = { reloadedReplay ->
                        snackBar.show("Successfully reloaded replay", SnackBar.Severity.SUCCESS)
                    }
                )
            }
        }
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
                useCase.removeReplay(it)
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
            useCase.replaceReplay(replayToNote, replayToNote.copy(notes = notes))
        }
        hideNoteReplayDialog()
    }

    fun addReplays(snackBar: SnackBar) {
        if (isLoading) {
            return
        }
        isTabLoading = true
        // need to be here because after we're clearing the text
        val urls = parseReplayUrls(replayUrlsText)
        scope.launch {
            val results = urls.map { url ->
                async {
                    replayService.fetch(url).getOrElse { error ->
                        withContext(Dispatchers.Main) {
                            snackBar.show("Failed to fetch replay: ${error.message}", SnackBar.Severity.ERROR)
                        }
                        null
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .distinctBy { it.reference }

            val duplicates = results.filter { resultReplay -> useCase.originalTeam.replays.any { it.reference == resultReplay.reference } }
            val error =
                if (duplicates.size < results.size) useCase.addReplays(results - duplicates).leftOrNull()
                else null
            withContext(Dispatchers.Main) {
                isTabLoading = false
                when {
                    error != null -> snackBar.show("Couldn't add replay: ${error.message}", SnackBar.Severity.ERROR)
                    duplicates.size == results.size -> snackBar.show(if (duplicates.size == 1) "The replay was already added" else "The replays were already added")
                    duplicates.isNotEmpty() -> snackBar.show("Added replays (some replay(s) were already added)")
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
}