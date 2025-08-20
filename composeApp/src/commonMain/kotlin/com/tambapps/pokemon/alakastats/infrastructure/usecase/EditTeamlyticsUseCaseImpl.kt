package com.tambapps.pokemon.alakastats.infrastructure.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.domain.usecase.EditTeamlyticsUseCase
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.time.Clock
import kotlin.uuid.Uuid

class EditTeamlyticsUseCaseImpl(
    private val teamlyticsRepository: TeamlyticsRepository
) : EditTeamlyticsUseCase {

    override suspend fun edit(
        team: Teamlytics,
        name: String,
        pokePaste: PokePaste,
        sdNames: List<String>
    ): Teamlytics = teamlyticsRepository.save(team.copy(
        name = name,
        pokePaste = pokePaste,
        sdNames = sdNames,
        lastUpdatedAt = Clock.System.now()
    ))

    override suspend fun create(
        name: String,
        pokePaste: PokePaste,
        sdNames: List<String>
    ): Teamlytics {
        val teamlytics = Teamlytics(
            id = Uuid.random(),
            name = name,
            pokePaste = pokePaste,
            replays = emptyList(),
            sdNames = sdNames,
            lastUpdatedAt = Clock.System.now()
        )
        return teamlyticsRepository.save(teamlytics)
    }
}