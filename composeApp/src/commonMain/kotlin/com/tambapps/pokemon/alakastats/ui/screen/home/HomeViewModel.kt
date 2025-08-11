package com.tambapps.pokemon.alakastats.ui.screen.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.usecase.ListTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(
    val imageService: PokemonImageService,
    private val listTeamlyticsUseCase: ListTeamlyticsUseCase
): ScreenModel {
    
    val teamlyticsList: SnapshotStateList<TeamlyticsPreview> = mutableStateListOf()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun loadTeams() {
        scope.launch {
            val previews = listTeamlyticsUseCase.list()
            teamlyticsList.clear()
            teamlyticsList.addAll(previews)
        }
    }
}