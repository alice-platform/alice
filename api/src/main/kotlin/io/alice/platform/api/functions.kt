package io.alice.platform.api

import org.slf4j.Logger
import java.util.*
import java.util.function.Function

fun interface Predicate<T> {
    fun test(target: T): Boolean

    infix fun and(predicate: Predicate<T>): Predicate<T> = Predicate {
        this.test(it) and predicate.test(it)
    }

    infix fun or(predicate: Predicate<T>): Predicate<T> = Predicate {
        this.test(it) or predicate.test(it)
    }

    infix fun xor(predicate: Predicate<T>): Predicate<T> = Predicate {
        this.test(it) xor predicate.test(it)
    }

    operator fun not(): Predicate<T> = Predicate {
        this.test(it).not()
    }

    companion object {
        @JvmStatic
        fun <T> cast(predicate: java.util.function.Predicate<T>) = Predicate<T> {
            predicate.test(it)
        }
    }
}

fun interface BiConsumer<T, U> {
    fun consume(t1: T, t2: U)

    fun chain(consumer: BiConsumer<T, U>): BiConsumer<T, U> = BiConsumer { a, b ->
        this.consume(a, b)
        consumer.consume(a, b)
    }

    companion object {
        @JvmStatic
        fun <T, U> cast(consumer: java.util.function.BiConsumer<T, U>): BiConsumer<T, U> = BiConsumer { t1, t2 ->
            consumer.accept(t1, t2)
        }
    }
}

fun interface Consumer<T> {
    fun consume(target: T)

    fun chain(consumer: Consumer<T>): Consumer<T> = Consumer {
        this.consume(it)
        consumer.consume(it)
    }

    companion object {
        @JvmStatic
        fun <T> cast(consumer: java.util.function.Consumer<T>): Consumer<T> = Consumer {
            consumer.accept(it)
        }
    }
}

fun interface Supplier<T> {
    @Throws(Exception::class)
    fun get(): T

    companion object {
        @JvmStatic
        fun <T> cast(supplier: java.util.function.Supplier<T>): Supplier<T> = Supplier {
            supplier.get()
        }
    }
}

fun interface Transformer<IN, OUT> {
    fun transform(input: IN): OUT

    companion object {
        @JvmStatic
        fun <IN, OUT> cast(fn: Function<IN, OUT>): Transformer<IN, OUT> = Transformer {
            fn.apply(it)
        }
    }
}

fun interface Runnable {
    suspend fun run()

    companion object {
        @JvmStatic
        fun cast(fn: java.lang.Runnable) = Runnable {
            fn.run()
        }
    }
}

fun interface Closeable {
    suspend fun close()

    companion object {
        @JvmStatic
        fun <IN, OUT> cast(fn: AutoCloseable) = Closeable {
            fn.close()
        }

        @JvmStatic
        fun <IN, OUT> cast(fn: java.io.Closeable) = Closeable {
            fn.close()
        }
    }
}

interface IO : Runnable, Closeable

interface AliceObject {
    /**
     * Main logger
     */
    val logger: Logger

    /**
     * Root Component
     */
    val alice: Alice
}

interface AliceObjectOperator : AliceObject, IO

interface Version : Comparable<Version> {
    val major: Int
    val minor: Int
    val patch: Int
    val release: String?

    companion object {
        private val MATCHER = Regex("([0-9]+\\.){1,2}[0-9]+(?:-.+)?")

        @JvmStatic
        fun current() = of(System.getProperty("alice.platform.version"))

        @JvmStatic
        @JvmOverloads
        fun of(major: Int, minor: Int, patch: Int, release: String? = null): Version =
            VersionImpl(major, minor, patch, release)

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

@DslMarker
annotation class AliceDsl
