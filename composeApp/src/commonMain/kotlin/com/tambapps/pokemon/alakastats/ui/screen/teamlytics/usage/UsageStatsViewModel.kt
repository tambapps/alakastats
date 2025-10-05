package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.usage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.withContext
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsageStatsViewModel(
    val team: Teamlytics,
    val pokemonImageService: PokemonImageService,
    ) {

    var isLoading by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)

    fun loadStats() {
        if (isLoading) {
            return
        }
        isLoading = true
        scope.launch {
            team.withContext {
                // TODO
            }
            isLoading = false
        }
    }




}
