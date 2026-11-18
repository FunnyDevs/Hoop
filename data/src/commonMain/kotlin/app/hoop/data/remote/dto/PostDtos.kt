package app.hoop.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Answer of `POST /files/{name}`: the stored name and its address. */
@Serializable
internal data class UploadedFileDto(val name: String, val url: String)

/** How Parse wants a file already uploaded to be referred to inside a Cloud Function parameter. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class FileReferenceDto(
    val name: String,
    val url: String,
    @SerialName("__type") @EncodeDefault val type: String = "File",
)

@Serializable
internal data class SaveNewPostBody(
    val latitude: Double,
    val longitude: Double,
    /** `Q` question, `P` photo, `V` video. */
    val type: String,
    val text: String,
    /** Absent for the initial Hoop: the server then uses it. */
    val hoopId: String? = null,
    val media: FileReferenceDto? = null,
    val mimeType: String? = null,
    /** Still image of a video, uploaded like [media]; the server ignores it for other types. */
    val thumbnail: FileReferenceDto? = null,
    /** `landscape`, `portrait` or `square`. */
    val orientation: String? = null,
    /** Pixels, as the viewer sees the media. */
    val width: Int? = null,
    val height: Int? = null,
    /** Videos only: the server refuses it for a photo. */
    val durationMillis: Int? = null,
    val address: String? = null,
    /** Client identifier: the server returns the same post if the request is repeated. */
    val cuuid: String,
)

@Serializable
internal data class PostDto(val objectId: String)
