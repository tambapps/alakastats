package com.tambapps.pokemon.alakastats.infrastructure.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowdownReplayServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** A replay every [spacing] seconds, the newest uploaded at [newest], as Showdown's search pages them */
    private fun serviceListing(
        count: Int,
        newest: Long = 10_000,
        rating: (Int) -> Int? = { 1500 },
        isPrivate: (Int) -> Boolean = { false },
        players: (Int) -> List<String> = { listOf("a", "b") },
        spacing: Long = 10,
        requestedUrls: MutableList<String> = mutableListOf(),
    ): ShowdownReplayService {
        val replays = (0 until count).map { index ->
            val playersJson = players(index).joinToString(",") { "\"$it\"" }
            """{"id":"gen9vgc-$index","uploadtime":${newest - index * spacing},"rating":${rating(index)},""" +
                """"players":[$playersJson],"private":${if (isPrivate(index)) 1 else 0},"format":"[Gen 9] VGC"}"""
        }
        val client = HttpClient(MockEngine { request ->
            requestedUrls.add(request.url.toString())
            val before = request.url.parameters["before"]?.toLong()
            val page = replays.withIndex()
                .filter { (index, _) -> before == null || newest - index * spacing < before }
                .take(51)
                .map { it.value }
            respond("[${page.joinToString(",")}]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(json) }
        }
        return ShowdownReplayService(client, ReplayAnalyticsService(client))
    }

    @Test
    fun aboveAMinimumRatingTheEarlierGamesOfAKeptSeriesAreKept() = runTest {
        // newest first: a series' last game, rated, then its earlier ones, without rating
        val service = serviceListing(
            count = 5,
            rating = { index -> when (index) { 0 -> 1600; 3 -> 1200; else -> null } },
            players = { index ->
                when (index) {
                    1 -> listOf("B", "A")
                    2 -> listOf("a", "b")
                    3, 4 -> listOf("c", "d")
                    else -> listOf("a", "b")
                }
            },
        )
        val replays = service.list("gen9vgcbo3", since = 0, minRating = 1500)
        // the series of c and d ended below the minimum: its earlier game isn't kept either
        assertEquals(listOf("gen9vgc-0", "gen9vgc-1", "gen9vgc-2"), replays.map { it.id })
    }

    @Test
    fun anUnratedReplayLongBeforeIsntAGameOfTheSameSeries() = runTest {
        val service = serviceListing(count = 2, rating = { index -> if (index == 0) 1600 else null }, spacing = 4 * 60 * 60)
        assertEquals(listOf("gen9vgc-0"), service.list("gen9vgcbo3", since = 0, minRating = 1500).map { it.id })
    }

    @Test
    fun theReplaysAreListedPageByPage() = runTest {
        val urls = mutableListOf<String>()
        val replays = serviceListing(120, requestedUrls = urls).list("gen9vgc", since = 0)
        assertEquals((0 until 120).map { "gen9vgc-$it" }, replays.map { it.id })
        assertEquals(3, urls.size)
        assertEquals("https://replay.pokemonshowdown.com/search.json?format=gen9vgc", urls.first())
        // each page starts before the last replay of the previous one
        assertEquals("https://replay.pokemonshowdown.com/search.json?format=gen9vgc&before=9500", urls[1])
    }

    @Test
    fun theListingStopsAtTheReplaysUploadedBeforeTheGivenTime() = runTest {
        val urls = mutableListOf<String>()
        val replays = serviceListing(500, requestedUrls = urls).list("gen9vgc", since = 10_000 - 600)
        assertEquals((0..60).map { "gen9vgc-$it" }, replays.map { it.id })
        assertEquals(2, urls.size)
    }

    @Test
    fun theReplaysBelowTheMinimumRatingOrWithoutOneAreLeftOut() = runTest {
        val service = serviceListing(10, rating = { index -> if (index == 0) null else 1400 + index * 20 })
        val replays = service.list("gen9vgc", since = 0, minRating = 1500)
        assertEquals((5 until 10).map { "gen9vgc-$it" }, replays.map { it.id })
    }

    @Test
    fun withoutMinimumTheUnratedReplaysAreKept() = runTest {
        val replays = serviceListing(3, rating = { null }).list("gen9vgc", since = 0)
        assertEquals(3, replays.size)
    }

    @Test
    fun thePrivateReplaysAreLeftOut() = runTest {
        val replays = serviceListing(4, isPrivate = { it % 2 == 0 }).list("gen9vgc", since = 0)
        assertEquals(listOf("gen9vgc-1", "gen9vgc-3"), replays.map { it.id })
    }

    @Test
    fun aSummaryKeepsTheTeamsTheirKnownItemsAndWhatWasBrought() = runTest {
        val log = """
            |player|p1|me|
            |player|p2|opponent|
            |poke|p1|Charizard, L50|
            |poke|p1|Pikachu, L50|
            |poke|p2|Garchomp, L50|
            |switch|p1a: Charizard|Charizard, L50|100/100
            |switch|p2a: Garchomp|Garchomp, L50|100/100
            |-mega|p1a: Charizard|Charizard|Charizardite Y
            |win|me
        """.trimIndent()
        val client = HttpClient(MockEngine {
            val body = """{"formatid":"gen9championsvgc2026regmc","players":["me","opponent"],"uploadtime":1794259449,""" +
                """"rating":1600,"log":${JsonPrimitive(log)}}"""
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(json) }
        }
        val summary = ShowdownReplayService(client, ReplayAnalyticsService(client))
            .fetchSummary("gen9championsvgc2026regmc-1").getOrNull()!!

        assertEquals("gen9championsvgc2026regmc-1", summary.id)
        assertEquals(1600, summary.rating)
        assertEquals("me", summary.winner?.value)
        val (me, opponent) = summary.players
        assertEquals(listOf("charizard", "pikachu"), me.team.map { it.value })
        // the Mega Stone, for the Mega Evolution to show
        assertEquals(listOf("charizardite-y", null), me.items.map { it?.value })
        assertEquals(listOf("charizard"), me.brought.map { it.value })
        assertEquals(listOf<String?>(null), opponent.items.map { it?.value })
    }

    @Test
    fun theListingStopsAsSoonAsThereAreMoreNewReplaysThanTheMaximum() = runTest {
        val urls = mutableListOf<String>()
        val replays = serviceListing(500, requestedUrls = urls).list("gen9vgc", since = 0, maxNew = 60)
        // one more than the maximum, telling there are too many
        assertEquals(61, replays.size)
        assertEquals(2, urls.size)
    }

    @Test
    fun theSavedReplaysDontCountInTheMaximum() = runTest {
        val saved = (0 until 30).map { "gen9vgc-$it" }.toSet()
        val replays = serviceListing(80).list("gen9vgc", since = 0, savedIds = saved, maxNew = 50)
        // the 30 saved ones and the 50 others, no more than the maximum
        assertEquals(80, replays.size)
    }

    @Test
    fun theProgressTellsHowManyReplaysWereKept() = runTest {
        val progress = mutableListOf<Int>()
        serviceListing(120).list("gen9vgc", since = 0) { progress.add(it) }
        assertEquals(listOf(51, 102, 120), progress)
    }
}
