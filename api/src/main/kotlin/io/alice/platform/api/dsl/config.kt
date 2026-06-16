package io.alice.platform.api.dsl

import io.alice.platform.api.config.ConfigurationProvider
import kotlin.reflect.KClass

fun <T : Any> ConfigurationProvider.get(path: String, clazz: KClass<T>) =
    get(path, clazz.java)

inline fun <reified T: Any> ConfigurationProvider.get(path: String) =
    get(path, T::class)