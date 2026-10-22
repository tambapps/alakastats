package com.tambapps.pokemon.alakastats.ui.screen.cloudsync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamMerge
import com.tambapps.pokemon.alakastats.infrastructure.sync.TeamSyncService
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar

/**
 * Tells the user about each team a sync merged, and what else a sync has to say, whatever the screen
 */
@Composable
fun SyncMergeNotifier(syncService: TeamSyncService) {
    val snackBar = LocalSnackBar.current
    LaunchedEffect(syncService) {
        syncService.merges.collect { merge -> snackBar.show(merge.userMessage()) }
    }
    LaunchedEffect(syncService) {
        syncService.notices.collect { notice -> snackBar.show(notice) }
    }
}

fun TeamMerge.userMessage(): String {
    val details = buildList {
        if (summary.replaysAdded > 0) add("${count(summary.replaysAdded, "replay")} added")
        if (summary.replaysRemoved > 0) add("${count(summary.replaysRemoved, "replay")} removed")
        if (summary.plansAdded > 0) add("${count(summary.plansAdded, "game plan")} added")
        if (summary.plansKeptTwice > 0) add("${count(summary.plansKeptTwice, "game plan")} kept in both versions")
        if (summary.textsKeptTwice > 0) add("${count(summary.textsKeptTwice, "note")} kept in both versions")
    }
    val message = "$teamName: merged with the Google Drive version"
    return if (details.isEmpty()) message else "$message (${details.joinToString()})"
}

private fun count(count: Int, word: String) = "$count $word" + if (count > 1) "s" else ""
