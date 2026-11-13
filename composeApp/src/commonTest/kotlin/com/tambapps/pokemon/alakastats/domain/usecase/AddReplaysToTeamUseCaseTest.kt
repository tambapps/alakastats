package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.error.TeamlyticsNotFound
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AddReplaysToTeamUseCaseTest {

    private class MemoryRepository(team: Teamlytics) : TeamlyticsRepository {
        val teams = mutableMapOf(team.id to team)
        val bases = mutableListOf<Teamlytics?>()

        override suspend fun list() = teams.values.toList()
        override suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics> =
            teams[id]?.right() ?: TeamlyticsNotFound(id).left()
        override suspend fun listPreviews(): List<TeamlyticsPreview> = emptyList()
        override suspend fun save(teamlytics: Teamlytics, base: Teamlytics?): Either<GetTeamlyticsError, Teamlytics> {
            bases.add(base)
            teams[teamlytics.id] = teamlytics
            return teamlytics.right()
        }
        override suspend fun delete(teamlytics: Teamlytics) { teams.remove(teamlytics.id) }
        override suspend fun delete(id: Uuid) { teams.remove(id) }
    }

    private fun replay(reference: String, nextBattleRef: String? = null) = ReplayAnalytics(
        players = listOf(player("me"), player("opponent")),
        uploadTime = 0,
        format = "gen9vgc2026regmcbo3",
        rating = null,
        version = "1",
        winner = UserName("me"),
        url = "https://replay.pokemonshowdown.com/$reference",
        reference = reference,
        nextBattleRef = nextBattleRef,
        notes = null,
    )

    private fun player(name: String) = Player(
        name = UserName(name),
        teamPreview = TeamPreview(emptyList()),
        selection = listOf(PokemonName("pikachu")),
        beforeElo = null,
        afterElo = null,
        terastallization = null,
        megaEvolution = null,
        ots = null,
        movesUsage = emptyMap(),
    )

    private val team = Teamlytics(
        id = Uuid.random(),
        name = "Team",
        pokePaste = PokePaste(emptyList()),
        replays = listOf(replay("already-in")),
        sdNames = listOf(UserName("me")),
        lastUpdatedAt = Instant.fromEpochMilliseconds(0),
        data = TeamlyticsData(emptyMap()),
        notes = null,
        matchupPlans = emptyList(),
        format = Format.REGULATION_MC,
    )

    @Test
    fun theReplaysNotInTheTeamYetAreAdded() = runTest {
        val repository = MemoryRepository(team)
        val added = AddReplaysToTeamUseCase(repository).add(team.id, listOf(replay("a"), replay("already-in"), replay("a")))

        assertEquals(1, added.getOrNull())
        assertEquals(setOf("already-in", "a"), repository.teams.getValue(team.id).replays.map { it.reference }.toSet())
        // merged with what may have been saved since
        assertEquals(team, repository.bases.single())
    }

    @Test
    fun theGamesOfASeriesAreAddedInTheirOrder() = runTest {
        val repository = MemoryRepository(team)
        // the game 1 tells the game 2 follows it
        AddReplaysToTeamUseCase(repository).add(team.id, listOf(replay("g2"), replay("g1", nextBattleRef = "g2")))

        val references = repository.teams.getValue(team.id).replays.map { it.reference }
        assertTrue(references.indexOf("g1") < references.indexOf("g2"), references.toString())
    }

    @Test
    fun nothingIsSavedWhenAllAreInTheTeam() = runTest {
        val repository = MemoryRepository(team)
        val added = AddReplaysToTeamUseCase(repository).add(team.id, listOf(replay("already-in")))

        assertEquals(0, added.getOrNull())
        assertTrue(repository.bases.isEmpty())
    }

    @Test
    fun anUnknownTeamIsAnError() = runTest {
        val added = AddReplaysToTeamUseCase(MemoryRepository(team)).add(Uuid.random(), listOf(replay("a")))
        assertTrue(added.isLeft())
    }
}
