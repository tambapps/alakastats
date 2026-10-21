package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.sync.SyncState
import com.tambapps.pokemon.alakastats.util.Identifiable
import io.github.xxfast.kstore.KStore
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import io.github.xxfast.kstore.storeOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.uuid.Uuid

actual fun createTeamlyticsKStorage() = createKStorage<Uuid, TeamlyticsEntity>("teamlytics")

actual fun createTeamlyticsPreviewKStorage() = createKStorage<Uuid, TeamlyticsPreviewEntity>("teamlytics-preview")

actual fun createSyncStateKStore(): KStore<SyncState> = indexedDbStoreOf("repositories/sync/state.kstore")

actual suspend fun downloadToFile(fileName: String, extension: String, bytes: ByteArray): Boolean {
    FileKit.download(bytes, "$fileName.$extension")
    return true
}

private inline fun <reified ID: @Serializable Any, reified T: @Serializable Identifiable<ID>> createKStorage(resourceName: String): KStorage<ID, T> {
    return KStorageImpl(indexedDbStoreOf(getIdsKey(resourceName))) { id ->
        indexedDbStoreOf(getEntityKey(resourceName, id))
    }
}

// the keys are the ones used when the stores were in the localStorage, see IndexedDb's migration
private inline fun <reified T : @Serializable Any> indexedDbStoreOf(key: String): KStore<T> =
    storeOf(codec = IndexedDbCodec(key, serializer<T>()), enableCache = false)

private fun getIdsKey(resourceName: String) = "repositories/$resourceName/ids.kstore"

private fun <ID: @Serializable Any> getEntityKey(resourceName: String, id: ID) = "repositories/$resourceName/$id.kstore"
