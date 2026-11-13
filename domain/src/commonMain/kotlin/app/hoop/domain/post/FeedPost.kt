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
 * [mediaKey] identifies the photo or video (a URL once the real backend is wired).
 */
class FeedPost(
    val id: PostId,
    val hoopId: HoopId,
    val type: PostType,
    val author: Author,
    val text: String,
    val mediaKey: String?,
    val location: GeoPoint,
    val distanceMeters: Int,
    val ageMinutes: Int,
    val replies: List<Reply>,
    /** Street where the post was published; every post has one (questions, photos, videos). */
    val street: String,
    /** How many people said "Ci volo" (they are going there). */
    val fliesCount: Int,
) {
    fun withReply(reply: Reply) = FeedPost(id, hoopId, type, author, text, mediaKey, location, distanceMeters, ageMinutes, replies + reply, street, fliesCount)

    override fun equals(other: Any?): Boolean = other is FeedPost && other.id == id
    override fun hashCode(): Int = id.hashCode()
}
