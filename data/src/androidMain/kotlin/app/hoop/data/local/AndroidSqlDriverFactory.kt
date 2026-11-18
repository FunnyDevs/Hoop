package app.hoop.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/** The database lives in the private storage of the app. */
class AndroidSqlDriverFactory(private val context: Context) : SqlDriverFactory {
    override fun create(): SqlDriver = AndroidSqliteDriver(HoopDatabase.Schema, context, DATABASE_FILE)
}

private const val DATABASE_FILE = "hoop.db"
