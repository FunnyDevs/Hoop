package app.hoop.domain.geo

import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.HoopPolicy
import app.hoop.domain.common.Violation
import kotlin.jvm.JvmInline

/** Value Object: radius of an Hoop in meters, within the limits set by the product. */
@JvmInline
value class Radius private constructor(val meters: Int) {
    companion object {
        val Default: Radius = Radius(HoopPolicy.RADIUS_DEFAULT_METERS)

        fun create(meters: Int): DomainResult<Radius> =
            if (meters in HoopPolicy.RADIUS_MIN_METERS..HoopPolicy.RADIUS_MAX_METERS) DomainResult.success(Radius(meters))
            else DomainResult.failure(Violation.RadiusOutOfRange)
    }
}
