package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.model.isShowdownReplayUrl
import com.tambapps.pokemon.alakastats.domain.model.isValidLink
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ManualReplayViewModelTest {

    private val team = Teamlytics(
        id = Uuid.random(),
        name = "Team",
        pokePaste = PokePaste(emptyList()),
        replays = emptyList(),
        sdNames = listOf(UserName("me")),
        lastUpdatedAt = Instant.fromEpochMilliseconds(0),
        data = TeamlyticsData(emptyMap()),
        notes = null,
        matchupPlans = emptyList(),
        format = Format.NONE,
    )

    private val video = "https://www.youtube.com/watch?v=abc"

    private fun viewModel() = ManualReplayViewModel(team, null)

    @Test
    fun linksAreShowdownOrOther() {
        assertTrue(isShowdownReplayUrl("https://replay.pokemonshowdown.com/gen9vgc-1"))
        assertFalse(isShowdownReplayUrl(video))
        assertTrue(isValidLink(video))
        assertTrue(isValidLink("https://youtu.be/abc"))
        assertFalse(isValidLink("youtube.com/watch?v=abc"))
        assertFalse(isValidLink("https://my video"))
    }

    @Test
    fun linkIsSavedAndKeptWhenEdited() {
        val creation = viewModel()
        creation.updateLink("  $video ")
        val replay = creation.buildReplayAnalytics()
        assertEquals(video, replay.url)
        assertNull(replay.showdownUrl)

        val edition = viewModel()
        edition.prepareEdition(replay)
        assertEquals(video, edition.link)
        assertEquals(video, edition.buildReplayAnalytics().url)
    }

    @Test
    fun blankLinkIsNoLink() {
        val viewModel = viewModel()
        viewModel.updateLink(" ")

        assertNull(viewModel.buildReplayAnalytics().url)
    }
}
