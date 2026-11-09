package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.repository.ShowdownReplayRepository
import com.tambapps.pokemon.alakastats.infrastructure.service.ShowdownReplayListing
import com.tambapps.pokemon.alakastats.infrastructure.service.ShowdownReplayService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Clock

val SHOWDOWN_REPLAYS_PERIODS = listOf(1, 3, 7)

// replays saved each time this many are fetched: what was fetched before a cancellation or an error is kept
private const val SAVE_BATCH_SIZE = 50

// requests to Showdown at once, not to overload it
private const val PARALLEL_REQUESTS = 4

private const val SECONDS_PER_DAY = 86_400L

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
}

class ShowdownReplaysViewModel(
    private val service: ShowdownReplayService,
    private val repository: ShowdownReplayRepository,
) : ViewModel() {

    /** The formats having replays on Showdown */
    val formats = Format.entries.filter { it.showdownId != null }

    var format by mutableStateOf(formats.first())
        private set
    var bestOf3 by mutableStateOf(false)
        private set
    var days by mutableIntStateOf(SHOWDOWN_REPLAYS_PERIODS.first())
    /** Empty for no minimum */
    var minRatingText by mutableStateOf("")

    var savedReplays by mutableStateOf<List<ShowdownReplaySummary>>(emptyList())
        private set
    var isLoadingSaved by mutableStateOf(true)
        private set

    var load by mutableStateOf<ShowdownReplaysLoad>(ShowdownReplaysLoad.Idle)
        private set

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
        loadSaved()
    }

    private fun loadSaved() {
        savedJob?.cancel()
        isLoadingSaved = true
        val formatId = formatId
        savedJob = viewModelScope.launch {
            savedReplays = repository.list(formatId).sortedByDescending { it.uploadTime }
            isLoadingSaved = false
        }
    }

    /** Lists the replays of the chosen period, for the user to know how many will be downloaded */
    fun listReplays(now: Long = Clock.System.now().epochSeconds) {
        if (isBusy) return
        val formatId = formatId
        val since = now - days * SECONDS_PER_DAY
        val minRating = minRating
        load = ShowdownReplaysLoad.Listing(0)
        loadJob = viewModelScope.launch {
            load = try {
                val listed = service.list(formatId, since, minRating) { load = ShowdownReplaysLoad.Listing(it) }
                val savedIds = repository.list(formatId).mapTo(mutableSetOf()) { it.id }
                val toDownload = listed.filter { it.id !in savedIds }
                ShowdownReplaysLoad.Listed(toDownload, alreadySaved = listed.size - toDownload.size)
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

    fun dismissResult() {
        if (!isBusy) load = ShowdownReplaysLoad.Idle
    }
}
