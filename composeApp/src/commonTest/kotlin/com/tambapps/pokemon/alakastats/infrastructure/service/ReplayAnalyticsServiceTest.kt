package com.tambapps.pokemon.alakastats.infrastructure.service

import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.transformer.toDomain
import com.tambapps.pokemon.alakastats.domain.transformer.toEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
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
import kotlin.test.assertNull

class ReplayAnalyticsServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun serviceReturning(log: String) = ReplayAnalyticsService(HttpClient(MockEngine {
        val body = """{"formatid":"gen9championsvgc2026regmabo3","players":["me","opponent"],"uploadtime":1788930485,""" +
                """"rating":null,"log":${JsonPrimitive(log)}}"""
        respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        install(ContentNegotiation) { json(json) }
    })

    @Test
    fun gameOfASeriesKnowsItsPlaceAndItsNextGame() = runTest {
        val log = """
            |player|p1|me|
            |player|p2|opponent|
            |uhtml|bestof|<h2><strong>Game 1</strong> of <a href="/game-bestof3-gen9championsvgc2026regmabo3-100">a best-of-3</a></h2>
            |uhtml|next|Next: <a href="/battle-gen9championsvgc2026regmabo3-102"><strong>Game 2 of 3</strong></a>
        """.trimIndent()

        val replay = serviceReturning(log).fetch("https://replay.pokemonshowdown.com/gen9championsvgc2026regmabo3-101").getOrNull()!!

        assertEquals(BestOf(1, 3, "game-bestof3-gen9championsvgc2026regmabo3-100"), replay.bestOf)
        assertEquals("gen9championsvgc2026regmabo3-102", replay.nextBattleRef)
        // kept when saved
        val saved = json.decodeFromString<ReplayAnalyticsEntity>(json.encodeToString(replay.toEntity()))
        assertEquals(replay.bestOf, saved.toDomain().bestOf)
    }

    @Test
    fun lastGameOfASeriesHasNoNextGame() = runTest {
        val log = """
            |player|p1|me|
            |player|p2|opponent|
            |uhtml|bestof|<h2><strong>Game 3</strong> of <a href="/game-bestof3-gen9championsvgc2026regmabo3-100">a best-of-3</a></h2>
        """.trimIndent()

        val replay = serviceReturning(log).fetch("https://replay.pokemonshowdown.com/gen9championsvgc2026regmabo3-103").getOrNull()!!

        assertEquals(3, replay.bestOf?.game)
        // the series' link isn't a next game
        assertNull(replay.nextBattleRef)
    }

    @Test
    fun replaySavedBeforeTheSeriesWereKnownHasNone() {
        val saved = """{"players":[],"uploadTime":0,"format":"gen9vgc","rating":null,"version":"1.0","winner":null,""" +
                """"url":null,"reference":"r","nextBattleRef":null}"""

        assertNull(json.decodeFromString<ReplayAnalyticsEntity>(saved).toDomain().bestOf)
    }
}
