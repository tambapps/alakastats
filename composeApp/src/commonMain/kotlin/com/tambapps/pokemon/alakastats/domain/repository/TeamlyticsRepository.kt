package com.tambapps.pokemon.alakastats.domain.repository

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import kotlin.uuid.Uuid

interface TeamlyticsRepository {

    suspend fun list(): List<Teamlytics>

    suspend fun listPreviews(): List<TeamlyticsPreview>

    suspend fun save(teamlytics: Teamlytics): Teamlytics

    suspend fun delete(teamlytics: Teamlytics)

    suspend fun delete(id: Uuid)

}