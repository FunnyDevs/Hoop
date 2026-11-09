package app.hoop.designsystem.component

import io.github.dellisd.spatialk.geojson.Polygon
import io.github.dellisd.spatialk.geojson.Position
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

internal object MapGeometry {
    const val EARTH_RADIUS_METERS = 6_371_008.8
    const val CIRCLE_STEPS = 96
    /** Meters per dp at zoom 0 on the equator for a 512 px tile world. */
    const val METERS_PER_DP_AT_ZOOM_ZERO = 78_271.516_964
    const val MIN_ZOOM = 1.0
    const val MAX_ZOOM = 17.0

    /** Zoom level at which a circle of [radiusMeters] takes [fitFraction] of [widthDp]. */
    fun zoomFor(radiusMeters: Int, latitude: Double, widthDp: Float, fitFraction: Float): Double {
        if (widthDp <= 0f) return MIN_ZOOM
        val metersPerDp = (2.0 * radiusMeters) / (widthDp * fitFraction)
        val zoom = ln(METERS_PER_DP_AT_ZOOM_ZERO * cos(latitude * PI / 180.0) / metersPerDp) / ln(2.0)
        return zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /** Polygon approximating a geodesic circle (great-circle destination points). */
    fun circlePolygon(latitude: Double, longitude: Double, radiusMeters: Double): Polygon {
        val lat = latitude * PI / 180.0
        val lon = longitude * PI / 180.0
        val angular = radiusMeters / EARTH_RADIUS_METERS
        val ring = (0..CIRCLE_STEPS).map { step ->
            val bearing = 2.0 * PI * step / CIRCLE_STEPS
            val destLat = asin(sin(lat) * cos(angular) + cos(lat) * sin(angular) * cos(bearing))
            val destLon = lon + atan2(sin(bearing) * sin(angular) * cos(lat), cos(angular) - sin(lat) * sin(destLat))
            Position(destLon * 180.0 / PI, destLat * 180.0 / PI)
        }
        return Polygon(listOf(ring))
    }
}
