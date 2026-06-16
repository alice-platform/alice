package io.alice.platform.api

import io.alice.platform.api.config.ConfigurationProvider
import io.alice.platform.api.engine.EngineProvider
import io.alice.platform.api.event.EventManager
import io.alice.platform.api.extensions.ExtensionsProvider
import io.alice.platform.api.modules.ModuleProvider
import io.alice.platform.api.objects.ObjectFactory
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Root component and entry point for the Alice platform.
 *
 * The Alice interface serves as the central hub for accessing all platform services
 * and configuring the chatbot environment. It follows a Gradle-like DSL pattern,
 * allowing declarative configuration through lambda-based builder functions.
 *
 * ## Platform Architecture
 *
 * Alice coordinates these main subsystems:
 * - **Configuration**: Application settings and environment properties
 * - **Engines**: Chat processing engines for different platforms
 * - **Modules**: Modular extensions and features
 * - **Objects**: Factory for creating and managing platform objects
 * - **Events**: Event dispatching and handling system
 * - **Extensions**: Custom extensions and plugins
 *
 * ## Lifecycle
 *
 * The platform goes through three main states:
 * 1. **INIT** - Configuration and initialization
 * 2. **START** - Running and processing
 * 3. **STOP** - Shutdown and cleanup
 *
 * Register callbacks for state transitions using [before] and [after] methods.
 *
 * ## Usage Examples
 *
 * ### Basic Setup
 * ```kotlin
 * val alice: Alice = // obtained from platform
 *
 * // Access core services
 * val config = alice.configuration
 * val engines = alice.engines
 * val modules = alice.modules
 * ```
 *
 * ### DSL Configuration
 * ```kotlin
 * alice.configuration { provider ->
 *     provider.apply("config.yaml")
 * }
 *
 * alice.engines { provider ->
 *     provider.install(MyEngineFactory, {
 *         // configure engine
 *     })
 * }
 *
 * alice.modules { provider ->
 *     provider.id("database-module") version "1.0.0" apply true
 * }
 * ```
 *
 * ### Lifecycle Hooks
 * ```kotlin
 * alice.before(AliceState.START) { app ->
 *     println("Starting up...")
 * }
 *
 * alice.after(AliceState.STOP) { app ->
 *     println("Shut down complete")
 * }
 * ```
 *
 * ## Thread Safety
 *
 * The Alice instance is typically not thread-safe during initialization.
 * Ensure configuration is completed before using from multiple threads.
 *
 * @property version the current platform version
 * @property configuration configuration management
 * @property engines chat engine provider
 * @property modules module management
 * @property objects object factory
 * @property events event management system
 * @property extensions extension provider
 * @see AliceObjectOperator for lifecycle support
 * @see AliceState for state information
 */
interface Alice : AliceObjectOperator {

    /**
     * The current version of the Alice platform.
     *
     * This is useful for version-specific initialization and compatibility checks.
     *
     * @return the platform version
     */
    val version: Version

    /**
     * Configuration provider for managing bot environment settings.
     *
     * Provides configuration from defined paths and loads default configuration
     * solutions provided by the platform. Supports multiple file formats:
     * - YAML/YML files
     * - TOML files
     * - JSON files
     * - Properties files
     * - Configuration files (.conf)
     *
     * @return the configuration provider
     * @see ConfigurationProvider.apply
     *
     * @sample
     * ```kotlin
     * val dbConfig = alice.configuration
     *     .getProperty("database.url")
     *     .getOrNull()
     * ```
     */
    val configuration: ConfigurationProvider

    /**
     * Engine provider for chat processing engines.
     *
     * The engine provider is the core of the platform - without at least one
     * installed engine, the platform cannot process messages.
     *
     * The provider handles:
     * - Engine registration and lifecycle
     * - Module loading for each engine
     * - Extension application
     * - Multi-engine coordination
     *
     * @return the engine provider
     *
     * @sample
     * ```kotlin
     * alice.engines { engines ->
     *     engines.install(DiscordEngineFactory) {
     *         token = "..."
     *     }
     * }
     * ```
     */
    val engines: EngineProvider

    /**
     * Module provider for managing platform modules and extensions.
     *
     * Modules provide additional functionality and services to the platform.
     * They can be dynamically registered and removed during runtime.
     *
     * @return the module provider
     *
     * @sample
     * ```kotlin
     * alice.modules { modules ->
     *     modules.id("auth-module") version "1.0.0" apply true
     * }
     * ```
     */
    val modules: ModuleProvider

    /**
     * Object factory for creating and managing platform objects.
     *
     * Provides object creation and management services including:
     * - Type-safe object creation
     * - Provider and Property creation
     * - Collection management
     * - Script integration
     *
     * @return the object factory
     */
    val objects: ObjectFactory

    /**
     * Event manager for handling platform events.
     *
     * Manages event registration, dispatching, and subscription.
     * Supports both annotated event handlers and direct dispatcher registration.
     *
     * @return the event manager
     *
     * @see EventManager for event handling
     * @sample
     * ```kotlin
     * alice.events { manager ->
     *     manager.onEvent(MessageEvent::class.java) { event ->
     *         println("Message: ${event.text}")
     *     }
     * }
     * ```
     */
    val events: EventManager

    /**
     * Extension provider for registering custom extensions and plugins.
     *
     * Extensions allow adding custom functionality to the platform without
     * modifying core code.
     *
     * @return the extension provider
     */
    val extensions: ExtensionsProvider

    /**
     * DSL function for configuring the configuration provider.
     *
     * This is a convenience method for the DSL pattern.
     *
     * @param configuration consumer that receives the configuration provider
     *
     * @sample
     * ```kotlin
     * @AliceDsl
     * alice.configuration { config ->
     *     config.apply("application.yaml")
     * }
     * ```
     */
    @AliceDsl
    fun configuration(configuration: Consumer<ConfigurationProvider>) {
        this.configuration.also { configuration.consume(it) }
    }

    /**
     * DSL function for configuring the engine provider.
     *
     * This is a convenience method for the DSL pattern.
     *
     * @param engines consumer that receives the engine provider
     *
     * @sample
     * ```kotlin
     * @AliceDsl
     * alice.engines { engines ->
     *     engines.install(MyEngineFactory) {
     *         // configuration
     *     }
     * }
     * ```
     */
    @AliceDsl
    fun engines(engines: Consumer<EngineProvider>) {
        this.engines.also { engines.consume(it) }
    }

    /**
     * DSL function for configuring the module provider.
     *
     * This is a convenience method for the DSL pattern.
     *
     * @param modules consumer that receives the module provider
     *
     * @sample
     * ```kotlin
     * @AliceDsl
     * alice.modules { modules ->
     *     modules.id("my-module") version "1.0" apply true
     * }
     * ```
     */
    @AliceDsl
    fun modules(modules: Consumer<ModuleProvider>) {
        this.modules.also { modules.consume(it) }
    }

    /**
     * DSL function for configuring the object factory.
     *
     * This is a convenience method for the DSL pattern.
     *
     * @param objects consumer that receives the object factory
     *
     * @sample
     * ```kotlin
     * @AliceDsl
     * alice.objects { factory ->
     *     val provider = factory.provider.of("value")
     * }
     * ```
     */
    @AliceDsl
    fun objects(objects: Consumer<ObjectFactory>) {
        this.objects.also { objects.consume(it) }
    }

    /**
     * DSL function for configuring the event manager.
     *
     * This is a convenience method for the DSL pattern.
     *
     * @param events consumer that receives the event manager
     *
     * @sample
     * ```kotlin
     * @AliceDsl
     * alice.events { manager ->
     *     manager.onEvent(MyEvent::class.java) { event ->
     *         // handle event
     *     }
     * }
     * ```
     */
    @AliceDsl
    fun events(events: Consumer<EventManager>) {
        this.events.also { events.consume(it) }
    }

    /**
     * DSL function for configuring the extension provider.
     *
     * This is a convenience method for the DSL pattern.
     *
     * @param extensions consumer that receives the extension provider
     */
    @AliceDsl
    fun extensions(extensions: Consumer<ExtensionsProvider>) {
        this.extensions.also { extensions.consume(it) }
    }

    /**
     * Registers a callback to be executed before entering a specific state.
     *
     * Multiple callbacks can be registered for the same state.
     * They are executed in the order of registration.
     *
     * Useful for initialization and setup operations that must occur
     * before a state transition.
     *
     * @param state the state to listen for
     * @param alice consumer receiving this Alice instance
     *
     * @sample
     * ```kotlin
     * alice.before(AliceState.START) { app ->
     *     println("Platform is starting up")
     *     app.configuration.apply("startup.yaml")
     * }
     * ```
     * @see AliceState
     */
    fun before(state: AliceState, alice: Consumer<Alice>)

    /**
     * Registers a callback to be executed after leaving a specific state.
     *
     * Multiple callbacks can be registered for the same state.
     * They are executed in the order of registration.
     *
     * Useful for cleanup and post-processing operations that must occur
     * after a state transition.
     *
     * @param state the state to listen for
     * @param alice consumer receiving this Alice instance
     *
     * @sample
     * ```kotlin
     * alice.after(AliceState.STOP) { app ->
     *     println("Platform has shut down")
     *     // final cleanup
     * }
     * ```
     * @see AliceState
     */
    fun after(state: AliceState, alice: Consumer<Alice>)
}

/**
 * Enumeration of runtime states in the Alice platform lifecycle.
 *
 * The platform transitions through these states during its lifetime.
 * State callbacks can be registered to execute before entering or after leaving each state.
 *
 * ## State Transitions
 *
 * Typical flow:
 * 1. INIT → START → STOP
 *
 * State machine ensures proper initialization and cleanup sequence.
 *
 * @see Alice.before
 * @see Alice.after
 */
enum class AliceState {
    /**
     * Initialization state.
     *
     * During this state, the platform initializes configuration, loads modules,
     * and registers engines. This is the ideal time to:
     * - Apply configuration files
     * - Register modules
     * - Install engines
     * - Setup event handlers
     *
     * Use [Alice.before] hook with this state to ensure systems are ready
     * before moving to START.
     */
    INIT,

    /**
     * Running state.
     *
     * The platform is fully initialized and processing messages.
     * All engines are running and event dispatching is active.
     *
     * Use [Alice.before] hook with this state to perform final setup,
     * or [Alice.after] hook to detect when startup is complete.
     */
    START,

    /**
     * Shutdown state.
     *
     * The platform is stopping gracefully. All engines are being shut down,
     * pending operations are being cleaned up.
     *
     * Use [Alice.before] hook to perform final operations before shutdown,
     * or [Alice.after] hook to ensure cleanup is complete.
     */
    STOP
}

/**
 * Lazy initialization property delegate for avoiding null checks.
 *
 * This delegate allows declaring properties that are initialized later,
 * throwing an exception if accessed before being set. This is useful for
 * lazy initialization patterns where you want type safety without nullability.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * class MyComponent {
 *     private var resource: String by LateInit({
 *         IllegalStateException("Resource not initialized")
 *     })
 *
 *     fun initialize(value: String) {
 *         resource = value
 *     }
 *
 *     fun use() {
 *         println(resource) // Safe access after initialization
 *     }
 * }
 * ```
 *
 * ## Error Handling
 *
 * When accessed before initialization, the delegate throws the exception
 * provided by the throwable supplier.
 *
 * @param T the type of the property
 * @param throwable a supplier for the exception to throw if accessed before initialization
 * @see ReadWriteProperty for Kotlin property delegation
 */
class LateInit<T>(
    private val throwable: () -> Throwable
) : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    /**
     * Reads the property value.
     *
     * @param thisRef the object instance (unused)
     * @param property the property being accessed
     * @return the current value
     * @throws Throwable if the property has not been initialized
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        value ?: throw throwable()

    /**
     * Sets the property value.
     *
     * @param thisRef the object instance (unused)
     * @param property the property being set
     * @param value the value to set
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}
