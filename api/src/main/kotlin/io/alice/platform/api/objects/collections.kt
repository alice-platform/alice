package io.alice.platform.api.objects

import io.alice.platform.api.AliceException
import io.alice.platform.api.Predicate
import io.alice.platform.api.Transformer

interface ObjectCollection<T : Any> : Iterable<T> {
    val size: Int

    val isEmpty: Boolean

    @Throws(AliceException::class)
    fun <R : T> get(type: Class<R>): R

    fun <R : T> ofType(type: Class<R>): Provider<R>

    fun <R : Any> map(transformer: Transformer<T, R>): ObjectCollection<R>

    fun <R : Any> flatMap(transformer: Transformer<T, Iterable<R>>): ObjectCollection<R>

    fun filter(predicate: Predicate<T>): ObjectCollection<T>
}

interface MutableObjectCollection<T : Any> : ObjectCollection<T> {
    fun <R : T> remove(value: Class<R>)

    fun <R : T> add(value: Class<R>, vararg constructor: Any)

    override fun <R : Any> map(transformer: Transformer<T, R>): MutableObjectCollection<R>

    override fun <R : Any> flatMap(transformer: Transformer<T, Iterable<R>>): MutableObjectCollection<R>

    override fun filter(predicate: Predicate<T>): MutableObjectCollection<T>
}

interface NamedObjectCollection<T : Any> : ObjectCollection<T> {
    fun named(name: String): Provider<T>
    fun <R : T> named(name: String, type: Class<R>): Provider<R>

    override fun filter(predicate: Predicate<T>): NamedObjectCollection<T>
    override fun <R : Any> map(transformer: Transformer<T, R>): NamedObjectCollection<R>

    fun mapped(): Map<String, T>
    fun entries(): Set<Map.Entry<String, T>>
    fun keys(): Set<String>
    fun values(): Collection<T>
}

interface MutableNamedObjectCollection<T : Any> : NamedObjectCollection<T> {

    fun remove(name: String)

    fun <R : T> add(name: String, value: Class<R>, vararg constructor: Any)
    fun <R : T> add(name: String, value: R)

    override fun <R : Any> map(transformer: Transformer<T, R>): MutableNamedObjectCollection<R>
    override fun <R : Any> flatMap(transformer: Transformer<T, Iterable<R>>): MutableObjectCollection<R>

    override fun filter(predicate: Predicate<T>): MutableNamedObjectCollection<T>

}
