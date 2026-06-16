package io.alice.platform.api.engine

import io.alice.platform.api.Alice
import io.alice.platform.api.AliceObjectOperator
import io.alice.platform.api.AliceRuntimeException
import io.alice.platform.api.event.EventManager
import io.alice.platform.api.objects.NamedObjectCollection

/**
 * Chat Engine Provider
 */
interface EngineProvider : NamedObjectCollection<Engine>, AliceObjectOperator {
    /**
     * Initialize chat engine before starting them
     */
    fun <E : Engine, F : Engine.Factory<C, E>, C> install(factory: F, config: C.() -> Unit = {})
}

/**
 * The Chat Engine
 */
interface Engine : AliceObjectOperator {
    var active: Boolean
    val events: EventManager
        get() = alice.events

    interface Factory<TConfig, TEngine : Engine> {
        val name: String
        fun init(alice: Alice, config: TConfig.() -> Unit): TEngine
    }
}

open class EngineException : AliceRuntimeException {
    constructor() : super()
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
