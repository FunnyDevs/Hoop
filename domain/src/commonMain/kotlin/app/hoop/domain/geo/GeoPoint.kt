package app.hoop.domain.geo

import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.GeoLimits
import app.hoop.domain.common.Violation
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Value Object: valid geographic coordinate. */
data class GeoPoint private constructor(val latitude: Double, val longitude: Double) {
    /** Distance to [other] along the surface of the Earth (haversine), in meters. */
    fun distanceMetersTo(other: GeoPoint): Int {
        val lat1 = latitude * PI / HALF_TURN_DEGREES
        val lat2 = other.latitude * PI / HALF_TURN_DEGREES
        val dLat = lat2 - lat1
        val dLon = (other.longitude - longitude) * PI / HALF_TURN_DEGREES
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return (2 * EARTH_RADIUS_METERS * asin(sqrt(a))).roundToInt()
    }

    companion object {
        fun create(latitude: Double, longitude: Double): DomainResult<GeoPoint> = when {
            latitude !in -GeoLimits.LATITUDE_MAX..GeoLimits.LATITUDE_MAX -> DomainResult.failure(Violation.LatitudeOutOfRange)
            longitude !in -GeoLimits.LONGITUDE_MAX..GeoLimits.LONGITUDE_MAX -> DomainResult.failure(Violation.LongitudeOutOfRange)
            else -> DomainResult.success(GeoPoint(latitude, longitude))
        }
    }
}

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val HALF_TURN_DEGREES = 180.0
