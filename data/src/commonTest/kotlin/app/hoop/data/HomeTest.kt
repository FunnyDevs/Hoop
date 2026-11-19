package app.hoop.data

import app.hoop.data.config.ParseConfig
import app.hoop.data.remote.ParseHomeApi
import app.hoop.data.remote.SessionHolder
import app.hoop.data.remote.createParseHttpClient
import app.hoop.data.repository.ParseFeedRepository
import app.hoop.data.repository.ParseHoopsRepository
import app.hoop.data.session.InMemorySessionTokenStore
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.errorOrNull
import app.hoop.domain.common.getOrNull
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.identity.NotificationKind
import app.hoop.domain.post.FlyState
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Hoops, feeds, comments and notifications against a Parse server that answers with fixed JSON. */
class HomeTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val config = ParseConfig("https://example.test/parse", "app", "")
    private val requests = mutableListOf<HttpRequestData>()
    private val position = GeoPoint.create(45.5, 9.2).getOrNull()!!
    private val radius = Radius.create(1000).getOrNull()!!

    /** The moment "now" is for the repositories: 2026-11-18T12:00:00Z. */
    private val now = 1_795_003_200_000L

    private fun api(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): ParseHomeApi =
        ParseHomeApi(createParseHttpClient(config, SessionHolder(InMemorySessionTokenStore()), MockEngine { request ->
            requests += request
            handler(request)
        }))

    private fun MockRequestHandleScope.ok(body: String) = respond(body, HttpStatusCode.OK, json)

    private fun HttpRequestData.bodyText(): String = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    private val hoopsJson = """{"result":[
        {"objectId":"home-id","name":"Home","isHome":true,"center":null,"radius":1500,"withNotifications":true},
        {"objectId":"work-id","name":"Lavoro","isHome":false,"center":{"latitude":45.47,"longitude":9.22},"radius":800.0,"withNotifications":true}
    ]}"""

    // ---- Hoops ----

    @Test fun hoopsAreReadFromTheServer() = runTest {
        val result = ParseHoopsRepository(api { ok(hoopsJson) }).getUserHoops().getOrNull()!!
        assertEquals("home-id", result.home.id.value)
        assertEquals(1500, result.home.radius.meters)
        val work = result.extras.single()
        assertEquals("work-id", work.id.value)
        assertEquals("Lavoro", work.name)
        assertEquals(800, work.radius.meters)
        assertEquals(45.47, work.center.latitude)
    }

    @Test fun addingAHoopCreatesItAndReadsThemAgain() = runTest {
        val repository = ParseHoopsRepository(api { request ->
            if (request.url.encodedPath.endsWith("/createHoop")) ok("""{"result":{"objectId":"work-id","radius":800}}""") else ok(hoopsJson)
        })
        val result = repository.addHoop("Lavoro", position, Radius.create(800).getOrNull()!!)
        assertEquals(1, result.getOrNull()!!.extras.size)
        val create = requests.first { it.url.encodedPath.endsWith("/createHoop") }.bodyText()
        assertTrue("\"latitude\":45.5" in create && "\"radius\":800" in create && "\"name\":\"Lavoro\"" in create, create)
    }

    @Test fun removingAndResizingCallTheServerFunctions() = runTest {
        val repository = ParseHoopsRepository(api { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/deleteHoops") -> ok("""{"result":{"deleted":["work-id"]}}""")
                path.endsWith("/updateHoop") -> ok("""{"result":{"objectId":"work-id","radius":900}}""")
                else -> ok(hoopsJson)
            }
        })
        repository.removeHoop(HoopId("work-id"))
        repository.resizeHoop(HoopId("work-id"), Radius.create(900).getOrNull()!!)
        assertTrue("\"ids\":[\"work-id\"]" in requests.first { it.url.encodedPath.endsWith("/deleteHoops") }.bodyText())
        val update = requests.first { it.url.encodedPath.endsWith("/updateHoop") }.bodyText()
        assertTrue("\"hoopId\":\"work-id\"" in update && "\"radius\":900" in update, update)
    }

    @Test fun answerWithoutHomeIsAnError() = runTest {
        val result = ParseHoopsRepository(api { ok("""{"result":[]}""") }).getUserHoops()
        assertEquals(DomainError.Unknown, result.errorOrNull())
    }

    @Test fun expiredSessionIsReported() = runTest {
        val result = ParseHoopsRepository(api { respond("""{"code":209,"error":"invalid session"}""", HttpStatusCode.BadRequest, json) }).getUserHoops()
        assertEquals(DomainError.Unauthorized, result.errorOrNull())
    }

    // ---- Feed ----

    private val feedJson = """{"result":[
        {"objectId":"q1","createdAt":"2026-11-18T11:55:00.000Z","text":"Chi apre?","type":"Q","address":"Via Tadino",
         "location":{"latitude":45.501,"longitude":9.2},"media":null,"commentsCount":3,"likesCount":2,"user":{"objectId":"u1","name":"Marta"}},
        {"objectId":"p1","createdAt":"2026-11-18T09:00:00.000Z","text":"Tramonto","type":"P","address":null,
         "location":{"latitude":45.5,"longitude":9.201},"media":{"url":"https://files.test/p1.jpg","thumbnailUrl":null},"commentsCount":0,"likesCount":0,"user":{"objectId":"u2","name":"Luca"}},
        {"objectId":"v1","createdAt":"2026-11-18T11:59:30.000Z","text":"Concerto","type":"V","address":"Piazza",
         "location":{"latitude":45.5,"longitude":9.2},"media":{"url":"https://files.test/v1.mp4","thumbnailUrl":"https://files.test/v1.jpg"},"commentsCount":1,"likesCount":5,"user":{"objectId":"u3","name":"Giulia"}},
        {"objectId":"x1","createdAt":"2026-11-18T09:00:00.000Z","text":"No position","type":"Q","location":null,"user":null}
    ]}"""

    private fun feed(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        ParseFeedRepository(api(handler), nowMillis = { now })

    private val home = Hoop.Home(HoopId("home-id"), radius)

    @Test fun feedAsksForTheHoopAndMapsThePosts() = runTest {
        val posts = feed { ok(feedJson) }.getFeed(home, position).getOrNull()!!
        val body = requests.single().bodyText()
        assertTrue("\"hoopId\":\"home-id\"" in body && "\"latitude\":45.5" in body, body)
        // The post without a position cannot be shown.
        assertEquals(listOf("q1", "p1", "v1"), posts.map { it.id.value })

        val question = posts[0]
        assertEquals(PostType.Question, question.type)
        assertEquals("Marta", question.author.name)
        assertEquals("Via Tadino", question.street)
        assertEquals(5, question.ageMinutes)
        assertEquals(3, question.repliesCount)
        assertEquals(2, question.fliesCount)
        assertNull(question.imageUrl)
        assertTrue(question.distanceMeters in 100..120, "got ${question.distanceMeters}")
        assertEquals("home-id", question.hoopId.value)
    }

    @Test fun datesInTheFormOfParseAreRead() = runTest {
        val posts = feed {
            ok("""{"result":[{"objectId":"q1","createdAt":{"__type":"Date","iso":"2026-11-18T11:55:00.000Z"},"text":"Chi apre?","type":"Q","location":{"latitude":45.5,"longitude":9.2},"user":{"objectId":"u1","name":"Marta"}}]}""")
        }.getFeed(home, position).getOrNull()!!
        assertEquals(5, posts.single().ageMinutes)
    }

    @Test fun photoShowsItsFileAndVideoShowsItsStill() = runTest {
        val posts = feed { ok(feedJson) }.getFeed(home, position).getOrNull()!!
        assertEquals("https://files.test/p1.jpg", posts[1].imageUrl)
        assertNull(posts[1].street)
        assertEquals("https://files.test/v1.jpg", posts[2].imageUrl)
        assertEquals(0, posts[2].ageMinutes)
    }

    @Test fun distancesOfAnExtraHoopStartFromItsCentre() = runTest {
        val centre = GeoPoint.create(45.5, 9.2).getOrNull()!!
        val work = Hoop.Extra(HoopId("work-id"), "Lavoro", centre, radius)
        val far = GeoPoint.create(41.9, 12.5).getOrNull()!!
        val posts = feed { ok(feedJson) }.getFeed(work, far).getOrNull()!!
        assertTrue(posts[2].distanceMeters < 5, "got ${posts[2].distanceMeters}")
        assertTrue("\"hoopId\":\"work-id\"" in requests.single().bodyText())
    }

    @Test fun networkFailureIsReported() = runTest {
        val result = feed { throw io.ktor.utils.io.errors.IOException("down") }.getFeed(home, position)
        assertEquals(DomainError.Network, result.errorOrNull())
    }

    // ---- Comments ----

    @Test fun repliesComeOldestFirst() = runTest {
        val replies = feed {
            ok("""{"result":[
                {"objectId":"c2","createdAt":"2026-11-18T11:58:00.000Z","text":"Dopo","user":{"objectId":"u2","name":"Luca"}},
                {"objectId":"c1","createdAt":"2026-11-18T11:50:00.000Z","text":"Prima","user":{"objectId":"u1","name":"Marta"}}
            ]}""")
        }.getReplies(PostId("q1")).getOrNull()!!
        assertEquals(listOf("c1", "c2"), replies.map { it.id })
        assertEquals(10, replies.first().ageMinutes)
        assertEquals("Marta", replies.first().author.name)
        assertTrue("\"postId\":\"q1\"" in requests.single().bodyText())
    }

    @Test fun replySendsAFreshClientIdentifier() = runTest {
        val repository = feed {
            ok("""{"result":{"objectId":"c9","createdAt":"2026-11-18T12:00:00.000Z","text":"Eccomi","user":{"objectId":"me","name":"Tu"}}}""")
        }
        val reply = (repository.addReply(PostId("q1"), "Eccomi") as DomainResult.Success).value
        assertEquals("c9", reply.id)
        repository.addReply(PostId("q1"), "Eccomi")
        val ids = requests.map { Regex("\"cuuid\":\"([^\"]+)\"").find(it.bodyText())!!.groupValues[1] }
        assertEquals(2, ids.toSet().size)
    }

    // ---- Notifications ----

    @Test fun commentsOnTheSamePostBecomeOneNotification() = runTest {
        val list = feed {
            ok("""{"result":[
                {"objectId":"n3","createdAt":"2026-11-18T11:59:00.000Z","type":"COMMENT","read":false,"actor":{"objectId":"u2","name":"Luca"},"post":{"objectId":"q1","createdAt":"2026-11-18T11:00:00.000Z","type":"Q"}},
                {"objectId":"n2","createdAt":"2026-11-18T11:58:00.000Z","type":"LIKE","read":false,"actor":{"objectId":"u3","name":"Giulia"},"post":{"objectId":"q1","createdAt":"2026-11-18T11:00:00.000Z","type":"Q"}},
                {"objectId":"n1","createdAt":"2026-11-18T11:57:00.000Z","type":"COMMENT","read":true,"actor":{"objectId":"u1","name":"Marta"},"post":{"objectId":"q1","createdAt":"2026-11-18T11:00:00.000Z","type":"Q"}},
                {"objectId":"n0","createdAt":"2026-11-18T11:56:00.000Z","type":"COMMENT","read":true,"actor":{"objectId":"u1","name":"Marta"},"post":{"objectId":"p9","createdAt":"2026-11-18T10:00:00.000Z","type":"P"}}
            ]}""")
        }.getNotifications().getOrNull()!!
        assertEquals(2, list.size)
        val first = list.first()
        assertEquals(NotificationKind.RepliesReceived, first.kind)
        assertEquals("Luca", first.actorName)
        assertEquals(2, first.count)
        assertEquals("q1", first.postId.value)
        assertEquals(1, list.last().count)
    }

    // ---- "Ci volo" ----

    @Test fun postKnowsIfTheUserFlewToIt() = runTest {
        val posts = feed {
            ok("""{"result":[
                {"objectId":"a","createdAt":"2026-11-18T11:55:00.000Z","text":"A","type":"Q","location":{"latitude":45.5,"longitude":9.2},"likesCount":4,"likedByMe":true},
                {"objectId":"b","createdAt":"2026-11-18T11:55:00.000Z","text":"B","type":"Q","location":{"latitude":45.5,"longitude":9.2},"likesCount":1}
            ]}""")
        }.getFeed(home, position).getOrNull()!!
        assertTrue(posts[0].flownByMe)
        assertEquals(4, posts[0].fliesCount)
        assertEquals(false, posts[1].flownByMe)
    }

    @Test fun togglingPostFlyReturnsTheServerState() = runTest {
        val result = feed { ok("""{"result":{"liked":true,"likesCount":7}}""") }.setFly(PostId("q1"), true)
        assertEquals(FlyState(true, 7), result.getOrNull())
        val request = requests.single()
        assertTrue(request.url.encodedPath.endsWith("/toggleLike"))
        assertTrue("\"postId\":\"q1\"" in request.bodyText())
        // The wanted state travels with the request, so repeating it cannot flip the like back.
        assertTrue("\"liked\":true" in request.bodyText(), request.bodyText())
    }

    @Test fun togglingReplyFlyCallsTheCommentFunction() = runTest {
        val result = feed { ok("""{"result":{"liked":false,"likesCount":0}}""") }.setReplyFly("c1", false)
        assertEquals(FlyState(false, 0), result.getOrNull())
        val request = requests.single()
        assertTrue(request.url.encodedPath.endsWith("/toggleCommentLike"))
        assertTrue("\"commentId\":\"c1\"" in request.bodyText())
        assertTrue("\"liked\":false" in request.bodyText(), request.bodyText())
    }

    @Test fun repliesCarryTheirFlies() = runTest {
        val reply = feed {
            ok("""{"result":[{"objectId":"c1","createdAt":"2026-11-18T11:50:00.000Z","text":"Ciao","likesCount":3,"likedByMe":true,"user":{"objectId":"u1","name":"Marta"}}]}""")
        }.getReplies(PostId("q1")).getOrNull()!!.single()
        assertEquals(3, reply.fliesCount)
        assertTrue(reply.flownByMe)
    }

    @Test fun flyWithoutConnectionFails() = runTest {
        val result = feed { throw io.ktor.utils.io.errors.IOException("down") }.setFly(PostId("q1"), true)
        assertEquals(DomainError.Network, result.errorOrNull())
    }
}
