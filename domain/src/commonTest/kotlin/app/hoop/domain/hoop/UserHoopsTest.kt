package app.hoop.domain.hoop

import app.hoop.domain.assertSuccess
import app.hoop.domain.assertViolation
import app.hoop.domain.common.HoopPolicy
import app.hoop.domain.common.Violation
import app.hoop.domain.geo.Radius
import kotlin.test.Test
import kotlin.test.assertEquals

class UserHoopsTest {
    private val hoops = UserHoops(testHome())

    @Test fun addExtra() = assertEquals(2, hoops.add(testExtra("a")).assertSuccess().all.size)

    @Test fun cannotExceedMaxExtras() {
        var current = hoops
        repeat(HoopPolicy.MAX_EXTRA_HOOPS) { current = current.add(testExtra("e$it")).assertSuccess() }
        current.add(testExtra("one-too-many")).assertViolation(Violation.TooManyHoops)
    }

    @Test fun homeCannotBeRemoved() = hoops.remove(hoops.home.id).assertViolation(Violation.HomeHoopCannotBeRemoved)
    @Test fun unknownHoopCannotBeRemoved() = hoops.remove(HoopId("nope")).assertViolation(Violation.HoopNotFound)

    @Test fun removeExtra() {
        val withExtra = hoops.add(testExtra("a")).assertSuccess()
        assertEquals(1, withExtra.remove(HoopId("a")).assertSuccess().all.size)
    }

    @Test fun resizeHome() {
        val radius = Radius.create(HoopPolicy.RADIUS_MAX_METERS).assertSuccess()
        assertEquals(radius, hoops.resize(hoops.home.id, radius).assertSuccess().home.radius)
    }
}
