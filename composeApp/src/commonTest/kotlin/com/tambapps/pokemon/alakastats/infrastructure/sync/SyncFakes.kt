package com.tambapps.pokemon.alakastats.infrastructure.sync

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudStorage
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorage
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.KStorageError
import com.tambapps.pokemon.alakastats.util.Identifiable
import kotlin.time.Clock
import kotlin.time.Instant

class FakeCloudStorage(private val clock: Clock) : CloudStorage {

    data class StoredFile(val name: String, val bytes: ByteArray, val properties: Map<String, String>, val modifiedAt: Instant)

    val files = linkedMapOf<String, StoredFile>()
    private var nextId = 0

    var listError: CloudError? = null
    var uploadError: CloudError? = null
    var downloadError: CloudError? = null
    var calls = 0
        private set
    var uploads = 0
        private set

    override suspend fun list(): Either<CloudError, List<CloudFile>> {
        calls++
        listError?.let { return it.left() }
        return files.map { (id, file) -> CloudFile(id, file.name, file.modifiedAt, file.properties) }.right()
    }

    override suspend fun download(fileId: String): Either<CloudError, ByteArray> {
        calls++
        downloadError?.let { return it.left() }
        return files[fileId]?.bytes?.right() ?: CloudError.NotFound("No file $fileId").left()
    }

    override suspend fun upload(
        name: String,
        bytes: ByteArray,
        properties: Map<String, String>,
        existingFileId: String?,
    ): Either<CloudError, CloudFile> {
        calls++
        uploads++
        uploadError?.let { return it.left() }
        if (existingFileId != null && existingFileId !in files) return CloudError.NotFound("No file $existingFileId").left()
        val id = existingFileId ?: "file-${nextId++}"
        val file = StoredFile(name, bytes, properties, clock.now())
        files[id] = file
        return CloudFile(id, name, file.modifiedAt, properties).right()
    }

    override suspend fun delete(fileId: String): Either<CloudError, Unit> {
        calls++
        return if (files.remove(fileId) != null) Unit.right() else CloudError.NotFound("No file $fileId").left()
    }
}

class InMemoryKStorage<ID : Any, T : Identifiable<ID>> : KStorage<ID, T> {
    private val entities = linkedMapOf<ID, T>()

    override suspend fun listIds() = entities.keys.toList()
    override suspend fun listEntities() = entities.values.toList()
    override suspend fun get(id: ID) = entities[id]
    override suspend fun save(e: T): Either<KStorageError, T> {
        entities[e.id] = e
        return e.right()
    }
    override suspend fun delete(e: T) = delete(e.id)
    override suspend fun delete(id: ID) {
        entities.remove(id)
    }
}

class InMemorySyncStateStore : SyncStateStore {
    var state = SyncState()
    override suspend fun load() = state
    override suspend fun save(state: SyncState) {
        this.state = state
    }
}

class FakeTokenProvider : OAuthTokenProvider {
    var interactiveError: CloudError? = null
    var signedOut = false
        private set

    override suspend fun accessToken(interactive: Boolean): Either<CloudError, String> {
        if (interactive) interactiveError?.let { return it.left() }
        return "token".right()
    }

    override suspend fun invalidate(token: String) = Unit

    override suspend fun signOut() {
        signedOut = true
    }
}

class MutableClock(var now: Instant) : Clock {
    override fun now() = now
}
