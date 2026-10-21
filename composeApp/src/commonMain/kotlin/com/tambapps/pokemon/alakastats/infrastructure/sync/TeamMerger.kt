package com.tambapps.pokemon.alakastats.infrastructure.sync

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.MatchupNotesEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsDataEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsNotesEntity
import com.tambapps.pokemon.alakastats.util.insertInChain
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * What the other version brought to the local one
 */
data class MergeSummary(
    val replaysAdded: Int = 0,
    val replaysRemoved: Int = 0,
    val plansAdded: Int = 0,
    val plansKeptTwice: Int = 0,
    val textsKeptTwice: Int = 0,
)

data class MergeResult(val team: TeamlyticsEntity, val summary: MergeSummary)

/**
 * Three-way merge of two versions of a team, changed apart since a common version (the base).
 * What only one side changed is taken from that side. When both changed the same thing:
 * - added items (replays, game plans) are all kept, an item deleted on one side is deleted, unless the other side
 *   changed it;
 * - a text (notes) is kept in both versions, one after the other;
 * - a game plan is kept in both versions;
 * - what can't be merged (name, paste, format, a replay's content) is taken from the newest version.
 * Without base, nothing can be told deleted: everything is kept.
 */
object TeamMerger {

    const val SEPARATOR = "\n\n--- Other version (from another device) ---\n"
    const val OTHER_VERSION_SUFFIX = " (other version)"

    /**
     * @param base the last version both sides had, null if unknown
     * @param local the version of this device
     * @param remote the other version
     * @param localIsNewer for what can't be merged, the newest version wins
     */
    fun merge(
        base: TeamFingerprint?,
        local: TeamlyticsEntity,
        remote: TeamlyticsEntity,
        localIsNewer: Boolean,
        now: Instant,
    ): MergeResult = Merge(base, localIsNewer).run(local, remote, now)

    private class Merge(private val base: TeamFingerprint?, private val localIsNewer: Boolean) {

        private var textsKeptTwice = 0
        private var plansKeptTwice = 0

        fun run(local: TeamlyticsEntity, remote: TeamlyticsEntity, now: Instant): MergeResult {
            val replays = mergeReplays(local.replays, remote.replays)
            val plans = mergePlans(local.matchupNotes.orEmpty(), remote.matchupNotes.orEmpty())
            val team = local.copy(
                name = pickNewest(base?.name, local.name, remote.name) { hash(it) },
                pokePaste = pickNewest(base?.pokePaste, local.pokePaste, remote.pokePaste) { hash(it) },
                format = pickNewest(base?.format, local.format, remote.format) { hash(it) },
                sdNames = mergeSdNames(local.sdNames, remote.sdNames),
                replays = replays.items,
                notes = mergeNotes(local.notes, remote.notes),
                matchupNotes = if (local.matchupNotes == null && remote.matchupNotes == null) null else plans.items,
                data = mergeData(local.data, remote.data),
                lastUpdatedAt = now,
            )
            val summary = MergeSummary(
                replaysAdded = replays.added,
                replaysRemoved = replays.removed,
                plansAdded = plans.added,
                plansKeptTwice = plansKeptTwice,
                textsKeptTwice = textsKeptTwice,
            )
            return MergeResult(team, summary)
        }

        private fun <T> pickNewest(baseHash: Long?, local: T, remote: T, hash: (T) -> Long): T {
            val localHash = hash(local)
            val remoteHash = hash(remote)
            return when {
                localHash == remoteHash -> local
                baseHash != null && localHash == baseHash -> remote
                baseHash != null && remoteHash == baseHash -> local
                localIsNewer -> local
                else -> remote
            }
        }

        private fun mergeText(baseHash: Long?, local: String?, remote: String?): String? {
            val localText = normalizedText(local)
            val remoteText = normalizedText(remote)
            return when {
                localText == remoteText -> localText
                baseHash != null && hashText(localText) == baseHash -> remoteText
                baseHash != null && hashText(remoteText) == baseHash -> localText
                // removed on one side, written on the other: the text wins
                localText == null -> remoteText
                remoteText == null -> localText
                // already merged, e.g. a merge done again after a failed upload
                localText.contains(remoteText) -> localText
                remoteText.contains(localText) -> remoteText
                else -> {
                    textsKeptTwice++
                    localText + SEPARATOR + remoteText
                }
            }
        }

        private fun mergeSdNames(local: List<String>, remote: List<String>): List<String> {
            val baseNames = base?.sdNames?.toSet() ?: return (local + remote).distinct()
            // kept if both have it, or if added by one side
            val kept = local.filter { it in remote || it !in baseNames }
            val addedByRemote = remote.filter { it !in local && it !in baseNames }
            return (kept + addedByRemote).distinct()
        }

        private fun mergeNotes(local: TeamlyticsNotesEntity?, remote: TeamlyticsNotesEntity?): TeamlyticsNotesEntity? {
            if (local == null && remote == null) return null
            val teamNotes = mergeText(base?.teamNotes, local?.teamNotes, remote?.teamNotes)
            val localPokemonNotes = local?.pokemonNotes.orEmpty()
            val remotePokemonNotes = remote?.pokemonNotes.orEmpty()
            val pokemonNotes = (localPokemonNotes.keys + remotePokemonNotes.keys).mapNotNull { pokemon ->
                // a pokemon missing from the base had no notes
                val baseHash = base?.let { it.pokemonNotes[pokemon] ?: hashText(null) }
                mergeText(baseHash, localPokemonNotes[pokemon], remotePokemonNotes[pokemon])?.let { pokemon to it }
            }.toMap()
            return TeamlyticsNotesEntity(teamNotes = teamNotes ?: "", pokemonNotes = pokemonNotes)
        }

        // computed from the paste and PokeApi, so a cache: no conflict, the local one is preferred
        private fun mergeData(local: TeamlyticsDataEntity?, remote: TeamlyticsDataEntity?): TeamlyticsDataEntity? {
            if (local == null || remote == null) return local ?: remote
            return TeamlyticsDataEntity(remote.pokemonData + local.pokemonData)
        }

        private fun mergeReplays(local: List<ReplayAnalyticsEntity>, remote: List<ReplayAnalyticsEntity>): MergedItems<ReplayAnalyticsEntity> {
            val merged = mergeItems(
                baseHashes = base?.replays?.mapValues { (_, replay) -> replay.content },
                local = local,
                remote = remote,
                key = { it.reference },
                hash = { hash(it) },
                mergeBoth = { localReplay, remoteReplay ->
                    val notes = mergeText(base?.replays?.get(localReplay.reference)?.notes, localReplay.notes, remoteReplay.notes)
                    listOf((if (localIsNewer) localReplay else remoteReplay).copy(notes = notes))
                },
            )
            // the replays coming from the other side go at their spot in the chain of battles
            val replays = merged.items.toMutableList()
            replays.insertInChain(merged.addedItems, reference = { it.reference }, nextReference = { it.nextBattleRef })
            return merged.copy(items = replays)
        }

        private fun mergePlans(local: List<MatchupNotesEntity>, remote: List<MatchupNotesEntity>): MergedItems<MatchupNotesEntity> {
            val merged = mergeItems(
                baseHashes = base?.matchupPlans,
                local = local,
                remote = remote,
                key = { it.id.toString() },
                hash = { hash(it) },
                mergeBoth = { localPlan, remotePlan ->
                    // already kept in both versions by a previous merge
                    if (local.any { it.hasSameContentAs(remotePlan) }) listOf(localPlan)
                    else {
                        plansKeptTwice++
                        listOf(localPlan, remotePlan.copy(id = Uuid.random(), name = remotePlan.name + OTHER_VERSION_SUFFIX))
                    }
                },
            )
            return merged.copy(items = merged.items + merged.addedItems)
        }

        private fun MatchupNotesEntity.hasSameContentAs(other: MatchupNotesEntity) =
            pokePaste == other.pokePaste && gamePlans == other.gamePlans

        /**
         * @param baseHashes the items of the base by key, null if there is no base
         * @param mergeBoth gives the item(s) to keep for an item both sides changed
         * @return the local items to keep (in their order), and apart the ones only the remote side has
         */
        private fun <T> mergeItems(
            baseHashes: Map<String, Long>?,
            local: List<T>,
            remote: List<T>,
            key: (T) -> String,
            hash: (T) -> Long,
            mergeBoth: (local: T, remote: T) -> List<T>,
        ): MergedItems<T> {
            val remoteByKey = remote.associateBy(key)
            val localKeys = local.map(key).toSet()
            var removed = 0

            val items = local.flatMap { localItem ->
                val itemKey = key(localItem)
                val remoteItem = remoteByKey[itemKey]
                val baseHash = baseHashes?.get(itemKey)
                val localHash = hash(localItem)
                when {
                    remoteItem != null -> {
                        val remoteHash = hash(remoteItem)
                        when {
                            localHash == remoteHash -> listOf(localItem)
                            baseHash != null && localHash == baseHash -> listOf(remoteItem)
                            baseHash != null && remoteHash == baseHash -> listOf(localItem)
                            else -> mergeBoth(localItem, remoteItem)
                        }
                    }
                    // no base, or added locally
                    baseHash == null -> listOf(localItem)
                    // deleted on the other side, but changed here: the change wins
                    localHash != baseHash -> listOf(localItem)
                    else -> {
                        removed++
                        emptyList()
                    }
                }
            }

            val addedItems = remote.filter { remoteItem ->
                val itemKey = key(remoteItem)
                if (itemKey in localKeys) return@filter false
                val baseHash = baseHashes?.get(itemKey)
                // added on the other side, or deleted here but changed on the other side
                baseHash == null || hash(remoteItem) != baseHash
            }
            return MergedItems(items, addedItems, added = addedItems.size, removed = removed)
        }
    }

    private data class MergedItems<T>(val items: List<T>, val addedItems: List<T>, val added: Int, val removed: Int)
}
