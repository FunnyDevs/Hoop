package app.hoop.media

import app.hoop.data.media.MediaFileReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Reads the files the camera saved in the private cache of the app (the camera gives their absolute path). */
class AndroidMediaFileReader : MediaFileReader {
    override suspend fun read(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { File(uri).takeIf { it.isFile }?.readBytes() }.getOrNull()
    }
}
