package app.hoop.config

import app.hoop.data.config.ParseConfig

/** Server configuration read from the platform (BuildConfig on Android, Info.plist on iOS). */
internal expect fun platformParseConfig(): ParseConfig
