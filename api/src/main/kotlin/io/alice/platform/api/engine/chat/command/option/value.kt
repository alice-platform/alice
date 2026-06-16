package io.alice.platform.api.engine.chat.command.option

import io.alice.platform.api.LateInit
import io.alice.platform.api.Supplier
import io.alice.platform.api.engine.chat.command.AbstractCliCommand
import io.alice.platform.api.engine.chat.command.CliCommandException
import io.alice.platform.api.engine.chat.command.ParameterHolder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class OptionCallTransformer(
    val name: String,
    val option: Option,
    val command: AbstractCliCommand<*, *>
) : Option by option {
    fun fail(message: String): Nothing = throw WrongOptionValue(message, option)

    fun message(message: String) = command.message(message)

    fun require(value: Boolean, lazyMessage: Supplier<String> = Supplier { "" }) {
        if (!value) fail(lazyMessage.get())
    }
}

class OptionTransformer(
    val option: Option,
    val command: AbstractCliCommand<*, *>
) : Option by option {
    fun fail(message: String): Nothing = throw WrongOptionValue(message, option)

    fun message(message: String) = command.message(message)

    fun require(value: Boolean, lazyMessage: Supplier<String> = Supplier { "" }) {
        if (!value) fail(lazyMessage.get())
    }
}

typealias OptionValueTransform<ValT> = OptionValueConvert<String, ValT>
typealias OptionValueConvert<IN, OUT> = OptionCallTransformer.(IN) -> OUT
typealias OptionArgsTransformer<ValT, EachT> = OptionCallTransformer.(List<ValT>) -> EachT
typealias OptionCallsTransformer<EachT, AllT> = OptionTransformer.(List<EachT>) -> AllT
typealias OptionValidator<AllT> = OptionTransformer.(AllT) -> Unit

class ValuedOption<AllT, EachT, ValueT> internal constructor(
    keys: Set<String>,
    override val length: Int,
    override val description: String?,
    override val hidden: Boolean,
    override val parser: OptionParser,
    override val required: Boolean,
    val valueSplitter: Regex?,
    val transformValue: OptionValueTransform<ValueT>,
    val transformEach: OptionArgsTransformer<ValueT, EachT>,
    val transformAll: OptionCallsTransformer<EachT, AllT>,
    val validator: OptionValidator<AllT>
) : OptionDelegate<AllT> {
    override var value: AllT by LateInit { IllegalStateException("Could not read from option delegate before parsing command line") }
        private set
    override var keys: Set<String> = keys
        private set

    override val alternativeKeys: Set<String> = emptySet()

    override fun finalize(command: AbstractCliCommand<*, *>, invocations: List<OptionParser.Invocation>) {
        val invocation = when (valueSplitter) {
            null -> invocations
            else -> invocations.map { inv -> inv.copy(values = inv.values.flatMap { it.split(valueSplitter) }) }
        }

        value = transformAll(OptionTransformer(this, command), invocation.map {
            val tc = OptionCallTransformer(it.name, this, command)
            transformEach(tc, it.values.map { v -> transformValue(tc, v) })
        })
    }

    override fun provideDelegate(
        thisRef: ParameterHolder,
        prop: KProperty<*>
    ): ReadOnlyProperty<ParameterHolder, AllT> =
        apply {
            require(alternativeKeys.isEmpty()) {
                "Alternative option keys are only allowed on the flag options."
            }
            keys = inferOptionKeys(keys, prop.name)
            thisRef.register(this)
        }

    override fun postValidate(command: AbstractCliCommand<*, *>) {
        validator(OptionTransformer(this, command), value)
    }

    fun <AllT, EachT, ValueT> clone(
        transformValue: OptionValueTransform<ValueT>,
        transformEach: OptionArgsTransformer<ValueT, EachT>,
        transformAll: OptionCallsTransformer<EachT, AllT>,
        validator: OptionValidator<AllT>,
        keys: Set<String> = this.keys,
        length: Int = this.length,
        description: String? = this.description,
        valueSplitter: Regex? = this.valueSplitter,
        hidden: Boolean = this.hidden,
        parser: OptionParser = this.parser,
        required: Boolean = this.required
    ) = ValuedOption(
        keys,
        length,
        description,
        hidden,
        parser,
        required,
        valueSplitter,
        transformValue,
        transformEach,
        transformAll,
        validator
    )

    fun clone(
        validator: OptionValidator<AllT> = this.validator,
        keys: Set<String> = this.keys,
        length: Int = this.length,
        description: String? = this.description,
        valueSplitter: Regex? = this.valueSplitter,
        hidden: Boolean = this.hidden,
        parser: OptionParser = this.parser,
        required: Boolean = this.required
    ) = ValuedOption(
        keys, length, description, hidden, parser, required, valueSplitter,
        transformValue, transformEach, transformAll, validator
    )
}

typealias NullableOption<EachT, ValueT> = ValuedOption<EachT?, EachT, ValueT>
typealias RawOption = NullableOption<String, String>

fun ParameterHolder.option(
    vararg keys: String,
    description: String? = null,
    required: Boolean = false,
    hidden: Boolean = false
): RawOption = ValuedOption(
    keys.toSet(), 1, description, hidden,
    ValuedOptionParser, required, null, { it },
    { it.single() }, { it.lastOrNull() }, { }
)

fun <AllT, EachT, ValueT> ValuedOption<AllT, EachT, ValueT>.description(description: String): ValuedOption<AllT, EachT, ValueT> =
    clone(description = description)

fun <AllT : Any, EachT, ValueT> ValuedOption<AllT, EachT, ValueT>.validate(
    validator: OptionValidator<AllT>
) = clone(validator)

@JvmName("nullableValidate")
inline fun <AllT : Any, EachT, ValueT> ValuedOption<AllT?, EachT, ValueT>.validate(
    crossinline validator: OptionValidator<AllT>
): OptionDelegate<AllT?> = clone({ if (it != null) validator(it) })

inline fun <AllT : Any, EachT, ValueT> ValuedOption<AllT, EachT, ValueT>.check(
    message: String,
    crossinline validator: (AllT) -> Boolean
): OptionDelegate<AllT> {
    return check({ message }, validator)
}

inline fun <AllT : Any, EachT, ValueT> ValuedOption<AllT, EachT, ValueT>.check(
    crossinline lazyMessage: (AllT) -> String = { it.toString() },
    crossinline validator: (AllT) -> Boolean
): OptionDelegate<AllT> {
    return validate { require(validator(it)) { lazyMessage(it) } }
}

@JvmName("nullableCheck")
inline fun <AllT : Any, EachT, ValueT> ValuedOption<AllT?, EachT, ValueT>.check(
    message: String,
    crossinline validator: (AllT) -> Boolean
): OptionDelegate<AllT?> {
    return check({ message }, validator)
}

@JvmName("nullableCheck")
inline fun <AllT : Any, EachT, ValueT> ValuedOption<AllT?, EachT, ValueT>.check(
    crossinline lazyMessage: (AllT) -> String = { it.toString() },
    crossinline validator: (AllT) -> Boolean
): OptionDelegate<AllT?> {
    return validate { require(validator(it)) { lazyMessage(it) } }
}

fun <AllT, EachT, ValueT> ValuedOption<AllT, EachT, ValueT>.deprecated(
    message: String? = "",
    error: Boolean = false
): OptionDelegate<AllT> =
    clone(transformValue, transformEach, deprecatedTransformer(message, error, transformAll), validator)

object ValuedOptionParser : OptionParser {
    override fun parseShort(
        option: Option,
        name: String,
        argv: List<String>,
        index: Int,
        optionIndex: Int
    ): OptionParser.ParseResult {
        val opt = argv[index]
        val includedValue = optionIndex != opt.lastIndex
        val explicitValue = if (includedValue) opt.substring(optionIndex + 1) else null
        return parseLong(option, name, argv, index, explicitValue)
    }

    override fun parseLong(
        option: Option,
        name: String,
        argv: List<String>,
        index: Int,
        explicitValue: String?
    ): OptionParser.ParseResult {
        require(option.length > 0) {
            "This parser can only be used with a fixed number of arguments. Try the flag parser instead."
        }

        val includedValue = explicitValue != null
        val consumed = if (includedValue) option.length else option.length + 1
        val endIndex = index + consumed - 1

        if (endIndex > argv.lastIndex) {
            throw IncorrectOptionValueCountException(option, name)
        }

        val invocation = if (option.length > 1) {
            var args = argv.slice((index + 1)..endIndex)
            if (explicitValue != null) args = listOf(explicitValue) + args
            OptionParser.Invocation(name, args)
        } else {
            OptionParser.Invocation(name, listOf(explicitValue ?: argv[index + 1]))
        }

        return OptionParser.ParseResult(consumed, invocation)
    }
}

fun <InT : Any, OutT : Any, ValueT> NullableOption<InT, ValueT>.transformValues(
    length: Int,
    transform: OptionArgsTransformer<ValueT, OutT>
): NullableOption<OutT, ValueT> {
    require(length != 0) { "Cannot set value length = 0. Use `flag()` instead." }
    require(length > 0) { "Value count cannot be a negative (`length < 0`)." }
    require(length > 1) { "Cannot set value length = 1. use `convert()` instead." }

    return clone(transformValue, transform, { it.lastOrNull() }, { }, length = length)
}

fun <EachT : Any, ValueT> NullableOption<EachT, ValueT>.pair(): NullableOption<Pair<ValueT, ValueT>, ValueT> =
    transformValues(2) { it[0] to it[1] }

fun <EachT : Any, ValueT> NullableOption<EachT, ValueT>.triple(): NullableOption<Triple<ValueT, ValueT, ValueT>, ValueT> =
    transformValues(3) { Triple(it[0], it[1], it[2]) }

fun <AllT, EachT : Any, ValueT> NullableOption<EachT, ValueT>.transformAll(
    required: Boolean = false,
    transform: OptionCallsTransformer<EachT, AllT>
): ValuedOption<AllT, EachT, ValueT> =
    clone(transformValue, transformEach, transform, { }, required = required)

inline fun <In : Any, Value : Any> NullableOption<In, In>.convert(
    crossinline conversion: OptionValueConvert<In, Value>
): NullableOption<Value, Value> {
    val transformer: OptionValueTransform<Value> = {
        try {
            conversion(transformValue(it))
        } catch (err: CliCommandException) {
            throw err
        } catch (err: Exception) {
            fail(err.message ?: "")
        }
    }

    return clone(transformer, { it.single() }, { it.lastOrNull() }, {})
}

fun <Each : Any, Value> NullableOption<Each, Value>.split(regex: Regex): ValuedOption<List<Value>?, List<Value>, Value> =
    clone(transformValue, { it }, { it.lastOrNull() }, { }, length = 1, valueSplitter = regex)

fun <Each : Any, Value> NullableOption<Each, Value>.split(delimiter: String): ValuedOption<List<Value>?, List<Value>, Value> =
    split(Regex.fromLiteral(delimiter))

fun RawOption.splitPair(delimiter: String = "="): NullableOption<Pair<String, String>, Pair<String, String>> =
    convert { it.substringBefore(delimiter) to it.substringAfter(delimiter, missingDelimiterValue = "") }

fun <T : Any> NullableOption<T, T>.default(value: T): OptionDelegate<T> =
    transformAll(required = false) {
        transformAll(it) ?: value
    }

fun <T : Any> NullableOption<T, T>.lazyDefault(value: Supplier<T>): OptionDelegate<T> =
    transformAll(required = false) { transformAll(it) ?: value.get() }
