package app.hoop.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.hoop.data.local.HoopDatabase
import app.hoop.data.local.SqlPendingPostsRepository
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The local queue against a real SQLite database (in memory), through the same schema the apps use. */
class SqlPendingPostsTest {
    private val position = (GeoPoint.create(45.5, 9.2) as DomainResult.Success).value
    private val home = Hoop.Home(HoopId("home"), Radius.Default)

    private var clock = 1_000_000L

    private fun repository(): SqlPendingPostsRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HoopDatabase.Schema.create(driver)
        return SqlPendingPostsRepository(driver) { clock }
    }

    private fun draft(id: String, type: PostType = PostType.Question, media: MediaRef? = null, address: String? = null) =
        (PostDraft.create(home, type, "Testo $id", media, position, id, address) as DomainResult.Success).value

    @Test fun keepsEveryFieldOfAQuestion() = runTest {
        val repo = repository()
        repo.add(draft("a", address = "Via Tadino 12"))
        val back = repo.all().single().draft
        assertEquals("a", back.clientId)
        assertEquals(PostType.Question, back.type)
        assertEquals("Testo a", back.text.value)
        assertEquals("Via Tadino 12", back.address)
        assertEquals(position, back.position)
        assertEquals(true, back.inInitialHoop)
        assertNull(back.media)
    }

    @Test fun keepsTheMediaOfAVideo() = runTest {
        val repo = repository()
        val video = MediaRef("/cache/c.mp4", "video/mp4", 1_700_000_000_000L, durationMillis = 3200, width = 1920, height = 1080, thumbnailUri = "/cache/t.jpg")
        repo.add(draft("v", PostType.Video, video))
        assertEquals(video, repo.all().single().draft.media)
    }

    @Test fun keepsTheOrderAndRemoves() = runTest {
        val repo = repository()
        repo.add(draft("a")); repo.add(draft("b")); repo.add(draft("c"))
        assertEquals(listOf("a", "b", "c"), repo.all().map { it.draft.clientId })
        repo.remove("b")
        assertEquals(listOf("a", "c"), repo.all().map { it.draft.clientId })
    }

    @Test fun tellsHowLongAPostHasBeenWaiting() = runTest {
        val repo = repository()
        repo.add(draft("a"))
        clock += 4 * 60_000L + 5_000L
        assertEquals(4, repo.all().single().waitingMinutes)
    }

    @Test fun addingTheSameAttemptTwiceKeepsOneEntry() = runTest {
        val repo = repository()
        repo.add(draft("a")); repo.add(draft("a"))
        assertEquals(1, repo.all().size)
    }
}
