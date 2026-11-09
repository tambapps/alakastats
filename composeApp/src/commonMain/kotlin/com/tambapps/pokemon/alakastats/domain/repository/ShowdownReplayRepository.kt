package com.tambapps.pokemon.alakastats.domain.repository

import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary

/**
 * The public Showdown replays saved on the device, by format
 */
interface ShowdownReplayRepository {

    /** @param formatId the format's id on Showdown, e.g. gen9championsvgc2026regmcbo3 */
    suspend fun list(formatId: String): List<ShowdownReplaySummary>

    /** Adds the replays to the saved ones of their format, a replay already saved being replaced */
    suspend fun save(formatId: String, replays: List<ShowdownReplaySummary>)
}
