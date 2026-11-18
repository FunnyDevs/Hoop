package app.hoop.data.local

import app.cash.sqldelight.db.SqlDriver

/** Opens the local database file; each platform provides its own (Android SQLite, iOS native SQLite). */
fun interface SqlDriverFactory {
    fun create(): SqlDriver
}
