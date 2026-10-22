package com.tambapps.pokemon.alakastats.infrastructure.cloud

import arrow.core.Either
import arrow.core.right
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.PRIVATE_SYNC_SCOPES
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.SHARING_SCOPES
import com.tambapps.pokemon.alakastats.infrastructure.cloud.drive.GoogleDriveCloudStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class GoogleDriveCloudStorageTest {

    private class CountingTokenProvider : OAuthTokenProvider {
        var generation = 1
        val invalidated = mutableListOf<String>()
        val askedScopes = mutableListOf<Set<String>>()
        override suspend fun accessToken(interactive: Boolean, scopes: Set<String>): Either<CloudError, String> {
            askedScopes.add(scopes)
            return "token-$generation".right()
        }
        override suspend fun invalidate(token: String) {
            invalidated.add(token)
            generation++
        }
        override suspend fun signOut() = Unit
    }

    private val tokenProvider = CountingTokenProvider()
    private val requests = mutableListOf<HttpRequestData>()
    private var retrySleeps = 0

    private fun storage(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) = GoogleDriveCloudStorage(
        httpClient = HttpClient(MockEngine { request ->
            requests.add(request)
            handler(request)
        }),
        tokenProvider = tokenProvider,
        json = Json { ignoreUnknownKeys = true },
        retryPolicy = RetryPolicy(maxAttempts = 3, sleep = { retrySleeps++ }),
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.error(status: Int, reason: String) =
        json("""{"error":{"code":$status,"message":"$reason","errors":[{"reason":"$reason"}]}}""", HttpStatusCode.fromValue(status))

    private val fileJson = """{"id":"f1","name":"team-1.json","modifiedTime":"2026-10-21T10:00:00.123Z","appProperties":{"teamId":"1"}}"""

    @Test
    fun listsAllPagesOfTheAppDataFolder() = runTest {
        val storage = storage { request ->
            if (request.url.parameters["pageToken"] == null) json("""{"files":[$fileJson],"nextPageToken":"p2"}""")
            else json("""{"files":[{"id":"f2","name":"team-2.json"}]}""")
        }

        val files = storage.list().getOrNull()!!

        assertEquals(listOf("f1", "f2"), files.map { it.id })
        assertEquals(mapOf("teamId" to "1"), files.first().properties)
        assertEquals(Instant.parse("2026-10-21T10:00:00.123Z"), files.first().modifiedAt)
        assertTrue(requests.all { it.url.parameters["spaces"] == "appDataFolder" })
        assertEquals("Bearer token-1", requests.first().headers[HttpHeaders.Authorization])
    }

    @Test
    fun createsWithAMultipartRequestInTheAppDataFolder() = runTest {
        val storage = storage { json(fileJson) }

        storage.upload("team-1.json", """{"name":"Rain"}""".encodeToByteArray(), mapOf("teamId" to "1"))

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/upload/drive/v3/files", request.url.encodedPath)
        assertEquals("multipart", request.url.parameters["uploadType"])
        val body = request.body as ByteArrayContent
        assertEquals("multipart/related", body.contentType?.withoutParameters()?.toString())
        val text = body.bytes().decodeToString()
        assertContains(text, """"parents":["appDataFolder"]""")
        assertContains(text, """"appProperties":{"teamId":"1"}""")
        assertContains(text, """{"name":"Rain"}""")
    }

    @Test
    fun updatesWithPatchAndWithoutParents() = runTest {
        val storage = storage { json(fileJson) }

        storage.upload("team-1.json", "{}".encodeToByteArray(), emptyMap(), existingFileId = "f1")

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/upload/drive/v3/files/f1", request.url.encodedPath)
        assertFalse((request.body as ByteArrayContent).bytes().decodeToString().contains("parents"))
    }

    @Test
    fun bigFilesUseAResumableUpload() = runTest {
        val storage = storage { request ->
            if (request.url.parameters["uploadType"] == "resumable") {
                respond("", HttpStatusCode.OK, headersOf(HttpHeaders.Location, "https://upload.example/session"))
            } else json(fileJson)
        }

        val result = storage.upload("big.json", ByteArray(6 * 1024 * 1024), emptyMap())

        assertEquals("f1", result.getOrNull()?.id)
        assertEquals(listOf(HttpMethod.Post, HttpMethod.Put), requests.map { it.method })
        assertEquals("https://upload.example/session", requests[1].url.toString())
    }

    @Test
    fun expiredTokenIsRefreshedSilently() = runTest {
        val storage = storage { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer token-1") error(401, "authError")
            else json(fileJson)
        }

        val result = storage.download("f1")

        assertTrue(result.isRight())
        assertEquals(listOf("token-1"), tokenProvider.invalidated)
        assertEquals("Bearer token-2", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun repeated401MeansAccessRevoked() = runTest {
        val storage = storage { error(401, "authError") }

        assertIs<CloudError.AccessRevoked>(storage.list().leftOrNull())
    }

    @Test
    fun rateLimitsAreRetriedWithBackoff() = runTest {
        var calls = 0
        val storage = storage {
            if (++calls < 3) error(429, "rateLimitExceeded") else json("""{"files":[]}""")
        }

        val result = storage.list()

        assertTrue(result.isRight())
        assertEquals(3, calls)
        assertEquals(2, retrySleeps)
    }

    @Test
    fun forbiddenRateLimitGivesUpAfterMaxAttempts() = runTest {
        val storage = storage { error(403, "userRateLimitExceeded") }

        assertIs<CloudError.RateLimited>(storage.list().leftOrNull())
        assertEquals(3, requests.size)
    }

    @Test
    fun serverErrorsAreRetried() = runTest {
        var calls = 0
        val storage = storage { if (++calls == 1) error(503, "backendError") else json("""{"files":[]}""") }

        assertTrue(storage.list().isRight())
        assertEquals(2, calls)
    }

    @Test
    fun fullStorageIsNotRetried() = runTest {
        val storage = storage { error(403, "storageQuotaExceeded") }

        assertIs<CloudError.StorageFull>(storage.upload("a", ByteArray(1), emptyMap()).leftOrNull())
        assertEquals(1, requests.size)
    }

    @Test
    fun missingFileIsNotFound() = runTest {
        val storage = storage { error(404, "notFound") }

        assertIs<CloudError.NotFound>(storage.delete("f1").leftOrNull())
    }

    @Test
    fun networkFailureIsReportedAfterOneRetry() = runTest {
        val storage = storage { throw IOException("offline") }

        assertIs<CloudError.Network>(storage.list().leftOrNull())
        assertEquals(2, requests.size)
        assertEquals(1, retrySleeps)
    }

    @Test
    fun transientNetworkFailureIsRetried() = runTest {
        var calls = 0
        val storage = storage { if (++calls == 1) throw IOException("connection reset") else json("""{"files":[]}""") }

        assertTrue(storage.list().isRight())
        assertEquals(2, calls)
    }

    private fun HttpRequestData.bodyText() = (body as ByteArrayContent).bytes().decodeToString()

    @Test
    fun privateSyncOnlyAsksTheAppDataScope() = runTest {
        storage { json("""{"files":[]}""") }.list()

        assertEquals(listOf(PRIVATE_SYNC_SCOPES), tokenProvider.askedScopes)
    }

    @Test
    fun sharedFileIsReadWithTheSharingScopes() = runTest {
        val storage = storage {
            json("""{"id":"s1","name":"Rain.json","version":"42","headRevisionId":"rev2","trashed":false,"ownedByMe":false,
                "owners":[{"emailAddress":"a@gmail.com"}],"capabilities":{"canEdit":true,"canShare":false},"appProperties":{"teamId":"1"}}""")
        }

        val file = storage.getSharedFile("s1").getOrNull()!!

        assertEquals(listOf(SHARING_SCOPES), tokenProvider.askedScopes)
        assertEquals(42, file.version)
        assertEquals("rev2", file.headRevisionId)
        assertEquals(ShareRole.EDITOR, file.role)
        assertEquals("a@gmail.com", file.ownerEmail)
        assertContains(requests.single().url.parameters["fields"]!!, "capabilities(canEdit,canShare)")
    }

    @Test
    fun existingFolderIsReused() = runTest {
        val storage = storage { json("""{"files":[{"id":"folder1"}]}""") }

        assertEquals("folder1", storage.ensureFolder("Alakastats").getOrNull())
        assertEquals(1, requests.size)
        assertContains(requests.single().url.parameters["q"]!!, "name = 'Alakastats'")
    }

    @Test
    fun missingFolderIsCreated() = runTest {
        val storage = storage { request ->
            if (request.method == HttpMethod.Get) json("""{"files":[]}""") else json("""{"id":"new"}""")
        }

        assertEquals("new", storage.ensureFolder("Alakastats").getOrNull())
        assertContains(requests[1].bodyText(), "application/vnd.google-apps.folder")
    }

    @Test
    fun sharedFileIsCreatedInTheFolder() = runTest {
        val storage = storage { json("""{"id":"s1","name":"Rain.json","version":"1"}""") }

        storage.uploadSharedFile("Rain.json", "{}".encodeToByteArray(), mapOf("teamId" to "1"), existingFileId = null, folderId = "folder1")

        assertContains(requests.single().bodyText(), """"parents":["folder1"]""")
        assertTrue("appDataFolder" !in requests.single().bodyText())
    }

    @Test
    fun shareSendsAnInvitationWithTheRole() = runTest {
        val storage = storage { json("{}") }

        storage.share("s1", "b@gmail.com", ShareRole.VIEWER, "Join on the web")

        val request = requests.single()
        assertEquals("/drive/v3/files/s1/permissions", request.url.encodedPath)
        assertEquals("true", request.url.parameters["sendNotificationEmail"])
        assertEquals("Join on the web", request.url.parameters["emailMessage"])
        val body = request.bodyText()
        assertContains(body, """"type":"user"""")
        assertContains(body, """"role":"reader"""")
        assertContains(body, """"emailAddress":"b@gmail.com"""")
    }

    @Test
    fun membersAreListedWithTheirRoles() = runTest {
        val storage = storage {
            json("""{"permissions":[{"id":"p1","role":"owner","emailAddress":"a@gmail.com"},{"id":"p2","role":"writer","emailAddress":"b@gmail.com"},{"id":"p3","role":"reader"}]}""")
        }

        val members = storage.listMembers("s1").getOrNull()!!

        assertEquals(listOf(ShareRole.OWNER, ShareRole.EDITOR, ShareRole.VIEWER), members.map { it.role })
    }

    @Test
    fun editorsCantInvite() = runTest {
        val storage = storage { json("""{"id":"s1"}""") }

        storage.setEditorsCanShare("s1", false)

        assertEquals(HttpMethod.Patch, requests.single().method)
        assertContains(requests.single().bodyText(), """"writersCanShare":false""")
    }

    @Test
    fun revisionsTellWhoWroteThem() = runTest {
        val storage = storage {
            json("""{"revisions":[{"id":"r1","lastModifyingUser":{"me":true}},{"id":"r2","lastModifyingUser":{"me":false}}]}""")
        }

        val revisions = storage.listRevisions("s1").getOrNull()!!

        assertEquals(listOf("r1" to true, "r2" to false), revisions.map { it.id to it.byMe })
    }

    @Test
    fun fileNotPickedIsAFileLevelError() = runTest {
        val storage = storage { error(403, "appNotAuthorizedToFile") }

        // not a revoked access: the other files still work
        assertIs<CloudError.FileAccessDenied>(storage.getSharedFile("s1").leftOrNull())
    }

    @Test
    fun accountEmailWithThePrivateScopeOnly() = runTest {
        val storage = storage { json("""{"user":{"emailAddress":"me@gmail.com"}}""") }

        assertEquals("me@gmail.com", storage.accountEmail().getOrNull())
        assertEquals("/drive/v3/about", requests.single().url.encodedPath)
        assertEquals(listOf(PRIVATE_SYNC_SCOPES), tokenProvider.askedScopes)
    }
}
