package com.tambapps.pokemon.alakastats.infrastructure.sync

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.MatchupNotesEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What a version of a team looked like, enough to tell what changed since then, without keeping a whole copy.
 * The merge never takes a value from it, only from the versions it merges.
 * Hashes are made on the JSON of the values, so that a field added to an entity with its default value doesn't
 * make every item look changed.
 */
@Serializable
data class TeamFingerprint(
    val name: Long,
    val pokePaste: Long,
    val format: Long,
    val sdNames: List<String>,
    val teamNotes: Long,
    val pokemonNotes: Map<String, Long>,
    /** by replay reference */
    val replays: Map<String, ReplayFingerprint>,
    /** by matchup plan id */
    val matchupPlans: Map<String, Long>,
) {
    companion object {
        fun of(team: TeamlyticsEntity) = TeamFingerprint(
            name = hash(team.name),
            pokePaste = hash(team.pokePaste),
            format = hash(team.format),
            sdNames = team.sdNames,
            teamNotes = hashText(team.notes?.teamNotes),
            pokemonNotes = team.notes?.pokemonNotes.orEmpty()
                .filterValues { it.isNotBlank() }
                .mapValues { (_, notes) -> hashText(notes) },
            replays = team.replays.associate { it.reference to ReplayFingerprint.of(it) },
            matchupPlans = team.matchupNotes.orEmpty().associate { it.id.toString() to hash(it) },
        )
    }
}

@Serializable
data class ReplayFingerprint(
    /** the whole replay, notes included */
    val content: Long,
    val notes: Long,
) {
    companion object {
        fun of(replay: ReplayAnalyticsEntity) = ReplayFingerprint(content = hash(replay), notes = hashText(replay.notes))
    }
}

private val json = Json

internal fun hash(value: String?): Long = if (value == null) NULL_HASH else fnv1a(value)

/** blank and missing texts are the same: no notes */
internal fun hashText(value: String?) = hash(normalizedText(value))

internal fun normalizedText(value: String?) = value?.takeIf { it.isNotBlank() }

internal fun hash(replay: ReplayAnalyticsEntity) = hash(replay, ReplayAnalyticsEntity.serializer())

internal fun hash(plan: MatchupNotesEntity) = hash(plan, MatchupNotesEntity.serializer())

private fun <T> hash(value: T, serializer: KSerializer<T>) = fnv1a(json.encodeToString(serializer, value))

private const val NULL_HASH = 0L

// 64 bits FNV-1a: deterministic on every platform, and way less collisions than String.hashCode()
private fun fnv1a(value: String): Long {
    var hash = -0x340d631b7bdddcdbL
    for (char in value) {
        hash = hash xor char.code.toLong()
        hash *= 0x100000001b3L
    }
    return hash
}
