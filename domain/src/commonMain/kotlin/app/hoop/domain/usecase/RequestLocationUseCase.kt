package app.hoop.domain.usecase

import app.hoop.domain.LocationProvider
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint

class RequestLocationUseCase(private val locationProvider: LocationProvider) {
    suspend operator fun invoke(): DomainResult<GeoPoint> = locationProvider.requestCurrentLocation()
}
