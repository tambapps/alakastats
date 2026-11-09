package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.model.UserName
import io.github.xxfast.kstore.Codec
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ShowdownReplaySummaryEntity
import io.github.xxfast.kstore.storeOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KStoreShowdownReplayRepositoryTest {

    private class MemoryCodec<T : Any> : Codec<T> {
        var value: T? = null
        override suspend fun encode(value: T?) { this.value = value }
        override suspend fun decode(): T? = value
    }

    private val daysCodecs = mutableMapOf<String, MemoryCodec<List<Long>>>()
    private val dayCodecs = mutableMapOf<Pair<String, Long>, MemoryCodec<List<ShowdownReplaySummaryEntity>>>()

    private val repository = KStoreShowdownReplayRepository(
        daysStore = { formatId -> storeOf(daysCodecs.getOrPut(formatId) { MemoryCodec() }, enableCache = false) },
        dayStore = { formatId, day -> storeOf(dayCodecs.getOrPut(formatId to day) { MemoryCodec() }, enableCache = false) },
    )

    private fun replay(id: String, uploadTime: Long, rating: Int? = 1500) = ShowdownReplaySummary(
        id = id,
        formatId = "gen9vgc",
        uploadTime = uploadTime,
        rating = rating,
        winner = UserName("Alaka"),
        players = listOf(
            ShowdownReplayPlayer(UserName("Alaka"), listOf(PokemonName("whimsicott")), listOf(PokemonName("whimsicott")), 1500, 1520),
            ShowdownReplayPlayer(UserName("Opponent"), listOf(PokemonName("pelipper")), emptyList(), null, null),
        ),
        bestOf = BestOf(1, 3, "series"),
    )

    @Test
    fun theSavedReplaysAreListedAsTheyWere() = runTest {
        val replays = listOf(replay("a", 100), replay("b", 200, rating = null))
        repository.save("gen9vgc", replays)
        assertEquals(replays, repository.list("gen9vgc"))
    }

    @Test
    fun theReplaysAreSavedByDayOfUpload() = runTest {
        repository.save("gen9vgc", listOf(replay("a", 100), replay("b", 86_400 + 5), replay("c", 200)))
        assertEquals(listOf(0L, 1L), daysCodecs.getValue("gen9vgc").value)
        assertEquals(setOf("gen9vgc" to 0L, "gen9vgc" to 1L), dayCodecs.keys)
    }

    @Test
    fun aSaveAddsToTheSavedReplaysAndReplacesTheSameOnes() = runTest {
        repository.save("gen9vgc", listOf(replay("a", 100), replay("b", 200)))
        repository.save("gen9vgc", listOf(replay("b", 200, rating = 1700), replay("c", 86_400 * 2)))
        val saved = repository.list("gen9vgc")
        assertEquals(listOf("a", "b", "c"), saved.map { it.id })
        assertEquals(1700, saved.single { it.id == "b" }.rating)
        assertEquals(listOf(0L, 2L), daysCodecs.getValue("gen9vgc").value)
    }

    @Test
    fun eachFormatHasItsOwnReplays() = runTest {
        repository.save("gen9vgc", listOf(replay("a", 100)))
        repository.save("gen9vgcbo3", listOf(replay("b", 100)))
        assertEquals(listOf("a"), repository.list("gen9vgc").map { it.id })
        assertEquals(listOf("b"), repository.list("gen9vgcbo3").map { it.id })
        assertEquals(emptyList(), repository.list("other"))
    }
}
