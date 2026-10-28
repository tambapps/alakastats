package com.tambapps.pokemon.alakastats.infrastructure.service

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.BestOf
import com.tambapps.pokemon.alakastats.domain.model.GameEndReason
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
    fun gameKnowsItsLengthItsKosAndHowItEnded() = runTest {
        // the faint logs give the nickname, the switch ones the species
        val log = """
            |player|p1|me|
            |player|p2|opponent|
            |poke|p1|Pikachu, L50|
            |poke|p2|Garchomp, L50|
            |teampreview|4
            |start
            |switch|p1a: Yami|Pikachu, L50|100/100
            |switch|p1b: Bob|Whimsicott, L50|100/100
            |switch|p2a: Chompy|Garchomp, L50|100/100
            |switch|p2b: Kingambit|Kingambit, L50|100/100
            |turn|1
            |faint|p1a: Yami
            |turn|2
            |switch|p1a: Rex|Incineroar, L50|100/100
            |faint|p2a: Chompy
            |faint|p1a: Rex
            |turn|3
            |-message|me forfeited.
            |win|opponent
        """.trimIndent()

        val replay = serviceReturning(log).fetch("https://replay.pokemonshowdown.com/gen9vgc-1").getOrNull()!!

        assertEquals(3, replay.turns)
        assertEquals(4, replay.pickCount)
        assertEquals(GameEndReason.FORFEIT, replay.endReason)
        assertEquals(listOf(PokemonName("pikachu"), PokemonName("incineroar")), replay.player1.fainted)
        assertEquals(listOf(PokemonName("garchomp")), replay.player2.fainted)
        assertEquals(2, replay.remainingPokemons(replay.player1))
        assertEquals(3, replay.remainingPokemons(replay.player2))
        val saved = json.decodeFromString<ReplayAnalyticsEntity>(json.encodeToString(replay.toEntity())).toDomain()
        assertEquals(replay, saved)
    }

    @Test
    fun timerLossIsTold() = runTest {
        val log = """
            |player|p1|me|
            |player|p2|opponent|
            |turn|7
            |-message|opponent lost due to inactivity.
            |win|me
        """.trimIndent()

        val replay = serviceReturning(log).fetch("https://replay.pokemonshowdown.com/gen9vgc-1").getOrNull()!!

        assertEquals(GameEndReason.TIMER, replay.endReason)
        assertEquals(7, replay.turns)
    }

    @Test
    fun replaySavedBeforeTheSeriesWereKnownHasNone() {
        val saved = """{"players":[],"uploadTime":0,"format":"gen9vgc","rating":null,"version":"1.0","winner":null,""" +
                """"url":null,"reference":"r","nextBattleRef":null}"""

        assertNull(json.decodeFromString<ReplayAnalyticsEntity>(saved).toDomain().bestOf)
    }
}
