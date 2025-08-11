package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.extensions.getOrEmpty
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

abstract class AbstractKStorage<ID: @Serializable Any, T : @Serializable Any>: KStorage<ID, T> {

    protected abstract val idsStore: KStore<List<ID>>

    protected abstract fun getStore(id: ID): KStore<T>
    protected abstract fun getId(entity: T): ID

    override suspend fun listIds() = idsStore.getOrEmpty()

    override suspend fun listEntities() = coroutineScope {
        val ids = listIds()
        val tasks = ids.map { id ->
            async { getStore(id).get() }
        }
        tasks.mapNotNull { it.await() }
    }

    override suspend fun save(e: T): T {
        val ids = listIds()
        val entityId = getId(e)
        if (!ids.contains(entityId)) {
            idsStore.update { (it ?: emptyList()) + entityId }
        }
        getStore(entityId).set(e)
        return e
    }

    override suspend fun delete(e: T) {
        val ids = listIds()
        val entityId = getId(e)
        if (!ids.contains(entityId)) {
            return
        }
        idsStore.update { (it ?: emptyList()) - entityId }
        getStore(entityId).delete()
    }
}