package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.error.LoadFormatDataError
import com.tambapps.pokemon.alakastats.domain.error.TeamlyticsNotFound
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ManualReplayLoaderViewModelTest {

    private val replay = ReplayAnalytics(
        players = emptyList(),
        uploadTime = 0,
        format = "",
        rating = null,
        version = "",
        winner = null,
        url = null,
        reference = "manual-abc",
        nextBattleRef = null,
        notes = null,
    )

    private val team = Teamlytics(
        id = Uuid.random(),
        name = "Team",
        pokePaste = PokePaste(emptyList()),
        replays = listOf(replay),
        sdNames = listOf(UserName("me")),
        lastUpdatedAt = Instant.fromEpochMilliseconds(0),
        data = TeamlyticsData(emptyMap()),
        notes = null,
        matchupPlans = emptyList(),
        format = Format.NONE,
    )

    private val repository = object : TeamlyticsRepository {
        override suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics> =
            if (id == team.id) team.right() else TeamlyticsNotFound(id).left()

        override suspend fun list(): List<Teamlytics> = error("unused")
        override suspend fun listPreviews(): List<TeamlyticsPreview> = error("unused")
        override suspend fun save(teamlytics: Teamlytics, base: Teamlytics?): Either<GetTeamlyticsError, Teamlytics> = error("unused")
        override suspend fun delete(teamlytics: Teamlytics) = error("unused")
        override suspend fun delete(id: Uuid) = error("unused")
    }

    private val formatDataRepository = object : FormatDataRepository {
        override suspend fun get(format: Format): Either<LoadFormatDataError, FormatData> = error("unused")
    }

    private fun loader(teamId: Uuid, replayReference: String? = null) =
        ManualReplayLoaderViewModel(teamId, replayReference, repository, formatDataRepository)

    @Test
    fun loadsTheTeamToCreateAReplay() = runTest {
        val loader = loader(team.id)
        assertEquals(ManualReplayLoadState.Loading, loader.state)
        loader.load()
        val state = assertIs<ManualReplayLoadState.Loaded>(loader.state)
        assertEquals(team, state.team)
        assertNull(state.replayToEdit)
    }

    @Test
    fun findsTheReplayToEditFromItsReference() = runTest {
        val loader = loader(team.id, replay.reference)
        loader.load()
        assertEquals(replay, assertIs<ManualReplayLoadState.Loaded>(loader.state).replayToEdit)
    }

    @Test
    fun failsOnAnUnknownReplay() = runTest {
        val loader = loader(team.id, "manual-unknown")
        loader.load()
        assertIs<ManualReplayLoadState.Failed>(loader.state)
    }

    @Test
    fun failsOnAnUnknownTeam() = runTest {
        val loader = loader(Uuid.random())
        loader.load()
        assertIs<ManualReplayLoadState.Failed>(loader.state)
    }
}
