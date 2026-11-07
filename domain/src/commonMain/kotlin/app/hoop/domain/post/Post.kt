package app.hoop.domain.post

import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.PostPolicy
import app.hoop.domain.common.Violation
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

/** Value Object: reference to a local media file to upload. */
data class MediaRef(val uri: String, val mimeType: String)

/** Entity: an already published post. */
class Post(
    val id: PostId,
    val authorId: UserId,
    val hoopId: HoopId?,
    val type: PostType,
    val text: PostText?,
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
 * the type must be accepted by the Hoop, a question requires text, photos and videos require a file.
 */
class PostDraft private constructor(
    val type: PostType,
    val hoopId: HoopId,
    val text: PostText?,
    val media: MediaRef?,
) {
    companion object {
        fun create(hoop: Hoop, type: PostType, rawText: String, media: MediaRef? = null): DomainResult<PostDraft> {
            if (!hoop.accepts(type)) return DomainResult.failure(Violation.PostTypeNotAllowedInHoop)
            val text: PostText? = when {
                type == PostType.Question || rawText.isNotBlank() -> when (val r = PostText.create(rawText)) {
                    is DomainResult.Success -> r.value
                    is DomainResult.Failure -> return r
                }
                else -> null
            }
            if (type != PostType.Question && media == null) return DomainResult.failure(Violation.PostMediaMissing)
            return DomainResult.success(PostDraft(type, hoop.id, text, if (type == PostType.Question) null else media))
        }
    }
}
