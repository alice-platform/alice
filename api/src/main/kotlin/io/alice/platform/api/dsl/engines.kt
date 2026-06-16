package io.alice.platform.api.dsl

import io.alice.platform.api.Alice
import io.alice.platform.api.engine.data.DataStoreEngine
import io.alice.platform.api.engine.web.WebEngineServer
import io.alice.platform.api.objects.Provider

val Alice.datastore: Provider<DataStoreEngine>
    get() = engines.named("datastore", DataStoreEngine::class.java)

val Alice.web: Provider<WebEngineServer>
    get() = engines.named("web", WebEngineServer::class.java)
