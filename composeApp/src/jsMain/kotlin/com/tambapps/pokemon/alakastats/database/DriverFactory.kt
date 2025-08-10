package com.tambapps.pokemon.alakastats.database

import app.cash.sqldelight.db.SqlDriver

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        return WebSqliteDriver().also { driver ->
            AlakastatsDatabase.Schema.create(driver)
        }
    }
}