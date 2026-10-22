package com.tambapps.pokemon.alakastats.infrastructure.sync

import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Why a team isn't shared anymore, which tells the user's other devices what to do with it
 */
enum class SharedTeamRemoval {
    /** the user left the team: removed from every device */
    LEFT,
    /** the owner deleted it: removed from every device */
    DELETED,
    /** the owner stopped sharing it, or the user lost the access: kept as a private team */
    UNSHARED,
}

/**
 * Which teams of the user are shared, and in which file. Stored in the app's hidden folder, so that every device of
 * the user knows it: a team shared, joined or left on one device is so on the others.
 */
@Serializable
data class SharedTeamsIndex(val teams: Map<Uuid, SharedIndexEntry> = emptyMap()) {

    val sharedTeamIds get() = teams.filterValues { it.removal == null }.keys

    fun with(teamId: Uuid, entry: SharedIndexEntry) = copy(teams = teams + (teamId to entry))

    /**
     * Each team takes the most recent entry of both indexes. At the same date, the removal wins: a team is removed
     * after it was added
     */
    fun merge(other: SharedTeamsIndex) = SharedTeamsIndex(
        (teams.keys + other.teams.keys).associateWith { id ->
            val mine = teams[id]
            val theirs = other.teams[id]
            when {
                mine == null -> theirs!!
                theirs == null -> mine
                theirs.updatedAt > mine.updatedAt -> theirs
                theirs.updatedAt == mine.updatedAt && mine.removal == null && theirs.removal != null -> theirs
                else -> mine
            }
        }
    )
}

/**
 * @param removal set once the team isn't shared anymore, kept so that the other devices learn about it
 */
@Serializable
data class SharedIndexEntry(
    val fileId: String,
    val updatedAt: Instant,
    val removal: SharedTeamRemoval? = null,
)

/**
 * This device's sync state of a shared team
 * @param synced the version last synced with the shared file, base of the merges
 */
@Serializable
data class SharedEntry(
    val fileId: String,
    val role: ShareRole,
    val synced: SyncedEntry? = null,
)
