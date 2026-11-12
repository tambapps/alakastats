package com.tambapps.pokemon.alakastats.infrastructure.service

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.SHOWDOWN_REPLAYS_URL
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Showdown gives one more than a page holds when there are more
private const val PAGE_SIZE = 50

// a guard against a never ending listing: about 150 000 replays, beyond 30 days of the busiest formats
private const val MAX_PAGES = 3_000

/**
 * A public replay, as listed by Showdown's search
 *
 * @param uploadTime in seconds since the epoch
 */
@Serializable
data class ShowdownReplayListing(
    val id: String,
    @SerialName("uploadtime")
    val uploadTime: Long,
    val rating: Int? = null,
    val players: List<String> = emptyList(),
    val private: Int = 0,
)

/**
 * The public replays of Pokémon Showdown
 */
class ShowdownReplayService(
    private val httpClient: HttpClient,
    private val replayAnalyticsService: ReplayAnalyticsService,
) {

    /**
     * Lists the public replays of a format uploaded since a time, newest first, going through Showdown's search page
     * by page
     *
     * @param since in seconds since the epoch
     * @param minRating the battle's minimum rating: the replays without one are left out, except the games of a series
     * played before its last one, which only the last one is rated for (see [isEarlierGameOf])
     * @param savedIds the replays already saved, listed but not counted in [maxNew]
     * @param maxNew the listing stops as soon as it has more replays not saved yet, null for no limit
     * @param onProgress called after each page, with how many replays were kept so far
     */
    suspend fun list(
        formatId: String,
        since: Long,
        minRating: Int? = null,
        savedIds: Set<String> = emptySet(),
        maxNew: Int? = null,
        onProgress: (Int) -> Unit = {},
    ): List<ShowdownReplayListing> {
        val replays = LinkedHashMap<String, ShowdownReplayListing>()
        // the rated replays kept, the last games of their series when in one
        val keptRated = mutableListOf<ShowdownReplayListing>()
        var newCount = 0
        var before: Long? = null
        for (pageIndex in 0 until MAX_PAGES) {
            val page = httpClient.get("$SHOWDOWN_REPLAYS_URL/search.json") {
                parameter("format", formatId)
                before?.let { parameter("before", it) }
            }.body<List<ShowdownReplayListing>>()
            for (replay in page) {
                if (replay.uploadTime < since) return replays.values.toList().also { onProgress(it.size) }
                val rating = replay.rating
                val kept = replay.private == 0 && when {
                    minRating == null -> true
                    rating != null -> (rating >= minRating).also { if (it) keptRated.add(replay) }
                    // newest first: the last game of its series, uploaded after it, was listed before
                    else -> keptRated.any { replay.isEarlierGameOf(it) }
                }
                // a page starts where the previous one ended: the replays uploaded the same second may come again
                if (kept && replays.put(replay.id, replay) == null && replay.id !in savedIds) {
                    newCount++
                    if (maxNew != null && newCount > maxNew) return replays.values.toList().also { onProgress(it.size) }
                }
            }
            onProgress(replays.size)
            if (page.size <= PAGE_SIZE) break
            before = page.last().uploadTime
        }
        return replays.values.toList()
    }

    /** Fetches a replay, to keep what's needed of it */
    suspend fun fetchSummary(id: String): Either<DomainError, ShowdownReplaySummary> =
        replayAnalyticsService.fetch("$SHOWDOWN_REPLAYS_URL/$id").map { it.toShowdownReplaySummary(id) }
}

// a series' games are played in a row: beyond it, the same players are in another series
private const val SERIES_MAX_SECONDS = 3 * 60 * 60L

/**
 * Whether this unrated replay may be a game of the series [lastGame] ended: Showdown rates a series once, on its last
 * game, its other games having no rating. The listing doesn't tell the series, so it's the same players a bit before
 */
internal fun ShowdownReplayListing.isEarlierGameOf(lastGame: ShowdownReplayListing): Boolean {
    val delay = lastGame.uploadTime - uploadTime
    return delay in 0..SERIES_MAX_SECONDS &&
        players.map { it.lowercase() }.toSet() == lastGame.players.map { it.lowercase() }.toSet()
}

internal fun ReplayAnalytics.toShowdownReplaySummary(id: String) = ShowdownReplaySummary(
    id = id,
    formatId = format,
    uploadTime = uploadTime,
    rating = rating,
    winner = winner?.takeIf { it.value.isNotEmpty() },
    players = players.map { player ->
        ShowdownReplayPlayer(
            name = player.name,
            team = player.teamPreview.pokemons.map { it.name },
            items = player.teamPreviewItems,
            brought = player.selection,
            beforeElo = player.beforeElo,
            afterElo = player.afterElo,
        )
    },
    bestOf = bestOf,
)
