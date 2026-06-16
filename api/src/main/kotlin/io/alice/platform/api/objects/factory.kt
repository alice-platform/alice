package io.alice.platform.api.objects

import io.alice.platform.api.AliceObject
import io.alice.platform.api.Supplier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Factory for creating and managing Alice platform objects.
 *
 * ObjectFactory is the central hub for object creation and management,
 * providing access to three main subsystems:
 * 1. **Providers** - For immutable lazy-evaluated values
 * 2. **Properties** - For mutable reactive values
 * 3. **Collections** - For managing groups of objects
 *
 * Following the Gradle plugin architecture pattern, ObjectFactory provides
 * domain-specific object management for the Alice platform.
 *
 * ## Factory Subsystems
 *
 * ```kotlin
 * val factory: ObjectFactory = alice.objects
 *
 * // Provider factory - immutable values
 * val provider = factory.provider.of("value")
 * val lazy = factory.provider.ofSupplied { computeValue() }
 *
 * // Property factory - mutable reactive values
 * val property = factory.property.of("initial")
 * property.set("new value")
 *
 * // Collection factory - managing groups
 * val list = factory.collections.list(1, 2, 3)
 * val named = factory.collections.named(
 *     "first" to item1,
 *     "second" to item2
 * )
 * ```
 *
 * ## Type Conversion
 *
 * The factory supports converting arbitrary objects to typed Providers:
 * ```kotlin
 * val data = mapOf("name" to "Alice", "version" to "1.0")
 * val config = factory.convert(data, ConfigObject::class.java)
 * val value = config.get() // Returns ConfigObject instance
 * ```
 *
 * ## Integration with DSL
 *
 * ```kotlin
 * alice.objects { factory ->
 *     val users = factory.provider.many(
 *         User("alice", "Alice"),
 *         User("bob", "Bob")
 *     )
 *
 *     val config = factory.property.of(appConfig)
 *     config.onUpdate { oldCfg, newCfg ->
 *         reloadServices(newCfg)
 *     }
 * }
 * ```
 *
 * @property provider factory for creating immutable Providers
 * @property property factory for creating mutable Properties
 * @property collections factory for creating collections
 * @property scripts manager for script execution and lifecycle
 * @see Provider for immutable values
 * @see Property for mutable reactive values
 * @see NamedObjectCollection for named object collections
 */
interface ObjectFactory : AliceObject {
    /**
     * Factory for creating immutable Provider instances.
     *
     * Use this factory to create read-only values and lazy computations
     * that can be composed with functional operations.
     *
     * @return the provider factory
     */
    val provider: ProviderFactory

    /**
     * Factory for creating mutable Property instances.
     *
     * Use this factory to create reactive values that support updates
     * and change notifications.
     *
     * @return the property factory
     */
    val property: PropertyFactory

    /**
     * Factory for creating collection instances.
     *
     * Use this factory to create lists, named collections, and other
     * group-based object containers.
     *
     * @return the collection factory
     */
    val collections: CollectionFactory

    /**
     * Manager for script execution and lifecycle integration.
     *
     * Use this to import external scripts and manage their execution
     * at different platform lifecycle stages.
     *
     * @return the script manager
     */
    val scripts: ScriptManager

    /**
     * Converts arbitrary data to a specific typed Provider.
     *
     * Attempts to convert the given data object to the target type,
     * returning a Provider containing the result.
     *
     * This is useful for:
     * - Configuration deserialization
     * - Type conversions
     * - Data mapping
     *
     * @param T the target type
     * @param data the data to convert (can be null)
     * @param type the target class
     * @return a provider containing the converted value
     *
     * @sample
     * ```kotlin
     * data class Config(val host: String, val port: Int)
     *
     * val rawData = mapOf("host" to "localhost", "port" to 8080)
     * val configProvider = factory.convert(rawData, Config::class.java)
     * val config = configProvider.get()
     * ```
     */
    fun <T : Any> convert(data: Any?, type: Class<T>): Provider<T>

    /**
     * Converts arbitrary data to a specific mutable Property.
     *
     * Similar to [convert], but returns a mutable Property instead of
     * an immutable Provider.
     *
     * @param T the target type
     * @param data the data to convert (can be null)
     * @param type the target class
     * @return a property containing the converted value
     */
    fun <T : Any> convertMutable(data: Any?, type: Class<T>): Property<T>
}

/**
 * Factory for creating immutable Provider instances.
 *
 * ProviderFactory provides methods for creating Providers from:
 * - Eager values
 * - Lazy suppliers
 * - Nullable values
 * - Empty values with custom exceptions
 * - Collections (Iterable, Map)
 * - Futures for async operations
 *
 * All methods return Providers that are read-only and compose well
 * with functional operations (map, flatMap, etc.).
 *
 * @see Provider for the Provider interface
 */
interface ProviderFactory {
    /**
     * Creates a Provider from a lazy supplier.
     *
     * The supplier is called each time [Provider.get] is invoked,
     * allowing for deferred computation.
     *
     * @param T the value type
     * @param target the supplier that produces values
     * @return a lazy provider
     *
     * @sample
     * ```kotlin
     * val provider = factory.ofSupplied {
     *     expensiveComputation()
     * }
     * ```
     */
    fun <T : Any> ofSupplied(target: Supplier<T>): Provider<T>

    /**
     * Creates a Provider from a nullable value.
     *
     * If the value is null, the provider is empty.
     *
     * @param T the value type
     * @param target the value (can be null)
     * @return a provider
     */
    fun <T : Any> ofNullable(target: T?): Provider<T>

    /**
     * Creates a Provider from an eager value.
     *
     * The value is stored immediately and returned for all accesses.
     *
     * @param T the value type
     * @param target the value
     * @return an eager provider
     *
     * @sample
     * ```kotlin
     * val provider = factory.of("fixed-value")
     * ```
     */
    fun <T : Any> of(target: T): Provider<T>

    /**
     * Creates an empty Provider with a custom exception.
     *
     * When [Provider.get] is called, it throws the exception from the supplier.
     *
     * @param T the value type
     * @param throwable supplier for the exception to throw
     * @return an empty provider
     *
     * @sample
     * ```kotlin
     * val provider = factory.empty(
     *     Supplier { IllegalStateException("Not initialized") }
     * )
     * ```
     */
    fun <T : Any> empty(
        throwable: Supplier<Throwable> = Supplier { NullPointerException("No value provided.") }
    ): Provider<T>

    /**
     * Creates an IterableProvider from multiple values.
     *
     * @param T the element type
     * @param values the elements
     * @return an iterable provider
     *
     * @sample
     * ```kotlin
     * val provider = factory.many(1, 2, 3, 4, 5)
     * ```
     */
    fun <T : Any> many(vararg values: T): IterableProvider<T>

    /**
     * Creates an IterableProvider from an iterable.
     *
     * @param T the element type
     * @param values the elements
     * @return an iterable provider
     */
    fun <T : Any> many(values: Iterable<T>): IterableProvider<T>

    /**
     * Creates a MapProvider from a map of key-value pairs.
     *
     * @param K the key type
     * @param V the value type
     * @param values the map
     * @return a map provider
     */
    fun <K : Any, V : Any> map(values: Map<K, V>): MapProvider<K, V>

    /**
     * Creates a MapProvider from key-value pairs.
     *
     * @param K the key type
     * @param V the value type
     * @param values the key-value pairs
     * @return a map provider
     *
     * @sample
     * ```kotlin
     * val map = factory.map(
     *     "name" to "Alice",
     *     "version" to "1.0",
     *     "active" to true
     * )
     * ```
     */
    fun <K : Any, V : Any> map(vararg values: Pair<K, V>): MapProvider<K, V>

    /**
     * Creates a Provider from a Future.
     *
     * The provider waits for the future to complete when [Provider.get] is called.
     * Uses a single-threaded executor by default for future completion.
     *
     * @param T the value type
     * @param future the future to wait for
     * @param executor the executor service for async operations
     * @return a provider that resolves the future
     *
     * @sample
     * ```kotlin
     * val future = executorService.submit { asyncComputation() }
     * val provider = factory.fromFuture(future)
     * ```
     */
    fun <T : Any> fromFuture(
        future: Future<T>,
        executor: ExecutorService = Executors.newSingleThreadExecutor()
    ): Provider<T>
}

/**
 * Factory for creating mutable Property instances.
 *
 * PropertyFactory extends ProviderFactory with mutation capabilities,
 * allowing creation of reactive values that can be updated and observed.
 *
 * All ProviderFactory methods return Property instead of Provider,
 * enabling read/write access and change notifications.
 *
 * @see Property for the Property interface
 * @see ProviderFactory for base creation methods
 */
interface PropertyFactory : ProviderFactory {
    /**
     * Creates a mutable Property delegating to an immutable Provider.
     *
     * Changes to the property affect reads but don't modify the underlying provider.
     *
     * @param T the value type
     * @param provider the provider to delegate to
     * @return a property backed by the provider
     */
    fun <T : Any> mutate(provider: Provider<T>): Property<T>

    override fun <T : Any> ofSupplied(target: Supplier<T>): Property<T>
    override fun <T : Any> ofNullable(target: T?): Property<T>
    override fun <T : Any> of(target: T): Property<T>
    override fun <T : Any> empty(throwable: Supplier<Throwable>): Property<T>

    override fun <T : Any> many(vararg values: T): IterableProperty<T>
    override fun <T : Any> many(values: Iterable<T>): IterableProperty<T>

    override fun <K : Any, V : Any> map(values: Map<K, V>): MapProperty<K, V>
    override fun <K : Any, V : Any> map(vararg values: Pair<K, V>): MapProperty<K, V>
    override fun <T : Any> fromFuture(future: Future<T>, executor: ExecutorService): Property<T>
}

/**
 * Factory for creating collection instances.
 *
 * CollectionFactory provides methods for creating different collection types:
 * - **MutableObjectCollection** - Mutable unordered collections
 * - **ObjectCollection** - Immutable unordered collections
 * - **NamedObjectCollection** - Immutable named collections
 * - **MutableNamedObjectCollection** - Mutable named collections
 *
 * Collections support type-safe access and transformation operations.
 *
 * @see ObjectCollection for immutable collections
 * @see NamedObjectCollection for named collections
 */
interface CollectionFactory {
    /**
     * Creates an empty mutable collection.
     *
     * @param T the element type
     * @return a mutable collection
     */
    fun <T : Any> mutableList(): MutableObjectCollection<T>

    /**
     * Creates an immutable collection from varargs.
     *
     * @param T the element type
     * @param values the elements
     * @return an immutable collection
     */
    fun <T : Any> list(vararg values: T): ObjectCollection<T>

    /**
     * Creates an immutable collection from an iterable.
     *
     * @param T the element type
     * @param values the elements
     * @return an immutable collection
     */
    fun <T : Any> list(values: Collection<T>): ObjectCollection<T>

    /**
     * Creates an immutable named collection from pairs.
     *
     * @param T the element type
     * @param values name-element pairs
     * @return a named collection
     *
     * @sample
     * ```kotlin
     * val services = factory.named(
     *     "auth" to authService,
     *     "db" to databaseService
     * )
     * ```
     */
    fun <T : Any> named(vararg values: Pair<String, T>): NamedObjectCollection<T>

    /**
     * Creates an immutable named collection from a map.
     *
     * @param T the element type
     * @param values the name-element map
     * @return a named collection
     */
    fun <T : Any> named(values: Map<String, T>): NamedObjectCollection<T>

    /**
     * Creates an empty mutable named collection.
     *
     * @param T the element type
     * @return a mutable named collection
     */
    fun <T : Any> mutableNamed(): MutableNamedObjectCollection<T>
}
