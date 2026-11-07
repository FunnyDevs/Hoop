package app.hoop.config

import platform.Foundation.NSBundle

// Values read from Info.plist (keys defined in the Xcode project).
private const val KEY_STYLE_URI = "HoopMapStyleUri"
private const val KEY_FALLBACK_LATITUDE = "HoopMapFallbackLatitude"
private const val KEY_FALLBACK_LONGITUDE = "HoopMapFallbackLongitude"

private fun plistDouble(key: String): Double = (NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String)?.toDoubleOrNull() ?: 0.0

internal actual fun platformMapConfig(): MapConfig = MapConfig(
    styleUri = NSBundle.mainBundle.objectForInfoDictionaryKey(KEY_STYLE_URI) as? String ?: "",
    fallbackLatitude = plistDouble(KEY_FALLBACK_LATITUDE),
    fallbackLongitude = plistDouble(KEY_FALLBACK_LONGITUDE),
)
