package app.hoop.config

import app.hoop.BuildConfig

internal actual fun platformMapConfig(): MapConfig = MapConfig(
    styleUri = BuildConfig.MAP_STYLE_URI,
    fallbackLatitude = BuildConfig.MAP_FALLBACK_LATITUDE,
    fallbackLongitude = BuildConfig.MAP_FALLBACK_LONGITUDE,
)
