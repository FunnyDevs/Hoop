package app.hoop.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.hoop.data.local.HoopDatabase
import app.hoop.data.local.SqlHomeCacheRepository
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.hoop.UserHoops
import app.hoop.domain.identity.UserId
import app.hoop.domain.post.Author
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The saved Home against a real SQLite database (in memory), through the same schema the apps use. */
class SqlHomeCacheTest {
    private fun point(lat: Double, lon: Double) = (GeoPoint.create(lat, lon) as DomainResult.Success).value
    private val spot = point(45.5, 9.2)
    private val home = Hoop.Home(HoopId("home"), Radius.Default)
    private val work = Hoop.Extra(HoopId("work"), "Lavoro", point(45.47, 9.19), Radius.Default)

    private var clock = 1_000_000L

    private fun repository(): SqlHomeCacheRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HoopDatabase.Schema.create(driver)
        return SqlHomeCacheRepository(driver) { clock }
    }

    private fun post(id: String, hoop: Hoop, type: PostType = PostType.Question, age: Int = 5) = FeedPost(
        id = PostId(id), hoopId = hoop.id, type = type, author = Author(UserId("u1"), "Anna"), text = "Testo $id",
        imageUrl = if (type == PostType.Question) null else "https://x/$id.jpg", location = spot, distanceMeters = 120,
        ageMinutes = age, repliesCount = 2, street = "Via Tadino 12", fliesCount = 3, flownByMe = true,
        videoUrl = if (type == PostType.Video) "https://x/$id.mp4" else null,
    )

    @Test fun nothingSavedMeansNothingToShow() = runTest {
        assertNull(repository().load())
    }

    @Test fun keepsHoopsPostsAndTheMomentOfTheSave() = runTest {
        val repo = repository()
        val hoops = UserHoops(home, listOf(work))
        val savedAt = repo.save(hoops, mapOf(home.id to listOf(post("a", home, PostType.Video), post("b", home)), work.id to listOf(post("c", work))))
        val back = repo.load()!!
        assertEquals(savedAt, back.savedAtEpochMillis)
        assertEquals(listOf("home", "work"), back.hoops.all.map { it.id.value })
        assertEquals("Lavoro", (back.hoops.extras.single()).name)
        assertEquals(listOf("a", "b"), back.feeds.getValue(home.id).map { it.id.value })
        val video = back.feeds.getValue(home.id).first()
        assertEquals("https://x/a.mp4", video.videoUrl)
        assertEquals("Via Tadino 12", video.street)
        assertEquals(true, video.flownByMe)
        assertEquals(3, video.fliesCount)
        assertEquals(listOf("c"), back.feeds.getValue(work.id).map { it.id.value })
    }

    @Test fun postsGetOlderWhileTheyAreSaved() = runTest {
        val repo = repository()
        repo.save(UserHoops(home), mapOf(home.id to listOf(post("a", home, age = 5))))
        clock += 90 * 60_000L
        assertEquals(95, repo.load()!!.feeds.getValue(home.id).single().ageMinutes)
    }

    @Test fun aNewSaveReplacesTheOldOne() = runTest {
        val repo = repository()
        repo.save(UserHoops(home, listOf(work)), mapOf(home.id to listOf(post("a", home)), work.id to listOf(post("c", work))))
        repo.save(UserHoops(home), mapOf(home.id to listOf(post("b", home))))
        val back = repo.load()!!
        assertEquals(listOf("home"), back.hoops.all.map { it.id.value })
        assertEquals(listOf("b"), back.feeds.getValue(home.id).map { it.id.value })
        assertNull(back.feeds[work.id])
    }

    @Test fun clearForgetsEverything() = runTest {
        val repo = repository()
        repo.save(UserHoops(home), mapOf(home.id to listOf(post("a", home))))
        repo.clear()
        assertNull(repo.load())
    }
}
