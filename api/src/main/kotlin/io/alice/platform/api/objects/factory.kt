package io.alice.platform.api.objects

import io.alice.platform.api.AliceObject
import io.alice.platform.api.Supplier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

interface ObjectFactory : AliceObject {
    val provider: ProviderFactory
    val property: PropertyFactory
    val collections: CollectionFactory
    val scripts: ScriptManager
    fun <T : Any> convert(data: Any?, type: Class<T>): Provider<T>
    fun <T : Any> convertMutable(data: Any?, type: Class<T>): Property<T>

}

interface ProviderFactory {
    fun <T : Any> ofSupplied(target: Supplier<T>): Provider<T>
    fun <T : Any> ofNullable(target: T?): Provider<T>
    fun <T : Any> of(target: T): Provider<T>
    fun <T : Any> empty(throwable: Supplier<Throwable> = Supplier { NullPointerException("No value provided.") }): Provider<T>

    fun <T : Any> many(vararg values: T): IterableProvider<T>
    fun <T : Any> many(values: Iterable<T>): IterableProvider<T>

    fun <K : Any, V : Any> map(values: Map<K, V>): MapProvider<K, V>
    fun <K : Any, V : Any> map(vararg values: Pair<K, V>): MapProvider<K, V>
    fun <T : Any> fromFuture(future: Future<T>, executor: ExecutorService = Executors.newSingleThreadExecutor()): Provider<T>
}

interface PropertyFactory : ProviderFactory {
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

interface CollectionFactory {
    fun <T : Any> mutableList(): MutableObjectCollection<T>
    fun <T : Any> list(vararg values: T): ObjectCollection<T>
    fun <T : Any> list(values: Collection<T>): ObjectCollection<T>
    fun <T : Any> named(vararg values: Pair<String, T>): NamedObjectCollection<T>
    fun <T : Any> named(values: Map<String, T>): NamedObjectCollection<T>
    fun <T : Any> mutableNamed(): MutableNamedObjectCollection<T>
}

