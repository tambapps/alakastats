package com.tambapps.pokemon.alakastats.ui.screen.quizzes

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.Nature

class NatureQuizSetupViewModel : ScreenModel {
    val natures: List<Nature> = Nature.entries

    val ignoredNatures: SnapshotStateList<Nature> = mutableStateListOf(
        Nature.DOCILE, Nature.BASHFUL, Nature.QUIRKY, Nature.SERIOUS
    )
    private var ignoredNaturesBackup: List<Nature> = emptyList()

    var showIgnoredNaturesDialog by mutableStateOf(false)
        private set

    fun openIgnoredNaturesDialog() {
        ignoredNaturesBackup = ignoredNatures.toList()
        showIgnoredNaturesDialog = true
    }

    fun confirmIgnoredNaturesDialog() {
        showIgnoredNaturesDialog = false
    }

    fun cancelIgnoredNaturesDialog() {
        ignoredNatures.clear()
        ignoredNatures.addAll(ignoredNaturesBackup)
        showIgnoredNaturesDialog = false
    }

    fun toggleIgnored(nature: Nature) {
        if (!ignoredNatures.remove(nature)) {
            ignoredNatures.add(nature)
        }
    }

    fun startQuiz() {
    }
}
