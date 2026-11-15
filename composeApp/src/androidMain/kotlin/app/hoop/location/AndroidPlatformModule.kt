package app.hoop.location

import android.content.Context
import app.hoop.data.session.SessionTokenStore
import app.hoop.domain.LocationProvider
import app.hoop.session.AndroidSessionTokenStore
import org.koin.dsl.module

/** Android device services, wired into Koin by the Application. */
fun androidPlatformModule(applicationContext: Context) = module {
    single { ActivityPermissionRequester() }
    single<SessionTokenStore> { AndroidSessionTokenStore(applicationContext) }
    single<LocationProvider> { AndroidLocationProvider(applicationContext, get()) }
}
