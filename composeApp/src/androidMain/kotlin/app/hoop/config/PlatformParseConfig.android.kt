package app.hoop.config

import app.hoop.BuildConfig
import app.hoop.data.config.ParseConfig

internal actual fun platformParseConfig(): ParseConfig = ParseConfig(
    baseUrl = BuildConfig.PARSE_BASE_URL,
    applicationId = BuildConfig.PARSE_APPLICATION_ID,
    clientKey = BuildConfig.PARSE_CLIENT_KEY,
)
