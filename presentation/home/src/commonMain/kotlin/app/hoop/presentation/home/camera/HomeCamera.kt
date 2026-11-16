package app.hoop.presentation.home.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.component.HoopBackHandler
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.common.PostPolicy
import app.hoop.domain.post.MediaRef
import app.hoop.presentation.home.CaptureUi
import app.hoop.presentation.home.HomeIntent
import app.hoop.presentation.home.resources.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource

private object CameraSpec {
    val edge = 16.dp
    val topExtra = 10.dp
    val round = 44.dp
    val roundIcon = 24.dp
    val flipGap = 12.dp
    val shutter = 96.dp
    val shutterInner = 72.dp
    val shutterRing = 5.dp
    val shutterBottom = 36.dp
    val hintAboveShutter = 14.dp
    val confirmButton = 76.dp
    val confirmGap = 40.dp
    // The confirmation controls are drawn on a 390 wide frame: they scale with the width of the screen.
    val designWidth = 390.dp
    val confirmBottom = 56.dp
    val discardIcon = 32.dp
    val confirmIcon = 36.dp
    val permissionGlyph = 32.dp
    val minConfirmBottom = 16.dp
    val pillHeight = 34.dp
    val progressHeight = 3.dp
    val audioButton = 36.dp
    val permissionIcon = 88.dp
    val permissionButtonHeight = 54.dp
    const val SCRIM_ALPHA = 0.35f
    const val RING_ALPHA = 0.45f
    const val PILL_ALPHA = 0.4f
    const val GLASS_ALPHA = 0.25f
    const val RECORDING_SCALE = 0.62f
    const val FRAME_MILLIS_PER_NANO = 1_000_000L
    const val SECOND_MILLIS = 1000L
    const val SECONDS_PER_MINUTE = 60L
    const val RING_START_ANGLE = -90f
    const val FULL_TURN = 360f
    val gradientTop = 140.dp
    val gradientBottom = 260.dp
}

/**
 * The full-screen flow of a new photo or video: the live camera, then the confirmation of the shot.
 * It sits above the whole home, so nothing else can be touched while it is open.
 */
@Composable
internal fun CaptureOverlay(capture: CaptureUi, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val media = capture.media
    HoopBackHandler(true) { onIntent(if (media == null) HomeIntent.CameraClosed else HomeIntent.CaptureDiscarded) }
    Box(modifier.fillMaxSize().background(HoopTheme.colors.brand).pointerInput(Unit) { awaitEachGesture { awaitFirstDown() } }) {
        if (media == null) CameraScreen(onIntent) else ConfirmScreen(media, onIntent)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Live camera
// ---------------------------------------------------------------------------------------------------------------------

@Composable
private fun CameraScreen(onIntent: (HomeIntent) -> Unit) {
    val permissions = rememberCameraPermissions()
    // The first time the screen opens, the system asks right away: the camera is the only reason to be here.
    LaunchedEffect(Unit) { if (permissions.access == CameraAccess.CanAsk) permissions.request() }
    if (permissions.access == CameraAccess.Granted) {
        LiveCamera(onIntent)
    } else {
        PermissionNeeded(permissions, onClose = { onIntent(HomeIntent.CameraClosed) })
    }
}

@Composable
private fun LiveCamera(onIntent: (HomeIntent) -> Unit) {
    val colors = HoopTheme.colors
    val camera = rememberCameraHandle()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var elapsedMillis by remember { mutableFloatStateOf(0f) }
    var startJob by remember { mutableStateOf<Job?>(null) }

    fun captured(media: MediaRef?) { if (media != null) onIntent(HomeIntent.MediaCaptured(media)) }
    fun stopRecording() {
        if (!recording) return
        recording = false
        scope.launch {
            startJob?.join()
            captured(camera.finishVideo())
        }
    }

    // The clock of the recording; a video ends by itself at the maximum length.
    LaunchedEffect(recording) {
        if (!recording) { elapsedMillis = 0f; return@LaunchedEffect }
        var startNanos = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (startNanos < 0) startNanos = now
                elapsedMillis = (now - startNanos).toFloat() / CameraSpec.FRAME_MILLIS_PER_NANO
            }
            if (elapsedMillis >= PostPolicy.VIDEO_MAX_MILLIS) { stopRecording(); break }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LiveCameraPreview(camera, Modifier.fillMaxSize())
        Shades()
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + CameraSpec.topExtra
        RoundGlass(HoopIcons.Close, stringResource(Res.string.camera_close), { onIntent(HomeIntent.CameraClosed) },
            Modifier.align(Alignment.TopStart).padding(start = CameraSpec.edge, top = top))
        Column(Modifier.align(Alignment.TopEnd).padding(end = CameraSpec.edge, top = top), verticalArrangement = Arrangement.spacedBy(CameraSpec.flipGap)) {
            RoundGlass(
                if (camera.isFlashOn) HoopIcons.Flash else HoopIcons.FlashOff,
                stringResource(if (camera.isFlashOn) Res.string.camera_flash_on else Res.string.camera_flash_off),
                camera::toggleFlash,
            )
            RoundGlass(HoopIcons.FlipCamera, stringResource(Res.string.camera_switch), camera::switchLens)
        }
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + CameraSpec.shutterBottom
        if (recording) {
            Pill(formatClock(elapsedMillis.toLong()), Modifier.align(Alignment.TopCenter).padding(top = top))
        } else {
            Text(
                stringResource(Res.string.camera_hint),
                Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + CameraSpec.shutter + CameraSpec.hintAboveShutter),
                style = HoopTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.onScrim, textAlign = TextAlign.Center,
            )
        }
        Shutter(
            recording = recording, progress = elapsedMillis / PostPolicy.VIDEO_MAX_MILLIS, enabled = camera.isReady && !busy,
            onTap = {
                busy = true
                scope.launch { captured(camera.takePhoto()); busy = false }
            },
            onHoldStart = {
                recording = true
                startJob = scope.launch { if (!camera.startVideo()) recording = false }
            },
            onHoldEnd = ::stopRecording,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottom),
        )
    }
}

/**
 * The shutter: a tap takes a photo, a press held for [PostPolicy.VIDEO_MIN_MILLIS] starts a video that lasts as long as
 * the finger stays down. The ring fills while recording.
 */
@Composable
private fun Shutter(
    recording: Boolean, progress: Float, enabled: Boolean,
    onTap: () -> Unit, onHoldStart: () -> Unit, onHoldEnd: () -> Unit, modifier: Modifier = Modifier,
) {
    val colors = HoopTheme.colors
    val shutterLabel = stringResource(Res.string.camera_shutter)
    Box(
        modifier.size(CameraSpec.shutter).semantics { contentDescription = shutterLabel }.pointerInput(enabled) {
            if (!enabled && !recording) return@pointerInput
            awaitEachGesture {
                awaitFirstDown()
                val upEarly = withTimeoutOrNull(PostPolicy.VIDEO_MIN_MILLIS) { waitForUpOrCancellation() }
                if (upEarly != null) { onTap(); return@awaitEachGesture }
                onHoldStart()
                waitForUpOrCancellation()
                onHoldEnd()
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val ring = CameraSpec.shutterRing.toPx()
            val inset = ring / 2f
            val arc = Size(size.width - ring, size.height - ring)
            drawCircle(colors.onScrim.copy(alpha = CameraSpec.RING_ALPHA), radius = (size.minDimension - ring) / 2f, style = Stroke(ring))
            if (recording) {
                drawArc(
                    colors.accent, CameraSpec.RING_START_ANGLE, CameraSpec.FULL_TURN * progress.coerceIn(0f, 1f), useCenter = false,
                    topLeft = Offset(inset, inset), size = arc, style = Stroke(ring, cap = StrokeCap.Round),
                )
            }
        }
        Box(
            Modifier.size(CameraSpec.shutterInner).scale(if (recording) CameraSpec.RECORDING_SCALE else 1f)
                .background(if (recording) colors.accent else colors.onScrim, HoopTheme.shapes.circle),
        )
    }
}

@Composable
private fun PermissionNeeded(permissions: CameraPermissions, onClose: () -> Unit) {
    val colors = HoopTheme.colors
    val blocked = permissions.access == CameraAccess.Blocked
    Box(Modifier.fillMaxSize()) {
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + CameraSpec.topExtra
        RoundGlass(HoopIcons.Close, stringResource(Res.string.camera_close), onClose, Modifier.align(Alignment.TopStart).padding(start = CameraSpec.edge, top = top))
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = HoopTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(HoopTheme.spacing.md),
        ) {
            Box(Modifier.size(CameraSpec.permissionIcon).background(colors.onScrim.copy(alpha = CameraSpec.GLASS_ALPHA), HoopTheme.shapes.circle), contentAlignment = Alignment.Center) {
                Icon(HoopIcons.Camera, null, tint = colors.onScrim, modifier = Modifier.size(CameraSpec.permissionGlyph))
            }
            Text(stringResource(Res.string.camera_permission_title), style = HoopTheme.typography.titleLarge, color = colors.onScrim, textAlign = TextAlign.Center)
            Text(
                stringResource(if (blocked) Res.string.camera_permission_blocked else Res.string.camera_permission_body),
                style = HoopTheme.typography.bodyLarge, color = colors.onScrim.copy(alpha = 0.85f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(HoopTheme.spacing.sm))
            Box(
                Modifier.fillMaxWidth().height(CameraSpec.permissionButtonHeight).background(colors.accent, HoopTheme.shapes.circle).clip(HoopTheme.shapes.circle)
                    .clickable(role = Role.Button) { if (blocked) permissions.openSettings() else permissions.request() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (blocked) Res.string.camera_permission_settings else Res.string.camera_permission_allow),
                    style = HoopTheme.typography.titleMedium, color = colors.onBrand,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Confirmation
// ---------------------------------------------------------------------------------------------------------------------

@Composable
private fun ConfirmScreen(media: MediaRef, onIntent: (HomeIntent) -> Unit) {
    val colors = HoopTheme.colors
    var muted by remember(media.uri) { mutableStateOf(true) }
    var progress by remember(media.uri) { mutableFloatStateOf(0f) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val unit = maxWidth / CameraSpec.designWidth
        if (media.isVideo) {
            LoopingVideo(media.uri, muted, { progress = it }, Modifier.fillMaxSize())
        } else {
            rememberMediaBitmap(media.uri)?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Shades()
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        if (media.isVideo) {
            Box(
                Modifier.align(Alignment.TopStart).padding(start = CameraSpec.edge, end = CameraSpec.edge, top = top + CameraSpec.topExtra - 4.dp)
                    .fillMaxWidth().height(CameraSpec.progressHeight).background(colors.onScrim.copy(alpha = 0.3f), HoopTheme.shapes.circle),
            ) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(CameraSpec.progressHeight).background(colors.onScrim, HoopTheme.shapes.circle))
            }
            Pill(formatClock(media.durationMillis ?: 0L), Modifier.align(Alignment.TopStart).padding(start = CameraSpec.edge, top = top + CameraSpec.topExtra + 12.dp))
            Box(
                Modifier.align(Alignment.TopEnd).padding(end = CameraSpec.edge, top = top + CameraSpec.topExtra + 10.dp).size(CameraSpec.audioButton)
                    .background(colors.brand.copy(alpha = CameraSpec.PILL_ALPHA), HoopTheme.shapes.circle).clip(HoopTheme.shapes.circle)
                    .semantics { contentDescription = "" }.clickable(role = Role.Button) { muted = !muted },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (muted) HoopIcons.VolumeOff else HoopIcons.Volume,
                    stringResource(if (muted) Res.string.capture_sound_on else Res.string.capture_sound_off),
                    tint = colors.onScrim, modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Pill(stringResource(Res.string.capture_taken_now), Modifier.align(Alignment.TopCenter).padding(top = top + CameraSpec.topExtra + 8.dp), HoopIcons.Clock)
        }
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = maxOf(CameraSpec.confirmBottom * unit, navBottom + CameraSpec.minConfirmBottom)),
            horizontalArrangement = Arrangement.spacedBy(CameraSpec.confirmGap * unit),
        ) {
            ConfirmButton(
                colors.onScrim.copy(alpha = CameraSpec.GLASS_ALPHA), HoopIcons.CloseLight, stringResource(Res.string.capture_discard), colors.onScrim,
                CameraSpec.confirmButton * unit, CameraSpec.discardIcon * unit,
            ) { onIntent(HomeIntent.CaptureDiscarded) }
            ConfirmButton(
                colors.accent, HoopIcons.CheckHeavy, stringResource(Res.string.capture_confirm), colors.onScrim,
                CameraSpec.confirmButton * unit, CameraSpec.confirmIcon * unit, glow = true,
            ) { onIntent(HomeIntent.CaptureConfirmed) }
        }
    }
}

@Composable
private fun ConfirmButton(
    background: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, tint: Color,
    size: Dp, iconSize: Dp, glow: Boolean = false, onClick: () -> Unit,
) {
    Box(
        Modifier.size(size).then(if (glow) Modifier.shadow(HoopTheme.elevation.badge, HoopTheme.shapes.circle) else Modifier)
            .background(background, HoopTheme.shapes.circle).clip(HoopTheme.shapes.circle)
            .semantics { contentDescription = description }.clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------------------------------

/** Darkens the top and the bottom of a full-screen picture so the controls stay readable. */
@Composable
private fun Shades() {
    val shade = HoopTheme.colors.brand
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(CameraSpec.gradientTop).background(Brush.verticalGradient(listOf(shade.copy(alpha = 0.4f), Color.Transparent))))
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(CameraSpec.gradientBottom)
                .background(Brush.verticalGradient(listOf(Color.Transparent, shade.copy(alpha = 0.5f)))),
        )
    }
}

@Composable
private fun RoundGlass(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    Box(
        modifier.size(CameraSpec.round).background(colors.brand.copy(alpha = CameraSpec.PILL_ALPHA - 0.05f), HoopTheme.shapes.circle).clip(HoopTheme.shapes.circle)
            .semantics { contentDescription = description }.clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = colors.onScrim, modifier = Modifier.size(CameraSpec.roundIcon))
    }
}

@Composable
private fun Pill(text: String, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val colors = HoopTheme.colors
    Row(
        modifier.height(CameraSpec.pillHeight).background(colors.brand.copy(alpha = CameraSpec.PILL_ALPHA), HoopTheme.shapes.circle).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = colors.onScrim, modifier = Modifier.size(16.dp))
        Text(text, style = HoopTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = colors.onScrim)
    }
}

/** Minutes and seconds of a length in milliseconds, as 0:12. */
internal fun formatClock(millis: Long): String {
    val total = millis / CameraSpec.SECOND_MILLIS
    val seconds = total % CameraSpec.SECONDS_PER_MINUTE
    return "${total / CameraSpec.SECONDS_PER_MINUTE}:${seconds.toString().padStart(2, '0')}"
}
