package app.hoop.data

import app.hoop.data.config.ParseConfig
import app.hoop.data.remote.ParseAuthApi
import app.hoop.data.remote.SessionHolder
import app.hoop.data.remote.createParseHttpClient
import app.hoop.data.repository.ParseAuthRepository
import app.hoop.data.session.InMemorySessionTokenStore
import app.hoop.domain.common.getOrNull
import app.hoop.domain.identity.LoginIdentifier
import app.hoop.domain.identity.Password
import io.ktor.client.request.HttpResponseData
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** runTest skips delays, which would fire the repository timeout at once: these tests run in real time. */
private fun realTimeTest(block: suspend () -> Unit) = runTest { withContext(Dispatchers.Default) { block() } }

class SessionTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val userJson = """{"objectId":"u1","username":"marta","email":"marta@hoop.app"}"""
    private val config = ParseConfig("https://example.test/parse", "app", "")

    private fun repository(store: InMemorySessionTokenStore, handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): ParseAuthRepository {
        val session = SessionHolder(store)
        val client = createParseHttpClient(config, session, MockEngine { request -> handler(request) })
        return ParseAuthRepository(ParseAuthApi(client), session)
    }

    @Test fun signInPostsCredentialsInTheBodyAndSavesTheToken() = realTimeTest {
        val store = InMemorySessionTokenStore()
        var login: HttpRequestData? = null
        val repo = repository(store) { request ->
            if (request.url.encodedPath.endsWith("/login")) {
                login = request
                respond("""{"objectId":"u1","username":"marta","sessionToken":"r:abc"}""", HttpStatusCode.OK, json)
            } else respond(userJson, HttpStatusCode.OK, json)
        }
        val result = repo.signIn(LoginIdentifier.create("marta").getOrNull()!!, Password.create("secret123").getOrNull()!!)
        assertNotNull(result.getOrNull())
        assertEquals(HttpMethod.Post, login?.method)
        assertEquals("", login?.url?.encodedQuery)
        assertEquals("r:abc", store.load())
    }

    @Test fun signInReadsTheEmailFromTheCloudFunctionWhenUsersMeLeavesItOut() = realTimeTest {
        val store = InMemorySessionTokenStore()
        val repo = repository(store) { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/login") -> respond("""{"objectId":"u1","username":"marta","sessionToken":"r:abc"}""", HttpStatusCode.OK, json)
                path.endsWith("/users/me") -> respond("""{"objectId":"u1","username":"marta"}""", HttpStatusCode.OK, json)
                path.endsWith("/functions/getCurrentUser") -> respond("""{"result":$userJson}""", HttpStatusCode.OK, json)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val user = repo.signIn(LoginIdentifier.create("marta").getOrNull()!!, Password.create("secret123").getOrNull()!!).getOrNull()
        assertEquals("marta@hoop.app", user?.email?.value)
    }

    @Test fun restoreWithoutTokenIsSignedOut() = realTimeTest {
        assertFalse(repository(InMemorySessionTokenStore()) { respondError(HttpStatusCode.InternalServerError) }.restoreSession())
    }

    @Test fun restoreWithValidTokenSendsItToUsersMe() = realTimeTest {
        val store = InMemorySessionTokenStore().apply { save("r:abc") }
        var seen: HttpRequestData? = null
        val repo = repository(store) { seen = it; respond(userJson, HttpStatusCode.OK, json) }
        assertTrue(repo.restoreSession())
        assertTrue(seen?.url?.encodedPath?.endsWith("/users/me") == true)
        assertEquals("r:abc", seen?.headers?.get("X-Parse-Session-Token"))
        assertEquals("r:abc", store.load())
    }

    @Test fun restoreWithRevokedTokenClearsIt() = realTimeTest {
        val store = InMemorySessionTokenStore().apply { save("r:old") }
        val repo = repository(store) { respond("""{"code":209,"error":"invalid session token"}""", HttpStatusCode.BadRequest, json) }
        assertFalse(repo.restoreSession())
        assertNull(store.load())
    }

    @Test fun restoreOfflineKeepsTheSession() = realTimeTest {
        val store = InMemorySessionTokenStore().apply { save("r:abc") }
        val repo = repository(store) { throw IOException("offline") }
        assertTrue(repo.restoreSession())
        assertEquals("r:abc", store.load())
    }

    @Test fun signOutClearsTheSavedToken() = realTimeTest {
        val store = InMemorySessionTokenStore().apply { save("r:abc") }
        val repo = repository(store) { respond("{}", HttpStatusCode.OK, json) }
        repo.signOut()
        assertNull(store.load())
    }
}
