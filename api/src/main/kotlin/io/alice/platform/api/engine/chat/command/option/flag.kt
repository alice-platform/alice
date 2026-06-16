package io.alice.platform.api.engine.chat.command.option

import io.alice.platform.api.LateInit
import io.alice.platform.api.Supplier
import io.alice.platform.api.engine.chat.command.AbstractCliCommand
import io.alice.platform.api.engine.chat.command.CliCommandException
import io.alice.platform.api.engine.chat.command.ParameterHolder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias FlagConversion<In, Out> = OptionTransformer.(In) -> Out

class FlagOption<T> internal constructor(
  keys: Set<String>,
  override val alternativeKeys: Set<String>,
  override val description: String?,
  override val hidden: Boolean,
  override val required: Boolean,
  val transform: OptionCallsTransformer<String, T>,
  val validator: OptionValidator<T>
) : OptionDelegate<T> {
  override var value: T by LateInit { IllegalStateException("Cannot read from option delegate before parsing command line") }
    private set

  override var keys: Set<String> = keys
    private set
  override val length: Int get() = 0
  override val parser: OptionParser = FlagOptionParser

  override fun finalize(command: AbstractCliCommand<*, *>, invocations: List<OptionParser.Invocation>) {
    value = transform(OptionTransformer(this, command), invocations.map { it.name })
  }

  override fun postValidate(command: AbstractCliCommand<*, *>) {
    validator(OptionTransformer(this, command), value)
  }

  override fun provideDelegate(thisRef: ParameterHolder, prop: KProperty<*>): ReadOnlyProperty<ParameterHolder, T> =
    apply {
      keys = inferOptionKeys(keys, prop.name)
      thisRef.register(this)
    }

  fun <T> clone(
    transform: OptionCallsTransformer<String, T>,
    validator: OptionValidator<T>,
    keys: Set<String> = this.keys,
    alternativeKeys: Set<String> = this.alternativeKeys,
    description: String? = this.description,
    hidden: Boolean = this.hidden,
    required: Boolean = this.required
  ): FlagOption<T> =
    FlagOption(keys, alternativeKeys, description, hidden, required, transform, validator)

  fun clone(
    validator: OptionValidator<T> = this.validator,
    keys: Set<String> = this.keys,
    alternativeKeys: Set<String> = this.alternativeKeys,
    description: String? = this.description,
    hidden: Boolean = this.hidden,
    required: Boolean = this.required
  ): FlagOption<T> =
    FlagOption(keys, alternativeKeys, description, hidden, required, transform, validator)
}

fun RawOption.flag(
  vararg alternativeKeys: String,
  default: Boolean = false
): FlagOption<Boolean> =
  FlagOption(keys, alternativeKeys.toSet(), description, hidden, required, {
    if (it.isEmpty()) default else it.last() !in alternativeKeys
  }, { })

fun <T> FlagOption<T>.description(description: String) =
  clone(description = description)

inline fun <In, Out> FlagOption<In>.convert(crossinline conversion: FlagConversion<In, Out>): FlagOption<Out> =
  clone({
    val original = transform(it)
    try {
      conversion(original)
    } catch (err: CliCommandException) {
      throw err
    } catch (err: Exception) {
      fail(err.message ?: "")
    }
  }, {})

fun RawOption.counted(): FlagOption<Int> =
  FlagOption(
    keys, emptySet(), description, hidden, required, { it.size }, { }
  )

fun <T> RawOption.switch(choices: Map<String, T>): FlagOption<T?> {
  require(choices.isNotEmpty()) { "Define at least one choice" }
  return FlagOption(
    choices.keys,
    emptySet(),
    description,
    hidden,
    required,
    { names -> names.map { choices.getValue(it) }.lastOrNull() },
    { })
}

fun <T> RawOption.switch(vararg choices: Pair<String, T>) = switch(choices.toMap())

fun <T> FlagOption<T?>.default(value: T) = clone({ transform(it) ?: value }, validator)

fun <T> FlagOption<T?>.lazyDefault(value: Supplier<T>) =
  clone({ transform(it) ?: value.get() }, {})

fun <T : Any> FlagOption<T?>.required() =
  clone(
    { transform(it) ?: throw OptionException(option, "Missing Option: \"${option.longestKey()}\"") },
    {},
    required = true
  )

fun <T : Any> FlagOption<T>.validate(validator: OptionValidator<T>): OptionDelegate<T> =
  clone(validator)

fun <T : Any> FlagOption<T>.deprecated(
  message: String? = "",
  error: Boolean = false
): OptionDelegate<T> =
  clone(deprecatedTransformer(message, error, transform), validator)

object FlagOptionParser : OptionParser {
  override fun parseShort(
    option: Option,
    name: String,
    argv: List<String>,
    index: Int,
    optionIndex: Int
  ): OptionParser.ParseResult {
    val consumed = if (optionIndex == argv[index].lastIndex) 1 else 0
    return OptionParser.ParseResult(consumed, name, emptyList())
  }

  override fun parseLong(
    option: Option,
    name: String,
    argv: List<String>,
    index: Int,
    explicitValue: String?
  ): OptionParser.ParseResult {
    if (explicitValue != null) throw IncorrectOptionValueCountException(option, name)
    return OptionParser.ParseResult(1, name, emptyList())
  }

}
