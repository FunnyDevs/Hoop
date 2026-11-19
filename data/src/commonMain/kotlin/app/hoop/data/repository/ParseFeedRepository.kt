package app.hoop.data.repository

import app.hoop.data.HoopLog
import app.hoop.data.local.currentTimeMillis
import app.hoop.data.remote.ParseHomeApi
import app.hoop.data.remote.dto.AuthorDto
import app.hoop.data.remote.dto.CommentDto
import app.hoop.data.remote.dto.FeedPostDto
import app.hoop.data.remote.dto.LoadCommentsBody
import app.hoop.data.remote.dto.NotificationDto
import app.hoop.data.remote.dto.SaveCommentBody
import app.hoop.data.remote.dto.SearchPostsBody
import app.hoop.data.remote.dto.ToggleCommentLikeBody
import app.hoop.data.remote.dto.ToggleLikeBody
import app.hoop.data.remote.toDomainError
import app.hoop.domain.FeedRepository
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.identity.AppNotification
import app.hoop.domain.identity.NotificationKind
import app.hoop.domain.identity.UserId
import app.hoop.domain.post.Author
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.FlyState
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType
import app.hoop.domain.post.Reply
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Posts around the Hoops, their comments and the notifications, read from the Parse server. */
@OptIn(ExperimentalUuidApi::class)
internal class ParseFeedRepository(
    private val api: ParseHomeApi,
    private val nowMillis: () -> Long = ::currentTimeMillis,
) : FeedRepository {

    override suspend fun getFeed(hoop: Hoop, position: GeoPoint): DomainResult<List<FeedPost>> = call("getFeed") {
        // Distances are measured from where the Hoop is: the user for Home, the chosen centre for an extra Hoop.
        val reference = (hoop as? Hoop.Extra)?.center ?: position
        api.searchPosts(SearchPostsBody(position.latitude, position.longitude, hoopId = hoop.id.value, limit = FEED_PAGE_SIZE))
            .mapNotNull { it.toFeedPost(hoop, reference) }
    }

    override suspend fun getReplies(postId: PostId): DomainResult<List<Reply>> = call("getReplies") {
        // The server answers newest first; a conversation reads oldest first.
        api.loadComments(LoadCommentsBody(postId.value, limit = COMMENTS_PAGE_SIZE)).map { it.toReply() }.reversed()
    }

    override suspend fun addReply(postId: PostId, text: String): DomainResult<Reply> = call("addReply") {
        api.saveComment(SaveCommentBody(postId.value, text, cuuid = Uuid.random().toString())).toReply()
    }

    override suspend fun setFly(postId: PostId, flown: Boolean): DomainResult<FlyState> = call("setFly") {
        api.toggleLike(ToggleLikeBody(postId.value, flown)).let { FlyState(it.liked, it.likesCount) }
    }

    override suspend fun setReplyFly(replyId: String, flown: Boolean): DomainResult<FlyState> = call("setReplyFly") {
        api.toggleCommentLike(ToggleCommentLikeBody(replyId, flown)).let { FlyState(it.liked, it.likesCount) }
    }

    override suspend fun getNotifications(): DomainResult<List<AppNotification>> = call("getNotifications") {
        api.getNotifications().toNotifications()
    }

    private suspend fun <T> call(name: String, block: suspend () -> T): DomainResult<T> = try {
        DomainResult.success(block())
    } catch (e: Throwable) {
        val error = e.toDomainError()
        HoopLog.home.w(e) { "$name failed -> $error" }
        DomainResult.failure(error)
    }

    /** `null` for a post the app cannot show (no position, unknown type). */
    private fun FeedPostDto.toFeedPost(hoop: Hoop, reference: GeoPoint): FeedPost? {
        val type = type.toPostType() ?: return null
        val spot = location?.let { (GeoPoint.create(it.latitude, it.longitude) as? DomainResult.Success)?.value } ?: return null
        return FeedPost(
            id = PostId(objectId),
            hoopId = hoop.id,
            type = type,
            author = user.toAuthor(),
            text = text,
            imageUrl = when (type) {
                PostType.Question -> null
                PostType.Photo -> media?.url
                PostType.Video -> media?.thumbnailUrl
            },
            location = spot,
            distanceMeters = reference.distanceMetersTo(spot),
            ageMinutes = ageMinutes(createdAt),
            repliesCount = commentsCount,
            street = address?.takeIf { it.isNotBlank() },
            fliesCount = likesCount,
            flownByMe = likedByMe,
            videoUrl = if (type == PostType.Video) media?.url else null,
        )
    }

    private fun CommentDto.toReply() = Reply(
        id = objectId, author = user.toAuthor(), text = text, ageMinutes = ageMinutes(createdAt), fliesCount = likesCount, flownByMe = likedByMe,
    )

    /**
     * Comments become "replies received", one entry per post with how many there are. Likes have no entry in the
     * home yet, and posts published nearby are not notified by the server, so only comments are listed.
     */
    private fun List<NotificationDto>.toNotifications(): List<AppNotification> =
        filter { it.type == TYPE_COMMENT && it.post != null }
            .groupBy { it.post!!.objectId }
            .values
            .map { group ->
                val latest = group.first()
                AppNotification(
                    id = latest.objectId,
                    kind = NotificationKind.RepliesReceived,
                    actorName = latest.actor?.name.orEmpty(),
                    postId = PostId(latest.post!!.objectId),
                    count = group.size,
                    distanceMeters = 0,
                )
            }

    private fun AuthorDto?.toAuthor() = Author(UserId(this?.objectId.orEmpty()), this?.name.orEmpty())

    private fun String.toPostType(): PostType? = when (this) {
        "Q" -> PostType.Question
        "P" -> PostType.Photo
        "V" -> PostType.Video
        else -> null
    }

    private fun ageMinutes(createdAt: String): Int {
        val created = runCatching { Instant.parse(createdAt).toEpochMilliseconds() }.getOrNull() ?: return 0
        return ((nowMillis() - created) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)
    }
}

private const val FEED_PAGE_SIZE = 50
private const val COMMENTS_PAGE_SIZE = 100
private const val TYPE_COMMENT = "COMMENT"
private const val MILLIS_PER_MINUTE = 60_000L
