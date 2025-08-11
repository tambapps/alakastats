package com.tambapps.pokemon.alakastats.domain.repository

import com.tambapps.pokemon.alakastats.domain.model.Teamlytics

interface TeamlyticsRepository {

    suspend fun list(): List<Teamlytics>

    suspend fun save(teamlytics: Teamlytics): Teamlytics

    suspend fun delete(teamlytics: Teamlytics)

}