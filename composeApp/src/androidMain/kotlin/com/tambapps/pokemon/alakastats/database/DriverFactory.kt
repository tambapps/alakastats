package com.tambapps.pokemon.alakastats.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver


actual class DriverFactory(private val context: Context) {
    actual suspend fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(schema = AlakastatsDatabase.Schema, context = context, name = "alakastats.db")
    }

}