package io.alice.platform.api.engine.chat.command.option

import io.alice.platform.api.engine.chat.command.AbstractCliCommand
import io.alice.platform.api.engine.chat.command.CliCommandException
import io.alice.platform.api.engine.chat.command.ParameterHolder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Option {
  val description: String?
  val parser: OptionParser
  val keys: Set<String>
  val alternativeKeys: Set<String>
  val length: Int
  val hidden: Boolean
  val required: Boolean

  fun printParameterDetails(): OptionDetails? = when {
    hidden -> null
    else -> OptionDetails(keys, description, required, length)
  }

  fun finalize(command: AbstractCliCommand<*, *>, invocations: List<OptionParser.Invocation>)
  fun postValidate(command: AbstractCliCommand<*, *>)
}

interface OptionDelegate<T> : Option, ReadOnlyProperty<ParameterHolder, T> {
  val value: T

  operator fun provideDelegate(thisRef: ParameterHolder, prop: KProperty<*>): ReadOnlyProperty<ParameterHolder, T>

  override fun getValue(thisRef: ParameterHolder, property: KProperty<*>): T = value
}

internal fun Option.longestKey(): String? = keys.maxByOrNull { it.length }

interface OptionParser {
  fun parseShort(option: Option, name: String, argv: List<String>, index: Int, optionIndex: Int): ParseResult
  fun parseLong(option: Option, name: String, argv: List<String>, index: Int, explicitValue: String?): ParseResult

  data class Invocation(val name: String, val values: List<String>)
  data class ParseResult(val consumed: Int, val invocation: Invocation) {
    constructor(consumed: Int, name: String, values: List<String>) : this(consumed, Invocation(name, values))
  }
}

data class OptionDetails(
  val keys: Set<String>,
  val description: String?,
  val required: Boolean,
  val length: Int
)

open class OptionException(
  val option: Option,
  message: String? = null
) : CliCommandException(message)

class IncorrectOptionValueCountException(
  option: Option,
  name: String
) : OptionException(
  option, when (option.length) {
    0 -> "Option \"$name\" does not take a value"
    1 -> "Option \"$name\" requires a value"
    else -> "Option \"$name\" requires ${option.length} values"
  }
)

class OptionNotFoundException(
  name: String,
  possibilities: List<String>
) : CliCommandException(
  "no such option: \"$name\"" + when (possibilities.size) {
    0 -> ""
    1 -> ". Did you mean \"${possibilities[0]}\"?"
    else -> possibilities.joinToString(prefix = ". (Possible options: ", postfix = ")")
  }
)

class WrongOptionValue(
  message: String,
  val option: Option
) : CliCommandException(message)

internal fun optionPrefixSplit(name: String): Pair<String, String> =
  when {
    name.length < 2 || name[0].isLetterOrDigit() -> "" to name
    name.length > 2 && name[0] == name[1] -> name.slice(0..1) to name.substring(2)
    else -> name.substring(0 until 1) to name.substring(1)
  }

internal fun inferOptionKeys(keys: Set<String>, propName: String): Set<String> {
  if (keys.isNotEmpty()) {
    val invalidName = keys.filterNot { it.matches(Regex("[!\"#\$%&'()*+,-./\\\\:;<=>?@\\[\\]^_`{|}~]{1,2}[\\w-_]+")) }
    require(invalidName.isEmpty()) {
      "Invalid option named keys: \"${invalidName.joinToString("\", \"", "\"", "\"")}\""
    }
    return keys
  }
  val normalizedName = "--" + propName.replace(Regex("[a-z][A-Z]")) {
    "${it.value[0]}-${it.value[1]}"
  }.lowercase()

  return setOf(normalizedName)
}

internal fun <EachT, AllT> deprecatedTransformer(
  message: String? = null,
  error: Boolean = false,
  transformAll: OptionCallsTransformer<EachT, AllT>
): OptionCallsTransformer<EachT, AllT> = {
  if (it.isNotEmpty()) {
    val msg = when (message) {
      null, "" -> "${if (error) "ERR: " else ""}Option ${option.longestKey()} is deprecated"
      else -> message
    }
    if (error) {
      throw CliCommandException(msg)
    } else if (message != null) {
      message(msg)
    }
  }
  transformAll(it)
}

