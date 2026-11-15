package app.hoop.location

import app.hoop.data.session.SessionTokenStore
import app.hoop.domain.LocationProvider
import app.hoop.session.IosSessionTokenStore
import org.koin.dsl.module

/** iOS device services, wired into Koin by the view controller. */
val iosPlatformModule = module {
    single<SessionTokenStore> { IosSessionTokenStore() }
    single<LocationProvider> { IosLocationProvider() }
}
