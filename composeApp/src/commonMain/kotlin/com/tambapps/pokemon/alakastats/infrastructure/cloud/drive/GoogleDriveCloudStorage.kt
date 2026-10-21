package com.tambapps.pokemon.alakastats.infrastructure.cloud.drive

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudError
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudFile
import com.tambapps.pokemon.alakastats.infrastructure.cloud.CloudStorage
import com.tambapps.pokemon.alakastats.infrastructure.cloud.RetryPolicy
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
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
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Google Drive v3 REST client storing files in the hidden appDataFolder, using the user's own quota.
 */
class GoogleDriveCloudStorage(
    private val httpClient: HttpClient,
    private val tokenProvider: OAuthTokenProvider,
    private val json: Json,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
) : CloudStorage {

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

    override suspend fun download(fileId: String) = call(
        request = {
            method = HttpMethod.Get
            url("$API_URL/files/$fileId")
            parameter("alt", "media")
        },
        handle = { it.bodyAsBytes() }
    )

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
        return if (bytes.size > MULTIPART_MAX_SIZE) resumableUpload(metadata, bytes, existingFileId)
        else multipartUpload(metadata, bytes, existingFileId)
    }

    override suspend fun delete(fileId: String) = call(
        request = {
            method = HttpMethod.Delete
            url("$API_URL/files/$fileId")
        },
        handle = { }
    )

    private suspend fun multipartUpload(metadata: DriveFileMetadataDto, bytes: ByteArray, existingFileId: String?) = call(
        request = {
            uploadTarget(existingFileId)
            parameter("uploadType", "multipart")
            parameter("fields", FILE_FIELDS)
            val boundary = "alakastats-${Uuid.random().toHexString()}"
            setBody(
                ByteArrayContent(
                    bytes = multipartRelatedBody(boundary, json.encodeToString(metadata), bytes),
                    contentType = ContentType("multipart", "related").withParameter("boundary", boundary)
                )
            )
        },
        handle = { json.decodeFromString<DriveFileDto>(it.bodyAsText()).toCloudFile() }
    )

    /**
     * Two steps upload for big files: one request to open a session, one to send the content
     */
    private suspend fun resumableUpload(metadata: DriveFileMetadataDto, bytes: ByteArray, existingFileId: String?): Either<CloudError, CloudFile> {
        val sessionUrl = call(
            request = {
                uploadTarget(existingFileId)
                parameter("uploadType", "resumable")
                parameter("fields", FILE_FIELDS)
                header("X-Upload-Content-Type", JSON_CONTENT_TYPE.toString())
                header("X-Upload-Content-Length", bytes.size.toString())
                setBody(ByteArrayContent(json.encodeToString(metadata).encodeToByteArray(), ContentType.Application.Json))
            },
            handle = { it.headers[HttpHeaders.Location] }
        ).getOrElse { return it.left() }
            ?: return CloudError.Unknown("No upload session URL returned by Google Drive").left()

        return call(
            request = {
                method = HttpMethod.Put
                url(sessionUrl)
                setBody(ByteArrayContent(bytes, JSON_CONTENT_TYPE))
            },
            handle = { json.decodeFromString<DriveFileDto>(it.bodyAsText()).toCloudFile() }
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

    /**
     * Sends the request with a fresh token, retrying once silently on 401 and with backoff on rate limits
     */
    private suspend fun <T> call(
        request: HttpRequestBuilder.() -> Unit,
        handle: suspend (HttpResponse) -> T,
    ): Either<CloudError, T> {
        val attempt = retryPolicy.run(shouldRetry = { it is Attempt.Retryable }) { attempt(request) }
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

    private suspend fun attempt(request: HttpRequestBuilder.() -> Unit): Attempt {
        val token = tokenProvider.accessToken(interactive = false).getOrElse { return Attempt.Failure(it) }
        var response = sendRetryingNetwork(token, request).getOrElse { return Attempt.Failure(it) }
        if (response.status.value == 401) {
            // the access token expired or was revoked: get a new one silently and try again once
            tokenProvider.invalidate(token)
            val newToken = tokenProvider.accessToken(interactive = false).getOrElse { return Attempt.Failure(it) }
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
            status == 403 && reasons.any { it in ACCESS_REASONS } -> Attempt.Failure(CloudError.AccessRevoked())
            status == 404 -> Attempt.Failure(CloudError.NotFound(message))
            else -> Attempt.Failure(CloudError.Unknown(message))
        }
    }

    private fun DriveFileDto.toCloudFile() = CloudFile(
        id = id,
        name = name,
        modifiedAt = modifiedTime?.let { runCatching { Instant.parse(it) }.getOrNull() },
        properties = appProperties.orEmpty(),
    )

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
        // Google recommends resumable uploads above 5MB
        internal const val MULTIPART_MAX_SIZE = 5 * 1024 * 1024
        private val JSON_CONTENT_TYPE = ContentType.Application.Json
        private val RATE_LIMIT_REASONS = setOf("rateLimitExceeded", "userRateLimitExceeded")
        private val STORAGE_FULL_REASONS = setOf("storageQuotaExceeded", "quotaExceeded")
        private val ACCESS_REASONS = setOf("insufficientPermissions", "appNotAuthorizedToFile", "authError")

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
