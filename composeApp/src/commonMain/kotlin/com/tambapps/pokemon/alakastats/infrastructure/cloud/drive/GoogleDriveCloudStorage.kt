package com.tambapps.pokemon.alakastats.infrastructure.cloud.drive

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.FileRevision
import com.tambapps.pokemon.alakastats.infrastructure.cloud.Member
import com.tambapps.pokemon.alakastats.infrastructure.cloud.RetryPolicy
import com.tambapps.pokemon.alakastats.infrastructure.cloud.ShareRole
import com.tambapps.pokemon.alakastats.infrastructure.cloud.SharedFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.SharingCloudStorage
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.PRIVATE_SYNC_SCOPES
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.SHARING_SCOPES
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Google Drive v3 REST client, using the user's own quota.
 * The private sync uses the hidden appDataFolder (scope drive.appdata), the shared teams visible files that the app
 * created or that the user picked (scope drive.file).
 */
class GoogleDriveCloudStorage(
    private val httpClient: HttpClient,
    private val tokenProvider: OAuthTokenProvider,
    private val json: Json,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
) : SharingCloudStorage {

    override suspend fun list(): Either<CloudError, List<CloudFile>> {
        val files = mutableListOf<CloudFile>()
        var pageToken: String? = null
        do {
            val page = call(
                request = {
                    method = HttpMethod.Get
                    url("$API_URL/files")
                    parameter("spaces", APP_DATA_FOLDER)
                    parameter("fields", "nextPageToken,files($FILE_FIELDS)")
                    parameter("pageSize", 1000)
                    pageToken?.let { parameter("pageToken", it) }
                },
                handle = { json.decodeFromString<DriveFileListDto>(it.bodyAsText()) }
            ).getOrElse { return it.left() }
            files.addAll(page.files.map { it.toCloudFile() })
            pageToken = page.nextPageToken
        } while (pageToken != null)
        return files.right()
    }

    override suspend fun download(fileId: String) = downloadWith(PRIVATE_SYNC_SCOPES, fileId)

    override suspend fun upload(
        name: String,
        bytes: ByteArray,
        properties: Map<String, String>,
        existingFileId: String?,
    ): Either<CloudError, CloudFile> {
        val metadata = DriveFileMetadataDto(
            name = name,
            appProperties = properties,
            parents = if (existingFileId == null) listOf(APP_DATA_FOLDER) else null
        )
        return uploadFile(PRIVATE_SYNC_SCOPES, metadata, bytes, existingFileId, FILE_FIELDS).map { it.toCloudFile() }
    }

    override suspend fun delete(fileId: String) = call(
        request = {
            method = HttpMethod.Delete
            url("$API_URL/files/$fileId")
        },
        handle = { }
    )

    override suspend fun ensureFolder(name: String): Either<CloudError, String> {
        // with drive.file, only the folders the app created are listed
        val existing = call(
            scopes = SHARING_SCOPES,
            request = {
                method = HttpMethod.Get
                url("$API_URL/files")
                parameter("q", "name = '${name.replace("'", "\\'")}' and mimeType = '$FOLDER_MIME_TYPE' and trashed = false")
                parameter("fields", "files(id)")
            },
            handle = { json.decodeFromString<DriveFileListDto>(it.bodyAsText()) }
        ).getOrElse { return it.left() }
        existing.files.firstOrNull()?.let { return it.id.right() }

        return call(
            scopes = SHARING_SCOPES,
            request = {
                method = HttpMethod.Post
                url("$API_URL/files")
                parameter("fields", "id")
                jsonBody(json.encodeToString(DriveFolderDto(name, FOLDER_MIME_TYPE)))
            },
            handle = { json.decodeFromString<DriveFileDto>(it.bodyAsText()).id }
        )
    }

    override suspend fun getSharedFile(fileId: String) = call(
        scopes = SHARING_SCOPES,
        request = {
            method = HttpMethod.Get
            url("$API_URL/files/$fileId")
            parameter("fields", SHARED_FILE_FIELDS)
        },
        handle = { json.decodeFromString<DriveFileDto>(it.bodyAsText()).toSharedFile() }
    )

    override suspend fun downloadSharedFile(fileId: String) = downloadWith(SHARING_SCOPES, fileId)

    override suspend fun uploadSharedFile(
        name: String,
        bytes: ByteArray,
        properties: Map<String, String>,
        existingFileId: String?,
        folderId: String?,
    ): Either<CloudError, SharedFile> {
        val metadata = DriveFileMetadataDto(
            name = name,
            appProperties = properties,
            parents = if (existingFileId == null) folderId?.let(::listOf) else null
        )
        return uploadFile(SHARING_SCOPES, metadata, bytes, existingFileId, SHARED_FILE_FIELDS).map { it.toSharedFile() }
    }

    override suspend fun trashSharedFile(fileId: String) = patchFile(fileId, buildJsonObject { put("trashed", true) })

    override suspend fun setEditorsCanShare(fileId: String, canShare: Boolean) =
        patchFile(fileId, buildJsonObject { put("writersCanShare", canShare) })

    override suspend fun share(fileId: String, email: String, role: ShareRole, message: String?) = call(
        scopes = SHARING_SCOPES,
        request = {
            method = HttpMethod.Post
            url("$API_URL/files/$fileId/permissions")
            parameter("sendNotificationEmail", true)
            message?.let { parameter("emailMessage", it) }
            // built by hand: the default values (as type) wouldn't be encoded
            jsonBody(json.encodeToString(buildJsonObject {
                put("type", "user")
                put("role", role.toDriveRole())
                put("emailAddress", email)
            }))
        },
        handle = { }
    )

    override suspend fun listMembers(fileId: String) = call(
        scopes = SHARING_SCOPES,
        request = {
            method = HttpMethod.Get
            url("$API_URL/files/$fileId/permissions")
            parameter("fields", "permissions(id,type,role,emailAddress,displayName)")
        },
        handle = { response ->
            json.decodeFromString<DrivePermissionListDto>(response.bodyAsText()).permissions
                .map { Member(it.id, it.emailAddress, it.displayName, it.role.toShareRole()) }
        }
    )

    override suspend fun setMemberRole(fileId: String, permissionId: String, role: ShareRole) = call(
        scopes = SHARING_SCOPES,
        request = {
            method = HttpMethod.Patch
            url("$API_URL/files/$fileId/permissions/$permissionId")
            jsonBody(json.encodeToString(buildJsonObject { put("role", role.toDriveRole()) }))
        },
        handle = { }
    )

    override suspend fun removeMember(fileId: String, permissionId: String) = call(
        scopes = SHARING_SCOPES,
        request = {
            method = HttpMethod.Delete
            url("$API_URL/files/$fileId/permissions/$permissionId")
        },
        handle = { }
    )

    override suspend fun listRevisions(fileId: String): Either<CloudError, List<FileRevision>> {
        val revisions = mutableListOf<FileRevision>()
        var pageToken: String? = null
        do {
            val page = call(
                scopes = SHARING_SCOPES,
                request = {
                    method = HttpMethod.Get
                    url("$API_URL/files/$fileId/revisions")
                    parameter("fields", "nextPageToken,revisions(id,modifiedTime,lastModifyingUser(me))")
                    parameter("pageSize", 1000)
                    pageToken?.let { parameter("pageToken", it) }
                },
                handle = { json.decodeFromString<DriveRevisionListDto>(it.bodyAsText()) }
            ).getOrElse { return it.left() }
            revisions.addAll(page.revisions.map {
                FileRevision(it.id, it.modifiedTime?.toInstantOrNull(), byMe = it.lastModifyingUser?.me == true)
            })
            pageToken = page.nextPageToken
        } while (pageToken != null)
        return revisions.right()
    }

    override suspend fun downloadRevision(fileId: String, revisionId: String) = call(
        scopes = SHARING_SCOPES,
        request = {
            method = HttpMethod.Get
            url("$API_URL/files/$fileId/revisions/$revisionId")
            parameter("alt", "media")
        },
        handle = { it.bodyAsBytes() }
    )

    private suspend fun downloadWith(scopes: Set<String>, fileId: String) = call(
        scopes = scopes,
        request = {
            method = HttpMethod.Get
            url("$API_URL/files/$fileId")
            parameter("alt", "media")
        },
        handle = { it.bodyAsBytes() }
    )

    private suspend fun patchFile(fileId: String, body: JsonObject) = call(
        scopes = SHARING_SCOPES,
        request = {
            method = HttpMethod.Patch
            url("$API_URL/files/$fileId")
            parameter("fields", "id")
            jsonBody(json.encodeToString(body))
        },
        handle = { }
    )

    private suspend fun uploadFile(
        scopes: Set<String>,
        metadata: DriveFileMetadataDto,
        bytes: ByteArray,
        existingFileId: String?,
        fields: String,
    ): Either<CloudError, DriveFileDto> =
        if (bytes.size > MULTIPART_MAX_SIZE) resumableUpload(scopes, metadata, bytes, existingFileId, fields)
        else multipartUpload(scopes, metadata, bytes, existingFileId, fields)

    private suspend fun multipartUpload(
        scopes: Set<String>,
        metadata: DriveFileMetadataDto,
        bytes: ByteArray,
        existingFileId: String?,
        fields: String,
    ) = call(
        scopes = scopes,
        request = {
            uploadTarget(existingFileId)
            parameter("uploadType", "multipart")
            parameter("fields", fields)
            val boundary = "alakastats-${Uuid.random().toHexString()}"
            setBody(
                ByteArrayContent(
                    bytes = multipartRelatedBody(boundary, json.encodeToString(metadata), bytes),
                    contentType = ContentType("multipart", "related").withParameter("boundary", boundary)
                )
            )
        },
        handle = { json.decodeFromString<DriveFileDto>(it.bodyAsText()) }
    )

    /**
     * Two steps upload for big files: one request to open a session, one to send the content
     */
    private suspend fun resumableUpload(
        scopes: Set<String>,
        metadata: DriveFileMetadataDto,
        bytes: ByteArray,
        existingFileId: String?,
        fields: String,
    ): Either<CloudError, DriveFileDto> {
        val sessionUrl = call(
            scopes = scopes,
            request = {
                uploadTarget(existingFileId)
                parameter("uploadType", "resumable")
                parameter("fields", fields)
                header("X-Upload-Content-Type", JSON_CONTENT_TYPE.toString())
                header("X-Upload-Content-Length", bytes.size.toString())
                jsonBody(json.encodeToString(metadata))
            },
            handle = { it.headers[HttpHeaders.Location] }
        ).getOrElse { return it.left() }
            ?: return CloudError.Unknown("No upload session URL returned by Google Drive").left()

        return call(
            scopes = scopes,
            request = {
                method = HttpMethod.Put
                url(sessionUrl)
                setBody(ByteArrayContent(bytes, JSON_CONTENT_TYPE))
            },
            handle = { json.decodeFromString<DriveFileDto>(it.bodyAsText()) }
        )
    }

    private fun HttpRequestBuilder.uploadTarget(existingFileId: String?) {
        if (existingFileId == null) {
            method = HttpMethod.Post
            url("$UPLOAD_URL/files")
        } else {
            method = HttpMethod.Patch
            url("$UPLOAD_URL/files/$existingFileId")
        }
    }

    private fun HttpRequestBuilder.jsonBody(body: String) {
        setBody(ByteArrayContent(body.encodeToByteArray(), ContentType.Application.Json))
    }

    /**
     * Sends the request with a fresh token, retrying once silently on 401 and with backoff on rate limits
     */
    private suspend fun <T> call(
        scopes: Set<String> = PRIVATE_SYNC_SCOPES,
        request: HttpRequestBuilder.() -> Unit,
        handle: suspend (HttpResponse) -> T,
    ): Either<CloudError, T> {
        val attempt = retryPolicy.run(shouldRetry = { it is Attempt.Retryable }) { attempt(scopes, request) }
        return when (attempt) {
            is Attempt.Success -> try {
                handle(attempt.response).right()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                CloudError.Network(e.message ?: "Network error", e).left()
            } catch (e: Exception) {
                CloudError.Unknown("Unexpected response from Google Drive: ${e.message}", e).left()
            }
            is Attempt.Failure -> attempt.error.left()
            is Attempt.Retryable -> CloudError.RateLimited().left()
        }
    }

    private suspend fun attempt(scopes: Set<String>, request: HttpRequestBuilder.() -> Unit): Attempt {
        val token = tokenProvider.accessToken(interactive = false, scopes).getOrElse { return Attempt.Failure(it) }
        var response = sendRetryingNetwork(token, request).getOrElse { return Attempt.Failure(it) }
        if (response.status.value == 401) {
            // the access token expired or was revoked: get a new one silently and try again once
            tokenProvider.invalidate(token)
            val newToken = tokenProvider.accessToken(interactive = false, scopes).getOrElse { return Attempt.Failure(it) }
            response = sendRetryingNetwork(newToken, request).getOrElse { return Attempt.Failure(it) }
            if (response.status.value == 401) return Attempt.Failure(CloudError.AccessRevoked())
        }
        if (response.status.isSuccess()) return Attempt.Success(response)
        return classifyError(response)
    }

    private suspend fun sendRetryingNetwork(token: String, request: HttpRequestBuilder.() -> Unit): Either<CloudError, HttpResponse> {
        val response = send(token, request)
        if (response.leftOrNull() !is CloudError.Network) return response
        // the connection can need a moment to be back, e.g. right after turning the Wi-Fi on
        retryPolicy.pause(0)
        return send(token, request)
    }

    private suspend fun send(token: String, request: HttpRequestBuilder.() -> Unit): Either<CloudError, HttpResponse> = try {
        httpClient.request {
            request()
            bearerAuth(token)
        }.right()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        CloudError.Network(e.message ?: "Network error", e).left()
    } catch (e: Exception) {
        CloudError.Unknown(e.message ?: "Request to Google Drive failed", e).left()
    }

    private suspend fun classifyError(response: HttpResponse): Attempt {
        val error = runCatching { json.decodeFromString<GoogleErrorResponseDto>(response.bodyAsText()).error }.getOrNull()
        val reasons = error?.errors?.mapNotNull { it.reason }.orEmpty()
        val message = error?.message ?: "Google Drive error ${response.status.value}"
        val status = response.status.value
        return when {
            status == 429 || status >= 500 -> Attempt.Retryable(message)
            status == 403 && reasons.any { it in RATE_LIMIT_REASONS } -> Attempt.Retryable(message)
            status == 403 && reasons.any { it in STORAGE_FULL_REASONS } -> Attempt.Failure(CloudError.StorageFull())
            status == 403 && reasons.any { it in FILE_ACCESS_REASONS } -> Attempt.Failure(CloudError.FileAccessDenied(message))
            status == 403 && reasons.any { it in ACCESS_REASONS } -> Attempt.Failure(CloudError.AccessRevoked())
            status == 404 -> Attempt.Failure(CloudError.NotFound(message))
            else -> Attempt.Failure(CloudError.Unknown(message))
        }
    }

    private fun DriveFileDto.toCloudFile() = CloudFile(
        id = id,
        name = name,
        modifiedAt = modifiedTime?.toInstantOrNull(),
        properties = appProperties.orEmpty(),
    )

    private fun DriveFileDto.toSharedFile() = SharedFile(
        id = id,
        name = name,
        version = version?.toLongOrNull() ?: 0,
        headRevisionId = headRevisionId,
        modifiedAt = modifiedTime?.toInstantOrNull(),
        properties = appProperties.orEmpty(),
        trashed = trashed,
        canEdit = capabilities?.canEdit == true,
        canShare = capabilities?.canShare == true,
        ownedByMe = ownedByMe,
        ownerEmail = owners.firstOrNull()?.emailAddress,
    )

    private fun String.toInstantOrNull() = runCatching { Instant.parse(this) }.getOrNull()

    private fun ShareRole.toDriveRole() = when (this) {
        ShareRole.OWNER -> "owner"
        ShareRole.EDITOR -> "writer"
        ShareRole.VIEWER -> "reader"
    }

    private fun String.toShareRole() = when (this) {
        "owner" -> ShareRole.OWNER
        "writer", "fileOrganizer", "organizer" -> ShareRole.EDITOR
        else -> ShareRole.VIEWER
    }

    private sealed interface Attempt {
        class Success(val response: HttpResponse) : Attempt
        class Failure(val error: CloudError) : Attempt
        class Retryable(val message: String) : Attempt
    }

    companion object {
        const val APP_DATA_FOLDER = "appDataFolder"
        private const val API_URL = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3"
        private const val FILE_FIELDS = "id,name,modifiedTime,appProperties"
        private const val SHARED_FILE_FIELDS =
            "id,name,version,headRevisionId,modifiedTime,appProperties,trashed,ownedByMe,owners(emailAddress),capabilities(canEdit,canShare)"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        // Google recommends resumable uploads above 5MB
        internal const val MULTIPART_MAX_SIZE = 5 * 1024 * 1024
        private val JSON_CONTENT_TYPE = ContentType.Application.Json
        private val RATE_LIMIT_REASONS = setOf("rateLimitExceeded", "userRateLimitExceeded")
        private val STORAGE_FULL_REASONS = setOf("storageQuotaExceeded", "quotaExceeded")
        // the app can't reach this file: not picked, or the access to it was removed
        private val FILE_ACCESS_REASONS = setOf("appNotAuthorizedToFile", "insufficientFilePermissions")
        private val ACCESS_REASONS = setOf("insufficientPermissions", "authError")

        internal fun multipartRelatedBody(boundary: String, metadataJson: String, content: ByteArray): ByteArray {
            val head = "--$boundary\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                    "$metadataJson\r\n" +
                    "--$boundary\r\n" +
                    "Content-Type: application/json\r\n\r\n"
            val tail = "\r\n--$boundary--\r\n"
            return head.encodeToByteArray() + content + tail.encodeToByteArray()
        }
    }
}
