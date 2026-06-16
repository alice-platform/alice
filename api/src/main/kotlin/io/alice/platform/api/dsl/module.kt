package io.alice.platform.api.dsl

import io.alice.platform.api.modules.Module
import io.alice.platform.api.modules.ModuleProvider
import kotlin.reflect.KClass

fun ModuleProvider.apply(module: KClass<out Module>) =
    apply(module.java)

inline fun <reified T : Module> ModuleProvider.apply() = apply(T::class)