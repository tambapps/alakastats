package com.tambapps.pokemon.alakastats.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AlakastatsDatabase.Schema,
            name = "alakastats.db"
        )
    }
}
EOF < /dev/null