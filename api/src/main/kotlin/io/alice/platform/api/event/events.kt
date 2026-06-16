package io.alice.platform.api.event

import io.alice.platform.api.Alice
import io.alice.platform.api.AliceObject
import io.alice.platform.api.engine.Engine
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ServerStartEvent(
    alice: Alice,
    @OptIn(ExperimentalTime::class) override val timestamp: Instant
) : Event, AliceObject by alice {
    override val source: Alice
        get() = alice
}

class ServerShutdownEvent(
    alice: Alice,
    @OptIn(ExperimentalTime::class) override val timestamp: Instant
) : Event, AliceObject by alice {
    override val source: Alice
        get() = alice
}

class EngineStartEvent(
    alice: Alice,
    override val source: Engine,
    @OptIn(ExperimentalTime::class) override val timestamp: Instant
) : Event, AliceObject by alice

class EngineStoppedEvent(
    alice: Alice,
    override val source: Engine,
    @OptIn(ExperimentalTime::class) override val timestamp: Instant
) : Event, AliceObject by alice

open class EngineActionEvent<E>(
    alice: Alice,
    override val source: Engine,
    @OptIn(ExperimentalTime::class) override val timestamp: Instant,
    val action: E
) : Event, AliceObject by alice

class ModuleLoadedEvent(
    alice: Alice,
    override val source: Module,
    @OptIn(ExperimentalTime::class) override val timestamp: Instant
) : Event, AliceObject by alice

class ModuleUnloadEvent(
    alice: Alice,
    override val source: Module,
    @OptIn(ExperimentalTime::class) override val timestamp: Instant
) : Event, AliceObject by alice
