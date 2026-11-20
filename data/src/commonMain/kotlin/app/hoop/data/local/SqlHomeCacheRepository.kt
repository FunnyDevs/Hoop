package app.hoop.data.local

import app.cash.sqldelight.db.SqlDriver
import app.hoop.data.HoopLog
import app.hoop.domain.HomeCacheRepository
import app.hoop.domain.HomeSnapshot
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps the last complete Home in the local SQLite database, replacing it as a whole on every save. */
class SqlHomeCacheRepository(driver: SqlDriver, private val nowMillis: () -> Long = ::currentTimeMillis) : HomeCacheRepository {
    private val database = HoopDatabase(driver)
    private val queries = database.cachedHomeQueries

    override suspend fun save(hoops: UserHoops, feeds: Map<HoopId, List<FeedPost>>): Long = withContext(Dispatchers.Default) {
        val savedAt = nowMillis()
        database.transaction {
            clearTables()
            hoops.all.forEachIndexed { index, hoop ->
                val extra = hoop as? Hoop.Extra
                queries.insertHoop(
                    hoopId = hoop.id.value,
                    position = index.toLong(),
                    isHome = if (hoop is Hoop.Home) 1L else 0L,
                    name = extra?.name,
                    latitude = extra?.center?.latitude,
                    longitude = extra?.center?.longitude,
                    radiusMeters = hoop.radius.meters.toLong(),
                )
                feeds[hoop.id].orEmpty().forEachIndexed { order, post -> insert(hoop.id, order, post) }
            }
            queries.setSavedAt(savedAt)
        }
        HoopLog.home.i { "cache: Home saved (${hoops.all.size} Hoops)" }
        savedAt
    }

    override suspend fun load(): HomeSnapshot? = withContext(Dispatchers.Default) {
        val savedAt = queries.selectSavedAt().executeAsOneOrNull() ?: return@withContext null
        val rows = queries.selectHoops().executeAsList()
        val home = rows.firstOrNull { it.isHome != 0L }?.let { row ->
            radius(row.radiusMeters)?.let { Hoop.Home(HoopId(row.hoopId), it) }
        } ?: return@withContext null
        val extras = rows.filter { it.isHome == 0L }.mapNotNull { row ->
            val center = if (row.latitude == null || row.longitude == null) null else point(row.latitude, row.longitude)
            val radius = radius(row.radiusMeters)
            if (center == null || radius == null) null else Hoop.Extra(HoopId(row.hoopId), row.name.orEmpty(), center, radius)
        }
        val aggregate = UserHoops(home, extras)
        // The posts are older now than when they were saved.
        val elapsed = ((nowMillis() - savedAt) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)
        val feeds = queries.selectPosts().executeAsList()
            .mapNotNull { row -> toPost(row, elapsed)?.let { HoopId(row.hoopId) to it } }
            .groupBy({ it.first }, { it.second })
        HomeSnapshot(aggregate, feeds, savedAt)
    }

    override suspend fun clear() = withContext(Dispatchers.Default) { database.transaction { clearTables() } }

    private fun clearTables() {
        queries.deleteHoops()
        queries.deletePosts()
        queries.deleteSavedAt()
    }

    private fun insert(hoopId: HoopId, order: Int, post: FeedPost) = queries.insertPost(
        hoopId = hoopId.value,
        postId = post.id.value,
        position = order.toLong(),
        type = post.type.name,
        authorId = post.author.id.value,
        authorName = post.author.name,
        text = post.text,
        imageUrl = post.imageUrl,
        videoUrl = post.videoUrl,
        latitude = post.location.latitude,
        longitude = post.location.longitude,
        distanceMeters = post.distanceMeters.toLong(),
        ageMinutes = post.ageMinutes.toLong(),
        repliesCount = post.repliesCount.toLong(),
        street = post.street,
        fliesCount = post.fliesCount.toLong(),
        flownByMe = if (post.flownByMe) 1L else 0L,
    )

    /** `null` for a row the domain would not accept: it is left out instead of breaking the whole Home. */
    private fun toPost(row: CachedPost, elapsedMinutes: Int): FeedPost? {
        val type = PostType.entries.firstOrNull { it.name == row.type } ?: return null
        val location = point(row.latitude, row.longitude) ?: return null
        return FeedPost(
            id = PostId(row.postId),
            hoopId = HoopId(row.hoopId),
            type = type,
            author = Author(UserId(row.authorId), row.authorName),
            text = row.text,
            imageUrl = row.imageUrl,
            location = location,
            distanceMeters = row.distanceMeters.toInt(),
            ageMinutes = row.ageMinutes.toInt() + elapsedMinutes,
            repliesCount = row.repliesCount.toInt(),
            street = row.street,
            fliesCount = row.fliesCount.toInt(),
            flownByMe = row.flownByMe != 0L,
            videoUrl = row.videoUrl,
        )
    }

    private fun point(latitude: Double, longitude: Double): GeoPoint? = (GeoPoint.create(latitude, longitude) as? DomainResult.Success)?.value

    private fun radius(meters: Long): Radius? = (Radius.create(meters.toInt()) as? DomainResult.Success)?.value
}

private const val MILLIS_PER_MINUTE = 60_000L
