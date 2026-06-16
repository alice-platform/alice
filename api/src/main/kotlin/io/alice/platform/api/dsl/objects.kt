package io.alice.platform.api.dsl

import io.alice.platform.api.AliceException
import io.alice.platform.api.objects.*
import kotlin.reflect.KClass

@JvmName("ofResult")
fun <T : Any> ProviderFactory.of(result: Result<T>): Provider<T> =
    ofSupplied { result.getOrThrow() }

@JvmName("ofResult")
fun <T : Any> PropertyFactory.of(result: Result<T>): Property<T> =
    ofSupplied { result.getOrThrow() }

fun <T : Any> ObjectCollection<Any>.ofType(type: KClass<T>): Provider<T> =
    ofType(type.java)

@Throws(AliceException::class)
fun <T : Any> ObjectCollection<Any>.get(type: KClass<T>): T =
    get(type.java)

inline fun <R : Any, reified T : R> ObjectCollection<R>.ofType(): Provider<T> =
    ofType(T::class.java)

@Throws(AliceException::class)
inline fun <R : Any, reified T : R> ObjectCollection<R>.getting(): T =
    get(T::class.java)

fun <T : Any> NamedObjectCollection<Any>.named(name: String, type: KClass<T>): Provider<T> =
    named(name, type.java)

@JvmName("inlineNamed")
inline fun <R : Any, reified T : R> NamedObjectCollection<R>.named(name: String): Provider<T> =
    named(name, T::class.java)

fun <T> Property<T>.assign(value: T) = set(value)

fun <T> Provider<T>.toResult() = try {
    Result.success(getOrThrow())
} catch (e: Throwable) {
    Result.failure(e)
}

fun DoubleProperty.plusAssign(value: Double) = plus(value)
fun DoubleProperty.minusAssign(value: Double) = minus(value)
fun DoubleProperty.timesAssign(value: Double) = times(value)
fun DoubleProperty.divAssign(value: Double) = div(value)
fun DoubleProperty.remAssign(value: Double) = rem(value)

fun DoubleProperty.plus(value: Double): DoubleProperty = map { it + value }
fun DoubleProperty.minus(value: Double): DoubleProperty = map { it - value }
fun DoubleProperty.times(value: Double): DoubleProperty = map { it * value }
fun DoubleProperty.div(value: Double): DoubleProperty = map { it / value }
fun DoubleProperty.rem(value: Double): DoubleProperty = map { it % value }

fun FloatProperty.plusAssign(value: Float) = plus(value)
fun FloatProperty.minusAssign(value: Float) = minus(value)
fun FloatProperty.timesAssign(value: Float) = times(value)
fun FloatProperty.divAssign(value: Float) = div(value)
fun FloatProperty.remAssign(value: Float) = rem(value)

fun FloatProperty.plus(value: Float): FloatProperty = map { it + value }
fun FloatProperty.minus(value: Float): FloatProperty = map { it - value }
fun FloatProperty.times(value: Float): FloatProperty = map { it * value }
fun FloatProperty.div(value: Float): FloatProperty = map { it / value }
fun FloatProperty.rem(value: Float): FloatProperty = map { it % value }

fun IntProperty.plusAssign(value: Int) = plus(value)
fun IntProperty.minusAssign(value: Int) = minus(value)
fun IntProperty.timesAssign(value: Int) = times(value)
fun IntProperty.divAssign(value: Int) = div(value)
fun IntProperty.remAssign(value: Int) = rem(value)

fun IntProperty.plus(value: Int): IntProperty = map { it + value }
fun IntProperty.minus(value: Int): IntProperty = map { it - value }
fun IntProperty.times(value: Int): IntProperty = map { it * value }
fun IntProperty.div(value: Int): IntProperty = map { it / value }
fun IntProperty.rem(value: Int): IntProperty = map { it % value }

fun LongProperty.plusAssign(value: Long) = plus(value)
fun LongProperty.minusAssign(value: Long) = minus(value)
fun LongProperty.timesAssign(value: Long) = times(value)
fun LongProperty.divAssign(value: Long) = div(value)
fun LongProperty.remAssign(value: Long) = rem(value)

fun LongProperty.plus(value: Long): LongProperty = map { it + value }
fun LongProperty.minus(value: Long): LongProperty = map { it - value }
fun LongProperty.times(value: Long): LongProperty = map { it * value }
fun LongProperty.div(value: Long): LongProperty = map { it / value }
fun LongProperty.rem(value: Long): LongProperty = map { it % value }
