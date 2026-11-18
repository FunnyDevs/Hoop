package app.hoop.media

import app.hoop.data.media.MediaFileReader
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/** Reads the files the camera saved in the private cache of the app. Written but never compiled (no Xcode here). */
@OptIn(ExperimentalForeignApi::class)
class IosMediaFileReader : MediaFileReader {
    override suspend fun read(uri: String): ByteArray? = withContext(Dispatchers.Default) {
        val data = NSData.dataWithContentsOfFile(uri) ?: return@withContext null
        ByteArray(data.length.toInt()).also { bytes ->
            if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
    }
}
