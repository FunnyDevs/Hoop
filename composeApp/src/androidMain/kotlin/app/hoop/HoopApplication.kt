package app.hoop

import android.app.Application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import app.hoop.di.initKoin
import app.hoop.location.androidPlatformModule

class HoopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug builds log everything; release builds only warnings and errors.
        Logger.setMinSeverity(if (BuildConfig.DEBUG) Severity.Verbose else Severity.Warn)
        initKoin(listOf(androidPlatformModule(applicationContext)))
    }
}
