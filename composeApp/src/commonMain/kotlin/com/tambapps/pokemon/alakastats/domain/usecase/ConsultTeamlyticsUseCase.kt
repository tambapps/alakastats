package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics

interface ConsultTeamlyticsUseCase {

    val team: Teamlytics

    val hasFilteredReplays: Boolean
}