package app.hoop.domain.post

import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.identity.UserId

/** Author of a post or reply, as shown to other users. */
data class Author(val id: UserId, val name: String)

/** A reply (to a question) or comment (on a photo or video). */
data class Reply(
    val id: String,
    val author: Author,
    val text: String,
    val ageMinutes: Int = 0,
    /** How many people said "Ci volo" to this comment. */
    val fliesCount: Int = 0,
    /** Whether the user said "Ci volo" to this comment. */
    val flownByMe: Boolean = false,
) {
    fun withFly(fly: FlyState) = copy(fliesCount = fly.count, flownByMe = fly.flown)
}

/** The "Ci volo" of a post or comment: whether the user has said it and how many people have. */
data class FlyState(val flown: Boolean, val count: Int)

/**
 * Read model of a post shown in a feed: the post plus what the UI needs around it.
 * [imageUrl] is the picture to show: the photo, or the still of a video (`null` for questions).
 * The replies are loaded when the post is opened: until then only [repliesCount] is known.
 */
class FeedPost(
    val id: PostId,
    val hoopId: HoopId,
    val type: PostType,
    val author: Author,
    val text: String,
    val imageUrl: String?,
    val location: GeoPoint,
    val distanceMeters: Int,
    val ageMinutes: Int,
    val repliesCount: Int,
    /** Street where the post was published; `null` if the device could not find it. */
    val street: String?,
    /** How many people said "Ci volo" (they are going there). */
    val fliesCount: Int,
    /** Whether the user said "Ci volo" to this post. */
    val flownByMe: Boolean = false,
    val replies: List<Reply> = emptyList(),
    /** The video itself, for a video post (its still is [imageUrl]); `null` for photos and questions. */
    val videoUrl: String? = null,
) {
    fun withReplies(loaded: List<Reply>) = copy(repliesCount = loaded.size, replies = loaded)

    fun withReply(reply: Reply) = copy(repliesCount = repliesCount + 1, replies = replies + reply)

    fun withFly(fly: FlyState) = copy(fliesCount = fly.count, flownByMe = fly.flown)

    /** The "Ci volo" of one of the loaded replies. */
    fun withReplyFly(replyId: String, fly: FlyState) = copy(replies = replies.map { if (it.id == replyId) it.withFly(fly) else it })

    private fun copy(
        repliesCount: Int = this.repliesCount,
        replies: List<Reply> = this.replies,
        fliesCount: Int = this.fliesCount,
        flownByMe: Boolean = this.flownByMe,
    ) = FeedPost(id, hoopId, type, author, text, imageUrl, location, distanceMeters, ageMinutes, repliesCount, street, fliesCount, flownByMe, replies, videoUrl)

    override fun equals(other: Any?): Boolean = other is FeedPost && other.id == id
    override fun hashCode(): Int = id.hashCode()
}
