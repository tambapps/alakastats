package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.time.Clock
import kotlin.uuid.Uuid

class EditTeamlyticsUseCase(
    private val teamlyticsRepository: TeamlyticsRepository
) {

    suspend fun edit(
        team: Teamlytics,
        name: String,
        pokePaste: PokePaste,
        sdNames: List<String>
    ): Either<GetTeamlyticsError, Teamlytics> = teamlyticsRepository.save(team.copy(
        name = name,
        pokePaste = pokePaste,
        sdNames = sdNames,
    ))

    suspend fun create(
        name: String,
        pokePaste: PokePaste,
        sdNames: List<String>
    ): Either<GetTeamlyticsError, Teamlytics> {
        val teamlytics = Teamlytics(
            id = Uuid.random(),
            name = name,
            pokePaste = pokePaste,
            replays = emptyList(),
            sdNames = sdNames,
            lastUpdatedAt = Clock.System.now(),
            notes = null
        )
        return teamlyticsRepository.save(teamlytics)
    }
}
