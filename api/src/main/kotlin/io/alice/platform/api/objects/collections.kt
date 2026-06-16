package io.alice.platform.api.objects

import io.alice.platform.api.AliceException
import io.alice.platform.api.Predicate
import io.alice.platform.api.Transformer

/**
 * Immutable collection of heterogeneous objects accessible by type.
 *
 * ObjectCollections support type-safe retrieval, filtering, and transformation of collections.
 * Unlike standard collections, they work with object hierarchies and type relationships.
 *
 * ## Type Safety
 *
 * Collections maintain type information, allowing retrieval of specific subtypes.
 * For example, a collection containing both `Animal` and `Dog` instances allows
 * retrieving all `Dog` instances specifically.
 *
 * ## Usage Examples
 *
 * ### Basic Retrieval
 * ```kotlin
 * val collection: ObjectCollection<Component> = // ...
 * val count = collection.size
 * val isEmpty = collection.isEmpty
 *
 * // Retrieve by exact type
 * val firstComponent = collection.get(Component::class.java)
 * ```
 *
 * ### Type-Filtered Retrieval
 * ```kotlin
 * // Get as a provider, handling case where type not in collection
 * val maybeService = collection.ofType(ServiceComponent::class.java)
 * val service = maybeService.getOrNull()
 * ```
 *
 * ### Filtering and Transformation
 * ```kotlin
 * val filtered = collection
 *     .filter(Predicate { it.isEnabled })
 *     .map(Transformer { it.name })
 * ```
 *
 * ### Iteration
 * ```kotlin
 * for (item in collection) {
 *     println(item)
 * }
 * ```
 *
 * @param T the base type of objects in this collection
 * @see MutableObjectCollection for mutable collections
 * @see NamedObjectCollection for named collections
 * @throws AliceException on type retrieval errors
 */
interface ObjectCollection<T : Any> : Iterable<T> {
    /**
     * Returns the number of objects in this collection.
     */
    val size: Int

    /**
     * Checks if this collection is empty.
     */
    val isEmpty: Boolean

    /**
     * Retrieves an object of the exact type from this collection.
     *
     * If no object of the given type exists in the collection,
     * an exception is thrown.
     *
     * @param type the class of the object to retrieve
     * @return the object of the given type
     * @throws AliceException if no object of the given type exists
     *
     * @sample
     * ```kotlin
     * val service = collection.get(MyService::class.java)
     * ```
     */
    @Throws(AliceException::class)
    fun <R : T> get(type: Class<R>): R

    /**
     * Retrieves an object of the given type as a provider.
     *
     * If the object doesn't exist, the provider is empty but doesn't throw.
     *
     * @param type the class of the object to retrieve
     * @return a provider containing the object if present
     *
     * @sample
     * ```kotlin
     * val service = collection
     *     .ofType(MyService::class.java)
     *     .getOrNull()
     * ```
     */
    fun <R : T> ofType(type: Class<R>): Provider<R>

    /**
     * Returns a new collection with elements transformed using the given transformer.
     *
     * @param transformer the transformation function
     * @return a new collection with transformed elements
     *
     * @sample
     * ```kotlin
     * val names = collection.map(Transformer { it.name })
     * ```
     */
    fun <R : Any> map(transformer: Transformer<T, R>): ObjectCollection<R>

    /**
     * Returns a new collection with elements transformed using the given transformer,
     * then flattened.
     *
     * @param transformer the transformation function returning iterables
     * @return a new flattened collection
     */
    fun <R : Any> flatMap(transformer: Transformer<T, Iterable<R>>): ObjectCollection<R>

    /**
     * Returns a new collection containing only elements matching the predicate.
     *
     * @param predicate the predicate to test elements
     * @return a new filtered collection
     *
     * @sample
     * ```kotlin
     * val enabled = collection.filter(Predicate { it.isActive })
     * ```
     */
    fun filter(predicate: Predicate<T>): ObjectCollection<T>
}

/**
 * Mutable collection of objects supporting add/remove operations.
 *
 * MutableObjectCollections extend [ObjectCollection] with runtime addition and removal
 * of objects by type. This allows dynamic composition of components.
 *
 * ## Usage Examples
 *
 * ### Adding Objects
 * ```kotlin
 * val collection: MutableObjectCollection<Component> = // ...
 *
 * // Add by class with constructor arguments
 * collection.add(MyComponent::class.java, "arg1", 42)
 * ```
 *
 * ### Removing Objects
 * ```kotlin
 * collection.remove(MyComponent::class.java)
 * ```
 *
 * ### Chained Operations
 * ```kotlin
 * val result = collection
 *     .filter(Predicate { it.isEnabled })
 *     .map(Transformer { it.descriptor })
 * ```
 *
 * @param T the base type of objects in this collection
 * @see ObjectCollection for immutable collections
 * @see NamedObjectCollection for named collections
 */
interface MutableObjectCollection<T : Any> : ObjectCollection<T> {
    /**
     * Removes an object of the given type from this collection.
     *
     * @param value the class of the object to remove
     */
    fun <R : T> remove(value: Class<R>)

    /**
     * Adds an object of the given type to this collection.
     *
     * The object is instantiated using the provided constructor arguments.
     *
     * @param value the class of the object to add
     * @param constructor the constructor arguments
     *
     * @sample
     * ```kotlin
     * collection.add(MyService::class.java, "config.yaml")
     * ```
     */
    fun <R : T> add(value: Class<R>, vararg constructor: Any)

    override fun <R : Any> map(transformer: Transformer<T, R>): MutableObjectCollection<R>

    override fun <R : Any> flatMap(transformer: Transformer<T, Iterable<R>>): MutableObjectCollection<R>

    override fun filter(predicate: Predicate<T>): MutableObjectCollection<T>
}

/**
 * Collection of objects accessible by unique string names.
 *
 * NamedObjectCollections extend [ObjectCollection] with name-based lookup,
 * enabling registry-like access patterns. Objects have unique names within
 * the collection.
 *
 * ## Usage Examples
 *
 * ### Named Lookup
 * ```kotlin
 * val services: NamedObjectCollection<Service> = // ...
 *
 * val userService = services.named("user-service").get()
 * val maybePay = services.named("payment-service").getOrNull()
 * ```
 *
 * ### Getting All Entries
 * ```kotlin
 * val allMapped = services.mapped() // Map<String, Service>
 * val keys = services.keys() // Set<String>
 * val values = services.values() // Collection<Service>
 * val entries = services.entries() // Set<Map.Entry<String, Service>>
 * ```
 *
 * ### Iteration with Names
 * ```kotlin
 * for ((name, service) in services.entries()) {
 *     println("Service $name: $service")
 * }
 * ```
 *
 * ### Filtering by Name and Value
 * ```kotlin
 * val active = services.filter(Predicate { it.isRunning })
 * val mapped = services.map(Transformer { it.status })
 * ```
 *
 * @param T the type of objects in this collection
 * @see MutableNamedObjectCollection for mutable named collections
 * @see ObjectCollection for unnamed collections
 */
interface NamedObjectCollection<T : Any> : ObjectCollection<T> {
    /**
     * Retrieves an object by its unique name.
     *
     * @param name the name of the object
     * @return a provider containing the object if present
     *
     * @sample
     * ```kotlin
     * val service = collection.named("database-service").get()
     * ```
     */
    fun named(name: String): Provider<T>

    /**
     * Retrieves an object by name and type.
     *
     * @param name the name of the object
     * @param type the class of the object
     * @return a provider containing the object if present and of the correct type
     */
    fun <R : T> named(name: String, type: Class<R>): Provider<R>

    override fun filter(predicate: Predicate<T>): NamedObjectCollection<T>

    override fun <R : Any> map(transformer: Transformer<T, R>): NamedObjectCollection<R>

    /**
     * Returns a map of all names to their corresponding objects.
     *
     * @return a map with string keys and object values
     */
    fun mapped(): Map<String, T>

    /**
     * Returns all name-object pairs as entries.
     *
     * @return a set of map entries
     */
    fun entries(): Set<Map.Entry<String, T>>

    /**
     * Returns all object names.
     *
     * @return a set of names
     */
    fun keys(): Set<String>

    /**
     * Returns all objects as a collection.
     *
     * @return a collection of objects
     */
    fun values(): Collection<T>
}

/**
 * Mutable named collection of objects supporting add/remove operations by name.
 *
 * MutableNamedObjectCollections combine named lookup with add/remove operations,
 * allowing dynamic registration and deregistration of named components.
 *
 * ## Usage Examples
 *
 * ### Adding Named Objects
 * ```kotlin
 * val collection: MutableNamedObjectCollection<Component> = // ...
 *
 * // Add by class with constructor arguments
 * collection.add("service-1", MyService::class.java, "config.yaml")

 * // Add instance directly
 * val instance = MyService()
 * collection.add("service-2", instance)
 * ```
 *
 * ### Removing Objects
 * ```kotlin
 * collection.remove("service-1")
 * ```
 *
 * ### Name Lookup with Filtering
 * ```kotlin
 * val result = collection
 *     .filter(Predicate { it.isHealthy })
 *     .map(Transformer { it.getStatus() })
 * ```
 *
 * ## Lifecycle
 *
 * When adding objects:
 * 1. Object is registered with the given name
 * 2. Must have a unique name in the collection
 * 3. Removing a name also removes the associated object
 *
 * @param T the type of objects in this collection
 * @see NamedObjectCollection for immutable named collections
 * @see MutableObjectCollection for mutable unnamed collections
 */
interface MutableNamedObjectCollection<T : Any> : NamedObjectCollection<T> {

    /**
     * Removes the object with the given name.
     *
     * @param name the name of the object to remove
     */
    fun remove(name: String)

    /**
     * Adds an object of the given type with the specified name.
     *
     * The object is instantiated using the provided constructor arguments.
     *
     * @param name the unique name for the object
     * @param value the class of the object to add
     * @param constructor the constructor arguments
     *
     * @sample
     * ```kotlin
     * collection.add("database", DatabaseService::class.java, "db.config")
     * ```
     */
    fun <R : T> add(name: String, value: Class<R>, vararg constructor: Any)

    /**
     * Adds an existing object instance with the specified name.
     *
     * @param name the unique name for the object
     * @param value the object instance to add
     *
     * @sample
     * ```kotlin
     * val service = MyService()
     * collection.add("my-service", service)
     * ```
     */
    fun <R : T> add(name: String, value: R)

    override fun <R : Any> map(transformer: Transformer<T, R>): MutableNamedObjectCollection<R>

    override fun <R : Any> flatMap(transformer: Transformer<T, Iterable<R>>): MutableObjectCollection<R>

    override fun filter(predicate: Predicate<T>): MutableNamedObjectCollection<T>

}
