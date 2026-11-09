package com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity

import kotlinx.serialization.Serializable

/**
 * A saved public Showdown replay. Its format is the one of the store it's in
 */
@Serializable
data class ShowdownReplaySummaryEntity(
    val id: String,
    val uploadTime: Long,
    val rating: Int? = null,
    val winner: String? = null,
    val players: List<ShowdownReplayPlayerEntity>,
    val bestOf: BestOfEntity? = null,
)

@Serializable
data class ShowdownReplayPlayerEntity(
    val name: String,
    val team: List<String>,
    // in the team's order, null when not known
    val items: List<String?> = emptyList(),
    val brought: List<String>,
    val beforeElo: Int? = null,
    val afterElo: Int? = null,
)
