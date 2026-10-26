package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsData
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.model.isShowdownReplayUrl
import com.tambapps.pokemon.alakastats.domain.model.isValidLink
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
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

    private val paste = """
        Incineroar @ Sitrus Berry
        Ability: Intimidate
        - Fake Out

        Garchomp @ Choice Scarf
        Ability: Rough Skin
        - Earthquake
    """.trimIndent()

    private val pokepasteParser = PokepasteParser()
    private val requestedUrls = mutableListOf<String>()
    private val httpClient = HttpClient(MockEngine { request ->
        requestedUrls.add(request.url.toString())
        if (request.url.toString() == "https://pokepast.es/abc/raw") respond(paste) else respond("", HttpStatusCode.NotFound)
    })

    private fun viewModel(canLoadPokepasteLinks: Boolean = true) =
        ManualReplayViewModel(team, null, pokepasteParser, httpClient, canLoadPokepasteLinks)

    // no mega in it: the base forms are the normalized names
    private val pasteTeam = pokepasteParser.parse(paste).pokemons.map { it.name.normalized }

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

    @Test
    fun pastedTextFillsTheOpponentTeam() = runTest {
        val viewModel = viewModel()
        viewModel.showPokepasteDialog()
        viewModel.updatePokepasteInput(paste)

        viewModel.doLoadOpponentPokepaste()

        assertEquals(pasteTeam, viewModel.opponentPokemonStates.map { it.name })
        assertFalse(viewModel.showPokepasteDialog)
        assertTrue(requestedUrls.isEmpty())
    }

    @Test
    fun pokepasteLinkIsFetchedAndReplacesTheOpponentTeam() = runTest {
        val viewModel = viewModel()
        viewModel.addOpponentPokemon(PokemonName("Pikachu"))
        viewModel.updatePokepasteInput(" https://pokepast.es/abc ")

        viewModel.doLoadOpponentPokepaste()

        assertEquals(listOf("https://pokepast.es/abc/raw"), requestedUrls)
        assertEquals(pasteTeam, viewModel.opponentPokemonStates.map { it.name })
    }

    @Test
    fun pokepasteLinkIsRefusedWhereItCantBeLoaded() = runTest {
        val viewModel = viewModel(canLoadPokepasteLinks = false)
        viewModel.showPokepasteDialog()
        viewModel.updatePokepasteInput("https://pokepast.es/abc")

        viewModel.doLoadOpponentPokepaste()

        assertEquals("Enter the pokepaste content, not the URL", viewModel.pokepasteError)
        assertTrue(requestedUrls.isEmpty())
        assertFalse(viewModel.isLoadingPokepaste)
    }

    @Test
    fun unknownPokepasteKeepsTheTeamAndTellsWhy() = runTest {
        val viewModel = viewModel()
        viewModel.showPokepasteDialog()
        viewModel.updatePokepasteInput("https://pokepast.es/unknown")

        viewModel.doLoadOpponentPokepaste()

        assertTrue(viewModel.pokepasteError!!.startsWith("Couldn't load the PokéPaste"))
        assertTrue(viewModel.showPokepasteDialog)
        assertTrue(viewModel.opponentPokemonStates.isEmpty())
    }
}
