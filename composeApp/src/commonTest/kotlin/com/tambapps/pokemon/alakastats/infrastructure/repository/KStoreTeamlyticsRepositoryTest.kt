package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.alakastats.domain.transformer.toDomain
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.sync.InMemoryKStorage
import com.tambapps.pokemon.alakastats.infrastructure.sync.MutableClock
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamWriteLock
import com.tambapps.pokemon.alakastats.infrastructure.sync.replay
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class KStoreTeamlyticsRepositoryTest {

    private val clock = MutableClock(Instant.fromEpochSeconds(1_700_000_000))
    private val parser = PokepasteParser()
    private val teams = InMemoryKStorage<Uuid, TeamlyticsEntity>()
    private val previews = InMemoryKStorage<Uuid, TeamlyticsPreviewEntity>()
    private val repository = KStoreTeamlyticsRepository(teams, previews, parser, TeamWriteLock(), clock)

    private val stored = TeamlyticsEntity(
        id = Uuid.random(),
        name = "Rain",
        pokePaste = "",
        replays = listOf(replay("r1")),
        sdNames = listOf("me"),
        lastUpdatedAt = clock.now(),
    )

    @Test
    fun saveFromAStaleCopyMergesWhatWasSavedMeanwhile() = runTest {
        teams.save(stored)
        // the screen loads the team
        val loaded = stored.toDomain(parser)
        // a sync brings a replay meanwhile
        clock.now += 1.seconds
        teams.save(stored.copy(replays = stored.replays + replay("synced"), lastUpdatedAt = clock.now()))

        // the screen saves from its copy
        clock.now += 1.seconds
        val saved = repository.save(loaded.copy(name = "Rain v2"), base = loaded).getOrNull()!!

        assertEquals("Rain v2", saved.name)
        assertEquals(listOf("r1", "synced"), saved.replays.map { it.reference })
        assertEquals(listOf("r1", "synced"), teams.get(stored.id)!!.replays.map { it.reference })
        assertEquals(2, previews.get(stored.id)!!.nbReplays)
    }

    @Test
    fun saveFromAnUpToDateCopyOverwrites() = runTest {
        teams.save(stored)
        val loaded = stored.toDomain(parser)

        clock.now += 1.seconds
        val saved = repository.save(loaded.copy(replays = emptyList()), base = loaded).getOrNull()!!

        // a deletion stays a deletion
        assertEquals(emptyList(), saved.replays)
        assertEquals(clock.now(), teams.get(stored.id)!!.lastUpdatedAt)
    }

    @Test
    fun saveWithoutBaseOverwrites() = runTest {
        teams.save(stored.copy(replays = stored.replays + replay("other")))

        val saved = repository.save(stored.toDomain(parser)).getOrNull()!!

        assertEquals(listOf("r1"), saved.replays.map { it.reference })
    }
}
