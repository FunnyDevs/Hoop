package app.hoop.location

import android.content.Context
import app.hoop.data.local.AndroidSqlDriverFactory
import app.hoop.data.local.SqlDriverFactory
import app.hoop.data.media.MediaFileReader
import app.hoop.data.session.SessionTokenStore
import app.hoop.domain.AddressResolver
import app.hoop.domain.LocationProvider
import app.hoop.media.AndroidMediaFileReader
import app.hoop.session.AndroidSessionTokenStore
import org.koin.dsl.module

/** Android device services, wired into Koin by the Application. */
fun androidPlatformModule(applicationContext: Context) = module {
    single { ActivityPermissionRequester() }
    single<SessionTokenStore> { AndroidSessionTokenStore(applicationContext) }
    single<SqlDriverFactory> { AndroidSqlDriverFactory(applicationContext) }
    single<MediaFileReader> { AndroidMediaFileReader() }
    single<AddressResolver> { AndroidAddressResolver(applicationContext) }
    single<LocationProvider> { AndroidLocationProvider(applicationContext, get()) }
}
