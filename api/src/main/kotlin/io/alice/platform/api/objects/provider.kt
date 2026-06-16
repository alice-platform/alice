package io.alice.platform.api.objects

import io.alice.platform.api.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

interface Property<T> : ReadWriteProperty<Any?, T>, Provider<T> {
    fun set(value: Provider<T>)
    fun set(value: T?)

    fun delegate(value: Provider<T>): Property<T>
    fun delegate(value: T?): Property<T>
    override fun ifPresent(consumer: Consumer<T>): Property<T>
    override fun <R : Any> map(transformer: Transformer<T, R>): Property<R>

    override fun <R : Any> flatMap(transformer: Transformer<T, Provider<R>>): Property<R>

    override fun onSuccess(function: Consumer<T>): Property<T>
    override fun onError(function: Consumer<Throwable>): Property<T>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): Property<T>
    override fun onCompleted(function: Runnable): Property<T>
    fun onUpdate(function: BiConsumer<T?, T?>): Property<T>
}

interface Provider<T> : ReadOnlyProperty<Any?, T> {
    val isPresent: Boolean

    fun get(): T

    fun getOrElse(default: T): T

    fun getOrNull(): T?

    fun getOrThrow(@Suppress("UNCHECKED_CAST") throwable: Supplier<Throwable> = { NullPointerException() } as Supplier<Throwable>): T

    fun ifPresent(consumer: Consumer<T>): Provider<T>

    fun <R : Any> map(transformer: Transformer<T, R>): Provider<R>

    fun <R : Any> flatMap(transformer: Transformer<T, Provider<R>>): Provider<R>

    fun onSuccess(function: Consumer<T>): Provider<T>
    fun onError(function: Consumer<Throwable>): Provider<T>
    fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): Provider<T>
    fun onCompleted(function: Runnable): Provider<T>

    operator fun invoke(function: T.() -> Unit)
}

interface IterableProvider<T> : Provider<Iterable<T>>, Iterable<T> {
    val size: Int
    val isEmpty: Boolean
    override fun ifPresent(consumer: Consumer<Iterable<T>>): IterableProvider<T>
    fun get(index: Int): Provider<T>
    fun getOrDefault(index: Int, default: T): Provider<T>
    fun getOrDefault(index: Int, default: Provider<T>): Provider<T>
    fun <R : Any> mapEach(transformer: Transformer<T, R>): IterableProvider<R>
    fun <R : Any> flatMapEach(transformer: Transformer<T, Iterable<R>>): IterableProvider<R>

    fun onSuccessEach(function: Consumer<T>): IterableProvider<T>
    override fun onSuccess(function: Consumer<Iterable<T>>): IterableProvider<T>
    override fun onError(function: Consumer<Throwable>): IterableProvider<T>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): IterableProvider<T>
    override fun onCompleted(function: Runnable): IterableProvider<T>
}

interface IterableProperty<T> : IterableProvider<T>, Property<Iterable<T>> {
    override fun get(index: Int): Property<T>
    override fun ifPresent(consumer: Consumer<Iterable<T>>): IterableProperty<T>
    override fun getOrDefault(index: Int, default: T): Property<T>
    override fun getOrDefault(index: Int, default: Provider<T>): Property<T>
    override fun <R : Any> mapEach(transformer: Transformer<T, R>): IterableProperty<R>
    override fun <R : Any> flatMapEach(transformer: Transformer<T, Iterable<R>>): IterableProperty<R>
    fun clear()
    fun add(value: T)
    operator fun plusAssign(value: T)

    override fun delegate(value: Provider<Iterable<T>>): IterableProperty<T>
    override fun delegate(value: Iterable<T>?): IterableProperty<T>

    override fun onSuccessEach(function: Consumer<T>): IterableProperty<T>
    override fun onSuccess(function: Consumer<Iterable<T>>): IterableProperty<T>
    override fun onError(function: Consumer<Throwable>): IterableProperty<T>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): IterableProperty<T>
    override fun onCompleted(function: Runnable): IterableProperty<T>
    override fun onUpdate(function: BiConsumer<Iterable<T>?, Iterable<T>?>): IterableProperty<T>
    fun onEntryUpdate(function: BiConsumer<IndexedValue<T>, IndexedValue<T>>): IterableProperty<T>
}

interface MapProvider<K, V> : Provider<Map<K, V>> {
    val isEmpty: Boolean
        get() = entries.isEmpty
    val keys: IterableProvider<K>
    val values: IterableProvider<V>
    val entries: IterableProvider<Map.Entry<K, V>>
    fun get(key: K): Provider<V>
    fun getOrDefault(key: K, default: V): Provider<V>
    fun getOrDefault(key: K, default: Provider<V>): Provider<V>
    override fun ifPresent(consumer: Consumer<Map<K, V>>): MapProvider<K, V>
    fun <R : Any> mapKeys(transformer: Transformer<K, R>): MapProvider<R, V>
    fun <R : Any> mapValues(transformer: Transformer<V, R>): MapProvider<K, R>
    fun <R : Any> mapIterable(transformer: Transformer<Map.Entry<K, V>, R>): IterableProvider<R>
    fun <R : Any> flatMapIterable(transformer: Transformer<Map.Entry<K, V>, Iterable<R>>): IterableProvider<R>

    fun onSuccessEach(function: BiConsumer<K, V>): MapProvider<K, V>
    override fun onSuccess(function: Consumer<Map<K, V>>): MapProvider<K, V>
    override fun onError(function: Consumer<Throwable>): MapProvider<K, V>
    override fun <X : Throwable> onError(type: Class<X>, function: Consumer<X>): MapProvider<K, V>
    override fun onCompleted(function: Runnable): MapProvider<K, V>
}

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
    fun onEntryUpdate(function: BiConsumer<Map.Entry<K, V>, Map.Entry<K, V>>): MapProperty<K, V>

    fun clear()
    fun put(key: K, value: V)
}

typealias BooleanProvider = Provider<Boolean>
typealias BooleanProperty = Property<Boolean>
typealias DoubleProvider = Provider<Double>
typealias DoubleProperty = Property<Double>
typealias FloatProvider = Provider<Float>
typealias FloatProperty = Property<Float>
typealias IntProvider = Provider<Int>
typealias IntProperty = Property<Int>
typealias LongProvider = Provider<Long>
typealias LongProperty = Property<Long>
typealias VoidProvider = Provider<Unit>
typealias VoidProperty = Property<Unit>
