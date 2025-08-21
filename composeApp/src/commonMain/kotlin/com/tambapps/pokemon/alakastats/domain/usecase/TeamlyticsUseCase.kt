package com.tambapps.pokemon.alakastats.domain.usecase

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import kotlin.uuid.Uuid

interface TeamlyticsUseCase {
    suspend fun get(id: Uuid): Teamlytics?
    suspend fun save(team: Teamlytics): Teamlytics
}