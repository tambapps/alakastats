package com.tambapps.pokemon.alakastats.database

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        // Use SQLDelight's official web-worker-driver with SQL.js
        val worker = Worker(
            js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
        )
        
        return WebWorkerDriver(worker).also { driver ->
            AlakastatsDatabase.Schema.awaitCreate(driver)
        }
    }
}