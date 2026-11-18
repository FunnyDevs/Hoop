package app.hoop.domain.post

import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.PostPolicy
import app.hoop.domain.common.Violation
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.identity.UserId
import kotlin.jvm.JvmInline

@JvmInline
value class PostId(val value: String)

/** Value Object: post text, non-blank after trimming and of limited length. */
@JvmInline
value class PostText private constructor(val value: String) {
    companion object {
        fun create(raw: String): DomainResult<PostText> {
            val trimmed = raw.trim()
            return when {
                trimmed.isEmpty() -> DomainResult.failure(Violation.PostTextBlank)
                trimmed.length > PostPolicy.TEXT_MAX_LENGTH -> DomainResult.failure(Violation.PostTextTooLong)
                else -> DomainResult.success(PostText(trimmed))
            }
        }
    }
}

/**
 * Value Object: reference to a local media file to upload.
 * Photos and videos can only come from the in-app camera, so a live capture carries the moment it was taken
 * ([capturedAtEpochMillis]) and, when the position was known, where ([location]).
 */
data class MediaRef(
    val uri: String,
    val mimeType: String,
    val capturedAtEpochMillis: Long? = null,
    val location: GeoPoint? = null,
    /** Length of a video; `null` for a photo. */
    val durationMillis: Long? = null,
    /** Size in pixels as the viewer sees it, after any rotation; `null` if the device could not read it. */
    val width: Int? = null,
    val height: Int? = null,
    /** Still image of a video (its first frame), already a JPEG file of the app's cache; `null` for a photo or if it could not be made. */
    val thumbnailUri: String? = null,
) {
    /** Landscape, portrait or square, when the size is known. */
    val orientation: MediaOrientation?
        get() {
            val w = width ?: return null
            val h = height ?: return null
            return when {
                w <= 0 || h <= 0 -> null
                w > h -> MediaOrientation.Landscape
                w < h -> MediaOrientation.Portrait
                else -> MediaOrientation.Square
            }
        }

    /** True when the file was captured by the in-app camera (it has a capture time). */
    val isLiveCapture: Boolean get() = capturedAtEpochMillis != null
}

/** How a photo or video is held when watched. */
enum class MediaOrientation { Landscape, Portrait, Square }

/** Entity: an already published post. */
class Post(
    val id: PostId,
    val authorId: UserId,
    val hoopId: HoopId?,
    val type: PostType,
    val text: PostText,
    val mediaUrl: String?,
    val createdAtEpochMillis: Long,
    val commentsCount: Int,
    val likesCount: Int,
) {
    override fun equals(other: Any?): Boolean = other is Post && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/**
 * Draft of a new post. It can only be created with [PostDraft.create], which applies the rules:
 * the type must be accepted by the Hoop, every post requires text (the caption for photos and videos), photos and videos require a file taken live with the camera.
 */
class PostDraft private constructor(
    val type: PostType,
    val hoopId: HoopId,
    /** True for the initial Hoop, which follows the user; the others are identified by [hoopId]. */
    val inInitialHoop: Boolean,
    val text: PostText,
    val media: MediaRef?,
    /** Where the user is when publishing: the post belongs to this place. */
    val position: GeoPoint,
    /** Street where the post is published, when the device could tell; the post is published without it otherwise. */
    val address: String?,
    /** Identifies this attempt: repeating it with the same id never creates the post twice. */
    val clientId: String,
) {
    companion object {
        /**
         * Rebuilds a draft that was saved on the device (see [app.hoop.domain.PendingPostsRepository]), applying the
         * rules that do not depend on the Hoop again. The Hoop itself was checked when the draft was first created.
         */
        fun restore(
            type: PostType,
            hoopId: HoopId,
            inInitialHoop: Boolean,
            rawText: String,
            media: MediaRef?,
            position: GeoPoint,
            address: String?,
            clientId: String,
        ): DomainResult<PostDraft> {
            val text = when (val r = PostText.create(rawText)) {
                is DomainResult.Success -> r.value
                is DomainResult.Failure -> return r
            }
            if (type != PostType.Question && media?.isLiveCapture != true) return DomainResult.failure(Violation.PostMediaNotLive)
            if (!inInitialHoop && type != PostType.Question) return DomainResult.failure(Violation.PostTypeNotAllowedInHoop)
            val cleanAddress = address?.trim()?.take(PostPolicy.ADDRESS_MAX_LENGTH)?.ifBlank { null }
            return DomainResult.success(
                PostDraft(type, hoopId, inInitialHoop, text, if (type == PostType.Question) null else media, position, cleanAddress, clientId),
            )
        }

        fun create(hoop: Hoop, type: PostType, rawText: String, media: MediaRef?, position: GeoPoint, clientId: String, address: String? = null): DomainResult<PostDraft> {
            if (!hoop.accepts(type)) return DomainResult.failure(Violation.PostTypeNotAllowedInHoop)
            // Every post needs text: the question itself, or the caption of a photo or video.
            val text: PostText = when (val r = PostText.create(rawText)) {
                is DomainResult.Success -> r.value
                is DomainResult.Failure -> return r
            }
            if (type != PostType.Question && media == null) return DomainResult.failure(Violation.PostMediaMissing)
            if (type != PostType.Question && media?.isLiveCapture != true) return DomainResult.failure(Violation.PostMediaNotLive)
            return DomainResult.success(
                PostDraft(type, hoop.id, hoop is Hoop.Home, text, if (type == PostType.Question) null else media, position, address?.trim()?.take(PostPolicy.ADDRESS_MAX_LENGTH)?.ifBlank { null }, clientId),
            )
        }
    }
}
