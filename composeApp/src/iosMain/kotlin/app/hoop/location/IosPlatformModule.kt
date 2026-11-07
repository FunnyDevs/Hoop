package app.hoop.location

import app.hoop.domain.LocationProvider
import org.koin.dsl.module

/** iOS device services, wired into Koin by the view controller. */
val iosPlatformModule = module {
    single<LocationProvider> { IosLocationProvider() }
}
