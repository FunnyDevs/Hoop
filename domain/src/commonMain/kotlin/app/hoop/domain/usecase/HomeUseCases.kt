package app.hoop.domain.usecase

import app.hoop.domain.FeedRepository
import app.hoop.domain.HoopsRepository
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import app.hoop.domain.common.flatMap
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.hoop.UserHoops
import app.hoop.domain.identity.AppNotification
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.PostId
import app.hoop.domain.post.Reply

class GetUserHoopsUseCase(private val hoops: HoopsRepository) {
    suspend operator fun invoke(): DomainResult<UserHoops> = hoops.getUserHoops()
}

class GetHoopFeedUseCase(private val feed: FeedRepository) {
    suspend operator fun invoke(hoop: Hoop, position: GeoPoint): DomainResult<List<FeedPost>> = feed.getFeed(hoop, position)
}

class GetRepliesUseCase(private val feed: FeedRepository) {
    suspend operator fun invoke(postId: PostId): DomainResult<List<Reply>> = feed.getReplies(postId)
}

class AddReplyUseCase(private val feed: FeedRepository) {
    suspend operator fun invoke(postId: PostId, text: String): DomainResult<Reply> {
        val trimmed = text.trim()
        return if (trimmed.isEmpty()) DomainResult.failure(Violation.PostTextBlank) else feed.addReply(postId, trimmed)
    }
}

class GetNotificationsUseCase(private val feed: FeedRepository) {
    suspend operator fun invoke(): DomainResult<List<AppNotification>> = feed.getNotifications()
}

class AddHoopUseCase(private val hoops: HoopsRepository) {
    suspend operator fun invoke(name: String, center: GeoPoint, radiusMeters: Int): DomainResult<UserHoops> =
        Radius.create(radiusMeters).flatMap { hoops.addHoop(name.trim(), center, it) }
}

class RemoveHoopUseCase(private val hoops: HoopsRepository) {
    suspend operator fun invoke(id: HoopId): DomainResult<UserHoops> = hoops.removeHoop(id)
}

class ResizeHoopUseCase(private val hoops: HoopsRepository) {
    suspend operator fun invoke(id: HoopId, radiusMeters: Int): DomainResult<UserHoops> =
        Radius.create(radiusMeters).flatMap { hoops.resizeHoop(id, it) }
}
