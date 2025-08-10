package com.tambapps.pokemon.alakastats.database

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    suspend fun createDriver(): SqlDriver
}

suspend fun createDatabase(driverFactory: DriverFactory): AlakastatsDatabase {
    val driver = driverFactory.createDriver()
    return AlakastatsDatabase(driver)
}