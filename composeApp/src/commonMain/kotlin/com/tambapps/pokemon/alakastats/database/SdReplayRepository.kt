package com.tambapps.pokemon.alakastats.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SdReplayRepository(private val databaseProvider: DatabaseProvider) {
    
    suspend fun insertReplay(
        url: String,
        uploadTime: Long,
        format: String,
        rating: Long? = null,
        parserVersion: String? = null,
        winner: String? = null,
        nextBattle: String? = null
    ) = withContext(Dispatchers.Default) {
        val database = databaseProvider.getDatabase()
        database.sdReplayQueries.insertReplay(url, uploadTime, format, rating, parserVersion, winner, nextBattle)
    }
    
    suspend fun getReplayByUrl(url: String) = withContext(Dispatchers.Default) {
        val database = databaseProvider.getDatabase()
        database.sdReplayQueries.selectReplayByUrl(url).executeAsOneOrNull()
    }
    
    suspend fun getAllReplays() = withContext(Dispatchers.Default) {
        val database = databaseProvider.getDatabase()
        database.sdReplayQueries.selectAllReplays().executeAsList()
    }
    
    suspend fun deleteReplayByUrl(url: String) = withContext(Dispatchers.Default) {
        val database = databaseProvider.getDatabase()
        database.sdReplayQueries.deleteReplay(url)
    }
    
    suspend fun getReplaysByFormat(format: String) = withContext(Dispatchers.Default) {
        val database = databaseProvider.getDatabase()
        database.sdReplayQueries.selectReplaysByFormat(format).executeAsList()
    }
}