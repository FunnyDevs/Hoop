package app.hoop.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class GeoPointDto(val latitude: Double, val longitude: Double)

/** `hoopDto` of the server. Home has no centre: it follows the position of the device. */
@Serializable
internal data class HoopDto(
    val objectId: String,
    val name: String? = null,
    val isHome: Boolean = false,
    val center: GeoPointDto? = null,
    val radius: Double,
    val withNotifications: Boolean = true,
)

@Serializable
internal data class CreateHoopBody(val latitude: Double, val longitude: Double, val radius: Int, val name: String)

@Serializable
internal data class UpdateHoopBody(val hoopId: String, val radius: Int)

@Serializable
internal data class DeleteHoopsBody(val ids: List<String>)

@Serializable
internal data class DeletedHoopsDto(val deleted: List<String> = emptyList())

/** Body of a Cloud Function that takes no parameters: Parse wants an empty JSON object. */
@Serializable
internal class NoParams

/** Public data of a user, as the server shows it to the others. */
@Serializable
internal data class AuthorDto(val objectId: String, val name: String? = null)

@Serializable
internal data class FeedMediaDto(val url: String, val thumbnailUrl: String? = null)

/** `postDto` of the server (the part the feed uses). */
@Serializable
internal data class FeedPostDto(
    val objectId: String,
    /** ISO 8601 (see [ParseDateAsIso]). */
    @Serializable(with = ParseDateAsIso::class) val createdAt: String,
    val text: String = "",
    /** `Q` question, `P` photo, `V` video. */
    val type: String,
    val address: String? = null,
    val location: GeoPointDto? = null,
    val media: FeedMediaDto? = null,
    val commentsCount: Int = 0,
    val likesCount: Int = 0,
    val likedByMe: Boolean = false,
    val user: AuthorDto? = null,
)

/** `hoopId` limits the answer to the posts of that Hoop (the initial one or an extra). */
@Serializable
internal data class SearchPostsBody(val latitude: Double, val longitude: Double, val hoopId: String? = null, val limit: Int? = null)

/** [liked] is the state the user wants: a repeated request cannot flip it back. */
@Serializable
internal data class ToggleLikeBody(val postId: String, val liked: Boolean)

@Serializable
internal data class ToggleCommentLikeBody(val commentId: String, val liked: Boolean)

/** Answer of `toggleLike` and `toggleCommentLike`: the state after the toggle. */
@Serializable
internal data class LikeResultDto(val liked: Boolean, val likesCount: Int)

@Serializable
internal data class LoadCommentsBody(val postId: String, val limit: Int? = null)

@Serializable
internal data class SaveCommentBody(val postId: String, val text: String, val cuuid: String)

@Serializable
internal data class CommentDto(
    val objectId: String,
    @Serializable(with = ParseDateAsIso::class) val createdAt: String,
    val text: String,
    val likesCount: Int = 0,
    val likedByMe: Boolean = false,
    val user: AuthorDto? = null,
)

@Serializable
internal data class NotificationDto(
    val objectId: String,
    @Serializable(with = ParseDateAsIso::class) val createdAt: String,
    /** `COMMENT` or `LIKE`. */
    val type: String,
    val actor: AuthorDto? = null,
    val post: FeedPostDto? = null,
)
