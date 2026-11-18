package app.hoop.domain.usecase

import app.hoop.domain.AddressResolver
import app.hoop.domain.PendingPost
import app.hoop.domain.PendingPostsRepository
import app.hoop.domain.PostsRepository
import app.hoop.domain.assertSuccess
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.hoop.testHome
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingPostsTest {
    private val position = GeoPoint.create(45.0, 9.0).assertSuccess()

    private class FakePosts(var answer: () -> DomainResult<PostId>) : PostsRepository {
        val sent = mutableListOf<String>()
        override suspend fun publish(draft: PostDraft, onProgress: (Float) -> Unit): DomainResult<PostId> = answer().also {
            if (it is DomainResult.Success) sent += draft.clientId
        }
    }

    private class FakePending : PendingPostsRepository {
        val drafts = LinkedHashMap<String, PostDraft>()
        override suspend fun add(draft: PostDraft) { drafts[draft.clientId] = draft }
        override suspend fun all() = drafts.values.map { PendingPost(it, 0) }
        override suspend fun remove(clientId: String) { drafts.remove(clientId) }
    }

    private val noAddress = AddressResolver { null }
    private val ok = { DomainResult.success(PostId("p1")) }
    private fun failure(error: DomainError): () -> DomainResult<PostId> = { DomainResult.failure(error) }

    private suspend fun publish(useCase: PublishPostUseCase, id: String) =
        useCase(testHome(), PostType.Question, "Chi apre?", null, position, id)

    @Test fun publishedPostIsNotQueued() = runTest {
        val pending = FakePending()
        val result = publish(PublishPostUseCase(FakePosts(ok), noAddress, pending), "a").assertSuccess()
        assertEquals(PublishOutcome.Published(PostId("p1")), result)
        assertEquals(0, pending.drafts.size)
    }

    @Test fun noConnectionKeepsThePostOnTheDevice() = runTest {
        val pending = FakePending()
        val result = publish(PublishPostUseCase(FakePosts(failure(DomainError.Network)), noAddress, pending), "a").assertSuccess()
        assertEquals(PublishOutcome.Queued(QueueCause.NoConnection), result)
        assertEquals(listOf("a"), pending.drafts.keys.toList())
    }

    @Test fun expiredSessionKeepsThePostToo() = runTest {
        val pending = FakePending()
        val result = publish(PublishPostUseCase(FakePosts(failure(DomainError.Unauthorized)), noAddress, pending), "a").assertSuccess()
        assertEquals(PublishOutcome.Queued(QueueCause.SessionExpired), result)
        assertEquals(1, pending.drafts.size)
    }

    @Test fun otherErrorsAreNotQueued() = runTest {
        val pending = FakePending()
        val result = publish(PublishPostUseCase(FakePosts(failure(DomainError.TooManyRequests)), noAddress, pending), "a")
        assertEquals(DomainError.TooManyRequests, (result as DomainResult.Failure).error)
        assertEquals(0, pending.drafts.size)
    }

    @Test fun waitingPostsGoOutOldestFirstAndLeaveTheQueue() = runTest {
        val pending = FakePending()
        val offline = PublishPostUseCase(FakePosts(failure(DomainError.Network)), noAddress, pending)
        publish(offline, "a"); publish(offline, "b")
        val posts = FakePosts(ok)
        val result = SendPendingPostsUseCase(posts, pending)()
        assertEquals(SendPendingResult(sent = 2, stillWaiting = 0, discarded = 0), result)
        assertEquals(listOf("a", "b"), posts.sent)
        assertEquals(0, pending.drafts.size)
    }

    @Test fun stopsAtTheFirstMissingConnectionAndKeepsTheRest() = runTest {
        val pending = FakePending()
        val offline = PublishPostUseCase(FakePosts(failure(DomainError.Network)), noAddress, pending)
        publish(offline, "a"); publish(offline, "b"); publish(offline, "c")
        var calls = 0
        val posts = FakePosts { if (++calls == 1) ok() else failure(DomainError.Network)() }
        val result = SendPendingPostsUseCase(posts, pending)()
        assertEquals(SendPendingResult(sent = 1, stillWaiting = 2, discarded = 0, stoppedBy = DomainError.Network), result)
        assertEquals(listOf("b", "c"), pending.drafts.keys.toList())
    }

    @Test fun postRefusedForGoodIsDiscarded() = runTest {
        val pending = FakePending()
        val offline = PublishPostUseCase(FakePosts(failure(DomainError.Network)), noAddress, pending)
        publish(offline, "a")
        val result = SendPendingPostsUseCase(FakePosts(failure(DomainError.Unknown)), pending)()
        assertEquals(SendPendingResult(sent = 0, stillWaiting = 0, discarded = 1), result)
        assertEquals(0, pending.drafts.size)
    }
}
