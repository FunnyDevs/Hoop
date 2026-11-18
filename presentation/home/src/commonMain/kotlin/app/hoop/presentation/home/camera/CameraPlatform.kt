package app.hoop.presentation.home.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import app.hoop.domain.post.MediaRef

/**
 * The live camera, seen from the shared UI. CameraX implements it on Android and AVFoundation on iOS.
 * Everything it produces lives in the app's private cache, never in the gallery: Hoop only publishes what was
 * shot at that moment.
 */
internal interface CameraHandle {
    /** True once the preview is running and a shot can be taken. */
    val isReady: Boolean

    /** True when the camera could not be opened, for example because another app is using it. */
    val isUnavailable: Boolean
    val isFlashOn: Boolean
    val isFrontLens: Boolean
    fun toggleFlash()
    fun switchLens()

    /** Tries to open the camera again after it was unavailable. */
    fun retry()

    /** Focuses and measures the light on a point of the preview, given as fractions (0..1) of its width and height. */
    fun focusAt(fractionX: Float, fractionY: Float)

    /** Takes a photo; `null` if the camera could not. */
    suspend fun takePhoto(): MediaRef?

    /** Starts recording with sound; false if it could not start. */
    suspend fun startVideo(): Boolean

    /** Stops the recording and returns the file; `null` if nothing was recorded. */
    suspend fun finishVideo(): MediaRef?
}

/** What the app is allowed to use. Both camera and microphone are needed: a video without sound is not accepted. */
internal enum class CameraAccess {
    /** Camera and microphone are allowed. */
    Granted,
    /** The user has not been asked yet, or can still be asked. */
    CanAsk,
    /** The user refused and the system will not ask again: only the settings can change it. */
    Blocked,
}

internal class CameraPermissions(
    val access: CameraAccess,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

@Composable
internal expect fun rememberCameraPermissions(): CameraPermissions

/** Opens the camera on the front lens when [frontLens] is true, on the back one otherwise. */
@Composable
internal expect fun rememberCameraHandle(frontLens: Boolean): CameraHandle

/** The live picture of the camera, filling its box. */
@Composable
internal expect fun LiveCameraPreview(handle: CameraHandle, modifier: Modifier)

/** A request to jump to a point of the video; every request is a new object, so asking for the same point twice works. */
internal class SeekRequest(val fraction: Float)

/** Plays a video file again and again; [onProgress] reports 0..1 of the current loop and [seek] moves the playback. */
@Composable
internal expect fun LoopingVideo(uri: String, muted: Boolean, seek: SeekRequest?, onProgress: (Float) -> Unit, modifier: Modifier)

/** Decodes a photo, or the first frame of a video, from a local file; `null` while it loads or if it cannot be read. */
@Composable
internal expect fun rememberMediaBitmap(uri: String?): ImageBitmap?

internal val MediaRef.isVideo: Boolean get() = mimeType.startsWith(VIDEO_MIME_PREFIX)

/** Local files of captured media are told apart from the sample media keys of the fake backend. */
internal fun isLocalMedia(key: String?): Boolean = key != null && !key.startsWith(SAMPLE_KEY_PREFIX)

internal fun isVideoFile(uri: String): Boolean = uri.endsWith(VIDEO_EXTENSION)

internal const val PHOTO_MIME_TYPE = "image/jpeg"
internal const val VIDEO_MIME_TYPE = "video/mp4"
internal const val VIDEO_EXTENSION = ".mp4"
private const val VIDEO_MIME_PREFIX = "video/"
private const val SAMPLE_KEY_PREFIX = "sample:"
