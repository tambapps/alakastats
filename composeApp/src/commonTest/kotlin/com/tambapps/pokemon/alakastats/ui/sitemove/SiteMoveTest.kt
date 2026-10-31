package com.tambapps.pokemon.alakastats.ui.sitemove

import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SiteMoveTest {

    @Test
    fun onlyTheNewAddressIsNotAnOldOne() {
        assertTrue(isOldWebAddress("tambapps.github.io"))
        assertFalse(isOldWebAddress("alakastats.com"))
        assertFalse(isOldWebAddress("www.alakastats.com"))
        // the development server
        assertFalse(isOldWebAddress("localhost"))
        assertFalse(isOldWebAddress("127.0.0.1"))
        // the native apps
        assertFalse(isOldWebAddress(null))
    }

    @Test
    fun theTeamsTheCloudDoesntHaveHaveToBeMoved() {
        val synced = team("synced")
        val notSynced = team("not synced")

        assertEquals(listOf(notSynced), teamsToMove(listOf(synced, notSynced), teamsInCloud = setOf(synced.id)))
        assertEquals(listOf(synced, notSynced), teamsToMove(listOf(synced, notSynced), teamsInCloud = emptySet()))
    }

    private fun team(name: String) = TeamlyticsPreview(
        id = Uuid.random(),
        name = name,
        sdNames = emptyList(),
        pokemons = emptyList(),
        nbReplays = 0,
        winrate = 0,
        lastUpdatedAt = Instant.DISTANT_PAST,
    )
}
