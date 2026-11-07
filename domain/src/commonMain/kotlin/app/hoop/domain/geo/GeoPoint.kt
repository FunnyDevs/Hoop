package app.hoop.domain.geo

import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.GeoLimits
import app.hoop.domain.common.Violation

/** Value Object: valid geographic coordinate. */
data class GeoPoint private constructor(val latitude: Double, val longitude: Double) {
    companion object {
        fun create(latitude: Double, longitude: Double): DomainResult<GeoPoint> = when {
            latitude !in -GeoLimits.LATITUDE_MAX..GeoLimits.LATITUDE_MAX -> DomainResult.failure(Violation.LatitudeOutOfRange)
            longitude !in -GeoLimits.LONGITUDE_MAX..GeoLimits.LONGITUDE_MAX -> DomainResult.failure(Violation.LongitudeOutOfRange)
            else -> DomainResult.success(GeoPoint(latitude, longitude))
        }
    }
}
