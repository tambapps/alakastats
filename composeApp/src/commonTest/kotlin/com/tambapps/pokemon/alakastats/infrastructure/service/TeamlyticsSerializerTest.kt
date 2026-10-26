package com.tambapps.pokemon.alakastats.infrastructure.service

import com.tambapps.pokemon.alakastats.domain.model.MANUAL_REFERENCE_PREFIX
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PlayerEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.sync.replay
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TeamlyticsSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }
    // e.g. offline: no replay can be fetched again
    private val httpClient = HttpClient(MockEngine { respond("", HttpStatusCode.ServiceUnavailable) })
    private val serializer = TeamlyticsSerializer(PokepasteParser(), json, ReplayAnalyticsService(httpClient))

    private fun player(name: String) = PlayerEntity(
        name = name,
        teamPreview = TeamPreviewEntity(emptyList()),
        selection = emptyList(),
        beforeElo = null,
        afterElo = null,
        terastallization = null,
        ots = null,
        movesUsage = emptyMap(),
    )

    private fun replayOf(reference: String, url: String?) = replay(reference, notes = "notes of $reference", winner = "me")
        .copy(players = listOf(player("me"), player("opponent")), url = url)

    private fun save(vararg replays: ReplayAnalyticsEntity) =
        json.encodeToString(
            TeamlyticsEntity(
                id = Uuid.random(),
                name = "Rain",
                pokePaste = "",
                replays = replays.toList(),
                sdNames = listOf("me"),
                lastUpdatedAt = Instant.fromEpochSeconds(1_700_000_000),
            )
        ).encodeToByteArray()

    @Test
    fun replaysThatCantBeFetchedAreKeptAsSaved() = runTest {
        val manual = replayOf(MANUAL_REFERENCE_PREFIX + "1", url = null)
        val video = replayOf(MANUAL_REFERENCE_PREFIX + "2", url = "https://www.youtube.com/watch?v=abc")
        val showdown = replayOf("gen9vgc-1", url = "https://replay.pokemonshowdown.com/gen9vgc-1")

        val team = serializer.loadTeam(save(manual, video, showdown)).getOrNull()!!

        assertEquals(listOf(manual.reference, video.reference, showdown.reference), team.replays.map { it.reference })
        assertEquals(listOf(null, video.url, showdown.url), team.replays.map { it.url })
        assertEquals(listOf(manual.notes, video.notes, showdown.notes), team.replays.map { it.notes })
    }

    @Test
    fun oldSaveWithOnlyUrlsDropsTheReplaysThatCantBeFetched() = runTest {
        val save = """{"name":"Rain","replays":[{"url":"https://replay.pokemonshowdown.com/gen9vgc-1","notes":"n"}]}"""

        val team = serializer.loadTeam(save.encodeToByteArray()).getOrNull()!!

        assertEquals(emptyList(), team.replays)
    }
}
