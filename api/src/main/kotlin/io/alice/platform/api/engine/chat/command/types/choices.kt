package io.alice.platform.api.engine.chat.command.types

import io.alice.platform.api.Transformer
import io.alice.platform.api.engine.chat.command.argument.ArgumentProcessor
import io.alice.platform.api.engine.chat.command.argument.RawArgument
import io.alice.platform.api.engine.chat.command.argument.convert
import io.alice.platform.api.engine.chat.command.option.NullableOption
import io.alice.platform.api.engine.chat.command.option.RawOption
import io.alice.platform.api.engine.chat.command.option.convert

private fun error(choice: String, choices: Map<String, *>) =
  "Invalid choice: $choice. (choose from ${choices.keys.joinToString()})"

fun <T : Any> RawArgument.choice(choices: Map<String, T>, ignoreCase: Boolean = false): ArgumentProcessor<T, T> {
  require(choices.isNotEmpty()) { "Must specify at least one choice" }
  val c = if (ignoreCase) choices.mapKeys { it.key.lowercase() } else choices
  return convert {
    c[if (ignoreCase) it.lowercase() else it] ?: fail(error(it, choices))
  }
}

fun <T : Any> RawArgument.choice(
  vararg choices: Pair<String, T>,
  ignoreCase: Boolean = false
): ArgumentProcessor<T, T> =
  choice(choices.toMap(), ignoreCase)

fun RawArgument.choice(vararg choices: String, ignoreCase: Boolean = false): ArgumentProcessor<String, String> =
  choice(choices.associateBy { it }, ignoreCase)

inline fun <reified T : Enum<T>> RawArgument.enum(
  ignoreCase: Boolean = true,
  key: Transformer<T, String> = Transformer { it.name }
): ArgumentProcessor<T, T> =
  choice(enumValues<T>().associateBy(key::transform), ignoreCase)


fun <T : Any> RawOption.choice(
  choices: Map<String, T>, ignoreCase: Boolean = false
): NullableOption<T, T> {
  require(choices.isNotEmpty()) { "Must specify at least one choice" }
  val c = if (ignoreCase) choices.mapKeys { it.key.lowercase() } else choices
  return convert {
    c[if (ignoreCase) it.lowercase() else it] ?: fail(error(it, choices))
  }
}

fun <T : Any> RawOption.choice(
  vararg choices: Pair<String, T>,
  ignoreCase: Boolean = false
): NullableOption<T, T> =
  choice(choices.toMap(), ignoreCase)

fun RawOption.choice(vararg choices: String, ignoreCase: Boolean = false): NullableOption<String, String> =
  choice(choices.associateBy { it }, ignoreCase)

inline fun <reified T : Enum<T>> RawOption.enum(
  ignoreCase: Boolean = true,
  key: Transformer<T, String> = Transformer { it.name }
): NullableOption<T, T> =
  choice(enumValues<T>().associateBy(key::transform), ignoreCase)
