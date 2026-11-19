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
)

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
    val replies: List<Reply> = emptyList(),
) {
    fun withReplies(loaded: List<Reply>) = copy(repliesCount = loaded.size, replies = loaded)

    fun withReply(reply: Reply) = copy(repliesCount = repliesCount + 1, replies = replies + reply)

    private fun copy(repliesCount: Int, replies: List<Reply>) =
        FeedPost(id, hoopId, type, author, text, imageUrl, location, distanceMeters, ageMinutes, repliesCount, street, fliesCount, replies)

    override fun equals(other: Any?): Boolean = other is FeedPost && other.id == id
    override fun hashCode(): Int = id.hashCode()
}
