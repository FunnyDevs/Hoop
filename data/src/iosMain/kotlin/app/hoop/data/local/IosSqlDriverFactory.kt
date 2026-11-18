package app.hoop.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/** Written but never compiled (no Xcode here). */
class IosSqlDriverFactory : SqlDriverFactory {
    override fun create(): SqlDriver = NativeSqliteDriver(HoopDatabase.Schema, DATABASE_FILE)
}

private const val DATABASE_FILE = "hoop.db"
