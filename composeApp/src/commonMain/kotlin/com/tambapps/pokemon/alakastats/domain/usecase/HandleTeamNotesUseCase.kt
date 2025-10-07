package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsNotes

interface HandleTeamNotesUseCase {
    suspend fun setNotes(team: Teamlytics, notes: TeamlyticsNotes?)
}
