package app.hoop.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** The area of a Hoop drawn on the map. */
class HoopMapArea(val id: String, val latitude: Double, val longitude: Double, val radiusMeters: Int, val color: Color, val active: Boolean)

/** A post drawn as a pin on the map. */
class HoopMapPin(val id: String, val latitude: Double, val longitude: Double, val kind: HoopPostKind, val selected: Boolean)

/** Where the camera looks: a point, and the radius that should fit in the visible area. */
class HoopMapFocus(val latitude: Double, val longitude: Double, val radiusMeters: Int)

private object HomeMapSpec {
    val outlineWidth = 2.dp
    val dashPattern = listOf(3, 2)
    const val AREA_ALPHA = 0.11f
    const val INACTIVE_ALPHA = 0.35f
    const val HALO_ALPHA = 0.18f
    val haloRadius = 20.dp
    const val FIT_FRACTION = 0.74f
    val dotRadius = 8.dp
    val dotStroke = 3.dp
    val animation = 700.milliseconds
}

/**
 * Real map (MapLibre) of the home: Hoop areas, post pins, the user's position.
 * The camera follows [focus] with animation; [contentPadding] keeps the focus clear of overlays.
 */
@Composable
fun HoopHomeMap(
    styleUri: String,
    areas: List<HoopMapArea>,
    pins: List<HoopMapPin>,
    userLatitude: Double,
    userLongitude: Double,
    focus: HoopMapFocus,
    mapWidth: Dp,
    onPinClick: (String) -> Unit,
    questionGlyph: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = HoopTheme.colors
    fun position(f: HoopMapFocus) = CameraPosition(
        target = Position(f.longitude, f.latitude),
        zoom = MapGeometry.zoomFor(f.radiusMeters, f.latitude, mapWidth.value, HomeMapSpec.FIT_FRACTION),
        padding = contentPadding,
    )
    val camera = rememberCameraState(position(focus))
    LaunchedEffect(focus.latitude, focus.longitude, focus.radiusMeters, mapWidth, contentPadding) {
        camera.animateTo(position(focus), HomeMapSpec.animation)
    }

    Box(modifier.fillMaxSize()) {
    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        styleUri = styleUri,
        cameraState = camera,
        gestureSettings = GestureSettings(
            isTiltGesturesEnabled = false, isZoomGesturesEnabled = false, isRotateGesturesEnabled = false,
            isScrollGesturesEnabled = false, isKeyboardGesturesEnabled = false,
        ),
        ornamentSettings = OrnamentSettings.AllDisabled.copy(padding = contentPadding, isAttributionEnabled = true, isLogoEnabled = true),
    ) {
        areas.forEach { area ->
            key(area.id) {
                val polygon = remember(area.latitude, area.longitude, area.radiusMeters) {
                    MapGeometry.circlePolygon(area.latitude, area.longitude, area.radiusMeters.toDouble())
                }
                val source = rememberGeoJsonSource("area-${area.id}", polygon)
                val strength = if (area.active) 1f else HomeMapSpec.INACTIVE_ALPHA
                FillLayer("area-fill-${area.id}", source, color = const(area.color), opacity = const(HomeMapSpec.AREA_ALPHA * strength))
                LineLayer(
                    "area-line-${area.id}", source, color = const(area.color), opacity = const(strength),
                    width = const(HomeMapSpec.outlineWidth), dasharray = const(HomeMapSpec.dashPattern),
                )
            }
        }
        val dot = rememberGeoJsonSource("user-dot", Point(Position(userLongitude, userLatitude)))
        CircleLayer("user-dot-halo", dot, color = const(colors.locationDot.copy(alpha = HomeMapSpec.HALO_ALPHA)), radius = const(HomeMapSpec.haloRadius))
        CircleLayer(
            "user-dot-layer", dot, color = const(colors.locationDot), radius = const(HomeMapSpec.dotRadius),
            strokeColor = const(colors.surface), strokeWidth = const(HomeMapSpec.dotStroke),
        )
    }
    // Pins are ordinary composables placed over the map: they can hold a glyph and animate freely.
    camera.position // read to follow the camera while it moves
    pins.forEach { pin ->
        val spot = runCatching { camera.screenLocationFromPosition(Position(pin.longitude, pin.latitude)) }.getOrNull()
        if (spot != null) {
            HoopPinMarker(pin.kind, pin.selected, questionGlyph, { onPinClick(pin.id) }, Modifier.offset(spot.x - PIN_HALF, spot.y - PIN_HALF))
        }
    }
    }
}

private val PIN_HALF = 15.dp
