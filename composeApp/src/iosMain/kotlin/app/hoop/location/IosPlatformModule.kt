package app.hoop.location

import app.hoop.data.local.IosSqlDriverFactory
import app.hoop.data.local.SqlDriverFactory
import app.hoop.data.media.MediaFileReader
import app.hoop.data.session.SessionTokenStore
import app.hoop.domain.AddressResolver
import app.hoop.domain.LocationProvider
import app.hoop.media.IosMediaFileReader
import app.hoop.session.IosSessionTokenStore
import org.koin.dsl.module

/** iOS device services, wired into Koin by the view controller. */
val iosPlatformModule = module {
    single<SessionTokenStore> { IosSessionTokenStore() }
    single<SqlDriverFactory> { IosSqlDriverFactory() }
    single<MediaFileReader> { IosMediaFileReader() }
    single<AddressResolver> { IosAddressResolver() }
    single<LocationProvider> { IosLocationProvider() }
}
