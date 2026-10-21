package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import io.github.xxfast.kstore.Codec
import io.github.xxfast.kstore.DefaultJson
import kotlinx.browser.localStorage
import kotlinx.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlin.js.Promise

/**
 * kstore codec saving each value as a JSON string in IndexedDB, whose quota is way bigger than the localStorage's
 * (a few MB shared by the whole origin)
 */
class IndexedDbCodec<T : @Serializable Any>(
    private val key: String,
    private val serializer: KSerializer<T>,
    private val format: StringFormat = DefaultJson,
) : Codec<T> {

    override suspend fun encode(value: T?) {
        if (value != null) IndexedDb.put(key, format.encodeToString(serializer, value))
        else IndexedDb.delete(key)
    }

    override suspend fun decode(): T? = IndexedDb.get(key)?.let { format.decodeFromString(serializer, it) }
}

/**
 * Key/value access to a single IndexedDB object store.
 * Only strings cross the Kotlin/JS boundary, and each operation runs its whole transaction in one JS promise:
 * a transaction commits by itself as soon as the event loop gets back control, so it can't be kept across a suspension.
 */
object IndexedDb {

    private const val LOCAL_STORAGE_PREFIX = "repositories/"

    private val mutex = Mutex()
    private var database: JsAny? = null

    suspend fun get(key: String): String? {
        val result = idbGet(database(), key).await<JsAny>()
        errorOf(result)?.let { throw IndexedDbException("Couldn't read $key: $it") }
        return valueOf(result)
    }

    suspend fun put(key: String, value: String) {
        idbPut(database(), key, value).await<JsString?>()?.let { throw IndexedDbException("Couldn't save $key: $it") }
    }

    suspend fun delete(key: String) {
        idbDelete(database(), key).await<JsString?>()?.let { throw IndexedDbException("Couldn't delete $key: $it") }
    }

    private suspend fun database(): JsAny = mutex.withLock {
        database ?: openDatabase().also {
            migrateFromLocalStorage(it)
            database = it
        }
    }

    // runs by itself at the first access, exposed for the tests
    internal suspend fun migrateFromLocalStorage() = migrateFromLocalStorage(database())

    private suspend fun openDatabase(): JsAny {
        val result = idbOpen().await<JsAny>()
        errorOf(result)?.let { throw IndexedDbException("Couldn't open the database: $it") }
        return databaseOf(result)
    }

    /**
     * The teams used to be stored in the localStorage, with the same keys. A key is removed from the localStorage
     * only once saved in IndexedDB: if interrupted, the migration goes on at the next launch
     */
    private suspend fun migrateFromLocalStorage(database: JsAny) {
        val keys = (0 until localStorage.length)
            .mapNotNull { localStorage.key(it) }
            .filter { it.startsWith(LOCAL_STORAGE_PREFIX) }
        for (key in keys) {
            val value = localStorage.getItem(key) ?: continue
            val error = idbPut(database, key, value).await<JsString?>()
            if (error != null) {
                println("Couldn't migrate $key to IndexedDB: $error")
                continue
            }
            localStorage.removeItem(key)
        }
    }
}

class IndexedDbException(message: String) : Exception(message)

// The promises below never reject: errors are given as strings, readable on the Kotlin side

private fun idbOpen(): Promise<JsAny> = js("""
    new Promise((resolve) => {
        const request = indexedDB.open('alakastats', 1);
        request.onupgradeneeded = () => request.result.createObjectStore('kstore');
        request.onsuccess = () => resolve({ database: request.result });
        request.onerror = () => resolve({ error: String(request.error) });
        request.onblocked = () => resolve({ error: 'the database is used by another tab with an older version of the app' });
    })
""")

private fun idbGet(database: JsAny, key: String): Promise<JsAny> = js("""
    new Promise((resolve) => {
        try {
            const request = database.transaction('kstore', 'readonly').objectStore('kstore').get(key);
            request.onsuccess = () => resolve({ value: request.result === undefined ? null : request.result });
            request.onerror = () => resolve({ error: String(request.error) });
        } catch (e) {
            resolve({ error: String(e) });
        }
    })
""")

private fun idbPut(database: JsAny, key: String, value: String): Promise<JsString?> = js("""
    new Promise((resolve) => {
        try {
            const transaction = database.transaction('kstore', 'readwrite');
            transaction.objectStore('kstore').put(value, key);
            // complete, not the request's success: the data is only saved once the transaction is
            transaction.oncomplete = () => resolve(null);
            transaction.onerror = () => resolve(String(transaction.error));
            transaction.onabort = () => resolve(String(transaction.error || 'aborted'));
        } catch (e) {
            resolve(String(e));
        }
    })
""")

private fun idbDelete(database: JsAny, key: String): Promise<JsString?> = js("""
    new Promise((resolve) => {
        try {
            const transaction = database.transaction('kstore', 'readwrite');
            transaction.objectStore('kstore').delete(key);
            transaction.oncomplete = () => resolve(null);
            transaction.onerror = () => resolve(String(transaction.error));
            transaction.onabort = () => resolve(String(transaction.error || 'aborted'));
        } catch (e) {
            resolve(String(e));
        }
    })
""")

private fun errorOf(result: JsAny): String? = js("result.error || null")

private fun valueOf(result: JsAny): String? = js("result.value")

private fun databaseOf(result: JsAny): JsAny = js("result.database")
