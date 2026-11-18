package app.hoop.data.remote

import app.hoop.data.remote.dto.FunctionResponse
import app.hoop.data.remote.dto.PostDto
import app.hoop.data.remote.dto.SaveNewPostBody
import app.hoop.data.remote.dto.UploadedFileDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class ParsePostsApi(private val client: HttpClient) {

    /** `POST /files/{name}`: the raw bytes with their own content type; the server needs the extension in the name. */
    suspend fun uploadFile(name: String, mimeType: String, bytes: ByteArray, onProgress: (Float) -> Unit = {}): UploadedFileDto =
        client.post(ParseProtocol.PATH_FILES + name) {
            contentType(ContentType.parse(mimeType))
            setBody(bytes)
            onUpload { sent, total -> if (total != null && total > 0) onProgress((sent.toFloat() / total).coerceIn(0f, 1f)) }
        }.parse()

    suspend fun saveNewPost(body: SaveNewPostBody): PostDto =
        client.post(ParseProtocol.PATH_FUNCTIONS + ParseProtocol.FUNCTION_SAVE_NEW_POST) { setBody(body) }
            .parse<FunctionResponse<PostDto>>().result
}
