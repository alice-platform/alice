package io.alice.platform.api.extensions

import io.alice.platform.api.AliceObject
import io.alice.platform.api.objects.NamedObjectCollection
import io.alice.platform.api.objects.Provider

/**
 * Provider for registering and managing platform extensions and plugins.
 *
 * ExtensionsProvider enables dynamic registration of custom functionality,
 * allowing third-party code to extend Alice platform capabilities
 * without modifying the core platform.
 *
 * ## Extension Architecture
 *
 * Extensions are named, type-safe services that can be:
 * - Registered at runtime
 * - Discovered by name or type
 * - Shared across the platform
 * - Removed when no longer needed
 *
 * ## Extension Registration
 *
 * There are two ways to register extensions:
 *
 * ### 1. Direct Instance Registration
 * ```kotlin
 * alice.extensions { provider ->
 *     provider.register("user-repository", MyUserRepository())
 *     provider.register("cache-service", RedisCacheService())
 *     provider.register("analytics", GoogleAnalytics())
 * }
 * ```
 *
 * ### 2. Factory-Based Registration
 * ```kotlin
 * alice.extensions { provider ->
 *     // Extension created on demand
 *     val dbService = provider.create(
 *         "database",
 *         DatabaseService::class.java,
 *         "jdbc:mysql://localhost:3306/alice",
 *         "root",
 *         "password"
 *     )
 * }
 * ```
 *
 * ## Extension Discovery
 *
 * As a [NamedObjectCollection], extensions can be discovered:
 * ```kotlin
 * val extensions = alice.extensions
 *
 * // Get by name
 * val cache = extensions.named("cache-service").getOrNull()
 *
 * // Get all extensions
 * val all = extensions.values()
 *
 * // Iterate extensions
 * for (extension in extensions) {
 *     println(extension::class.simpleName)
 * }
 * ```
 *
 * ## Use Cases
 *
 * Extensions commonly provide:
 * - **Data Access** - Database repositories, caches
 * - **External Services** - APIs, webhooks, analytics
 * - **Custom Logic** - Business rules, processors
 * - **Monitoring** - Metrics, logging, tracing
 * - **Security** - Authentication, authorization
 *
 * ```kotlin
 * class MyModule : Module {
 *     override fun apply(alice: Alice) {
 *         alice.extensions { provider ->
 *             // Register a custom authenticator
 *             provider.register(
 *                 "auth",
 *                 JWTAuthenticator(alice.configuration)
 *             )
 *
 *             // Register a database repository
 *             provider.register(
 *                 "users",
 *                 PostgresUserRepository("jdbc:postgresql://localhost/alice")
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * ## Lifecycle
 *
 * Extensions are registered during INIT state and available throughout the
 * platform's lifetime. They should be cleaned up during the STOP state
 * if they hold resources.
 *
 * ## Access from Event Handlers
 *
 * ```kotlin
 * alice.events { manager ->
 *     manager.onEvent(MessageEvent::class.java) { event ->
 *         val cache = alice.extensions
 *             .named("cache-service")
 *             .getOrNull()
 *
 *         cache?.get(event.source)
 *     }
 * }
 * ```
 *
 * @see NamedObjectCollection for collection operations
 * @see Alice.extensions for DSL access
 * @see Provider for lazy initialization
 */
interface ExtensionsProvider : NamedObjectCollection<Any>, AliceObject {

    /**
     * Creates and registers an extension instance using constructor arguments.
     *
     * The extension class is instantiated using the provided constructor arguments
     * and registered under the given name.
     *
     * ## Constructor Argument Matching
     *
     * The factory attempts to find a constructor that matches the provided arguments.
     * Arguments are matched by type. If multiple constructors are possible,
     * the most specific match is used.
     *
     * ## Return Value
     *
     * Returns a Provider containing the created extension. This allows:
     * - Lazy initialization
     * - Error handling
     * - Access to both the provider and the registered extension
     *
     * @param T the extension type
     * @param name the name to register the extension under
     * @param type the class of the extension to create
     * @param values constructor arguments
     * @return a provider containing the created extension
     *
     * @sample
     * ```kotlin
     * val service = provider.create(
     *     "database",
     *     DatabaseService::class.java,
     *     "jdbc:mysql://localhost",
     *     3306,
     *     true
     * )
     *
     * val db = service.get()
     * ```
     *
     * @throws IllegalArgumentException if no suitable constructor is found
     * @throws InstantiationException if the class cannot be instantiated
     */
    fun <T : Any> create(
        name: String,
        type: Class<T>,
        vararg values: Any
    ): Provider<T>

    /**
     * Registers an extension instance under the given name.
     *
     * The extension is stored as-is and is immediately available for lookup
     * by the given name.
     *
     * ## Registration Semantics
     *
     * - If an extension with the same name is already registered, it is replaced
     * - The extension becomes immediately available
     * - The extension is stored as Any type (Type information is lost)
     * - Safe casting should be performed when retrieving
     *
     * @param T the extension type
     * @param name the name to register the extension under
     * @param value the extension instance to register
     *
     * @sample
     * ```kotlin
     * val userService = UserServiceImpl()
     * provider.register("users", userService)
     *
     * val retrieved = provider.named("users").get() as UserService
     * ```
     *
     * @throws NullPointerException if value is null (some implementations)
     */
    fun <T : Any> register(name: String, value: T)
}
