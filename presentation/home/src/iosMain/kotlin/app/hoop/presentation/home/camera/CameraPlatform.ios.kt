package app.hoop.presentation.home.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.UIKitView
import app.hoop.domain.post.MediaRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Image
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureFileOutput
import platform.AVFoundation.AVCaptureFileOutputRecordingDelegateProtocol
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCaptureMovieFileOutput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isTorchAvailable
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setTorchMode
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVCaptureExposureModeAutoExpose
import platform.AVFoundation.AVCaptureFocusModeAutoFocus
import platform.AVFoundation.exposureMode
import platform.AVFoundation.exposurePointOfInterest
import platform.AVFoundation.focusMode
import platform.AVFoundation.focusPointOfInterest
import platform.AVFoundation.isExposurePointOfInterestSupported
import platform.AVFoundation.isFocusPointOfInterestSupported
import platform.CoreGraphics.CGPointMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSError
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.coroutines.resume

private const val CAPTURES_DIR = "captures"
private const val MILLIS_PER_SECOND = 1000.0
private const val PROGRESS_POLL_MILLIS = 40L
private const val VIDEO_TIMESCALE = 600
private const val PHOTO_EXTENSION = ".jpg"

private fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()

private fun newCapturePath(extension: String): String {
    val dir = NSTemporaryDirectory() + CAPTURES_DIR
    NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    return "$dir/${nowMillis()}$extension"
}

// ---------------------------------------------------------------------------------------------------------------------
// Permissions: camera and microphone, both required (NSCameraUsageDescription and NSMicrophoneUsageDescription needed)
// ---------------------------------------------------------------------------------------------------------------------

@Composable
internal actual fun rememberCameraPermissions(): CameraPermissions {
    var refresh by remember { mutableIntStateOf(0) }
    val video = refresh.let { AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) }
    val audio = refresh.let { AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio) }
    val access = when {
        video == AVAuthorizationStatusAuthorized && audio == AVAuthorizationStatusAuthorized -> CameraAccess.Granted
        video == AVAuthorizationStatusNotDetermined || audio == AVAuthorizationStatusNotDetermined -> CameraAccess.CanAsk
        else -> CameraAccess.Blocked
    }
    return CameraPermissions(
        access = access,
        request = {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { _ ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { _ -> dispatch_async(dispatch_get_main_queue()) { refresh++ } }
            }
        },
        openSettings = {
            NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
                UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any?>(), completionHandler = null)
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------------------------------
// Live camera (AVFoundation)
// ---------------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalForeignApi::class)
private class IosCameraHandle : CameraHandle {
    override var isReady by mutableStateOf(false)
        private set
    override var isFlashOn by mutableStateOf(false)
        private set
    override var isFrontLens by mutableStateOf(false)
        private set

    val session = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()
    private val movieOutput = AVCaptureMovieFileOutput()
    private var videoInput: AVCaptureDeviceInput? = null
    private var audioInput: AVCaptureDeviceInput? = null
    private var photoDelegate: NSObject? = null
    private var recordingDelegate: NSObject? = null
    private var finished: CompletableDeferred<MediaRef?>? = null
    private var videoStartedAt = 0L

    init { configure() }

    private fun configure() {
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetHigh
        attachVideo()
        AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio)?.let { microphone ->
            val input = AVCaptureDeviceInput.deviceInputWithDevice(microphone, error = null)
            if (input != null && session.canAddInput(input)) { session.addInput(input); audioInput = input }
        }
        if (session.canAddOutput(photoOutput)) session.addOutput(photoOutput)
        if (session.canAddOutput(movieOutput)) session.addOutput(movieOutput)
        session.commitConfiguration()
    }

    private fun attachVideo() {
        videoInput?.let { session.removeInput(it) }
        val position = if (isFrontLens) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
        val device = AVCaptureDevice.defaultDeviceWithDeviceType(AVCaptureDeviceTypeBuiltInWideAngleCamera, AVMediaTypeVideo, position) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) ?: return
        if (session.canAddInput(input)) { session.addInput(input); videoInput = input }
    }

    fun start() {
        if (!session.running) session.startRunning()
        isReady = true
    }

    fun stop() {
        if (movieOutput.recording) movieOutput.stopRecording()
        if (session.running) session.stopRunning()
        isReady = false
    }

    /** The layer showing the preview, needed to turn a point of the screen into a point of the camera. */
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun focusAt(fractionX: Float, fractionY: Float) {
        val layer = previewLayer ?: return
        val device = videoInput?.device ?: return
        val size = layer.bounds.useContents { size }
        val point = layer.captureDevicePointOfInterestForPoint(CGPointMake(fractionX * size.width, fractionY * size.height))
        if (!device.lockForConfiguration(null)) return
        if (device.isFocusPointOfInterestSupported()) {
            device.focusPointOfInterest = point
            device.focusMode = AVCaptureFocusModeAutoFocus
        }
        if (device.isExposurePointOfInterestSupported()) {
            device.exposurePointOfInterest = point
            device.exposureMode = AVCaptureExposureModeAutoExpose
        }
        device.unlockForConfiguration()
    }

    override fun toggleFlash() { isFlashOn = !isFlashOn }

    override fun switchLens() {
        if (movieOutput.recording) return
        isFrontLens = !isFrontLens
        session.beginConfiguration()
        attachVideo()
        session.commitConfiguration()
    }

    override suspend fun takePhoto(): MediaRef? {
        val capturedAt = nowMillis()
        val path = newCapturePath(PHOTO_EXTENSION)
        val saved = suspendCancellableCoroutine { continuation ->
            val settings = AVCapturePhotoSettings.photoSettings().also {
                it.flashMode = if (isFlashOn) AVCaptureFlashModeOn else AVCaptureFlashModeOff
            }
            val delegate = object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
                override fun captureOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: NSError?) {
                    val data = if (error == null) didFinishProcessingPhoto.fileDataRepresentation() else null
                    val ok = data != null && data.writeToFile(path, atomically = true)
                    if (continuation.isActive) continuation.resume(ok)
                    photoDelegate = null
                }
            }
            photoDelegate = delegate
            photoOutput.capturePhotoWithSettings(settings, delegate = delegate)
        }
        return if (saved) MediaRef(path, PHOTO_MIME_TYPE, capturedAtEpochMillis = capturedAt) else null
    }

    override suspend fun startVideo(): Boolean {
        if (!isReady || movieOutput.recording) return false
        val path = newCapturePath(VIDEO_EXTENSION)
        val result = CompletableDeferred<MediaRef?>()
        finished = result
        videoStartedAt = nowMillis()
        val delegate = object : NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {
            override fun captureOutput(
                output: AVCaptureFileOutput, didFinishRecordingToOutputFileAtURL: NSURL, fromConnections: List<*>, error: NSError?,
            ) {
                val duration = nowMillis() - videoStartedAt
                result.complete(
                    if (error == null) MediaRef(path, VIDEO_MIME_TYPE, capturedAtEpochMillis = videoStartedAt, durationMillis = duration) else null,
                )
                setTorch(false)
                recordingDelegate = null
            }
        }
        recordingDelegate = delegate
        setTorch(isFlashOn)
        movieOutput.startRecordingToOutputFileURL(NSURL.fileURLWithPath(path), recordingDelegate = delegate)
        return true
    }

    override suspend fun finishVideo(): MediaRef? {
        if (!movieOutput.recording) return null
        movieOutput.stopRecording()
        return finished?.await()
    }

    private fun setTorch(on: Boolean) {
        val device = videoInput?.device ?: return
        if (!device.hasTorch || !device.isTorchAvailable()) return
        if (device.lockForConfiguration(null)) {
            device.setTorchMode(if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff)
            device.unlockForConfiguration()
        }
    }
}

@Composable
internal actual fun rememberCameraHandle(): CameraHandle {
    val handle = remember { IosCameraHandle() }
    DisposableEffect(handle) {
        handle.start()
        onDispose { handle.stop() }
    }
    return handle
}

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun LiveCameraPreview(handle: CameraHandle, modifier: Modifier) {
    val camera = handle as? IosCameraHandle ?: return
    val previewLayer = remember {
        AVCaptureVideoPreviewLayer(session = camera.session).also {
            it.videoGravity = AVLayerVideoGravityResizeAspectFill
            camera.previewLayer = it
        }
    }
    UIKitView(
        factory = { UIView().also { it.layer.addSublayer(previewLayer) } },
        modifier = modifier,
        onResize = { view, rect -> view.setFrame(rect); previewLayer.setFrame(rect) },
    )
}

// ---------------------------------------------------------------------------------------------------------------------
// Playback of a recorded video, in a loop
// ---------------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun LoopingVideo(uri: String, muted: Boolean, seek: SeekRequest?, onProgress: (Float) -> Unit, modifier: Modifier) {
    val player = remember(uri) { AVPlayer(playerItem = AVPlayerItem(uRL = NSURL.fileURLWithPath(uri))) }
    val layer = remember(player) { AVPlayerLayer.playerLayerWithPlayer(player).also { it.videoGravity = AVLayerVideoGravityResizeAspectFill } }
    player.muted = muted
    DisposableEffect(player) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(AVPlayerItemDidPlayToEndTimeNotification, player.currentItem, null) {
            player.seekToTime(CMTimeMake(0, VIDEO_TIMESCALE))
            player.play()
        }
        player.play()
        onDispose {
            player.pause()
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
    LaunchedEffect(seek) {
        val request = seek ?: return@LaunchedEffect
        val total = player.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: return@LaunchedEffect
        if (total > 0.0 && !total.isNaN()) player.seekToTime(CMTimeMakeWithSeconds(total * request.fraction.coerceIn(0f, 1f), VIDEO_TIMESCALE))
    }
    LaunchedEffect(player) {
        while (true) {
            val total = player.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
            val now = CMTimeGetSeconds(player.currentTime())
            onProgress(if (total > 0.0 && !total.isNaN()) (now / total).toFloat().coerceIn(0f, 1f) else 0f)
            delay(PROGRESS_POLL_MILLIS)
        }
    }
    UIKitView(
        factory = { UIView().also { it.layer.addSublayer(layer) } },
        modifier = modifier,
        onResize = { view, rect -> view.setFrame(rect); layer.setFrame(rect) },
    )
}

// ---------------------------------------------------------------------------------------------------------------------
// Decoding photos and the first frame of videos
// ---------------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberMediaBitmap(uri: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = uri?.let { decode(it) }
    }
    return bitmap
}

@OptIn(ExperimentalForeignApi::class)
private fun decode(path: String): ImageBitmap? {
    val data: NSData? = if (isVideoFile(path)) firstFrame(path) else NSData.dataWithContentsOfFile(path)
    val bytes = data?.toByteArray() ?: return null
    return runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}

@OptIn(ExperimentalForeignApi::class)
private fun firstFrame(path: String): NSData? {
    val generator = AVAssetImageGenerator(aSSET = AVURLAsset.URLAssetWithURL(NSURL.fileURLWithPath(path), options = null))
    generator.appliesPreferredTrackTransform = true
    val cgImage = generator.copyCGImageAtTime(CMTimeMake(0, VIDEO_TIMESCALE), actualTime = null, error = null) ?: return null
    return UIImagePNGRepresentation(UIImage.imageWithCGImage(cgImage))
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { bytes ->
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}
