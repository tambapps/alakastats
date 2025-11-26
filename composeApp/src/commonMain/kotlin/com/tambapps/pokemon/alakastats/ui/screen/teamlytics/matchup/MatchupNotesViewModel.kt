package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup

import com.tambapps.pokemon.alakastats.domain.usecase.ConsultTeamlyticsUseCase
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService

class MatchupNotesViewModel(
    val useCase: ConsultTeamlyticsUseCase,
    val pokemonImageService: PokemonImageService,
) {

    val team get() = useCase.originalTeam
    val matchupNotes get() = team.matchupNotes
    val hasMatchupNotes get() = matchupNotes.isNotEmpty()
}