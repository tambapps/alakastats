package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import io.github.xxfast.kstore.storeOf
import kotlinx.browser.localStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class IndexedDbTest {

    @Serializable
    data class Item(val name: String, val values: List<Int>)

    private fun key() = "test/${Uuid.random()}.kstore"

    @Test
    fun putGetDelete() = runTest {
        val key = key()
        assertNull(IndexedDb.get(key))

        IndexedDb.put(key, "hello")
        assertEquals("hello", IndexedDb.get(key))

        IndexedDb.put(key, "updated")
        assertEquals("updated", IndexedDb.get(key))

        IndexedDb.delete(key)
        assertNull(IndexedDb.get(key))
    }

    @Test
    fun storesMoreThanTheLocalStorageQuota() = runTest {
        val key = key()
        // ~10 MB: the localStorage can't hold it
        val big = "x".repeat(10 * 1024 * 1024)

        IndexedDb.put(key, big)

        assertEquals(big.length, IndexedDb.get(key)?.length)
        IndexedDb.delete(key)
    }

    @Test
    fun kstoreWithTheCodec() = runTest {
        val store = storeOf(codec = IndexedDbCodec(key(), serializer<Item>()), enableCache = false)

        store.set(Item("Rain", listOf(1, 2)))
        assertEquals(Item("Rain", listOf(1, 2)), store.get())

        store.update { it?.copy(values = it.values + 3) }
        assertEquals(listOf(1, 2, 3), store.get()?.values)

        store.delete()
        assertNull(store.get())
    }

    @Test
    fun migratesTheLocalStorageKeys() = runTest {
        val migrated = "repositories/test-migration/${Uuid.random()}.kstore"
        val other = "other-app-key-${Uuid.random()}"
        localStorage.setItem(migrated, """{"name":"Rain","values":[1]}""")
        localStorage.setItem(other, "kept")

        IndexedDb.migrateFromLocalStorage()

        // same key, same content: readable by the codec as is
        val store = storeOf(codec = IndexedDbCodec(migrated, serializer<Item>()), enableCache = false)
        assertEquals(Item("Rain", listOf(1)), store.get())
        assertNull(localStorage.getItem(migrated))
        // keys of other apps on the same origin are left alone
        assertEquals("kept", localStorage.getItem(other))

        localStorage.removeItem(other)
        IndexedDb.delete(migrated)
    }
}
