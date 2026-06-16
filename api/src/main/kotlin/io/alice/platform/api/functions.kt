package io.alice.platform.api

import org.slf4j.Logger
import java.util.*
import java.util.function.Function

/**
 * Functional interface for testing a value against a condition.
 *
 * Predicates support logical composition through `and`, `or`, `xor` operations and negation.
 * They are commonly used for filtering and validation in the Alice platform.
 *
 * ## Usage Examples
 *
 * ### Basic Predicate
 * ```kotlin
 * val isPositive: Predicate<Int> = Predicate { it > 0 }
 * if (isPositive.test(5)) {
 *     println("Number is positive")
 * }
 * ```
 *
 * ### Predicate Composition
 * ```kotlin
 * val isEven: Predicate<Int> = Predicate { it % 2 == 0 }
 * val isPositive: Predicate<Int> = Predicate { it > 0 }
 *
 * // Logical AND
 * val isPositiveEven = isPositive and isEven
 *
 * // Logical OR
 * val isPositiveOrNegative = isPositive or (!isPositive)
 *
 * // Negation
 * val isNotEven = !isEven
 * ```
 *
 * @param T the type of the value to test
 * @see Consumer for side-effect operations
 * @see Transformer for value transformations
 */
fun interface Predicate<T> {
    /**
     * Tests whether the given value satisfies this predicate.
     *
     * @param target the value to test
     * @return `true` if the value satisfies the predicate, `false` otherwise
     */
    fun test(target: T): Boolean

    /**
     * Returns a new predicate that represents the logical AND of this predicate and another.
     *
     * The returned predicate will return `true` only if both this predicate and the given
     * predicate return `true` for the tested value.
     *
     * @param predicate the other predicate to combine with
     * @return a new combined predicate
     *
     * @sample
     * ```kotlin
     * val isPositive = Predicate { it > 0 }
     * val isEven = Predicate { it % 2 == 0 }
     * val combined = isPositive and isEven // Returns true only for positive even numbers
     * ```
     */
    infix fun and(predicate: Predicate<T>): Predicate<T> = Predicate {
        this.test(it) and predicate.test(it)
    }

    /**
     * Returns a new predicate that represents the logical OR of this predicate and another.
     *
     * The returned predicate will return `true` if either this predicate or the given
     * predicate returns `true` for the tested value.
     *
     * @param predicate the other predicate to combine with
     * @return a new combined predicate
     */
    infix fun or(predicate: Predicate<T>): Predicate<T> = Predicate {
        this.test(it) or predicate.test(it)
    }

    /**
     * Returns a new predicate that represents the logical XOR of this predicate and another.
     *
     * The returned predicate will return `true` if exactly one of the predicates
     * returns `true` for the tested value.
     *
     * @param predicate the other predicate to combine with
     * @return a new combined predicate
     */
    infix fun xor(predicate: Predicate<T>): Predicate<T> = Predicate {
        this.test(it) xor predicate.test(it)
    }

    /**
     * Returns a new predicate that represents the logical negation of this predicate.
     *
     * The returned predicate will return the opposite boolean value of this predicate.
     *
     * @return a new negated predicate
     *
     * @sample
     * ```kotlin
     * val isPositive = Predicate { it > 0 }
     * val isNotPositive = !isPositive // Returns true for non-positive numbers
     * ```
     */
    operator fun not(): Predicate<T> = Predicate {
        this.test(it).not()
    }

    companion object {
        /**
         * Converts a Java `Predicate` to an Alice `Predicate`.
         *
         * @param predicate the Java predicate to convert
         * @return an Alice predicate wrapping the Java predicate
         */
        @JvmStatic
        fun <T> cast(predicate: java.util.function.Predicate<T>) = Predicate<T> {
            predicate.test(it)
        }
    }
}

/**
 * Functional interface for consuming two values and performing a side effect.
 *
 * BiConsumers are used for operations that take two arguments and don't return a value,
 * such as accumulation, logging, or event handling.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val logger: BiConsumer<String, String> = BiConsumer { key, value ->
 *     println("Key: $key, Value: $value")
 * }
 *
 * // Chain multiple consumers
 * val combined = logger.chain(BiConsumer { key, value ->
 *     fireAnalyticsEvent(key, value)
 * })
 * ```
 *
 * @param T the type of the first argument
 * @param U the type of the second argument
 * @see Consumer for single-argument consumption
 */
fun interface BiConsumer<T, U> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param t1 the first argument
     * @param t2 the second argument
     */
    fun consume(t1: T, t2: U)

    /**
     * Returns a new BiConsumer that performs this operation followed by another.
     *
     * If either consumer throws an exception, the exception is propagated to the caller
     * and the second consumer is not executed.
     *
     * @param consumer the other consumer to chain
     * @return a new chained BiConsumer
     *
     * @sample
     * ```kotlin
     * val consumer1 = BiConsumer { k: String, v: Int -> println(k) }
     * val consumer2 = BiConsumer { k: String, v: Int -> println(v) }
     * val chained = consumer1.chain(consumer2)
     * chained.consume("key", 42) // Executes both consumers in sequence
     * ```
     */
    fun chain(consumer: BiConsumer<T, U>): BiConsumer<T, U> = BiConsumer { a, b ->
        this.consume(a, b)
        consumer.consume(a, b)
    }

    companion object {
        /**
         * Converts a Java `BiConsumer` to an Alice `BiConsumer`.
         *
         * @param consumer the Java BiConsumer to convert
         * @return an Alice BiConsumer wrapping the Java BiConsumer
         */
        @JvmStatic
        fun <T, U> cast(consumer: java.util.function.BiConsumer<T, U>): BiConsumer<T, U> = BiConsumer { t1, t2 ->
            consumer.accept(t1, t2)
        }
    }
}

/**
 * Functional interface for consuming a single value and performing a side effect.
 *
 * Consumers are used for operations that take one argument and don't return a value,
 * such as logging, event dispatch, or side-effect operations.
 *
 * ## Usage Examples
 *
 * ### Basic Consumer
 * ```kotlin
 * val logger: Consumer<String> = Consumer { message ->
 *     println(message)
 * }
 * logger.consume("Hello, World!")
 * ```
 *
 * ### Consumer Chaining
 * ```kotlin
 * val logConsumer = Consumer { message: String -> println(message) }
 * val analyticsConsumer = Consumer { message: String -> sendToAnalytics(message) }
 *
 * val combined = logConsumer.chain(analyticsConsumer)
 * combined.consume("User action") // Executes both consumers in sequence
 * ```
 *
 * @param T the type of the value to consume
 * @see BiConsumer for two-argument consumption
 * @see Supplier for value production
 */
fun interface Consumer<T> {
    /**
     * Performs this operation on the given value.
     *
     * @param target the value to consume
     */
    fun consume(target: T)

    /**
     * Returns a new Consumer that performs this operation followed by another.
     *
     * If either consumer throws an exception, the exception is propagated to the caller
     * and the second consumer is not executed.
     *
     * @param consumer the other consumer to chain
     * @return a new chained Consumer
     *
     * @sample
     * ```kotlin
     * val consumer1 = Consumer { println(it) }
     * val consumer2 = Consumer { logToFile(it) }
     * val chained = consumer1.chain(consumer2)
     * chained.consume("message") // Executes both consumers in sequence
     * ```
     */
    fun chain(consumer: Consumer<T>): Consumer<T> = Consumer {
        this.consume(it)
        consumer.consume(it)
    }

    companion object {
        /**
         * Converts a Java `Consumer` to an Alice `Consumer`.
         *
         * @param consumer the Java Consumer to convert
         * @return an Alice Consumer wrapping the Java Consumer
         */
        @JvmStatic
        fun <T> cast(consumer: java.util.function.Consumer<T>): Consumer<T> = Consumer {
            consumer.accept(it)
        }
    }
}

/**
 * Functional interface for producing a value, with support for throwing exceptions.
 *
 * Suppliers are used for lazy value computation, factory methods, and deferred operations.
 * Unlike Java's `Supplier`, Alice's `Supplier` allows checked exceptions to propagate.
 *
 * ## Usage Examples
 *
 * ### Basic Supplier
 * ```kotlin
 * val dateSupplier: Supplier<Long> = Supplier { System.currentTimeMillis() }
 * val timestamp = dateSupplier.get()
 * ```
 *
 * ### Supplier with Exception Handling
 * ```kotlin
 * val configSupplier: Supplier<Config> = Supplier {
 *     loadConfigFromFile("/path/to/config")
 *     // May throw IOException or other checked exceptions
 * }
 * ```
 *
 * @param T the type of the value produced by this supplier
 * @see Consumer for value consumption
 * @see Transformer for value transformation
 */
fun interface Supplier<T> {
    /**
     * Produces a value. May throw a checked exception.
     *
     * @return the produced value
     * @throws Exception if an error occurs during value production
     */
    @Throws(Exception::class)
    fun get(): T

    companion object {
        /**
         * Converts a Java `Supplier` to an Alice `Supplier`.
         *
         * @param supplier the Java Supplier to convert
         * @return an Alice Supplier wrapping the Java Supplier
         */
        @JvmStatic
        fun <T> cast(supplier: java.util.function.Supplier<T>): Supplier<T> = Supplier {
            supplier.get()
        }
    }
}

/**
 * Functional interface for transforming a value from one type to another.
 *
 * Transformers are core to the Provider pattern and functional composition,
 * allowing mapping operations across Providers and collections.
 *
 * ## Usage Examples
 *
 * ### Basic Transformation
 * ```kotlin
 * val stringToInt: Transformer<String, Int> = Transformer { it.toInt() }
 * val result = stringToInt.transform("42")
 * ```
 *
 * ### Used with Providers
 * ```kotlin
 * val numberProvider: Provider<String> = factory.of("100")
 * val doubled: Provider<Int> = numberProvider.map(Transformer { it.toInt() * 2 })
 * ```
 *
 * ### Chained Transformations
 * ```kotlin
 * val toInt = Transformer { s: String -> s.toInt() }
 * val double = Transformer { i: Int -> i * 2 }
 * // Chain through provider operations
 * ```
 *
 * @param IN the type of the input value
 * @param OUT the type of the output value
 * @see Provider for functional mapping operations
 */
fun interface Transformer<IN, OUT> {
    /**
     * Transforms the given input value to an output value.
     *
     * @param input the value to transform
     * @return the transformed value
     */
    fun transform(input: IN): OUT

    companion object {
        /**
         * Converts a Java `Function` to an Alice `Transformer`.
         *
         * @param fn the Java Function to convert
         * @return an Alice Transformer wrapping the Java Function
         */
        @JvmStatic
        fun <IN, OUT> cast(fn: Function<IN, OUT>): Transformer<IN, OUT> = Transformer {
            fn.apply(it)
        }
    }
}

/**
 * Functional interface for executing asynchronous operations.
 *
 * Unlike `java.lang.Runnable`, Alice's `Runnable` is a suspend function,
 * allowing integration with Kotlin coroutines for asynchronous execution.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val operation: Runnable = Runnable {
 *     delay(1000) // Suspend for 1 second
 *     println("Operation completed")
 * }
 * ```
 *
 * ## Thread Safety
 * Implementations should handle their own synchronization if needed.
 *
 * @see Closeable for resource cleanup
 * @see IO for combined operations
 */
fun interface Runnable {
    /**
     * Executes the operation asynchronously.
     *
     * This is a suspend function and should be called from within a coroutine context.
     */
    suspend fun run()

    companion object {
        /**
         * Converts a Java `Runnable` to an Alice `Runnable`.
         *
         * The Java runnable is executed synchronously within the suspend context.
         *
         * @param fn the Java Runnable to convert
         * @return an Alice Runnable wrapping the Java Runnable
         */
        @JvmStatic
        fun cast(fn: java.lang.Runnable) = Runnable {
            fn.run()
        }
    }
}

/**
 * Functional interface for closing/cleaning up resources asynchronously.
 *
 * Unlike `java.lang.AutoCloseable`, Alice's `Closeable` is a suspend function,
 * allowing non-blocking resource cleanup in coroutine contexts.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val resource: Closeable = Closeable {
 *     withContext(Dispatchers.IO) {
 *         database.close()
 *     }
 * }
 *
 * // Use with try-finally pattern
 * try {
 *     // Use resource
 * } finally {
 *     resource.close()
 * }
 * ```
 *
 * ## Implementation Notes
 * - Should be idempotent (safe to call multiple times)
 * - Should not throw exceptions; catch and log internally
 * - Must be called within a coroutine context
 *
 * @see Runnable for generic asynchronous operations
 * @see IO for combined initialization and cleanup
 */
fun interface Closeable {
    /**
     * Closes/cleans up this resource asynchronously.
     *
     * This is a suspend function and should be called from within a coroutine context.
     * Should be idempotent and safe to call multiple times.
     */
    suspend fun close()

    companion object {
        /**
         * Converts a Java `AutoCloseable` to an Alice `Closeable`.
         *
         * @param fn the Java AutoCloseable to convert
         * @return an Alice Closeable wrapping the Java AutoCloseable
         */
        @JvmStatic
        fun <IN, OUT> cast(fn: AutoCloseable) = Closeable {
            fn.close()
        }

        /**
         * Converts a Java `java.io.Closeable` to an Alice `Closeable`.
         *
         * @param fn the Java Closeable to convert
         * @return an Alice Closeable wrapping the Java Closeable
         */
        @JvmStatic
        fun <IN, OUT> cast(fn: java.io.Closeable) = Closeable {
            fn.close()
        }
    }
}

/**
 * Combined interface for objects that support both initialization (run) and cleanup (close) operations.
 *
 * Used for resources that need both setup and teardown phases in their lifecycle.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val service: IO = object : IO {
 *     override suspend fun run() {
 *         // Initialize service
 *         database.connect()
 *     }
 *
 *     override suspend fun close() {
 *         // Cleanup service
 *         database.disconnect()
 *     }
 * }
 *
 * try {
 *     service.run() // Initialize
 *     // Use service
 * } finally {
 *     service.close() // Cleanup
 * }
 * ```
 *
 * @see Runnable for initialization
 * @see Closeable for resource cleanup
 */
interface IO : Runnable, Closeable

/**
 * Base interface for all Alice platform objects.
 *
 * Every object in the Alice platform should implement this interface to gain access
 * to the logger and the root Alice component, enabling consistent logging and
 * access to platform services.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * class MyComponent : AliceObject {
 *     override val logger: Logger
 *         get() = // provided by implementation
 *
 *     override val alice: Alice
 *         get() = // provided by implementation
 *
 *     fun doWork() {
 *         logger.info("Starting work")
 *         val config = alice.configuration
 *     }
 * }
 * ```
 *
 * @property logger the SLF4J logger for this component
 * @property alice reference to the root Alice platform component
 * @see AliceObjectOperator for objects that also support IO operations
 */
interface AliceObject {
    /**
     * Main logger for this component.
     *
     * This is an SLF4J logger instance that should be used for all logging
     * within this component.
     */
    val logger: Logger

    /**
     * Root Component of the Alice platform.
     *
     * Provides access to all platform services including configuration,
     * engines, modules, objects, events, and extensions.
     */
    val alice: Alice
}

/**
 * Extended interface for Alice objects that support lifecycle operations.
 *
 * Objects implementing this interface can be started and stopped, making them
 * suitable for long-running services, background workers, and resource-intensive components.
 *
 * @see IO for the lifecycle interface definition
 * @see AliceObject for the base interface
 */
interface AliceObjectOperator : AliceObject, IO

/**
 * Immutable version information following semantic versioning.
 *
 * Versions are compared using semantic versioning rules: major > minor > patch > release.
 * This interface provides comprehensive version comparison and parsing capabilities.
 *
 * ## Version Format
 *
 * Supported formats:
 * - `1.2.3` (major.minor.patch)
 * - `1.2.3-ALPHA` (with release identifier)
 *
 * Release identifiers are optional and typically indicate pre-release versions (e.g., ALPHA, BETA, RC).
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val v1 = Version.of(1, 2, 3)
 * val v2 = Version.of(1, 2, 4)
 * val v3 = Version.of("1.2.3-ALPHA")
 *
 * println(v1 < v2) // true
 * println(v1.compareTo(v3) > 0) // true, release versions are greater than pre-release
 * ```
 *
 * @property major the major version number
 * @property minor the minor version number
 * @property patch the patch version number
 * @property release optional release identifier (e.g., "ALPHA", "BETA")
 * @see Comparable for comparison contract
 */
interface Version : Comparable<Version> {
    /**
     * Major version number (X in X.Y.Z).
     */
    val major: Int

    /**
     * Minor version number (Y in X.Y.Z).
     */
    val minor: Int

    /**
     * Patch version number (Z in X.Y.Z).
     */
    val patch: Int

    /**
     * Optional release identifier (e.g., "ALPHA", "BETA", "RC1").
     */
    val release: String?

    companion object {
        private val MATCHER = Regex("([0-9]+\\.){1,2}[0-9]+(?:-.+)?")

        /**
         * Gets the current Alice platform version from system properties.
         *
         * @return the current version
         * @throws IllegalArgumentException if the version property is invalid
         */
        @JvmStatic
        fun current() = of(System.getProperty("alice.platform.version"))

        /**
         * Creates a version from individual components.
         *
         * @param major the major version number
         * @param minor the minor version number
         * @param patch the patch version number
         * @param release optional release identifier
         * @return a new Version instance
         */
        @JvmStatic
        @JvmOverloads
        fun of(major: Int, minor: Int, patch: Int, release: String? = null): Version =
            VersionImpl(major, minor, patch, release)

        /**
         * Parses a version string in the format `X.Y.Z` or `X.Y.Z-RELEASE`.
         *
         * @param raw the version string to parse
         * @return a new Version instance
         * @throws IllegalArgumentException if the version string format is invalid
         *
         * @sample
         * ```kotlin
         * val v1 = Version.of("1.2.3")
         * val v2 = Version.of("1.2.3-ALPHA")
         * ```
         */
        @JvmStatic
        fun of(raw: String): Version {
            if (raw.matches(MATCHER)) {
                var (major, minor, patch) = raw.split('.')
                var release: String? = null

                if (patch.contains('-')) {
                    val pSplit = patch.split('-', limit = 2)
                    patch = pSplit[0]
                    release = pSplit[1]
                }

                return of(major.toInt(), minor.toInt(), patch.toInt(), release?.uppercase())
            } else {
                throw IllegalArgumentException("Version string doesn't even matched to [<major>.<minor>.<patch>] - actual: $raw")
            }
        }
    }
}

private class VersionImpl(
    override val major: Int,
    override val minor: Int,
    override val patch: Int,
    override val release: String?
) : Version {

    override fun compareTo(other: Version): Int =
        when {
            this === other -> 0
            major != other.major -> major.compareTo(other.major)
            minor != other.minor -> minor.compareTo(other.minor)
            patch != other.patch -> patch.compareTo(other.patch)
            release != null && other.release != null && release != other.release -> release.compareTo(other.release!!)
            else -> 0
        }

    override fun equals(other: Any?): Boolean = when {
        other != null && other is Version -> other.compareTo(this) == 0
        else -> false
    }

    override fun hashCode(): Int {
        return Objects.hash(major, minor, patch, release)
    }

    override fun toString(): String {
        return "$major.$minor.$patch${if (release != null) "-$release" else ""}"
    }
}

/**
 * Marker annotation for Alice DSL functions.
 *
 * Functions marked with this annotation are intended to be used with Kotlin's
 * domain-specific language (DSL) features, enabling clean, readable configuration syntax.
 *
 * ## Usage
 *
 * ```kotlin
 * @AliceDsl
 * fun configuration(block: Consumer<ConfigurationProvider>) {
 *     // DSL implementation
 * }
 * ```
 */
@DslMarker
annotation class AliceDsl
