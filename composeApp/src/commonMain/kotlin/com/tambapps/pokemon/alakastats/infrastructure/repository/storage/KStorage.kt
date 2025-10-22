package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import arrow.core.Either
import com.tambapps.pokemon.alakastats.util.Identifiable
import kotlinx.serialization.Serializable

data class KStorageError(val message: String, val throwable: Throwable? = null)

interface KStorage<ID: @Serializable Any, T: @Serializable Identifiable<ID>> {

    suspend fun listIds(): List<ID>

    suspend fun listEntities(): List<T>

    suspend fun get(id: ID): T?

    suspend fun save(e: T): Either<KStorageError, T>

    suspend fun delete(e: T)

    suspend fun delete(id: ID)
}