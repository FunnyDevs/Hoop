package app.hoop.presentation.home.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import app.hoop.domain.post.MediaRef
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.max

private const val CAPTURES_DIR = "captures"
private const val PHOTO_EXTENSION = ".jpg"
private const val NANOS_PER_MILLI = 1_000_000L
private const val MAX_BITMAP_SIDE = 1600
private const val PROGRESS_POLL_MILLIS = 40L
private const val PACKAGE_SCHEME = "package"

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

// ---------------------------------------------------------------------------------------------------------------------
// Permissions: camera and microphone, both required
// ---------------------------------------------------------------------------------------------------------------------

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

@Composable
internal actual fun rememberCameraPermissions(): CameraPermissions {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    // Bumped when the answer may have changed: after the system dialog and when coming back from the settings.
    var refresh by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        refresh++
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val granted = refresh.let { REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED } }
    val activity = context.findActivity()
    val canStillAsk = refresh.let { REQUIRED_PERMISSIONS.any { activity?.shouldShowRequestPermissionRationale(it) == true } }
    val access = when {
        granted -> CameraAccess.Granted
        asked && !canStillAsk -> CameraAccess.Blocked
        else -> CameraAccess.CanAsk
    }
    return CameraPermissions(
        access = access,
        request = { launcher.launch(REQUIRED_PERMISSIONS) },
        openSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(PACKAGE_SCHEME, context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
    )
}

// ---------------------------------------------------------------------------------------------------------------------
// Live camera (CameraX)
// ---------------------------------------------------------------------------------------------------------------------

private class FinishedVideo(val file: File, val durationMillis: Long)

/** CameraX camera: a live preview, JPEG photos and MP4 video with sound, saved in the private cache of the app. */
private class AndroidCameraHandle(private val context: Context) : CameraHandle {
    override var isReady by mutableStateOf(false)
        private set
    override var isFlashOn by mutableStateOf(false)
        private set
    override var isFrontLens by mutableStateOf(false)
        private set

    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    private val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
        .build()
    private val videoCapture = VideoCapture.withOutput(recorder)

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewView: PreviewView? = null
    private var owner: LifecycleOwner? = null
    private var recording: Recording? = null
    private var finished: CompletableDeferred<FinishedVideo?>? = null
    private var videoStartedAt = 0L

    fun attach(view: PreviewView, lifecycleOwner: LifecycleOwner) {
        if (previewView === view && owner === lifecycleOwner) return
        previewView = view
        owner = lifecycleOwner
        bind()
    }

    fun release() {
        recording?.stop()
        recording = null
        provider?.unbindAll()
        isReady = false
    }

    private fun bind() {
        val view = previewView ?: return
        val lifecycleOwner = owner ?: return
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = future.get()
            provider = cameraProvider
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            val selector = if (isFrontLens) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, videoCapture)
                applyFlash()
                isReady = true
            } catch (e: Exception) {
                isReady = false
            }
        }, mainExecutor)
    }

    override fun toggleFlash() {
        isFlashOn = !isFlashOn
        applyFlash()
    }

    override fun switchLens() {
        if (recording != null) return
        isFrontLens = !isFrontLens
        isReady = false
        bind()
    }

    private fun applyFlash() {
        imageCapture.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        // While filming the flash is a torch.
        if (recording != null) camera?.cameraControl?.enableTorch(isFlashOn)
    }

    override suspend fun takePhoto(): MediaRef? {
        val file = newFile(PHOTO_EXTENSION)
        val capturedAt = System.currentTimeMillis()
        val options = ImageCapture.OutputFileOptions.Builder(file)
            .setMetadata(ImageCapture.Metadata().apply { isReversedHorizontal = isFrontLens })
            .build()
        val saved = suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) { if (continuation.isActive) continuation.resume(true) }
                override fun onError(exception: ImageCaptureException) { if (continuation.isActive) continuation.resume(false) }
            })
        }
        return if (saved) MediaRef(file.absolutePath, PHOTO_MIME_TYPE, capturedAtEpochMillis = capturedAt) else null
    }

    override suspend fun startVideo(): Boolean {
        if (!isReady || recording != null) return false
        val file = newFile(VIDEO_EXTENSION)
        val started = CompletableDeferred<Boolean>()
        val result = CompletableDeferred<FinishedVideo?>()
        finished = result
        videoStartedAt = System.currentTimeMillis()
        return try {
            val pending = recorder.prepareRecording(context, FileOutputOptions.Builder(file).build()).withAudioEnabled()
            recording = pending.start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> started.complete(true)
                    is VideoRecordEvent.Finalize -> {
                        started.complete(false)
                        if (event.hasError()) {
                            file.delete()
                            result.complete(null)
                        } else {
                            result.complete(FinishedVideo(file, event.recordingStats.recordedDurationNanos / NANOS_PER_MILLI))
                        }
                    }
                    else -> Unit
                }
            }
            if (isFlashOn) camera?.cameraControl?.enableTorch(true)
            val ok = started.await()
            if (!ok) recording = null
            ok
        } catch (e: SecurityException) {
            recording = null
            false
        }
    }

    override suspend fun finishVideo(): MediaRef? {
        val active = recording ?: return null
        recording = null
        active.stop()
        camera?.cameraControl?.enableTorch(false)
        val video = finished?.await() ?: return null
        return MediaRef(video.file.absolutePath, VIDEO_MIME_TYPE, capturedAtEpochMillis = videoStartedAt, durationMillis = video.durationMillis)
    }

    private fun newFile(extension: String): File {
        val dir = File(context.cacheDir, CAPTURES_DIR).apply { mkdirs() }
        return File(dir, "${System.currentTimeMillis()}$extension")
    }
}

@Composable
internal actual fun rememberCameraHandle(): CameraHandle {
    val context = LocalContext.current.applicationContext
    val handle = remember { AndroidCameraHandle(context) }
    DisposableEffect(handle) { onDispose { handle.release() } }
    return handle
}

@Composable
internal actual fun LiveCameraPreview(handle: CameraHandle, modifier: Modifier) {
    val owner = LocalLifecycleOwner.current
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // TextureView underneath: the controls of the app can sit on top of it without z-order surprises.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { view -> (handle as? AndroidCameraHandle)?.attach(view, owner) },
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------------------------------------------------
// Playback of a recorded video, centered and cropped to fill like the photos
// ---------------------------------------------------------------------------------------------------------------------

private class LoopingVideoView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var path: String? = null
    private var muted = true
    private var frameWidth = 0
    private var frameHeight = 0

    init { surfaceTextureListener = this }

    fun show(newPath: String, isMuted: Boolean) {
        muted = isMuted
        player?.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
        if (newPath != path) {
            path = newPath
            if (isAvailable) surfaceTexture?.let(::start)
        }
    }

    /** 0..1 of the current loop. */
    fun progress(): Float {
        val p = player ?: return 0f
        val total = p.duration
        return if (total > 0) (p.currentPosition.toFloat() / total).coerceIn(0f, 1f) else 0f
    }

    private fun start(texture: SurfaceTexture) {
        val file = path ?: return
        player?.release()
        player = MediaPlayer().apply {
            setSurface(Surface(texture))
            setDataSource(file)
            isLooping = true
            setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
            setOnVideoSizeChangedListener { _, w, h -> frameWidth = w; frameHeight = h; applyCover() }
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    private fun applyCover() {
        if (frameWidth == 0 || frameHeight == 0 || width == 0 || height == 0) return
        val scale = max(width.toFloat() / frameWidth, height.toFloat() / frameHeight)
        val matrix = Matrix().apply { setScale(frameWidth * scale / width, frameHeight * scale / height, width / 2f, height / 2f) }
        setTransform(matrix)
    }

    fun release() {
        player?.release()
        player = null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) = start(surface)
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = applyCover()
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { release(); return true }
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
}

@Composable
internal actual fun LoopingVideo(uri: String, muted: Boolean, onProgress: (Float) -> Unit, modifier: Modifier) {
    val view = remember { mutableStateOf<LoopingVideoView?>(null) }
    LaunchedEffect(view.value) {
        val video = view.value ?: return@LaunchedEffect
        while (true) {
            onProgress(video.progress())
            delay(PROGRESS_POLL_MILLIS)
        }
    }
    AndroidView(
        factory = { context -> LoopingVideoView(context).also { view.value = it } },
        update = { it.show(uri, muted) },
        onRelease = { it.release() },
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------------------------------------------------
// Decoding photos and the first frame of videos
// ---------------------------------------------------------------------------------------------------------------------

@Composable
internal actual fun rememberMediaBitmap(uri: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) { runCatching { decode(uri) }.getOrNull() }
    }
    return bitmap
}

private fun decode(path: String): ImageBitmap? {
    if (!File(path).exists()) return null
    return if (isVideoFile(path)) firstFrame(path) else photo(path)
}

private fun firstFrame(path: String): ImageBitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.asImageBitmap()
    } finally {
        retriever.release()
    }
}

private fun photo(path: String): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_BITMAP_SIDE) sample *= 2
    val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val matrix = Matrix()
    when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return decoded.asImageBitmap()
    }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).asImageBitmap()
}
