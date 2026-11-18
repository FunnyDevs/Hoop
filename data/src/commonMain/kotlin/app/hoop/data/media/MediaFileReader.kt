package app.hoop.data.media

/**
 * Reads the bytes of a local file taken by the in-app camera, so that it can be uploaded.
 * Implemented per platform in the app, like [app.hoop.data.session.SessionTokenStore]: the shared code
 * only knows the location the camera gave ([app.hoop.domain.post.MediaRef.uri]).
 */
fun interface MediaFileReader {
    /** Returns the content of the file, or `null` if it no longer exists or cannot be read. */
    suspend fun read(uri: String): ByteArray?
}
