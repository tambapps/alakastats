package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamlyticsPreviewEntity
import com.tambapps.pokemon.alakastats.util.Identifiable
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import io.github.xxfast.kstore.file.extensions.listStoreOf
import io.github.xxfast.kstore.file.extensions.storeOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSURL
import kotlin.uuid.Uuid
import org.koin.core.component.KoinComponent

actual fun createTeamlyticsKStorage() = createKStorage<Uuid, TeamlyticsEntity>("teamlytics")

actual fun createTeamlyticsPreviewKStorage() = createKStorage<Uuid, TeamlyticsPreviewEntity>("teamlytics-previews")

actual suspend fun downloadToFile(fileName: String, extension: String, bytes: ByteArray): Boolean {
    val file = FileKit.openFileSaver(suggestedName = fileName, extension = extension) ?: return false
    file.write(bytes)
    return true
}

private inline fun <reified ID: @Serializable Any, reified T: @Serializable Identifiable<ID>> createKStorage(resourceName: String): KStorage<ID, T> {
    return object : KoinComponent {
    }.let { koinComponent ->
        KStorageImpl(
            listStoreOf(
                file = getIdsPath(resourceName),
                enableCache = false
            )
        ) { id ->
            storeOf(
                file = getEntityPath(resourceName, id),
                enableCache = false,
                version = 0
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getRepositoryDir(resourceName: String): String {
    val fileManager = NSFileManager.defaultManager
    val documentDirectory = fileManager.URLsForDirectory(
        directory = NSDocumentDirectory,
        inDomains = NSUserDomainMask
    ).first() as NSURL

    val repositoryPath = documentDirectory.path + "/repositories/$resourceName/"

    // Create directory if it doesn't exist
    fileManager.createDirectoryAtPath(
        path = repositoryPath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )

    return repositoryPath
}

private fun getIdsPath(resourceName: String) = Path(getRepositoryDir(resourceName), "ids.kstore")

private fun <ID: @Serializable Any> getEntityPath(resourceName: String, id: ID) = Path(getRepositoryDir(resourceName), "$id.kstore")
