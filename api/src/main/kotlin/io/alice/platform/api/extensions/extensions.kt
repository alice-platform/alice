package io.alice.platform.api.extensions

import io.alice.platform.api.AliceObject
import io.alice.platform.api.objects.NamedObjectCollection
import io.alice.platform.api.objects.Provider

interface ExtensionsProvider : NamedObjectCollection<Any>, AliceObject {
    fun <T : Any> create(name: String, type: Class<T>, vararg values: Any): Provider<T>
    fun <T : Any> register(name: String, value: T)
}
