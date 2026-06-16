package io.alice.platform.api.engine.chat.command.types

import io.alice.platform.api.engine.chat.command.argument.RawArgument
import io.alice.platform.api.engine.chat.command.argument.WrongArgumentValue
import io.alice.platform.api.engine.chat.command.argument.convert
import io.alice.platform.api.engine.chat.command.option.RawOption
import io.alice.platform.api.engine.chat.command.option.WrongOptionValue
import io.alice.platform.api.engine.chat.command.option.convert

/** Convert the argument values to a `Double` */
fun RawArgument.double() = convert { it.toDoubleOrNull() ?: throw WrongArgumentValue("$it is not a valid floating point value", argument) }

/** Convert the option values to a `Double` */
fun RawOption.double() = convert { it.toDoubleOrNull() ?: throw WrongOptionValue("$it is not a valid floating point value", option) }

/** Convert the argument values to a `Float` */
fun RawArgument.float() = convert { it.toFloatOrNull() ?: throw WrongArgumentValue("$it is not a valid floating point value", argument) }

/** Convert the option values to a `Float` */
fun RawOption.float() = convert { it.toFloatOrNull() ?: throw WrongOptionValue("$it is not a valid floating point value", option) }

/** Convert the argument values to a `Double` */
fun RawArgument.int() = convert { it.toIntOrNull() ?: throw WrongArgumentValue("$it is not a valid integer", argument) }

/** Convert the option values to a `Double` */
fun RawOption.int() = convert { it.toIntOrNull() ?: throw WrongOptionValue("$it is not a valid integer", option) }

/** Convert the argument values to a `Double` */
fun RawArgument.long() = convert { it.toDoubleOrNull() ?: throw WrongArgumentValue("$it is not a valid integer", argument) }

/** Convert the option values to a `Double` */
fun RawOption.long() = convert { it.toDoubleOrNull() ?: throw WrongOptionValue("$it is not a valid integer", option) }
