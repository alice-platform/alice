package io.alice.platform.api.objects

import io.alice.platform.api.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

/**
 * Mutable version of [Provider] that allows changing the underlying value or provider.
 *
 * Properties support both read and write operations, reactive callbacks, and change notifications.
 * They follow the reactive stream pattern with monadic operations for functional composition.
 *
 * ## Usage Examples
 *
 * ### Basic Property
 * ```kotlin
 * val factory: ObjectFactory = // ...
 * val property: Property<String> = factory.property.of("initial")
 *
 * // Read the value
 * val value = property.get()

 * // Update the value
 * property.set("new value")
 *
 * // React to changes
 * property.onUpdate { oldValue, newValue ->
 *     println("Value changed from $oldValue to $newValue")
 * }
 * ```
 *
 * ### Property Transformation
 * ```kotlin
 * val stringProp: Property<String> = factory.property.of("42")
 * val intProp: Property<Int> = stringProp.map(Transformer { it.toInt() })
 * ```
 *
 * ### Delegating Property
 * ```kotlin
 * val source = factory.property.of("source")
 * val delegated = factory.property.of("initial")
 * delegated.delegate(source) // Now delegates to source
 * ```
 *
 * @param T the type of the value held by this property
 * @see Provider for immutable value access
 * @see ReadWriteProperty for Kotlin property delegation support
 */
interface Property<T> : ReadWriteProperty<Any?, T>, Provider<T> {
    /**
     * Sets this property to delegate to another provider.
     *
     * After calling this, the property will return values from the given provider.
     *
     * @param value the provider to delegate to
     * @return this property for method chaining
     */
    fun set(value: Provider<T>)

    /**
     * Sets the property value directly.
     *
     * @param value the new value (can be null)
     * @return this property for method chaining
     */
    fun set(value: T?)

    /**
     * Sets this property to delegate to another provider.
     *
     * @param value the provider to delegate to
     * @return this property for method chaining
     */
    fun delegate(value: Provider<T>): Property<T>

    /**
     * Sets the property value to a direct value.
     *
     * @param value the value to set (can be null)
     * @return this property for method chaining
     */
    fun delegate(value: T?): Property<T>

    override fun ifPresent(consumer: Consumer<T>): Property<T>
    override fun <R : Any> map(transformer: Transformer<T, R>): Property<R>
    override fun <R : Any> flatMap(transformer: Transformer<T, Provider<R>>): Property<R>
    override fun onSuccess(function: Consumer<T>): Property<T>
    override fun onError(function: Consumer<Throwable>): Property<T>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): Property<T>
    override fun onCompleted(function: Runnable): Property<T>

    /**
     * Registers a callback to be invoked when the property value changes.
     *
     * The callback receives both the old and new values. The old value may be null if
     * this is the first update.
     *
     * @param function the callback function
     * @return this property for method chaining
     *
     * @sample
     * ```kotlin
     * property.onUpdate { oldValue, newValue ->
     *     println("Changed from $oldValue to $newValue")
     * }
     * ```
     */
    fun onUpdate(function: BiConsumer<T?, T?>): Property<T>
}

/**
 * Immutable provider for lazy value evaluation with functional composition.
 *
 * Providers are core to the Alice platform's reactive architecture. They support:
 * - Lazy value computation
 * - Functional transformation (map, flatMap)
 * - Error handling and recovery
 * - Reactive callbacks (onSuccess, onError, onCompleted)
 *
 * ## Usage Examples
 *
 * ### Basic Provider
 * ```kotlin
 * val factory: ObjectFactory = // ...
 * val provider: Provider<String> = factory.provider.of("Hello")
 * val value = provider.get() // "Hello"
 * ```
 *
 * ### Lazy Evaluation
 * ```kotlin
 * val supplier: Supplier<Int> = Supplier { expensiveComputation() }
 * val provider: Provider<Int> = factory.provider.ofSupplied(supplier)
 * // expensiveComputation() is not called until get() is invoked
 * val result = provider.get()
 * ```
 *
 * ### Functional Composition
 * ```kotlin
 * val stringProvider = factory.provider.of("42")
 * val intProvider = stringProvider.map(Transformer { it.toInt() })
 * val doubledProvider = intProvider.map(Transformer { it * 2 })
 * ```
 *
 * ### Error Handling
 * ```kotlin
 * val provider = factory.provider.of("value")
 * provider
 *     .onSuccess(Consumer { println("Got: $it") })
 *     .onError(Consumer { println("Error: ${it.message}") })
 *     .onCompleted(Runnable { println("Done") })
 * ```
 *
 * @param T the type of the value produced by this provider
 * @see Property for mutable providers
 * @see IterableProvider for collections of values
 * @see MapProvider for map-based values
 */
interface Provider<T> : ReadOnlyProperty<Any?, T> {
    /**
     * Checks if a value is currently available.
     *
     * For eager providers, this is always true. For lazy providers, this indicates
     * whether the value has been computed.
     *
     * @return true if a value is available, false otherwise
     */
    val isPresent: Boolean

    /**
     * Returns the value held by this provider.
     *
     * For eager providers, this returns the value immediately.
     * For lazy providers, this triggers computation if needed.
     *
     * @return the value
     * @throws exception if the provider is empty or computation fails
     */
    fun get(): T

    /**
     * Returns the value held by this provider, or the default if no value is available.
     *
     * @param default the value to return if this provider is empty
     * @return the provider's value or the default
     */
    fun getOrElse(default: T): T

    /**
     * Returns the value held by this provider, or null if no value is available.
     *
     * @return the provider's value or null
     */
    fun getOrNull(): T?

    /**
     * Returns the value held by this provider, throwing an exception if not available.
     *
     * @param throwable a supplier for the exception to throw
     * @return the provider's value
     * @throws Throwable if the provider is empty
     *
     * @sample
     * ```kotlin
     * val value = provider.getOrThrow(Supplier {
     *     IllegalStateException("No value available")
     * })
     * ```
     */
    fun getOrThrow(@Suppress("UNCHECKED_CAST") throwable: Supplier<Throwable> = { NullPointerException() } as Supplier<Throwable>): T

    /**
     * Executes a consumer if a value is available.
     *
     * This is a non-blocking operation that executes the consumer immediately with the value.
     *
     * @param consumer the consumer to execute
     * @return this provider for method chaining
     *
     * @sample
     * ```kotlin
     * provider.ifPresent(Consumer { value ->
     *     println("Value is present: $value")
     * })
     * ```
     */
    fun ifPresent(consumer: Consumer<T>): Provider<T>

    /**
     * Returns a new provider that applies a transformation to the value.
     *
     * @param transformer the transformation to apply
     * @return a new provider containing the transformed value
     *
     * @sample
     * ```kotlin
     * val stringProvider = factory.provider.of("42")
     * val intProvider = stringProvider.map(Transformer { it.toInt() })
     * ```
     */
    fun <R : Any> map(transformer: Transformer<T, R>): Provider<R>

    /**
     * Returns a new provider that applies a transformation returning a new provider.
     *
     * This is useful for operations that return providers themselves, automatically
     * flattening the result.
     *
     * @param transformer the transformation returning a provider
     * @return a new provider containing the final value
     *
     * @sample
     * ```kotlin
     * val configProvider = factory.provider.of("config.yaml")
     * val loadedProvider = configProvider.flatMap(Transformer { filename ->
     *     factory.provider.ofSupplied(Supplier { loadConfig(filename) })
     * })
     * ```
     */
    fun <R : Any> flatMap(transformer: Transformer<T, Provider<R>>): Provider<R>

    /**
     * Registers a callback to be executed when the value is successfully retrieved.
     *
     * @param function the callback function
     * @return this provider for method chaining
     */
    fun onSuccess(function: Consumer<T>): Provider<T>

    /**
     * Registers a callback to be executed if an error occurs.
     *
     * @param function the callback function
     * @return this provider for method chaining
     */
    fun onError(function: Consumer<Throwable>): Provider<T>

    /**
     * Registers a callback to be executed if a specific error type occurs.
     *
     * @param type the exception type to handle
     * @param function the callback function
     * @return this provider for method chaining
     */
    fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): Provider<T>

    /**
     * Registers a callback to be executed when the operation completes (success or error).
     *
     * @param function the callback function
     * @return this provider for method chaining
     */
    fun onCompleted(function: Runnable): Provider<T>

    /**
     * Invokes the given block with the provider's value.
     *
     * This is a convenience method for inline operations on the value.
     *
     * @param function the block to execute with the value
     *
     * @sample
     * ```kotlin
     * provider { println(this) }
     * ```
     */
    operator fun invoke(function: T.() -> Unit)
}

/**
 * Provider for collections of values with per-element operations.
 *
 * IterableProviders extend the Provider pattern to collections, supporting:
 * - Index-based access
 * - Per-element transformations (mapEach, flatMapEach)
 * - Per-element callbacks (onSuccessEach)
 * - Collection-level operations
 *
 * ## Usage Examples
 *
 * ### Creating Iterable Providers
 * ```kotlin
 * val factory: ObjectFactory = // ...
 * val provider = factory.provider.many(1, 2, 3, 4, 5)
 * ```
 *
 * ### Per-Element Transformation
 * ```kotlin
 * val numbers = factory.provider.many(1, 2, 3)
 * val doubled = numbers.mapEach(Transformer { it * 2 })
 * // Contains [2, 4, 6]
 * ```
 *
 * ### Index-Based Access
 * ```kotlin
 * val items = factory.provider.many("a", "b", "c")
 * val firstItem = items.get(0).getOrNull() // "a"
 * val secondOrDefault = items.getOrDefault(1, "default").get() // "b"
 * ```
 *
 * @param T the type of elements in the iterable
 * @see IterableProperty for mutable iterable providers
 * @see Provider for single-value providers
 */
interface IterableProvider<T> : Provider<Iterable<T>>, Iterable<T> {
    /**
     * Returns the number of elements in this provider.
     */
    val size: Int

    /**
     * Checks if this provider is empty.
     */
    val isEmpty: Boolean

    override fun ifPresent(consumer: Consumer<Iterable<T>>): IterableProvider<T>

    /**
     * Returns a provider for the element at the given index.
     *
     * @param index the index of the element
     * @return a provider for the element at that index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun get(index: Int): Provider<T>

    /**
     * Returns a provider for the element at the given index, or a default value.
     *
     * @param index the index of the element
     * @param default the default value if index is out of range
     * @return a provider for the element or default
     */
    fun getOrDefault(index: Int, default: T): Provider<T>

    /**
     * Returns a provider for the element at the given index, delegating to a default provider.
     *
     * @param index the index of the element
     * @param default the default provider if index is out of range
     * @return a provider for the element or default provider
     */
    fun getOrDefault(index: Int, default: Provider<T>): Provider<T>

    /**
     * Returns a new provider that applies a transformation to each element.
     *
     * @param transformer the transformation to apply to each element
     * @return a new provider with transformed elements
     *
     * @sample
     * ```kotlin
     * val numbers = factory.provider.many(1, 2, 3)
     * val strings = numbers.mapEach(Transformer { it.toString() })
     * ```
     */
    fun <R : Any> mapEach(transformer: Transformer<T, R>): IterableProvider<R>

    /**
     * Returns a new provider that applies a transformation returning iterables to each element,
     * then flattens the result.
     *
     * @param transformer the transformation to apply to each element
     * @return a new provider with flattened transformed elements
     */
    fun <R : Any> flatMapEach(transformer: Transformer<T, Iterable<R>>): IterableProvider<R>

    /**
     * Registers a callback to be executed for each element.
     *
     * @param function the callback function
     * @return this provider for method chaining
     */
    fun onSuccessEach(function: Consumer<T>): IterableProvider<T>

    override fun onSuccess(function: Consumer<Iterable<T>>): IterableProvider<T>
    override fun onError(function: Consumer<Throwable>): IterableProvider<T>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): IterableProvider<T>
    override fun onCompleted(function: Runnable): IterableProvider<T>
}

/**
 * Mutable version of [IterableProvider] supporting add/remove operations.
 *
 * IterableProperties extend both IterableProvider and Property, supporting both
 * per-element transformations and collection mutations.
 *
 * ## Usage Examples
 *
 * ### Creating and Mutating
 * ```kotlin
 * val factory: ObjectFactory = // ...
 * val list: IterableProperty<String> = factory.property.many()
 *
 * list.add("first")
 * list += "second"
 * list.clear()
 * ```
 *
 * ### Reactive Updates
 * ```kotlin
 * list.onEntryUpdate { oldEntry, newEntry ->
 *     println("Entry changed from $oldEntry to $newEntry")
 * }
 * ```
 *
 * @param T the type of elements in the iterable
 * @see IterableProvider for immutable iterable providers
 * @see Property for mutable single-value providers
 */
interface IterableProperty<T> : IterableProvider<T>, Property<Iterable<T>> {
    override fun get(index: Int): Property<T>
    override fun ifPresent(consumer: Consumer<Iterable<T>>): IterableProperty<T>
    override fun getOrDefault(index: Int, default: T): Property<T>
    override fun getOrDefault(index: Int, default: Provider<T>): Property<T>
    override fun <R : Any> mapEach(transformer: Transformer<T, R>): IterableProperty<R>
    override fun <R : Any> flatMapEach(transformer: Transformer<T, Iterable<R>>): IterableProperty<R>

    /**
     * Removes all elements from this property.
     */
    fun clear()

    /**
     * Adds an element to this property.
     *
     * @param value the element to add
     */
    fun add(value: T)

    /**
     * Adds an element using the += operator.
     *
     * @param value the element to add
     */
    operator fun plusAssign(value: T)

    override fun delegate(value: Provider<Iterable<T>>): IterableProperty<T>
    override fun delegate(value: Iterable<T>?): IterableProperty<T>

    override fun onSuccessEach(function: Consumer<T>): IterableProperty<T>
    override fun onSuccess(function: Consumer<Iterable<T>>): IterableProperty<T>
    override fun onError(function: Consumer<Throwable>): IterableProperty<T>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): IterableProperty<T>
    override fun onCompleted(function: Runnable): IterableProperty<T>
    override fun onUpdate(function: BiConsumer<Iterable<T>?, Iterable<T>?>): IterableProperty<T>

    /**
     * Registers a callback to be executed when an element is updated.
     *
     * The callback receives both the old and new indexed values.
     *
     * @param function the callback function
     * @return this property for method chaining
     */
    fun onEntryUpdate(function: BiConsumer<IndexedValue<T>, IndexedValue<T>>): IterableProperty<T>
}

/**
 * Provider for map values with key and value operations.
 *
 * MapProviders extend the Provider pattern to maps, supporting:
 * - Key/value-based access
 * - Key and value transformations
 * - Per-entry callbacks
 * - Map-level operations
 *
 * ## Usage Examples
 *
 * ### Creating Map Providers
 * ```kotlin
 * val factory: ObjectFactory = // ...
 * val mapProvider = factory.provider.map(
 *     "key1" to "value1",
 *     "key2" to "value2"
 * )
 * ```
 *
 * ### Accessing Keys and Values
 * ```kotlin
 * val keys = mapProvider.keys
 * val values = mapProvider.values
 * val entries = mapProvider.entries
 *
 * val value = mapProvider.get("key1").getOrNull()
 * ```
 *
 * ### Transformations
 * ```kotlin
 * val transformed = mapProvider
 *     .mapValues(Transformer { it.uppercase() })
 *     .mapKeys(Transformer { it.lowercase() })
 * ```
 *
 * @param K the type of keys in the map
 * @param V the type of values in the map
 * @see MapProperty for mutable map providers
 * @see IterableProvider for collections of entries
 */
interface MapProvider<K, V> : Provider<Map<K, V>> {
    /**
     * Checks if the map is empty.
     */
    val isEmpty: Boolean
        get() = entries.isEmpty

    /**
     * Returns a provider for all keys in the map.
     */
    val keys: IterableProvider<K>

    /**
     * Returns a provider for all values in the map.
     */
    val values: IterableProvider<V>

    /**
     * Returns a provider for all key-value entries.
     */
    val entries: IterableProvider<Map.Entry<K, V>>

    /**
     * Returns a provider for the value associated with the given key.
     *
     * @param key the key to look up
     * @return a provider for the value, or empty if key not found
     */
    fun get(key: K): Provider<V>

    /**
     * Returns a provider for the value associated with the given key, or a default.
     *
     * @param key the key to look up
     * @param default the default value if key not found
     * @return a provider for the value or default
     */
    fun getOrDefault(key: K, default: V): Provider<V>

    /**
     * Returns a provider for the value associated with the given key, delegating to a default provider.
     *
     * @param key the key to look up
     * @param default the default provider if key not found
     * @return a provider for the value or default provider
     */
    fun getOrDefault(key: K, default: Provider<V>): Provider<V>

    override fun ifPresent(consumer: Consumer<Map<K, V>>): MapProvider<K, V>

    /**
     * Returns a new provider with transformed keys.
     *
     * @param transformer the transformation to apply to each key
     * @return a new provider with transformed keys
     */
    fun <R : Any> mapKeys(transformer: Transformer<K, R>): MapProvider<R, V>

    /**
     * Returns a new provider with transformed values.
     *
     * @param transformer the transformation to apply to each value
     * @return a new provider with transformed values
     */
    fun <R : Any> mapValues(transformer: Transformer<V, R>): MapProvider<K, R>

    /**
     * Returns a provider for an iterable of transformed entries.
     *
     * @param transformer the transformation to apply to each entry
     * @return a new provider containing the transformed entries
     */
    fun <R : Any> mapIterable(transformer: Transformer<Map.Entry<K, V>, R>): IterableProvider<R>

    /**
     * Returns a provider for an iterable of transformed entries, with flattening.
     *
     * @param transformer the transformation to apply to each entry
     * @return a new provider containing the flattened transformed entries
     */
    fun <R : Any> flatMapIterable(transformer: Transformer<Map.Entry<K, V>, Iterable<R>>): IterableProvider<R>

    /**
     * Registers a callback to be executed for each key-value pair.
     *
     * @param function the callback function
     * @return this provider for method chaining
     */
    fun onSuccessEach(function: BiConsumer<K, V>): MapProvider<K, V>

    override fun onSuccess(function: Consumer<Map<K, V>>): MapProvider<K, V>
    override fun onError(function: Consumer<Throwable>): MapProvider<K, V>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): MapProvider<K, V>
    override fun onCompleted(function: Runnable): MapProvider<K, V>
}

/**
 * Mutable version of [MapProvider] supporting put/clear operations.
 *
 * MapProperties extend both MapProvider and Property, supporting both
 * transformations and mutations of the underlying map.
 *
 * ## Usage Examples
 *
 * ### Creating and Mutating
 * ```kotlin
 * val factory: ObjectFactory = // ...
 * val map: MapProperty<String, String> = factory.property.map()
 *
 * map.put("key1", "value1")
 * map.clear()
 *
 * map.onEntryUpdate { oldEntry, newEntry ->
 *     println("Entry changed from $oldEntry to $newEntry")
 * }
 * ```
 *
 * @param K the type of keys in the map
 * @param V the type of values in the map
 * @see MapProvider for immutable map providers
 * @see Property for mutable single-value providers
 */
interface MapProperty<K, V> : MapProvider<K, V>, Property<Map<K, V>> {
    override val keys: IterableProperty<K>
    override val values: IterableProperty<V>
    override val entries: IterableProperty<Map.Entry<K, V>>
    override fun delegate(value: Provider<Map<K, V>>): MapProperty<K, V>
    override fun delegate(value: Map<K, V>?): MapProperty<K, V>
    override fun get(key: K): Property<V>
    override fun getOrDefault(key: K, default: V): Property<V>
    override fun getOrDefault(key: K, default: Provider<V>): Property<V>
    override fun ifPresent(consumer: Consumer<Map<K, V>>): MapProperty<K, V>
    override fun <R : Any> mapKeys(transformer: Transformer<K, R>): MapProperty<R, V>
    override fun <R : Any> mapValues(transformer: Transformer<V, R>): MapProperty<K, R>
    override fun <R : Any> mapIterable(transformer: Transformer<Map.Entry<K, V>, R>): IterableProperty<R>
    override fun <R : Any> flatMapIterable(transformer: Transformer<Map.Entry<K, V>, Iterable<R>>): IterableProperty<R>
    override fun onSuccessEach(function: BiConsumer<K, V>): MapProperty<K, V>
    override fun onSuccess(function: Consumer<Map<K, V>>): MapProperty<K, V>
    override fun onError(function: Consumer<Throwable>): MapProperty<K, V>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): MapProperty<K, V>
    override fun onCompleted(function: Runnable): MapProperty<K, V>
    override fun onUpdate(function: BiConsumer<Map<K, V>?, Map<K, V>?>): MapProperty<K, V>

    /**
     * Registers a callback to be executed when an entry is updated.
     *
     * The callback receives both the old and new entry values.
     *
     * @param function the callback function
     * @return this property for method chaining
     */
    fun onEntryUpdate(function: BiConsumer<Map.Entry<K, V>, Map.Entry<K, V>>): MapProperty<K, V>

    /**
     * Removes all entries from this property.
     */
    fun clear()

    /**
     * Puts a key-value pair into this property.
     *
     * @param key the key
     * @param value the value
     */
    fun put(key: K, value: V)
}

/**
 * Type alias for Boolean providers.
 *
 * ```kotlin
 * val flag: BooleanProvider = factory.provider.of(true)
 * ```
 */
typealias BooleanProvider = Provider<Boolean>

/**
 * Type alias for Boolean properties.
 */
typealias BooleanProperty = Property<Boolean>

/**
 * Type alias for Double providers.
 */
typealias DoubleProvider = Provider<Double>

/**
 * Type alias for Double properties.
 */
typealias DoubleProperty = Property<Double>

/**
 * Type alias for Float providers.
 */
typealias FloatProvider = Provider<Float>

/**
 * Type alias for Float properties.
 */
typealias FloatProperty = Property<Float>

/**
 * Type alias for Int providers.
 */
typealias IntProvider = Provider<Int>

/**
 * Type alias for Int properties.
 */
typealias IntProperty = Property<Int>

/**
 * Type alias for Long providers.
 */
typealias LongProvider = Provider<Long>

/**
 * Type alias for Long properties.
 */
typealias LongProperty = Property<Long>

/**
 * Type alias for Unit (Void) providers.
 */
typealias VoidProvider = Provider<Unit>

/**
 * Type alias for Unit (Void) properties.
 */
typealias VoidProperty = Property<Unit>
