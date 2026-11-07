package app.hoop.config

import app.hoop.data.config.ParseConfig
import platform.Foundation.NSBundle

// Values read from Info.plist (keys defined in the Xcode project; secrets are not versioned).
private const val KEY_BASE_URL = "HoopParseBaseUrl"
private const val KEY_APPLICATION_ID = "HoopParseApplicationId"
private const val KEY_CLIENT_KEY = "HoopParseClientKey"

private fun plistString(key: String): String = NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: ""

internal actual fun platformParseConfig(): ParseConfig = ParseConfig(
    baseUrl = plistString(KEY_BASE_URL),
    applicationId = plistString(KEY_APPLICATION_ID),
    clientKey = plistString(KEY_CLIENT_KEY),
)
