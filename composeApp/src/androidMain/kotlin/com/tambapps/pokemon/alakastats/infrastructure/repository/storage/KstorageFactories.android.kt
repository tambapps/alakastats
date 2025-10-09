package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import android.content.Context
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.util.Identifiable
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import io.github.xxfast.kstore.file.extensions.listStoreOf
import io.github.xxfast.kstore.file.extensions.storeOf
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

actual fun createTeamlyticsKStorage() = createKStorage<Uuid, TeamlyticsEntity>("teamlytics")

actual fun createTeamlyticsPreviewKStorage() = createKStorage<Uuid, TeamlyticsPreviewEntity>("teamlytics-previews")

actual suspend fun downloadToFile(fileName: String, extension: String, bytes: ByteArray): Boolean {
    val file = FileKit.openFileSaver(suggestedName = fileName, extension = extension) ?: return false
    file.write(bytes)
    return true
}

private inline fun <reified ID: @Serializable Any, reified T: @Serializable Identifiable<ID>> createKStorage(resourceName: String): KStorage<ID, T> {
    return object : KoinComponent {
        val context: Context by inject()
    }.let { koinComponent ->
        KStorageImpl(
            listStoreOf(
                file = getIdsPath(koinComponent.context, resourceName),
                enableCache = false
            )
        ) { id ->
            storeOf(
                file = getEntityPath(koinComponent.context, resourceName, id),
                enableCache = false,
                version = 0
            )
        }
    }
}

private fun getRepositoryDir(context: Context, resourceName: String) = File(context.filesDir, "repositories/$resourceName/").apply {
    mkdirs() // Ensure the directory exists
}

private fun getIdsPath(context: Context, resourceName: String) = Path(getRepositoryDir(context, resourceName).absolutePath, "ids.kstore")
private fun <ID: @Serializable Any> getEntityPath(context: Context, resourceName: String, id: ID) = Path(getRepositoryDir(context, resourceName).absolutePath, "$id.kstore")