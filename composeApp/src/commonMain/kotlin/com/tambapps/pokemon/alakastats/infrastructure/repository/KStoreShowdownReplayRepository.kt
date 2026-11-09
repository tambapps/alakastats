package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.repository.ShowdownReplayRepository
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.BestOfEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ShowdownReplayPlayerEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ShowdownReplaySummaryEntity
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.extensions.getOrEmpty
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val SECONDS_PER_DAY = 86_400L

/**
 * Saves the replays of each format by day of upload (UTC), each day in its own store: a save only rewrites the days it
 * adds replays to, and the oldest days can be deleted on their own
 *
 * @param daysStore for each format, the days having replays, as days since the epoch
 * @param dayStore the replays of a format uploaded a day
 */
class KStoreShowdownReplayRepository(
    private val daysStore: (formatId: String) -> KStore<List<Long>>,
    private val dayStore: (formatId: String, day: Long) -> KStore<List<ShowdownReplaySummaryEntity>>,
) : ShowdownReplayRepository {

    // the saves read and rewrite the stores
    private val mutex = Mutex()

    override suspend fun list(formatId: String): List<ShowdownReplaySummary> = mutex.withLock {
        daysStore(formatId).getOrEmpty()
            .flatMap { day -> dayStore(formatId, day).getOrEmpty() }
            .map { it.toDomain(formatId) }
    }

    override suspend fun save(formatId: String, replays: List<ShowdownReplaySummary>) = mutex.withLock {
        val replaysByDay = replays.groupBy { it.uploadTime / SECONDS_PER_DAY }
        for ((day, dayReplays) in replaysByDay) {
            val ids = dayReplays.mapTo(mutableSetOf()) { it.id }
            dayStore(formatId, day).update { saved ->
                saved.orEmpty().filter { it.id !in ids } + dayReplays.map { it.toEntity() }
            }
        }
        // the days after their replays: a day is never listed without its store
        daysStore(formatId).update { days -> (days.orEmpty() + replaysByDay.keys).distinct().sorted() }
        Unit
    }
}

private fun ShowdownReplaySummaryEntity.toDomain(formatId: String) = ShowdownReplaySummary(
    id = id,
    formatId = formatId,
    uploadTime = uploadTime,
    rating = rating,
    winner = winner?.let(::UserName),
    players = players.map { player ->
        ShowdownReplayPlayer(
            name = UserName(player.name),
            team = player.team.map(::PokemonName),
            brought = player.brought.map(::PokemonName),
            beforeElo = player.beforeElo,
            afterElo = player.afterElo,
        )
    },
    bestOf = bestOf?.let { BestOf(it.game, it.of, it.seriesRef) },
)

private fun ShowdownReplaySummary.toEntity() = ShowdownReplaySummaryEntity(
    id = id,
    uploadTime = uploadTime,
    rating = rating,
    winner = winner?.value,
    players = players.map { player ->
        ShowdownReplayPlayerEntity(
            name = player.name.value,
            team = player.team.map { it.value },
            brought = player.brought.map { it.value },
            beforeElo = player.beforeElo,
            afterElo = player.afterElo,
        )
    },
    bestOf = bestOf?.let { BestOfEntity(it.game, it.of, it.seriesRef) },
)
