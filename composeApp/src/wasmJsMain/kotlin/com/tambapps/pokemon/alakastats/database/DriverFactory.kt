package com.tambapps.pokemon.alakastats.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        // Create worker with static URL for WASM compatibility
        val worker = createSqlWorker()
        
        return WebWorkerDriver(worker).also { driver ->
            AlakastatsDatabase.Schema.create(driver)
        }
    }
}

// External function to create worker - defined in JS
external fun createSqlWorker(): Worker