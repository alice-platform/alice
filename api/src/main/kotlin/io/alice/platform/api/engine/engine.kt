package io.alice.platform.api.engine

import io.alice.platform.api.Alice
import io.alice.platform.api.AliceObjectOperator
import io.alice.platform.api.AliceRuntimeException
import io.alice.platform.api.event.EventManager
import io.alice.platform.api.objects.NamedObjectCollection

/**
 * Provider for installing and managing chat processing engines.
 *
 * EngineProvider is the central hub for engine lifecycle management,
 * following the Gradle plugin pattern. Engines handle message processing
 * across different platforms (Discord, Telegram, Slack, etc.).
 *
 * ## Engine Architecture
 *
 * The platform supports multiple engines running simultaneously,
 * allowing a single bot to connect to multiple services.
 * Each engine:
 * - Processes platform-specific message formats
 * - Translates between platform APIs and Alice models
 * - Manages platform-specific connections and authentication
 * - Emits events through the centralized EventManager
 *
 * ## Engine Lifecycle
 *
 * ```
 * INIT (configuration) → INSTALL → REGISTER → ACTIVE → STOP
 * ```
 *
 * 1. **Configuration** - Factory and config provided
 * 2. **Installation** - Engine instantiated via factory.init()
 * 3. **Registration** - Engine added to provider collection
 * 4. **Active** - Engine processes messages
 * 5. **Stop** - Engine gracefully shuts down
 *
 * ## Installation DSL
 *
 * The provider follows the Gradle plugin installation pattern:
 *
 * ```kotlin
 * alice.engines { provider ->
 *     // Install a Discord engine
 *     provider.install(DiscordEngineFactory) {
 *         token = "your-token-here"
 *         prefix = "!"
 *         intents = listOf("GUILD_MESSAGES", "DIRECT_MESSAGES")
 *     }
 *
 *     // Install multiple engines
 *     provider.install(TelegramEngineFactory) {
 *         botToken = "telegram-token"
 *         webhook = "https://your-domain.com/webhook"
 *     }
 *
 *     provider.install(SlackEngineFactory) {
 *         appToken = "slack-app-token"
 *         botToken = "slack-bot-token"
 *     }
 * }
 * ```
 *
 * ## Engine Discovery
 *
 * As a [NamedObjectCollection], the EngineProvider provides:
 * - `named(name)` - Get engine by name
 * - `mapped()` - Get all engines as a map
 * - `keys()` - Get all engine names
 * - `values()` - Get all engine instances
 * - `size` - Number of installed engines
 *
 * ```kotlin
 * val engines = alice.engines
 * val discordEngine = engines.named("discord").getOrNull()
 * val allEngines = engines.mapped()
 * ```
 *
 * ## Lifecycle Hooks
 *
 * ```kotlin
 * alice.before(AliceState.START) { app ->
 *     // Engines are installed but not yet active
 *     app.engines { provider ->
 *         provider.named("discord").ifPresent { engine ->
 *             engine.active = true // Enable the engine
 *         }
 *     }
 * }
 * ```
 *
 * @see Engine for individual engine interface
 * @see Engine.Factory for creating custom engines
 * @see NamedObjectCollection for engine collection operations
 */
interface EngineProvider : NamedObjectCollection<Engine>, AliceObjectOperator {

    /**
     * Installs a chat engine using the provided factory.
     *
     * The factory pattern allows engines to be installed without
     * knowing implementation details. The configuration is provided
     * as a lambda receiver, similar to Gradle plugin configuration.
     *
     * ## Type Parameters
     *
     * - **E** - The engine type (must implement Engine)
     * - **F** - The factory type (must implement Engine.Factory)
     * - **C** - The configuration type specific to this engine
     *
     * ## Configuration DSL
     *
     * The config parameter is a lambda with C as receiver:
     * ```kotlin
     * install(DiscordEngineFactory) {
     *     token = "..."      // 'this' is DiscordConfig
     *     prefix = "!"
     *     intents = listOf("...")
     * }
     * ```
     *
     * ## Factory Contract
     *
     * The factory must:
     * 1. Provide a unique [Engine.Factory.name]
     * 2. Implement [Engine.Factory.init] to create the engine instance
     * 3. Handle all configuration through the config lambda
     *
     * ## Example Factory
     *
     * ```kotlin
     * object MyEngineFactory : Engine.Factory<MyEngineConfig, MyEngine> {
     *     override val name = "my-engine"
     *
     *     override fun init(
     *         alice: Alice,
     *         config: MyEngineConfig.() -> Unit
     *     ): MyEngine {
     *         val cfg = MyEngineConfig().apply(config)
     *         return MyEngine(alice, cfg)
     *     }
     * }
     *
     * data class MyEngineConfig {
     *     var apiKey: String = ""
     *     var endpoint: String = "https://api.example.com"
     * }
     *
     * class MyEngine(
     *     override val alice: Alice,
     *     val config: MyEngineConfig
     *  ) : Engine {
     *     override var active = false
     *     // ... implementation
     * }
     * ```
     *
     * @param E the engine type
     * @param F the factory type
     * @param C the configuration type
     * @param factory the engine factory
     * @param config optional configuration lambda (defaults to no-op)
     *
     * @throws IllegalArgumentException if factory name is not unique
     * @throws Exception if factory.init() fails
     *
     * @sample
     * ```kotlin
     * alice.engines { provider ->
     *     provider.install(DiscordEngineFactory) {
     *         token = System.getenv("DISCORD_TOKEN")
     *         prefix = "!"
     *     }
     *
     *     provider.install(TelegramEngineFactory) {
     *         botToken = System.getenv("TELEGRAM_TOKEN")
     *     }
     * }
     * ```
     */
    fun <E : Engine, F : Engine.Factory<C, E>, C> install(
        factory: F,
        config: C.() -> Unit = {}
    )
}

/**
 * A chat processing engine that handles message translation and platform integration.
 *
 * Engines are responsible for:
 * - Connecting to platform services (Discord, Telegram, etc.)
 * - Translating platform-specific formats to Alice models
 * - Processing messages through the Alice pipeline
 * - Emitting events to the EventManager
 * - Managing platform-specific connections and authentication
 *
 * ## Engine State
 *
 * ```
 * INACTIVE → ACTIVE → PROCESSING → INACTIVE
 * ```
 *
 * The [active] property controls whether the engine is accepting and processing messages.
 * Set `active = true` to enable, `active = false` to disable.
 *
 * ## Message Processing Flow
 *
 * 1. Platform sends message to engine
 * 2. Engine translates to Alice MessageEvent
 * 3. Engine emits event via EventManager
 * 4. Event handlers process the message
 * 5. Engine sends response back to platform
 *
 * ## Event Integration
 *
 * Engines emit events through [events] (EventManager):
 * ```kotlin
 * val messageEvent = MessageEvent(
 *     source = "user-123",
 *     text = "Hello bot"
 * )
 * events.dispatch(messageEvent)
 * ```
 *
 * ## Implementation Example
 *
 * ```kotlin
 * class DiscordEngine(
 *     override val alice: Alice,
 *     val token: String
 * ) : Engine {
 *     override var active = false
 *     private val client = JDA.createDefault(token)
 *
 *     init {
 *         client.addEventListener(DiscordListener(this))
 *     }
 *
 *     suspend fun start() {
 *         client.awaitReady()
 *         active = true
 *     }
 *
 *     suspend fun shutdown() {
 *         active = false
 *         client.shutdown()
 *     }
 * }
 * ```
 *
 * ## Lifecycle Hooks
 *
 * ```kotlin
 * alice.before(AliceState.START) { app ->
 *     // Initialize engines
 *     app.engines { provider ->
 *         provider.values().forEach { engine ->
 *             engine.active = true
 *         }
 *     }
 * }
 *
 * alice.after(AliceState.STOP) { app ->
 *     // Cleanup engines
 *     app.engines { provider ->
 *         provider.values().forEach { engine ->
 *             engine.active = false
 *         }
 *     }
 * }
 * ```
 *
 * @property active controls whether this engine is processing messages
 * @property events the EventManager for emitting engine events
 * @see Engine.Factory for the factory pattern
 * @see EngineProvider for engine installation
 * @see EventManager for event handling
 */
interface Engine : AliceObjectOperator {

    /**
     * Controls whether this engine is active and processing messages.
     *
     * - `true` - Engine is processing messages
     * - `false` - Engine is inactive and not processing
     *
     * Setting this property triggers engine startup/shutdown.
     * Implementations should handle state transitions gracefully.
     *
     * @sample
     * ```kotlin
     * engine.active = true   // Start processing
     * engine.active = false  // Stop processing
     * ```
     */
    var active: Boolean

    /**
     * The EventManager for this engine and platform.
     *
     * Use this to:
     * - Dispatch events when messages are received
     * - Subscribe to global platform events
     * - Coordinate between engines
     *
     * This is a delegate to [alice.events] for convenience.
     *
     * @return the EventManager instance
     *
     * @sample
     * ```kotlin
     * events.dispatch(MessageEvent(
     *     source = "user-123",
     *     text = "Hello"
     * ))
     * ```
     */
    val events: EventManager
        get() = alice.events

    /**
     * Factory pattern for creating engine instances.
     *
     * Implementations of this interface provide configuration and
     * instantiation logic for a specific engine type, decoupling
     * engine creation from engine usage.
     *
     * Similar to Gradle plugin factories, allowing clean DSL-style
     * configuration during engine installation.
     *
     * ## Implementation Contract
     *
     * Implementations must:
     * 1. Provide a unique [name]
     * 2. Implement [init] to create engine instances
     * 3. Handle all configuration through the config receiver
     * 4. Return fully initialized Engine instances
     *
     * @param TConfig the configuration type for this engine
     * @param TEngine the engine type produced by this factory
     * @see EngineProvider.install for installation method
     */
    interface Factory<TConfig, TEngine : Engine> {
        /**
         * The unique name identifying this engine type.
         *
         * Used for logging, debugging, and engine lookup.
         * Examples: "discord", "telegram", "slack"
         *
         * @return unique engine name
         */
        val name: String

        /**
         * Creates and initializes an engine instance.
         *
         * This is called by the EngineProvider during engine installation.
         * The factory should:
         * 1. Create a configuration object
         * 2. Apply the config lambda to it
         * 3. Use configuration to initialize the engine
         * 4. Return the fully configured engine
         *
         * @param alice the root platform instance
         * @param config lambda to configure engine settings
         * @return initialized engine instance
         * @throws Exception if initialization fails
         *
         * @sample
         * ```kotlin
         * override fun init(
         *     alice: Alice,
         *     config: DiscordConfig.() -> Unit
         * ): DiscordEngine {
         *     val cfg = DiscordConfig().apply(config)
         *     return DiscordEngine(alice, cfg)
         * }
         * ```
         */
        fun init(alice: Alice, config: TConfig.() -> Unit): TEngine
    }
}

/**
 * Exception thrown when an engine operation fails.
 *
 * Common causes:
 * - Invalid configuration
 * - Connection failures
 * - Message processing errors
 * - Platform-specific API errors
 *
 * @see Engine for engine interface
 * @see EngineProvider for engine installation
 */
open class EngineException : AliceRuntimeException {
    /**
     * Creates an EngineException with no message or cause.
     */
    constructor() : super()

    /**
     * Creates an EngineException with the given cause.
     *
     * @param cause the exception that caused this engine error
     */
    constructor(cause: Throwable?) : super(cause)

    /**
     * Creates an EngineException with the given message.
     *
     * @param message description of the error
     */
    constructor(message: String?) : super(message)

    /**
     * Creates an EngineException with message and cause.
     *
     * @param message description of the error
     * @param cause the exception that caused this engine error
     */
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
