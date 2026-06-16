package io.alice.platform.api.engine.data

import io.alice.platform.api.engine.Engine
import io.alice.platform.api.objects.Provider

interface DataStoreEngine : Engine {
    fun <T, ID> of(entity: Class<T>): EntityRepository<T, ID>
}

interface EntityRepository<T, ID> {
    fun findById(id: ID): Provider<T>
}