package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayFilters
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayStorage
import com.tambapps.pokemon.alakastats.domain.model.withSeriesRatings
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.repository.ShowdownReplayRepository
import com.tambapps.pokemon.alakastats.domain.usecase.AddReplaysToTeamUseCase
import com.tambapps.pokemon.alakastats.infrastructure.service.ReplayAnalyticsService
import com.tambapps.pokemon.alakastats.infrastructure.service.ShowdownReplayListing
import com.tambapps.pokemon.alakastats.ui.SnackBar
import com.tambapps.pokemon.alakastats.infrastructure.service.ShowdownReplayService
import com.tambapps.pokemon.alakastats.ui.model.ShowdownReplaySort
import com.tambapps.pokemon.alakastats.ui.model.sortedBy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Clock

val SHOWDOWN_REPLAYS_PERIODS = listOf(1, 3, 7, 14, 30)

// replays saved each time this many are fetched: what was fetched before a cancellation or an error is kept
private const val SAVE_BATCH_SIZE = 50

// requests to Showdown at once, not to overload it
private const val PARALLEL_REQUESTS = 4

private const val SECONDS_PER_DAY = 86_400L

/**
 * The most replays a load downloads: beyond it, the download is too long, and the filters, working on all the saved
 * replays in memory, too slow
 */
const val MAX_REPLAYS_PER_LOAD = 10_000

/**
 * Where the loading of a format's public replays is at
 */
sealed interface ShowdownReplaysLoad {
    data object Idle : ShowdownReplaysLoad

    /** @param found the replays listed so far */
    data class Listing(val found: Int) : ShowdownReplaysLoad

    /**
     * Waiting for the user's go-ahead
     *
     * @param alreadySaved the listed replays that are already saved, not downloaded again
     */
    data class Listed(val toDownload: List<ShowdownReplayListing>, val alreadySaved: Int) : ShowdownReplaysLoad

    data class Downloading(val done: Int, val total: Int, val failed: Int) : ShowdownReplaysLoad

    data class Done(val saved: Int, val failed: Int) : ShowdownReplaysLoad

    data class Failed(val message: String) : ShowdownReplaysLoad

    /** More than [MAX_REPLAYS_PER_LOAD] replays to download: the user has to narrow the load */
    data object TooMany : ShowdownReplaysLoad
}

/**
 * The saved replays of a format and best-of
 */
data class ShowdownReplaysFormatStorage(
    val format: Format,
    val bestOf3: Boolean,
    val formatId: String,
    val storage: ShowdownReplayStorage,
)

/**
 * Where the adding of the selected replays to a team is at: their whole replays are fetched first
 */
data class AddToTeamProgress(val teamName: String, val fetched: Int, val total: Int)

class ShowdownReplaysViewModel(
    private val service: ShowdownReplayService,
    private val repository: ShowdownReplayRepository,
    private val replayAnalyticsService: ReplayAnalyticsService,
    private val addReplaysToTeam: AddReplaysToTeamUseCase,
) : ViewModel() {

    /** The formats having replays on Showdown */
    val formats = Format.entries.filter { it.showdownId != null }

    var format by mutableStateOf(formats.first())
        private set
    var bestOf3 by mutableStateOf(false)
        private set
    /** The period to load the replays of, in days */
    var days by mutableIntStateOf(SHOWDOWN_REPLAYS_PERIODS.first())
    /** The minimum Elo of the replays to load, empty for no minimum */
    var minRatingText by mutableStateOf("")

    /**
     * The saved replays of each format, shown in the storage dialog, null when it's closed. Empty while being computed
     */
    var storage by mutableStateOf<List<ShowdownReplaysFormatStorage>?>(null)
        private set
    var isComputingStorage by mutableStateOf(false)
        private set

    /** Whether the dialog choosing what to load is shown */
    var showLoadDialog by mutableStateOf(false)

    var filters by mutableStateOf(ShowdownReplayFilters())
    var sort by mutableStateOf(ShowdownReplaySort())

    var savedReplays by mutableStateOf<List<ShowdownReplaySummary>>(emptyList())
        private set
    var isLoadingSaved by mutableStateOf(true)
        private set

    var load by mutableStateOf<ShowdownReplaysLoad>(ShowdownReplaysLoad.Idle)
        private set

    /** The replay played back in the viewer, null when none */
    var viewedReplay by mutableStateOf<ShowdownReplaySummary?>(null)

    /** The replay the user is asked to confirm the deletion of, from the viewer, null when not asked */
    var replayToDelete by mutableStateOf<ShowdownReplaySummary?>(null)

    /** Whether the games of a best-of-3 are grouped together, for the best-of-3 formats */
    var groupBySeries by mutableStateOf(true)

    /** The saved replays the user is asked to confirm the deletion of, null when not asked */
    var replaysToDelete by mutableStateOf<List<ShowdownReplaySummary>?>(null)
        private set
    /** Whether [replaysToDelete] are the selected replays rather than those the filters show */
    var isDeletingSelection by mutableStateOf(false)
        private set

    /** Whether a click on a replay selects it rather than playing it back */
    var isSelecting by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf<Set<String>>(emptySet())
        private set

    /** The teams to add the selected replays to, shown in the team picker, null when it's closed */
    var teamsToPick by mutableStateOf<List<TeamlyticsPreview>?>(null)
        private set
    /** Null when not adding the selected replays to a team */
    var addToTeamProgress by mutableStateOf<AddToTeamProgress?>(null)
        private set

    /** The saved replays the filters keep, in the sort's order */
    val shownReplays by derivedStateOf { savedReplays.filter(filters::matches).sortedBy(sort) }

    val isBusy get() = load is ShowdownReplaysLoad.Listing || load is ShowdownReplaysLoad.Downloading

    val minRating get() = minRatingText.trim().toIntOrNull()

    private val formatId get() = if (bestOf3) format.showdownBo3Id!! else format.showdownId!!

    private var loadJob: Job? = null
    private var savedJob: Job? = null

    init {
        loadSaved()
    }

    fun selectFormat(newFormat: Format) {
        if (isBusy || newFormat == format) return
        format = newFormat
        onFormatChanged()
    }

    fun selectBestOf3(value: Boolean) {
        if (isBusy || value == bestOf3) return
        bestOf3 = value
        onFormatChanged()
    }

    private fun onFormatChanged() {
        load = ShowdownReplaysLoad.Idle
        stopSelection()
        loadSaved()
    }

    fun startSelection() {
        isSelecting = true
    }

    fun stopSelection() {
        isSelecting = false
        selectedIds = emptySet()
    }

    fun toggleSelection(replay: ShowdownReplaySummary) {
        selectedIds = if (replay.id in selectedIds) selectedIds - replay.id else selectedIds + replay.id
    }

    /** Selects all the replays, e.g. the games of a series, or unselects them when they all are */
    fun toggleSelection(replays: List<ShowdownReplaySummary>) {
        val ids = replays.mapTo(mutableSetOf()) { it.id }
        selectedIds = if (selectedIds.containsAll(ids)) selectedIds - ids else selectedIds + ids
    }

    /** Whether all the replays the filters show are selected */
    val areAllShownSelected get() = shownReplays.isNotEmpty() && shownReplays.all { it.id in selectedIds }

    /** Selects all the replays the filters show, or unselects them when they all are */
    fun toggleSelectAll() = toggleSelection(shownReplays)

    private val selectedReplays get() = savedReplays.filter { it.id in selectedIds }
    /** Asks to delete the selected replays */
    fun askSelectionDeletion() {
        if (isBusy || selectedIds.isEmpty()) return
        replaysToDelete = selectedReplays
        isDeletingSelection = true
    }

    /** Opens the team picker, to add the selected replays to a team */
    fun askTeam() {
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            teamsToPick = addReplaysToTeam.listTeams().sortedByDescending { it.lastUpdatedAt }
        }
    }

    fun dismissTeamPicker() {
        teamsToPick = null
    }

    /**
     * Adds the selected replays to the team: the app only keeps a summary of them, their whole replays are fetched again
     * from Showdown
     */
    fun addSelectionToTeam(team: TeamlyticsPreview, snackBar: SnackBar) {
        val replays = selectedReplays
        teamsToPick = null
        if (replays.isEmpty() || addToTeamProgress != null) return
        addToTeamProgress = AddToTeamProgress(team.name, 0, replays.size)
        viewModelScope.launch {
            var fetchedCount = 0
            val semaphore = Semaphore(PARALLEL_REQUESTS)
            val fetched = coroutineScope {
                replays.map { replay ->
                    async {
                        semaphore.withPermit { replayAnalyticsService.fetch(replay.url) }.also {
                            fetchedCount++
                            addToTeamProgress = AddToTeamProgress(team.name, fetchedCount, replays.size)
                        }
                    }
                }.awaitAll()
            }
            val failed = fetched.count { it.isLeft() }
            val result = addReplaysToTeam.add(team.id, fetched.mapNotNull { it.getOrNull() })
            addToTeamProgress = null
            result.fold(
                ifLeft = { error -> snackBar.show("Couldn't add the replays to ${team.name}: ${error.message}", SnackBar.Severity.ERROR) },
                ifRight = { added ->
                    val alreadyIn = replays.size - failed - added
                    snackBar.show(
                        buildString {
                            append(if (added == 1) "1 replay added to ${team.name}" else "$added replays added to ${team.name}")
                            if (alreadyIn > 0) append(", $alreadyIn already in it")
                            if (failed > 0) append(", $failed couldn't be downloaded")
                        },
                        if (failed > 0) SnackBar.Severity.WARNING else SnackBar.Severity.SUCCESS,
                    )
                    stopSelection()
                },
            )
        }
    }

    private fun loadSaved() {
        savedJob?.cancel()
        isLoadingSaved = true
        val formatId = formatId
        savedJob = viewModelScope.launch {
            // the series' first games rated as their last: the Elo filter and sort take them with it
            savedReplays = repository.list(formatId).withSeriesRatings().sortedByDescending { it.uploadTime }
            isLoadingSaved = false
        }
    }

    /** Lists the replays of the chosen period, for the user to know how many will be downloaded */
    fun listReplays(now: Long = Clock.System.now().epochSeconds) {
        showLoadDialog = false
        if (isBusy) return
        val formatId = formatId
        val since = now - days * SECONDS_PER_DAY
        val minRating = minRating
        load = ShowdownReplaysLoad.Listing(0)
        loadJob = viewModelScope.launch {
            load = try {
                val savedIds = repository.list(formatId).mapTo(mutableSetOf()) { it.id }
                // stopped as soon as there are too many, not to list them all for nothing
                val listed = service.list(formatId, since, minRating, savedIds, maxNew = MAX_REPLAYS_PER_LOAD) {
                    load = ShowdownReplaysLoad.Listing(it)
                }
                val toDownload = listed.filter { it.id !in savedIds }
                if (toDownload.size > MAX_REPLAYS_PER_LOAD) ShowdownReplaysLoad.TooMany
                else ShowdownReplaysLoad.Listed(toDownload, alreadySaved = listed.size - toDownload.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ShowdownReplaysLoad.Failed("Couldn't list the replays: ${e.message}")
            }
        }
    }

    /** Downloads and saves the listed replays */
    fun download() {
        val listed = load as? ShowdownReplaysLoad.Listed ?: return
        val formatId = formatId
        val total = listed.toDownload.size
        load = ShowdownReplaysLoad.Downloading(0, total, 0)
        loadJob = viewModelScope.launch {
            var done = 0
            var failed = 0
            try {
                val semaphore = Semaphore(PARALLEL_REQUESTS)
                for (batch in listed.toDownload.chunked(SAVE_BATCH_SIZE)) {
                    val summaries = coroutineScope {
                        batch.map { replay ->
                            async {
                                semaphore.withPermit { service.fetchSummary(replay.id) }.also { result ->
                                    done++
                                    if (result.isLeft()) failed++
                                    load = ShowdownReplaysLoad.Downloading(done, total, failed)
                                }
                            }
                        }.awaitAll()
                    }.mapNotNull { it.getOrNull() }
                    repository.save(formatId, summaries)
                }
                load = ShowdownReplaysLoad.Done(saved = done - failed, failed = failed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                load = ShowdownReplaysLoad.Failed("Couldn't save the replays: ${e.message}")
            } finally {
                loadSaved()
            }
        }
    }

    /** Stops the listing or the download. The replays already downloaded stay saved */
    fun cancel() {
        loadJob?.cancel()
        loadJob = null
        load = ShowdownReplaysLoad.Idle
    }

    /** Asks to delete the saved replays the filters show */
    fun askDeletion() {
        if (isBusy) return
        replaysToDelete = shownReplays
        isDeletingSelection = false
    }

    fun confirmDeletion() {
        val replays = replaysToDelete ?: return
        replaysToDelete = null
        if (isDeletingSelection) stopSelection()
        if (replays.isEmpty()) return
        val formatId = formatId
        viewModelScope.launch {
            repository.delete(formatId, replays)
            loadSaved()
        }
    }

    /** Deletes one saved replay */
    fun deleteReplay(replay: ShowdownReplaySummary) {
        viewModelScope.launch {
            repository.delete(replay.formatId, listOf(replay))
            loadSaved()
        }
    }

    /** Opens the storage dialog, with the room each format's replays take */
    fun showStorage() {
        storage = emptyList()
        computeStorage()
    }

    private fun computeStorage() {
        isComputingStorage = true
        viewModelScope.launch {
            val formatsStorage = formats.flatMap { format ->
                listOf(false, true).map { bestOf3 ->
                    val formatId = if (bestOf3) format.showdownBo3Id!! else format.showdownId!!
                    ShowdownReplaysFormatStorage(format, bestOf3, formatId, repository.storage(formatId))
                }
            }.filter { it.storage.count > 0 }
            // unless closed meanwhile
            if (storage != null) storage = formatsStorage
            isComputingStorage = false
        }
    }

    fun dismissStorage() {
        storage = null
    }

    /** Deletes all the saved replays of a format */
    fun deleteFormat(formatStorage: ShowdownReplaysFormatStorage) {
        if (isBusy) return
        viewModelScope.launch {
            repository.deleteAll(formatStorage.formatId)
            if (formatStorage.formatId == formatId) loadSaved()
            computeStorage()
        }
    }

    fun dismissDeletion() {
        replaysToDelete = null
    }

    /** Back to the load dialog, to choose a shorter period or a higher minimum Elo */
    fun narrowLoad() {
        load = ShowdownReplaysLoad.Idle
        showLoadDialog = true
    }

    fun dismissResult() {
        if (!isBusy) load = ShowdownReplaysLoad.Idle
    }
}
