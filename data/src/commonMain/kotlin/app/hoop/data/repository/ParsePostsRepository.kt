package app.hoop.data.repository

import app.hoop.data.HoopLog
import app.hoop.data.media.MediaFileReader
import app.hoop.data.remote.ParsePostsApi
import app.hoop.data.remote.dto.FileReferenceDto
import app.hoop.data.remote.dto.SaveNewPostBody
import app.hoop.data.remote.toDomainError
import app.hoop.domain.PostsRepository
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.post.MediaOrientation
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType
import kotlinx.coroutines.CancellationException

/**
 * Publishes posts on the Parse server. A photo or video is uploaded first (`POST /files`), then the
 * Cloud Function `saveNewPost` creates the post and checks again every rule of the domain.
 */
internal class ParsePostsRepository(
    private val api: ParsePostsApi,
    private val mediaReader: MediaFileReader,
) : PostsRepository {

    override suspend fun publish(draft: PostDraft, onProgress: (Float) -> Unit): DomainResult<PostId> {
        // The draft's identifier names the uploaded file and marks the post (`cuuid`): the server returns the
        // post it already created if the same attempt is repeated, so a retry never duplicates it.
        val clientId = draft.clientId
        HoopLog.posts.d { "publish: type=${draft.type} home=${draft.inInitialHoop} media=${draft.media != null}" }
        return try {
            val media = draft.media
            val files = media?.let { uploadedFor(clientId) ?: upload(it, clientId, onProgress) ?: return DomainResult.failure(DomainError.Unknown) }
            onProgress(PROGRESS_BEFORE_POST)
            val post = api.saveNewPost(
                SaveNewPostBody(
                    latitude = draft.position.latitude,
                    longitude = draft.position.longitude,
                    type = draft.type.serverCode(),
                    text = draft.text.value,
                    hoopId = if (draft.inInitialHoop) null else draft.hoopId.value,
                    media = files?.media,
                    mimeType = media?.mimeType,
                    thumbnail = if (draft.type == PostType.Video) files?.thumbnail else null,
                    orientation = media?.orientation?.serverValue(),
                    width = media?.width?.takeIf { it > 0 },
                    height = media?.height?.takeIf { it > 0 },
                    durationMillis = if (draft.type == PostType.Video) media?.durationMillis?.toInt() else null,
                    address = draft.address,
                    cuuid = clientId,
                ),
            )
            uploaded = null
            onProgress(1f)
            HoopLog.posts.i { "publish: created ${post.objectId}" }
            DomainResult.success(PostId(post.objectId))
        } catch (e: Throwable) {
            val error = e.toDomainError()
            HoopLog.posts.w(e) { "publish failed -> $error" }
            DomainResult.failure(error)
        }
    }

    /** The files of an attempt: the photo or video and, for a video, its still image (missing if it could not be made). */
    private class UploadedFiles(val media: FileReferenceDto, val thumbnail: FileReferenceDto?)

    /** Files already uploaded by an earlier try of the same attempt: when only the post failed, a retry must not upload them again. */
    private var uploaded: Pair<String, UploadedFiles>? = null

    private fun uploadedFor(clientId: String): UploadedFiles? =
        uploaded?.takeIf { it.first == clientId }?.second?.also { HoopLog.posts.d { "upload: reusing the files of the earlier try" } }

    /** Uploads the local file and the still image of a video; `null` if the file cannot be read any more. */
    private suspend fun upload(media: MediaRef, clientId: String, onProgress: (Float) -> Unit): UploadedFiles? {
        val bytes = mediaReader.read(media.uri)
        if (bytes == null) {
            HoopLog.posts.e { "upload: file not readable" }
            return null
        }
        val stored = api.uploadFile("$clientId.${media.fileExtension()}", media.mimeType, bytes) { onProgress(it * PROGRESS_MEDIA_SHARE) }
        HoopLog.posts.d { "upload: ${bytes.size} bytes stored" }
        val files = UploadedFiles(FileReferenceDto(stored.name, stored.url), uploadThumbnail(media, clientId))
        // Kept from now on: if the post fails, the retry only repeats the post.
        uploaded = clientId to files
        return files
    }

    /** The still image never holds the post back: without it the post is published with no thumbnail. */
    private suspend fun uploadThumbnail(media: MediaRef, clientId: String): FileReferenceDto? {
        val uri = media.thumbnailUri ?: return null
        return try {
            val bytes = mediaReader.read(uri) ?: return null
            val stored = api.uploadFile("$clientId$THUMBNAIL_SUFFIX", THUMBNAIL_MIME_TYPE, bytes)
            FileReferenceDto(stored.name, stored.url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            HoopLog.posts.w(e) { "upload: thumbnail skipped" }
            null
        }
    }

    private fun MediaOrientation.serverValue(): String = when (this) {
        MediaOrientation.Landscape -> "landscape"
        MediaOrientation.Portrait -> "portrait"
        MediaOrientation.Square -> "square"
    }

    private fun PostType.serverCode(): String = when (this) {
        PostType.Question -> "Q"
        PostType.Photo -> "P"
        PostType.Video -> "V"
    }

    /** The server tells photos from videos by the extension of the name. */
    private fun MediaRef.fileExtension(): String = when {
        mimeType == "video/quicktime" -> "mov"
        mimeType.startsWith("video/") -> "mp4"
        mimeType == "image/png" -> "png"
        mimeType == "image/webp" -> "webp"
        mimeType == "image/heic" -> "heic"
        else -> "jpg"
    }
}

private const val THUMBNAIL_SUFFIX = "-thumb.jpg"
private const val THUMBNAIL_MIME_TYPE = "image/jpeg"

/** The photo or video is most of the work; the still image and the post itself are quick. */
private const val PROGRESS_MEDIA_SHARE = 0.85f
private const val PROGRESS_BEFORE_POST = 0.92f
