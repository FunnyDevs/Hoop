package app.hoop.data.local

import app.cash.sqldelight.db.SqlDriver
import app.hoop.data.HoopLog
import app.hoop.domain.PendingPost
import app.hoop.domain.PendingPostsRepository
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps the waiting posts in the local SQLite database. */
class SqlPendingPostsRepository(driver: SqlDriver, private val nowMillis: () -> Long = ::currentTimeMillis) : PendingPostsRepository {
    private val queries = HoopDatabase(driver).pendingPostQueries

    override suspend fun add(draft: PostDraft) = withContext(Dispatchers.Default) {
        val media = draft.media
        queries.upsert(
            clientId = draft.clientId,
            type = draft.type.name,
            hoopId = draft.hoopId.value,
            inInitialHoop = if (draft.inInitialHoop) 1L else 0L,
            text = draft.text.value,
            mediaUri = media?.uri,
            mimeType = media?.mimeType,
            capturedAtEpochMillis = media?.capturedAtEpochMillis,
            durationMillis = media?.durationMillis,
            width = media?.width?.toLong(),
            height = media?.height?.toLong(),
            thumbnailUri = media?.thumbnailUri,
            latitude = draft.position.latitude,
            longitude = draft.position.longitude,
            address = draft.address,
            queuedAtEpochMillis = nowMillis(),
        )
        HoopLog.posts.i { "queue: post kept on the device" }
    }

    override suspend fun all(): List<PendingPost> = withContext(Dispatchers.Default) {
        val now = nowMillis()
        queries.selectAll().executeAsList().mapNotNull { row ->
            val position = (GeoPoint.create(row.latitude, row.longitude) as? DomainResult.Success)?.value
            val type = PostType.entries.firstOrNull { it.name == row.type }
            val draft = if (position == null || type == null) null else {
                val media = row.mediaUri?.let {
                    MediaRef(
                        uri = it,
                        mimeType = row.mimeType.orEmpty(),
                        capturedAtEpochMillis = row.capturedAtEpochMillis,
                        durationMillis = row.durationMillis,
                        width = row.width?.toInt(),
                        height = row.height?.toInt(),
                        thumbnailUri = row.thumbnailUri,
                    )
                }
                (PostDraft.restore(type, HoopId(row.hoopId), row.inInitialHoop != 0L, row.text, media, position, row.address, row.clientId)
                    as? DomainResult.Success)?.value
            }
            // A row that no longer makes sense is dropped instead of blocking the queue forever.
            if (draft == null) {
                queries.deleteByClientId(row.clientId)
                HoopLog.posts.w { "queue: unreadable entry dropped" }
            }
            draft?.let { PendingPost(it, ((now - row.queuedAtEpochMillis) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)) }
        }
    }

    override suspend fun remove(clientId: String) = withContext(Dispatchers.Default) { queries.deleteByClientId(clientId) }
}

private const val MILLIS_PER_MINUTE = 60_000L
