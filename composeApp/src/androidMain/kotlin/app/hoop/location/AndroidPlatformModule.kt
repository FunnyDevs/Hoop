package app.hoop.location

import android.content.Context
import app.hoop.domain.LocationProvider
import org.koin.dsl.module

/** Android device services, wired into Koin by the Application. */
fun androidPlatformModule(applicationContext: Context) = module {
    single { ActivityPermissionRequester() }
    single<LocationProvider> { AndroidLocationProvider(applicationContext, get()) }
}
