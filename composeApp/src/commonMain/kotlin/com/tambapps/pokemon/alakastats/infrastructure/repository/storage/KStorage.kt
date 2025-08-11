package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.util.Identifiable
import kotlinx.serialization.Serializable

interface KStorage<ID: @Serializable Any, T: @Serializable Identifiable<ID>> {

    suspend fun listIds(): List<ID>

    suspend fun listEntities(): List<T>

    suspend fun save(e: T): T

    suspend fun delete(e: T)
}