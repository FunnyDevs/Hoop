package app.hoop.data

import app.hoop.data.config.ParseConfig
import app.hoop.data.media.MediaFileReader
import app.hoop.data.remote.ParsePostsApi
import app.hoop.data.remote.SessionHolder
import app.hoop.data.remote.createParseHttpClient
import app.hoop.data.repository.ParsePostsRepository
import app.hoop.data.session.InMemorySessionTokenStore
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import app.hoop.domain.common.getOrNull
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostsTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val config = ParseConfig("https://example.test/parse", "app", "")
    private val position = GeoPoint.create(45.5, 9.2).getOrNull()!!
    private val radius = Radius.create(1000).getOrNull()!!
    private val home = Hoop.Home(HoopId("home-server-id"), radius)
    private val extra = Hoop.Extra(HoopId("hoop-1"), "Lavoro", position, radius)
    private val photo = MediaRef("/cache/shot.jpg", "image/jpeg", capturedAtEpochMillis = 1L)
    private val photoBytes = byteArrayOf(1, 2, 3, 4)

    private val requests = mutableListOf<HttpRequestData>()
    private val uploadedBytes = mutableListOf<ByteArray>()

    private fun repository(
        reader: MediaFileReader = MediaFileReader { photoBytes },
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): ParsePostsRepository {
        val client = createParseHttpClient(config, SessionHolder(InMemorySessionTokenStore()), MockEngine { request ->
            requests += request
            // The upload body is a stream that closes with the request: read it while it is open.
            if ("/files/" in request.url.encodedPath) uploadedBytes += request.body.toByteArray()
            handler(request)
        })
        return ParsePostsRepository(ParsePostsApi(client), reader)
    }

    private fun MockRequestHandleScope.created(request: HttpRequestData): HttpResponseData = when {
        request.url.encodedPath.contains("/files/") ->
            respond("""{"name":"stored.jpg","url":"https://files.test/stored.jpg"}""", HttpStatusCode.Created, json)
        else -> respond("""{"result":{"objectId":"p1"}}""", HttpStatusCode.OK, json)
    }

    private fun HttpRequestData.bodyText(): String = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    private fun draft(hoop: Hoop, type: PostType, text: String, media: MediaRef? = null, address: String? = null) =
        (PostDraft.create(hoop, type, text, media, position, "client-1", address) as DomainResult.Success).value

    @Test fun questionInHomeSendsOnlyTheFunctionCallWithoutHoopId() = runTest {
        val result = repository { created(it) }.publish(draft(home, PostType.Question, "Chi apre?"))
        assertEquals("p1", result.getOrNull()?.value)
        assertEquals(1, requests.size)
        val body = requests.single().bodyText()
        assertTrue(requests.single().url.encodedPath.endsWith("/functions/saveNewPost"))
        assertTrue("\"type\":\"Q\"" in body && "\"text\":\"Chi apre?\"" in body && "\"latitude\":45.5" in body)
        assertTrue("hoopId" !in body && "media" !in body)
    }

    @Test fun addressIsSentWhenKnown() = runTest {
        repository { created(it) }.publish(draft(home, PostType.Question, "Chi apre?", address = "Via Tadino 12"))
        repository { created(it) }.publish(draft(home, PostType.Question, "Chi apre?"))
        assertTrue("\"address\":\"Via Tadino 12\"" in requests[0].bodyText())
        assertTrue("address" !in requests[1].bodyText())
    }

    @Test fun questionInAnExtraHoopCarriesItsId() = runTest {
        repository { created(it) }.publish(draft(extra, PostType.Question, "Chi apre?"))
        assertTrue("\"hoopId\":\"hoop-1\"" in requests.single().bodyText())
    }

    @Test fun photoIsUploadedFirstThenReferencedByThePost() = runTest {
        val result = repository { created(it) }.publish(draft(home, PostType.Photo, "Che luce", photo))
        assertEquals("p1", result.getOrNull()?.value)
        assertEquals(2, requests.size)
        val upload = requests[0]
        assertTrue(upload.url.encodedPath.endsWith(".jpg") && "/files/" in upload.url.encodedPath)
        assertContentEquals(photoBytes, uploadedBytes.first())
        assertEquals("image/jpeg", upload.body.contentType.toString())
        val body = requests[1].bodyText()
        assertTrue("\"__type\":\"File\"" in body && "\"name\":\"stored.jpg\"" in body && "\"type\":\"P\"" in body)
        assertTrue("\"mimeType\":\"image/jpeg\"" in body)
    }

    @Test fun theClientIdNamesTheFileAndMarksThePost() = runTest {
        repository { created(it) }.publish(draft(home, PostType.Photo, "Che luce", photo))
        assertTrue("/files/client-1.jpg" in requests[0].url.encodedPath)
        assertTrue("\"cuuid\":\"client-1\"" in requests[1].bodyText())
    }

    @Test fun missingTokenOnAFunctionIsUnauthorized() = runTest {
        val repo = repository { respond("""{"code":141,"error":"no user"}""", HttpStatusCode.BadRequest, json) }
        val result = repo.publish(draft(home, PostType.Question, "Chi apre?"))
        assertEquals(DomainError.Unauthorized, (result as DomainResult.Failure).error)
    }

    @Test fun retryAfterAFailedPostDoesNotUploadTheFileAgain() = runTest {
        var failPost = true
        val repo = repository { request ->
            when {
                request.url.encodedPath.contains("/files/") -> created(request)
                failPost -> respond("""{"code":1,"error":"boom"}""", HttpStatusCode.InternalServerError, json)
                else -> created(request)
            }
        }
        val attempt = draft(home, PostType.Photo, "Che luce", photo)
        assertTrue(repo.publish(attempt) is DomainResult.Failure)
        failPost = false
        assertEquals("p1", repo.publish(attempt).getOrNull()?.value)
        assertEquals(1, requests.count { "/files/" in it.url.encodedPath })
        assertEquals(2, requests.count { it.url.encodedPath.endsWith("/functions/saveNewPost") })
        assertEquals(requests.filter { it.url.encodedPath.endsWith("/functions/saveNewPost") }[0].bodyText(),
            requests.filter { it.url.encodedPath.endsWith("/functions/saveNewPost") }[1].bodyText())
    }

    @Test fun aNewAttemptUploadsItsOwnFile() = runTest {
        val repo = repository { created(it) }
        repo.publish(draft(home, PostType.Photo, "Uno", photo))
        repo.publish(
            (PostDraft.create(home, PostType.Photo, "Due", photo, position, "client-2") as DomainResult.Success).value,
        )
        assertEquals(2, requests.count { "/files/" in it.url.encodedPath })
    }

    @Test fun photoCarriesOrientationAndSizeButNoDuration() = runTest {
        val sized = photo.copy(width = 3000, height = 4000)
        repository { created(it) }.publish(draft(home, PostType.Photo, "Che luce", sized))
        val body = requests.last().bodyText()
        assertTrue("\"orientation\":\"portrait\"" in body && "\"width\":3000" in body && "\"height\":4000" in body)
        assertTrue("durationMillis" !in body && "thumbnail" !in body)
    }

    @Test fun videoUploadsItsThumbnailAndSendsDuration() = runTest {
        val video = MediaRef("/cache/clip.mp4", "video/mp4", 1L, durationMillis = 3200, width = 1920, height = 1080, thumbnailUri = "/cache/thumb.jpg")
        repository { created(it) }.publish(draft(home, PostType.Video, "Live", video))
        val uploads = requests.filter { "/files/" in it.url.encodedPath }
        assertEquals(2, uploads.size)
        assertTrue(uploads[1].url.encodedPath.endsWith("client-1-thumb.jpg"))
        assertEquals("image/jpeg", uploads[1].body.contentType.toString())
        val body = requests.last().bodyText()
        assertTrue("\"durationMillis\":3200" in body && "\"orientation\":\"landscape\"" in body)
        assertTrue("\"thumbnail\":{" in body && "\"__type\":\"File\"" in body)
    }

    @Test fun aFailedThumbnailNeverBlocksTheVideo() = runTest {
        val video = MediaRef("/cache/clip.mp4", "video/mp4", 1L, durationMillis = 3200, thumbnailUri = "/cache/thumb.jpg")
        val repo = repository { request ->
            if (request.url.encodedPath.endsWith("-thumb.jpg")) respond("""{"code":5002,"error":"bad"}""", HttpStatusCode.BadRequest, json)
            else created(request)
        }
        assertEquals("p1", repo.publish(draft(home, PostType.Video, "Live", video)).getOrNull()?.value)
        assertTrue("thumbnail" !in requests.last().bodyText())
    }

    @Test fun progressGrowsAndEndsAtOne() = runTest {
        val seen = mutableListOf<Float>()
        repository { created(it) }.publish(draft(home, PostType.Photo, "Che luce", photo)) { seen += it }
        assertTrue(seen.isNotEmpty() && seen.last() == 1f)
        assertEquals(seen.sorted(), seen)
    }

    @Test fun rateLimiterBecomesTooManyRequests() = runTest {
        val repo = repository { respond("Too many requests", HttpStatusCode.TooManyRequests) }
        val result = repo.publish(draft(home, PostType.Question, "Chi apre?")) {}
        assertEquals(DomainError.TooManyRequests, (result as DomainResult.Failure).error)
    }

    @Test fun videoIsNamedMp4() = runTest {
        val video = MediaRef("/cache/clip.mp4", "video/mp4", capturedAtEpochMillis = 1L, durationMillis = 3000)
        repository { created(it) }.publish(draft(home, PostType.Video, "Live", video))
        assertTrue(requests[0].url.encodedPath.endsWith(".mp4"))
        assertTrue("\"type\":\"V\"" in requests[1].bodyText())
    }

    @Test fun unreadableFileFailsWithoutCallingTheServer() = runTest {
        val result = repository(reader = MediaFileReader { null }) { created(it) }.publish(draft(home, PostType.Photo, "Che luce", photo))
        assertEquals(DomainError.Unknown, (result as DomainResult.Failure).error)
        assertTrue(requests.isEmpty())
    }

    @Test fun serverRefusalForDistanceBecomesAViolation() = runTest {
        val repo = repository {
            respond("""{"code":5004,"error":"too far"}""", HttpStatusCode.BadRequest, json)
        }
        val result = repo.publish(draft(extra, PostType.Question, "Chi apre?"))
        assertEquals(DomainError.Validation(Violation.PositionOutsideHoop), (result as DomainResult.Failure).error)
    }

    @Test fun offlineIsANetworkError() = runTest {
        val repo = repository { throw io.ktor.utils.io.errors.IOException("offline") }
        val result = repo.publish(draft(home, PostType.Question, "Chi apre?"))
        assertEquals(DomainError.Network, (result as DomainResult.Failure).error)
        assertNull((result as? DomainResult.Success)?.value)
    }
}
