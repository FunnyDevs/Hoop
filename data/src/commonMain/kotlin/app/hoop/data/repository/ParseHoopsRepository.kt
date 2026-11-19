package app.hoop.data.repository

import app.hoop.data.HoopLog
import app.hoop.data.remote.ParseHomeApi
import app.hoop.data.remote.dto.CreateHoopBody
import app.hoop.data.remote.dto.DeleteHoopsBody
import app.hoop.data.remote.dto.HoopDto
import app.hoop.data.remote.dto.UpdateHoopBody
import app.hoop.data.remote.toDomainError
import app.hoop.domain.HoopsRepository
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import app.hoop.domain.common.flatMap
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.hoop.UserHoops
import kotlin.math.roundToInt

/** The Hoops of the user on the Parse server. Every change answers with the Hoops as the server now has them. */
internal class ParseHoopsRepository(private val api: ParseHomeApi) : HoopsRepository {

    override suspend fun getUserHoops(): DomainResult<UserHoops> = call("getUserHoops") {
        api.getUserHoops().toAggregate()
    }

    override suspend fun addHoop(name: String, center: GeoPoint, radius: Radius): DomainResult<UserHoops> = call("addHoop") {
        api.createHoop(CreateHoopBody(center.latitude, center.longitude, radius.meters, name))
        api.getUserHoops().toAggregate()
    }

    override suspend fun removeHoop(id: HoopId): DomainResult<UserHoops> = call("removeHoop") {
        api.deleteHoops(DeleteHoopsBody(listOf(id.value)))
        api.getUserHoops().toAggregate()
    }

    override suspend fun resizeHoop(id: HoopId, radius: Radius): DomainResult<UserHoops> = call("resizeHoop") {
        api.updateHoop(UpdateHoopBody(id.value, radius.meters))
        api.getUserHoops().toAggregate()
    }

    private suspend fun call(name: String, block: suspend () -> UserHoops?): DomainResult<UserHoops> = try {
        block()?.let { DomainResult.success(it) } ?: DomainResult.failure(DomainError.Unknown)
    } catch (e: Throwable) {
        val error = e.toDomainError()
        HoopLog.home.w(e) { "$name failed -> $error" }
        DomainResult.failure(error)
    }

    /** `null` if the answer has no Home or holds a Hoop the domain would not accept. */
    private fun List<HoopDto>.toAggregate(): UserHoops? {
        val homeDto = firstOrNull { it.isHome } ?: return null
        val home = Hoop.Home(HoopId(homeDto.objectId), homeDto.radiusOrNull() ?: return null)
        val extras = filterNot { it.isHome }.mapNotNull { dto ->
            val center = dto.center?.let { (GeoPoint.create(it.latitude, it.longitude) as? DomainResult.Success)?.value }
            val radius = dto.radiusOrNull()
            if (center == null || radius == null) null else Hoop.Extra(HoopId(dto.objectId), dto.name.orEmpty(), center, radius)
        }
        return UserHoops(home, extras)
    }

    private fun HoopDto.radiusOrNull(): Radius? = (Radius.create(radius.roundToInt()) as? DomainResult.Success)?.value
}
