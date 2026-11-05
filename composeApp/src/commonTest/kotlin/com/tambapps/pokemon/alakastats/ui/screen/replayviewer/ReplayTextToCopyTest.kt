package com.tambapps.pokemon.alakastats.ui.screen.replayviewer

import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayTextToCopyTest {

    private fun replay(url: String?) = ReplayAnalytics(
        players = emptyList(),
        uploadTime = 0,
        format = "",
        rating = null,
        version = "",
        winner = null,
        url = url,
        reference = "gen9vgc2026-123",
        nextBattleRef = null,
        notes = null,
    )

    @Test
    fun aReplayWithAnUrlCopiesItsPage() {
        assertEquals(
            "https://replay.pokemonshowdown.com/gen9vgc2026-123",
            replay("https://replay.pokemonshowdown.com/gen9vgc2026-123.json").textToCopy(),
        )
    }

    @Test
    fun aReplayWithoutUrlCopiesItsReference() {
        assertEquals("gen9vgc2026-123", replay(url = null).textToCopy())
    }
}
