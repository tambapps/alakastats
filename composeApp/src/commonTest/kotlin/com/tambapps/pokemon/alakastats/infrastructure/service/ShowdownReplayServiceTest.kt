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
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowdownReplayServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** A replay every 10 seconds, the newest uploaded at [newest], as Showdown's search pages them */
    private fun serviceListing(
        count: Int,
        newest: Long = 10_000,
        rating: (Int) -> Int? = { 1500 },
        isPrivate: (Int) -> Boolean = { false },
        requestedUrls: MutableList<String> = mutableListOf(),
    ): ShowdownReplayService {
        val replays = (0 until count).map { index ->
            """{"id":"gen9vgc-$index","uploadtime":${newest - index * 10},"rating":${rating(index)},""" +
                """"players":["a","b"],"private":${if (isPrivate(index)) 1 else 0},"format":"[Gen 9] VGC"}"""
        }
        val client = HttpClient(MockEngine { request ->
            requestedUrls.add(request.url.toString())
            val before = request.url.parameters["before"]?.toLong()
            val page = replays.withIndex()
                .filter { (index, _) -> before == null || newest - index * 10 < before }
                .take(51)
                .map { it.value }
            respond("[${page.joinToString(",")}]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(json) }
        }
        return ShowdownReplayService(client, ReplayAnalyticsService(client))
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
    fun theProgressTellsHowManyReplaysWereKept() = runTest {
        val progress = mutableListOf<Int>()
        serviceListing(120).list("gen9vgc", since = 0) { progress.add(it) }
        assertEquals(listOf(51, 102, 120), progress)
    }
}
