package app.hoop.data

import co.touchlab.kermit.Logger

/**
 * Loggers of the data layer, one tag per area so they can be filtered
 * (Android: `adb logcat -s HoopHttp HoopAuth`). Never log passwords, tokens or message bodies.
 */
internal object HoopLog {
    val http = Logger.withTag("HoopHttp")
    val auth = Logger.withTag("HoopAuth")
}
