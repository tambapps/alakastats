package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.pokepaste.parser.PokePaste

interface EditTeamlyticsUseCase {
    suspend fun edit(
        team: Teamlytics,
        name: String,
        pokePaste: PokePaste,
        sdNames: List<String>
    ): Teamlytics

    suspend fun create(
        name: String,
        pokePaste: PokePaste,
        sdNames: List<String>
    ): Teamlytics
}