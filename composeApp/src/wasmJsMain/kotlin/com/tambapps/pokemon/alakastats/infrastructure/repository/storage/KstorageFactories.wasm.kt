package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.util.Identifiable
import io.github.xxfast.kstore.storage.storeOf
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

actual fun createTeamlyticsKStorage() = createKStorage<Uuid, TeamlyticsEntity>("teamlytics")

actual fun createTeamlyticsPreviewKStorage() = createKStorage<Uuid, TeamlyticsPreviewEntity>("teamlytics-preview")

private inline fun <reified ID: @Serializable Any, reified T: @Serializable Identifiable<ID>> createKStorage(resourceName: String): KStorage<ID, T> {
    return KStorageImpl(
        storeOf(
            key = getIdsKey(resourceName),
            enableCache = false,
        )
    ) { id ->
        storeOf(
            key = getEntityKey(resourceName, id),
            enableCache = false,
        )
    }
}


private fun getIdsKey(resourceName: String) = "repositories/$resourceName/ids.kstore"

private fun <ID: @Serializable Any> getEntityKey(resourceName: String, id: ID) = "repositories/$resourceName/$id.kstore"