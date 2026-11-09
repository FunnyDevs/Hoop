package app.hoop.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.theme.HoopTheme
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.FillLayer
import dev.sargunv.maplibrecompose.compose.layer.LineLayer
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.GestureSettings
import dev.sargunv.maplibrecompose.core.OrnamentSettings
import dev.sargunv.maplibrecompose.expressions.dsl.const
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds

private object RadiusMapSpec {
    const val AREA_ALPHA = 0.14f
    val outlineWidth = 2.dp
    val dashPattern = listOf(3, 2)
    val dotRadius = 7.dp
    val dotStroke = 3.dp
    val cameraAnimation = 350.milliseconds
    const val DASH_DIVISOR = 14f
}

/**
 * Real map (MapLibre) showing the area of a Hoop: a dashed circle of [radiusMeters] around ([latitude], [longitude]).
 * The camera follows the radius so the circle always fits; [contentPadding] keeps it clear of overlays such as a bottom sheet.
 */
@Composable
fun HoopRadiusMap(
    latitude: Double,
    longitude: Double,
    radiusMeters: Int,
    styleUri: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    mapWidth: Dp,
    fitFraction: Float = DEFAULT_FIT_FRACTION,
) {
    val colors = HoopTheme.colors
    // Layers are only composed once the style has loaded: until then (or when offline) a drawn map is shown.
    var styleLoaded by remember { mutableStateOf(false) }
    val camera = rememberCameraState(
        CameraPosition(target = Position(longitude, latitude), zoom = MapGeometry.zoomFor(radiusMeters, latitude, mapWidth.value, fitFraction), padding = contentPadding),
    )
    LaunchedEffect(latitude, longitude, radiusMeters, mapWidth, contentPadding, fitFraction) {
        camera.animateTo(
            CameraPosition(target = Position(longitude, latitude), zoom = MapGeometry.zoomFor(radiusMeters, latitude, mapWidth.value, fitFraction), padding = contentPadding),
            duration = RadiusMapSpec.cameraAnimation,
        )
    }
    val circle = remember(latitude, longitude, radiusMeters) { MapGeometry.circlePolygon(latitude, longitude, radiusMeters.toDouble()) }
    val center = remember(latitude, longitude) { Point(Position(longitude, latitude)) }

    MaplibreMap(
        modifier = modifier.fillMaxSize(),
        styleUri = styleUri,
        cameraState = camera,
        gestureSettings = GestureSettings(
            isTiltGesturesEnabled = false, isZoomGesturesEnabled = false, isRotateGesturesEnabled = false,
            isScrollGesturesEnabled = false, isKeyboardGesturesEnabled = false,
        ),
        ornamentSettings = OrnamentSettings.AllDisabled.copy(padding = contentPadding, isAttributionEnabled = true, isLogoEnabled = true),
    ) {
        LaunchedEffect(Unit) { styleLoaded = true }
        val area = rememberGeoJsonSource("hoop-area", circle)
        val dot = rememberGeoJsonSource("hoop-center", center)
        FillLayer("hoop-area-fill", area, color = const(colors.accent), opacity = const(RadiusMapSpec.AREA_ALPHA))
        LineLayer(
            "hoop-area-outline", area, color = const(colors.accent), width = const(RadiusMapSpec.outlineWidth),
            dasharray = const(RadiusMapSpec.dashPattern),
        )
        CircleLayer(
            "hoop-center-dot", dot, color = const(colors.locationDot), radius = const(RadiusMapSpec.dotRadius),
            strokeColor = const(colors.surface), strokeWidth = const(RadiusMapSpec.dotStroke),
        )
    }
    if (!styleLoaded) OfflineRadiusBackdrop(radiusFraction = fitFraction, contentPadding = contentPadding, modifier = modifier)
}

/** Drawn stand-in for the map while the style loads or when the device is offline. */
@Composable
private fun OfflineRadiusBackdrop(radiusFraction: Float, contentPadding: PaddingValues, modifier: Modifier) {
    val colors = HoopTheme.colors
    val bottom = contentPadding.calculateBottomPadding()
    Box(modifier.fillMaxSize()) {
        HoopMapBackdrop()
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, (size.height - bottom.toPx()) / 2f)
            val radius = size.width * radiusFraction / 2f
            drawCircle(colors.accent.copy(alpha = RadiusMapSpec.AREA_ALPHA), radius, center)
            drawCircle(
                colors.accent, radius, center,
                style = Stroke(RadiusMapSpec.outlineWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(radius / RadiusMapSpec.DASH_DIVISOR, radius / RadiusMapSpec.DASH_DIVISOR))),
            )
            drawCircle(colors.surface, RadiusMapSpec.dotRadius.toPx() + RadiusMapSpec.dotStroke.toPx(), center)
            drawCircle(colors.locationDot, RadiusMapSpec.dotRadius.toPx(), center)
        }
    }
}

private const val DEFAULT_FIT_FRACTION = 0.62f
