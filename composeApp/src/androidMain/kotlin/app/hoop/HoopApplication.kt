package app.hoop

import android.app.Application
import app.hoop.di.initKoin
import app.hoop.location.androidPlatformModule

class HoopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(listOf(androidPlatformModule(applicationContext)))
    }
}
