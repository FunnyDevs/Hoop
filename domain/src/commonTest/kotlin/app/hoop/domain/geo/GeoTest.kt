package app.hoop.domain.geo

import app.hoop.domain.assertSuccess
import app.hoop.domain.assertViolation
import app.hoop.domain.common.HoopPolicy
import app.hoop.domain.common.Violation
import kotlin.test.Test
import kotlin.test.assertEquals

class GeoTest {
    @Test fun radiusBounds() {
        Radius.create(HoopPolicy.RADIUS_MIN_METERS - 1).assertViolation(Violation.RadiusOutOfRange)
        Radius.create(HoopPolicy.RADIUS_MAX_METERS + 1).assertViolation(Violation.RadiusOutOfRange)
        assertEquals(HoopPolicy.RADIUS_MIN_METERS, Radius.create(HoopPolicy.RADIUS_MIN_METERS).assertSuccess().meters)
    }
    @Test fun defaultRadius() = assertEquals(HoopPolicy.RADIUS_DEFAULT_METERS, Radius.Default.meters)
    @Test fun latitudeBounds() = GeoPoint.create(91.0, 0.0).assertViolation(Violation.LatitudeOutOfRange)
    @Test fun longitudeBounds() = GeoPoint.create(0.0, -181.0).assertViolation(Violation.LongitudeOutOfRange)
}
