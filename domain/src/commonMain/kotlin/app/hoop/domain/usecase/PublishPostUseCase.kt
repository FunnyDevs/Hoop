package app.hoop.domain.usecase

import app.hoop.domain.AddressResolver
import app.hoop.domain.PendingPostsRepository
import app.hoop.domain.PostsRepository
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.PostPolicy
import app.hoop.domain.common.flatMap
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType
import kotlinx.coroutines.withTimeoutOrNull

/** What happened to a post the user asked to publish. */
sealed interface PublishOutcome {
    /** The server has it. */
    data class Published(val id: PostId) : PublishOutcome
    /** The post could not be sent now: it is kept on the device and will be sent later. */
    data class Queued(val cause: QueueCause) : PublishOutcome
}

/** Why a post is waiting. */
enum class QueueCause {
    /** There is no connection. */
    NoConnection,
    /** The session expired: the user must sign in again. */
    SessionExpired,
}

/**
 * Publishes a post. The street is looked up on a best-effort basis: without it the post is published all the same.
 * If the network is down or the session expired, the post is saved on the device instead of being lost, and
 * [SendPendingPostsUseCase] sends it later.
 */
class PublishPostUseCase(
    private val posts: PostsRepository,
    private val addresses: AddressResolver,
    private val pending: PendingPostsRepository,
) {
    suspend operator fun invoke(
        hoop: Hoop,
        type: PostType,
        text: String,
        media: MediaRef?,
        position: GeoPoint,
        /** Same value on every retry of the same post. */
        clientId: String,
        onProgress: (Float) -> Unit = {},
    ): DomainResult<PublishOutcome> {
        // Implementations of the port return null on any failure, so only the time needs bounding here.
        val address = withTimeoutOrNull(PostPolicy.ADDRESS_LOOKUP_TIMEOUT_MILLIS) { addresses.streetAt(position) }
        return PostDraft.create(hoop, type, text, media, position, clientId, address).flatMap { draft ->
            when (val result = posts.publish(draft, onProgress)) {
                is DomainResult.Success -> DomainResult.success(PublishOutcome.Published(result.value))
                is DomainResult.Failure -> when (result.error) {
                    DomainError.Network -> queue(draft, QueueCause.NoConnection)
                    DomainError.Unauthorized -> queue(draft, QueueCause.SessionExpired)
                    else -> result
                }
            }
        }
    }

    private suspend fun queue(draft: PostDraft, cause: QueueCause): DomainResult<PublishOutcome> {
        pending.add(draft)
        return DomainResult.success(PublishOutcome.Queued(cause))
    }
}
