package com.tambapps.pokemon.alakastats.domain.repository

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import kotlin.uuid.Uuid

interface TeamlyticsRepository {

    suspend fun list(): List<Teamlytics>

    suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics>

    suspend fun listPreviews(): List<TeamlyticsPreview>

    /**
     * @param base the version the team was changed from. If the stored team changed since (e.g. a sync brought
     * changes), both are merged instead of overwriting the stored one. Null for a new team, or to overwrite
     * @return the saved team, merged if needed
     */
    suspend fun save(teamlytics: Teamlytics, base: Teamlytics? = null): Either<GetTeamlyticsError, Teamlytics>

    suspend fun delete(teamlytics: Teamlytics)

    suspend fun delete(id: Uuid)
}