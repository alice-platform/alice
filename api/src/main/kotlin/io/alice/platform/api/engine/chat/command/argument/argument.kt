package io.alice.platform.api.engine.chat.command.argument

import io.alice.platform.api.LateInit
import io.alice.platform.api.Predicate
import io.alice.platform.api.Supplier
import io.alice.platform.api.Transformer
import io.alice.platform.api.engine.chat.command.AbstractCliCommand
import io.alice.platform.api.engine.chat.command.CliCommandException
import io.alice.platform.api.engine.chat.command.ParameterHolder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Argument {
  val name: String
  val length: Int
  val required: Boolean
  val description: String?

  fun printParameterDetails(): ArgumentDetails?
  fun finalize(command: AbstractCliCommand<*, *>, values: List<String>)
  fun postValidate(command: AbstractCliCommand<*, *>)
}

interface ArgumentDelegate<out T> : Argument, ReadOnlyProperty<ParameterHolder, T> {
  operator fun provideDelegate(thisRef: ParameterHolder, prop: KProperty<*>): ReadOnlyProperty<ParameterHolder, T>
}

class ArgumentTransformer(
  val argument: Argument,
  val command: AbstractCliCommand<*, *>
) : Argument by argument {
  fun fail(message: String): Nothing = throw WrongArgumentValue(message, argument)

  fun message(message: String) = command.message(message)

  fun require(value: Boolean, lazyMessage: Supplier<String> = Supplier { "" }) {
    if (!value) fail(lazyMessage.get())
  }
}

typealias ArgumentValueTransformer<T> = ArgumentValueConverter<String, T>
typealias ArgumentValueConverter<In, Out> = ArgumentTransformer.(In) -> Out
typealias ArgumentCallsTransformer<All, Each> = ArgumentTransformer.(List<Each>) -> All
typealias ArgumentValidator<T> = ArgumentTransformer.(T) -> Unit

class ArgumentProcessor<All, Value>(
  name: String,
  override val length: Int,
  override val required: Boolean,
  override val description: String?,
  val transformValue: ArgumentValueTransformer<Value>,
  val transformAll: ArgumentCallsTransformer<All, Value>,
  val validator: ArgumentValidator<All>
) : ArgumentDelegate<All> {

  override var name: String = name
    private set

  internal var value: All by LateInit { IllegalStateException("Cannot read from argument delegate before parsing command line") }
    private set

  init {
    require(length != 0) { "Arguments cannot be equal then 0" }
  }

  override fun getValue(thisRef: ParameterHolder, property: KProperty<*>): All = value


  override fun printParameterDetails() = ArgumentDetails(
    name, description, required || length > 1, length, length < 0
  )

  override fun finalize(command: AbstractCliCommand<*, *>, values: List<String>) {
    val ctx = ArgumentTransformer(this, command)
    value = transformAll(ctx, values.map { transformValue(ctx, it) })
  }

  override fun postValidate(command: AbstractCliCommand<*, *>) {
    validator(ArgumentTransformer(this, command), value)
  }

  override fun provideDelegate(thisRef: ParameterHolder, prop: KProperty<*>): ReadOnlyProperty<ParameterHolder, All> =
    apply {
      if (name.isBlank()) name = prop.name.replace(Regex("[a-z][A-Z]")) {
        "${it.value[0]}_${it.value[1]}"
      }.uppercase().replace("-", "_")
      thisRef.register(this)
    }

  fun <Value, All> clone(
    transformValue: ArgumentValueTransformer<Value>,
    transformAll: ArgumentCallsTransformer<All, Value>,
    validator: ArgumentValidator<All>,
    name: String = this.name,
    length: Int = this.length,
    required: Boolean = this.required,
    description: String? = this.description
  ) = ArgumentProcessor(name, length, required, description, transformValue, transformAll, validator)

  fun clone(
    validator: ArgumentValidator<All> = this.validator,
    name: String = this.name,
    length: Int = this.length,
    required: Boolean = this.required,
    description: String? = this.description
  ) = ArgumentProcessor(name, length, required, description, transformValue, transformAll, validator)
}

typealias RawArgument = ArgumentProcessor<String, String>

fun ParameterHolder.argument(
  name: String = "",
  description: String? = null,
): RawArgument = ArgumentProcessor(
  name, 1, true, description, { it }, { it.single() }, { }
)

fun <All, Value> ArgumentProcessor<All, Value>.description(description: String): ArgumentProcessor<All, Value> =
  clone(description = description)

fun <In, Value, Out> ArgumentProcessor<In, Value>.transformAll(
  length: Int = this.length, required: Boolean = this.required,
  transform: ArgumentCallsTransformer<Out, Value>
): ArgumentProcessor<Out, Value> =
  clone(
    transformValue, transform, {},
    length = length,
    required = required
  )

fun <All : Any, Value> ArgumentProcessor<All, Value>.optional(): ArgumentProcessor<All?, Value> =
  transformAll(required = false) { if (it.isEmpty()) null else transformAll(it) }

fun <T : Any> ArgumentProcessor<T, T>.multiple(required: Boolean = false): ArgumentProcessor<List<T>, T> =
  transformAll(length = -1, required = required) { it }

fun <T : Any> ArgumentProcessor<T, T>.unique(): ArgumentProcessor<Set<T>, T> =
  transformAll(length = -1) { it.toSet() }

fun <T : Any> ArgumentProcessor<T, T>.pair(): ArgumentProcessor<Pair<T, T>, T> =
  transformAll(length = 2) { it[0] to it[1] }

fun <T : Any> ArgumentProcessor<T, T>.triple(): ArgumentProcessor<Triple<T, T, T>, T> =
  transformAll(length = 3) { Triple(it[0], it[1], it[2]) }

fun <T : Any> ArgumentProcessor<T, T>.default(value: T): ArgumentDelegate<T> =
  transformAll(required = false) { it.firstOrNull() ?: value }

fun <T : Any> ArgumentProcessor<T, T>.lazyDefault(value: Supplier<T>): ArgumentDelegate<T> =
  transformAll(required = false) { it.firstOrNull() ?: value.get() }

inline fun <In : Any, Value : Any> ArgumentProcessor<In, In>.convert(
  crossinline conversion: ArgumentValueConverter<In, Value>
): ArgumentProcessor<Value, Value> {
  val converter: ArgumentValueTransformer<Value> = {
    try {
      conversion(transformValue(it))
    } catch (err: CliCommandException) {
      throw err
    } catch (err: Exception) {
      fail(err.message ?: "")
    }
  }

  return clone(converter, { it.single() }, { })
}

fun <All : Any, Value> ArgumentProcessor<All, Value>.validate(
  validator: ArgumentValidator<All>
): ArgumentDelegate<All> = clone(validator)

@JvmName("nullableValidate")
fun <All : Any, Value> ArgumentProcessor<All?, Value>.validate(
  validator: ArgumentValidator<All>
): ArgumentDelegate<All?> = clone({ if (it != null) validator(it) })

fun <All : Any, Value> ArgumentProcessor<All, Value>.check(
  message: String,
  validator: Predicate<All>
): ArgumentDelegate<All> = check({ message }, validator)

fun <All : Any, Value> ArgumentProcessor<All, Value>.check(
  lazyMessage: Transformer<All, String> = Transformer { it.toString() },
  validator: Predicate<All>
): ArgumentDelegate<All> = validate { require(validator.test(it)) { lazyMessage.transform(it) } }

@JvmName("nullableCheck")
fun <All : Any, Value> ArgumentProcessor<All?, Value>.check(
  message: String,
  validator: Predicate<All>
): ArgumentDelegate<All?> = check({ message }, validator)

@JvmName("nullableLazyCheck")
fun <All : Any, Value> ArgumentProcessor<All?, Value>.check(
  lazyMessage: Transformer<All, String> = Transformer { it.toString() },
  validator: Predicate<All>
): ArgumentDelegate<All?> = validate { require(validator.test(it)) { lazyMessage.transform(it) } }

data class ArgumentDetails(
  val name: String,
  val description: String?,
  val required: Boolean,
  val length: Int,
  val infinite: Boolean
)

class WrongArgumentValue(
  message: String,
  argument: Argument
) : ArgumentException(argument, message)

open class ArgumentException(
  val argument: Argument,
  message: String? = null
) : CliCommandException(message)

class IncorrectArgumentValueCountException(
  argument: Argument,
  name: String = argument.name
) : ArgumentException(
  argument, when (argument.length) {
    0 -> "Option \"$name\" does not take a value"
    1 -> "Option \"$name\" requires a value"
    else -> "Option \"$name\" requires ${argument.length} values"
  }
)
