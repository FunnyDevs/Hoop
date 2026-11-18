package app.hoop.domain.usecase

import app.hoop.domain.PendingPostsRepository
import app.hoop.domain.PostsRepository
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult

/** What a pass over the waiting posts did; [stoppedBy] is why it stopped early (no connection, or the session expired). */
data class SendPendingResult(val sent: Int, val stillWaiting: Int, val discarded: Int, val stoppedBy: DomainError? = null)

/**
 * Sends the posts kept on the device, oldest first.
 * - Sent: removed from the queue.
 * - No connection or session expired: it stops and keeps everything for the next pass.
 * - Refused by the server for good (or its file is gone): discarded, so it is not retried forever.
 */
class SendPendingPostsUseCase(private val posts: PostsRepository, private val pending: PendingPostsRepository) {
    suspend operator fun invoke(): SendPendingResult {
        val waiting = pending.all()
        var sent = 0
        var discarded = 0
        for ((index, item) in waiting.withIndex()) {
            val draft = item.draft
            when (val result = posts.publish(draft)) {
                is DomainResult.Success -> {
                    pending.remove(draft.clientId)
                    sent++
                }
                is DomainResult.Failure -> when (result.error) {
                    DomainError.Network, DomainError.Unauthorized ->
                        return SendPendingResult(sent, waiting.size - index, discarded, stoppedBy = result.error)
                    else -> {
                        pending.remove(draft.clientId)
                        discarded++
                    }
                }
            }
        }
        return SendPendingResult(sent, 0, discarded)
    }
}

/** The posts waiting to be sent, oldest first, for the screen that lists them. */
class GetPendingPostsUseCase(private val pending: PendingPostsRepository) {
    suspend operator fun invoke() = pending.all()
}

/** Gives up a waiting post: it is removed from the device and will never be sent. */
class CancelPendingPostUseCase(private val pending: PendingPostsRepository) {
    suspend operator fun invoke(clientId: String) = pending.remove(clientId)
}
