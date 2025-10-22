package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import arrow.core.Either
import com.tambapps.pokemon.alakastats.util.Identifiable
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.extensions.getOrEmpty
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

abstract class AbstractKStorage<ID: @Serializable Any, T : @Serializable Identifiable<ID>>: KStorage<ID, T> {

    protected abstract val idsStore: KStore<List<ID>>

    protected abstract fun getStore(id: ID): KStore<T>

    override suspend fun listIds() = idsStore.getOrEmpty()

    override suspend fun listEntities() = coroutineScope {
        val ids = listIds()
        val tasks = ids.map { id ->
            async { getStore(id).get() }
        }
        tasks.mapNotNull { it.await() }
    }

    override suspend fun get(id: ID) = getStore(id).get()

    override suspend fun save(e: T): Either<KStorageError, T> = Either.catch {
        val ids = listIds()
        val entityId = e.id
        if (!ids.contains(entityId)) {
            idsStore.update { (it ?: emptyList()) + entityId }
        }
        getStore(entityId).set(e)
        e
    }.mapLeft { throwable ->
        KStorageError("Failed to save entity: ${throwable.message}", throwable)
    }

    override suspend fun delete(e: T) = delete(e.id)

    override suspend fun delete(id: ID) {
        val ids = listIds()
        if (!ids.contains(id)) {
            return
        }
        idsStore.update { (it ?: emptyList()) - id }
        getStore(id).delete()
    }
}