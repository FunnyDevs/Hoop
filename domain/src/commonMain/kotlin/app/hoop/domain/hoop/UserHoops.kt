package app.hoop.domain.hoop

import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.HoopPolicy
import app.hoop.domain.common.Violation
import app.hoop.domain.geo.Radius

/**
 * Aggregate Root: all the Hoops of a user. Guarantees the invariants:
 * there is always exactly one initial Hoop, it cannot be removed, extras are at most [HoopPolicy.MAX_EXTRA_HOOPS].
 * It is immutable: every change returns a new aggregate.
 */
class UserHoops(val home: Hoop.Home, val extras: List<Hoop.Extra> = emptyList()) {
    init {
        require(extras.size <= HoopPolicy.MAX_EXTRA_HOOPS) { "Too many extra Hoops" }
    }

    val all: List<Hoop> get() = listOf(home) + extras

    fun find(id: HoopId): Hoop? = all.firstOrNull { it.id == id }

    fun add(extra: Hoop.Extra): DomainResult<UserHoops> =
        if (extras.size >= HoopPolicy.MAX_EXTRA_HOOPS) DomainResult.failure(Violation.TooManyHoops)
        else DomainResult.success(UserHoops(home, extras + extra))

    fun remove(id: HoopId): DomainResult<UserHoops> = when {
        id == home.id -> DomainResult.failure(Violation.HomeHoopCannotBeRemoved)
        extras.none { it.id == id } -> DomainResult.failure(Violation.HoopNotFound)
        else -> DomainResult.success(UserHoops(home, extras.filterNot { it.id == id }))
    }

    fun resize(id: HoopId, radius: Radius): DomainResult<UserHoops> = when {
        id == home.id -> DomainResult.success(UserHoops(home.withRadius(radius), extras))
        extras.none { it.id == id } -> DomainResult.failure(Violation.HoopNotFound)
        else -> DomainResult.success(UserHoops(home, extras.map { if (it.id == id) it.withRadius(radius) else it }))
    }
}
