package app.hoop.data.local

import app.hoop.domain.PendingPost
import app.hoop.domain.PendingPostsRepository
import app.hoop.domain.post.PostDraft

/** Keeps the waiting posts in memory only: used when no platform database is provided (tests, fake backend). */
class InMemoryPendingPostsRepository : PendingPostsRepository {
    private val drafts = LinkedHashMap<String, PostDraft>()

    override suspend fun add(draft: PostDraft) {
        drafts.remove(draft.clientId)
        drafts[draft.clientId] = draft
    }

    override suspend fun all(): List<PendingPost> = drafts.values.map { PendingPost(it, waitingMinutes = 0) }

    override suspend fun remove(clientId: String) {
        drafts.remove(clientId)
    }
}
