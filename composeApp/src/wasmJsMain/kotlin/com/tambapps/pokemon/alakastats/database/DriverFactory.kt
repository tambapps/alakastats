package com.tambapps.pokemon.alakastats.database

import app.cash.sqldelight.db.SqlDriver

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        // TODO: Implement proper WASM SQLite driver
        // For now, throw an exception as WASM SQLite support needs additional configuration
        throw NotImplementedError("WASM SQLite driver not yet implemented. Use Android or iOS for database functionality.")
    }
}