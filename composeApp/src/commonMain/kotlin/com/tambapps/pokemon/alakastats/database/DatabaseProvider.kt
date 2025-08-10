package com.tambapps.pokemon.alakastats.database

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DatabaseProvider(private val driverFactory: DriverFactory) {
    private var database: AlakastatsDatabase? = null
    private val mutex = Mutex()
    
    suspend fun getDatabase(): AlakastatsDatabase {
        return mutex.withLock {
            database ?: run {
                val driver = driverFactory.createDriver()
                val newDatabase = AlakastatsDatabase(driver)
                database = newDatabase
                newDatabase
            }
        }
    }
}