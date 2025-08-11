package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.uuid.Uuid

class CreateTeamlyticsUseCase(
    private val teamlyticsRepository: TeamlyticsRepository
) {

    suspend fun create(
        name: String,
        pokePaste: PokePaste,
        sdNames: List<String>
    ): Teamlytics {
        val teamlytics = Teamlytics(
            id = Uuid.random(),
            name = name,
            pokePaste = pokePaste,
            replays = emptyList(),
            sdNames = sdNames
        )
        return teamlyticsRepository.save(teamlytics)
    }
}