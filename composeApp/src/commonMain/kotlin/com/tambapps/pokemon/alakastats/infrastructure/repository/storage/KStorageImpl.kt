package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.alakastats.util.Identifiable
import io.github.xxfast.kstore.KStore
import kotlinx.serialization.Serializable

class KStorageImpl<ID: @Serializable Any, T: @Serializable Identifiable<ID>>(
    override val idsStore: KStore<List<ID>>,
    private val storeSupplier: (id: ID) -> KStore<T>
) : AbstractKStorage<ID, T>() {

    override fun getStore(id: ID) = storeSupplier.invoke(id)

}