package app.hoop.data.local

import app.hoop.domain.HomeCacheRepository
import app.hoop.domain.HomeSnapshot
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.hoop.UserHoops
import app.hoop.domain.post.FeedPost

/** Keeps the Home in memory only: used when no platform database is provided (tests, fake backend). */
class InMemoryHomeCacheRepository(private val nowMillis: () -> Long = ::currentTimeMillis) : HomeCacheRepository {
    private var snapshot: HomeSnapshot? = null

    override suspend fun load(): HomeSnapshot? = snapshot

    override suspend fun save(hoops: UserHoops, feeds: Map<HoopId, List<FeedPost>>): Long =
        nowMillis().also { snapshot = HomeSnapshot(hoops, feeds, it) }

    override suspend fun clear() {
        snapshot = null
    }
}
