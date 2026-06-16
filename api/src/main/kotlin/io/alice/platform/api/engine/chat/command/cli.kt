package io.alice.platform.api.engine.chat.command

import io.alice.platform.api.Predicate
import io.alice.platform.api.engine.chat.command.argument.Argument
import io.alice.platform.api.engine.chat.command.argument.ArgumentException
import io.alice.platform.api.engine.chat.command.argument.IncorrectArgumentValueCountException
import io.alice.platform.api.engine.chat.command.option.Option
import io.alice.platform.api.engine.chat.command.option.OptionNotFoundException
import io.alice.platform.api.engine.chat.command.option.OptionParser
import io.alice.platform.api.engine.chat.command.option.optionPrefixSplit
import kotlin.math.max
import kotlin.math.min

abstract class AbstractCliCommand<TEvent, TCEvent : CommandEvent<TEvent>>(
  name: String,
  alias: Array<String> = arrayOf(),
  description: String? = null,
  access: Predicate<TCEvent> = Predicate { true },
  group: Command.Group<TEvent, TCEvent> = Command.Group("unknown"),
  internal val treatUnknownOptionsAsArgs: Boolean = false
) : AbstractCommand<TEvent, TCEvent>(
  name, alias, description, access, group
), ParameterHolder {

  internal val _arguments = mutableSetOf<Argument>()
  internal val _options = mutableSetOf<Option>()

  final override val arguments: Set<Argument>
    get() = _arguments
  final override val options: Set<Option>
    get() = _options

  final override fun execute(event: TCEvent) {
    try {
      parse(event.argv)
      handle(event)
    } catch (e: PrintMessage) {
      when (e.type) {
        PrintMessage.Type.HELP -> printHelp(event)
        PrintMessage.Type.MESSAGE -> event.printMessage(e.message!!)
        PrintMessage.Type.ERROR -> event.printException(e)
      }
    } catch (e: Exception) {
      event.printException(e)
    }
  }

  private fun parse(argv: Array<String>) {
    Parser.parse(argv.toList(), this)
  }

  protected abstract fun printHelp(event: TCEvent)
  protected abstract fun TCEvent.printException(exception: Exception)
  protected abstract fun TCEvent.printMessage(message: String, error: Boolean = false)

  fun error(message: String, cause: Throwable) {
    throw PrintMessage(message, PrintMessage.Type.ERROR, cause)
  }

  fun message(message: String, isError: Boolean = false) {
    throw PrintMessage(message, if (isError) PrintMessage.Type.ERROR else PrintMessage.Type.MESSAGE)
  }

  protected open fun respondWithNoArg(event: TCEvent) {
    printHelp(event)
  }

  final override fun register(argument: Argument) {
    this._arguments += argument
  }

  final override fun register(option: Option) {
    this._options += option
  }

  abstract fun handle(event: TCEvent)
}

interface ParameterHolder {
  val arguments: Set<Argument>
  val options: Set<Option>

  fun printMessage(message: String, error: Boolean = false)

  fun register(argument: Argument)
  fun register(option: Option)
}

internal object Parser {
  internal fun <TEvent, TCEvent : CommandEvent<TEvent>> parse(
    argv: List<String>,
    command: AbstractCliCommand<TEvent, TCEvent>
  ) {
    val tokens = argv
    val optionsByName = HashMap<String, Option>()
    val arguments = command._arguments.toList()
    val prefixes = mutableSetOf<String>()
    val longNames = mutableSetOf<String>()

    for (option in command.options) {
      require(option.keys.isNotEmpty() || option.alternativeKeys.isNotEmpty()) {
        "options must have at least one key"
      }

      for (name in option.keys + option.alternativeKeys) {
        optionsByName[name] = option
        if (name.length > 2) longNames += name
        prefixes += prefixOptionSepartor(name).first
      }
    }
    prefixes.remove("")

    if (tokens.lastIndex == 0) {
      throw PrintMessage("", PrintMessage.Type.HELP)
    }

    val posArgv = ArrayList<String>()
    var i = 0
    var parseOpts = true
    val invocations = mutableListOf<OptInvocation>()

    fun isLongOptionWithEq(prefix: String, token: String): Boolean = when {
      '=' !in token -> false
      prefix.isEmpty() -> false
      prefix.length > 1 -> true
      token.substringBefore('=').lowercase() in longNames.map { it.lowercase() } -> true
      token.take(2) in optionsByName -> false
      else -> true
    }

    fun consumeParser(result: OptParseResult) {
      posArgv += result.unknown
      invocations += result.known
      i += result.consumed
    }

    loop@ while (i <= tokens.lastIndex) {
      val tok = tokens[i]
      val prefix = optionPrefixSplit(tok).first
      when {
        parseOpts && tok == "--" -> {
          i++
          parseOpts = false
        }
        parseOpts && (prefix.length > 1 && prefix in prefixes || tok in longNames || isLongOptionWithEq(
          prefix,
          tok
        )) -> {
          consumeParser(parseLong(command.treatUnknownOptionsAsArgs, tokens, tok, i, optionsByName))
        }
        parseOpts && tok.length >= 2 && prefix.isNotEmpty() && prefix in prefixes -> {
          consumeParser(parseShort(command.treatUnknownOptionsAsArgs, tokens.toList(), tok, i, optionsByName))
        }
        else -> {
          posArgv += tokens[i]
          i++
        }
      }
    }

    val optionInvocations = invocations.groupBy({ it.opt }, { it.inv })

    optionInvocations.forEach { (o, inv) -> o.finalize(command, inv) }
    command._options.forEach { o ->
      if (o !in optionInvocations) {
        o.finalize(command, emptyList())
      }
    }

    val (excess, parsedArgv) = parseArgvs(posArgv, arguments)
    parsedArgv.forEach { (it, v) -> it.finalize(command, v) }

    if (excess > 0) {
      val actual = posArgv.takeLast(excess).joinToString(" ", limit = 3, prefix = "(", postfix = ")")
      val message = if (excess == 1) "Got unexpected extra argument $actual"
        else "Got unexpected extra $excess arguments $actual"

      throw CliCommandException(message)
    }

    command._options.forEach {
      it.postValidate(command)
    }

    command._arguments.forEach {
      it.postValidate(command)
    }
  }

  private fun parseArgvs(
    posArgv: List<String>,
    arguments: List<Argument>
  ): Pair<Int, MutableMap<Argument, List<String>>> {
    val out = linkedMapOf<Argument, List<String>>().withDefault { listOf() }

    val endSize = arguments.asReversed()
      .takeWhile { it.length > 0 }
      .sumOf { it.length }

    var i = 0
    for (arg in arguments) {
      val remain = posArgv.size - i
      val consumed = when {
        arg.length <= 0 -> maxOf(if (arg.required) 1 else 0, remain - endSize)
        arg.length > 0 && !arg.required && remain == 0 -> 0
        else -> arg.length
      }
      if (consumed > remain) {
        if (remain == 0) throw ArgumentException(arg, "Missing argument \"${arg.name}\"")
        else throw IncorrectArgumentValueCountException(arg)
      }
      out[arg] = out.getValue(arg) + posArgv.subList(i, i + consumed)
      i += consumed
    }

    val excess = posArgv.size - i
    return excess to out
  }

  private fun parseLong(
    ignoreUnknown: Boolean,
    tokens: List<String>,
    tok: String,
    index: Int,
    optionsByName: Map<String, Option>
  ): OptParseResult {
    val eqI = tok.indexOf('=')
    val (name, value) = if (eqI >= 0) {
      tok.substring(0, eqI) to tok.substring(eqI + 1)
    } else {
      tok to null
    }
    val option = optionsByName[name] ?: if (ignoreUnknown) {
      return OptParseResult(1, listOf(tok), emptyList())
    } else {
      val possibilities = optionsByName.filterNot { it.value.hidden }.keys.toList()
        .map { it to jaroWinklerSimilarity(name, it) }
        .filter { it.second > 0.8 }
        .sortedByDescending { it.second }
        .map { it.first }

      throw OptionNotFoundException(name, possibilities)
    }

    val result = option.parser.parseLong(option, name, tokens, index, value)
    return OptParseResult(result.consumed, emptyList(), listOf(OptInvocation(option, result.invocation)))
  }

  private fun parseShort(
    ignoreUnknown: Boolean,
    tokens: List<String>,
    tok: String,
    index: Int,
    optionsByName: Map<String, Option>
  ): OptParseResult {
    val prefix = tok[0].toString()
    val inv = mutableListOf<OptInvocation>()
    for ((i, opt) in tok.withIndex()) {
      if (i == 0) continue

      val name = prefix + opt
      val option = optionsByName[name] ?: if (ignoreUnknown && tok.length == 2) {
        return OptParseResult(1, listOf(tok), emptyList())
      } else {
        val possibilities = when {
          prefix == "-" && "-$tok" in optionsByName -> listOf("-$tok")
          else -> emptyList()
        }
        throw OptionNotFoundException(name, possibilities)
      }
      val result = option.parser.parseShort(option, name, tokens, index, i)
      inv += OptInvocation(option, result.invocation)
      if (result.consumed > 0) return OptParseResult(result.consumed, emptyList(), inv)
    }
    throw IllegalStateException("Error parsing short option ${tokens[index]}: no parser consumed value.")
  }
}

open class CliCommandException : CommandException {
  constructor() : super()
  constructor(cause: Throwable?) : super(cause)
  constructor(message: String?) : super(message)
  constructor(message: String?, cause: Throwable?) : super(message, cause)
}

class PrintMessage(message: String, val type: Type, cause: Throwable? = null) : CliCommandException(message, cause) {
  enum class Type {
    HELP, ERROR, MESSAGE
  }
}

private data class OptInvocation(val opt: Option, val inv: OptionParser.Invocation)
private data class OptParseResult(val consumed: Int, val unknown: List<String>, val known: List<OptInvocation>)

internal fun prefixOptionSepartor(name: String) = when {
  name.length < 2 || name[0].isLetterOrDigit() -> "" to name
  name.length > 2 && name[0] == name[1] -> name.slice(0..1) to name.substring(2)
  else -> name.substring(0, 1) to name.substring(1)
}

internal fun jaroWinklerSimilarity(s1: String, s2: String): Double {
  // Unlike classic Jaro-Winkler, we don't set a limit on the prefix length
  val prefixLength = s1.commonPrefixWith(s2).length
  val jaro = jaroSimilarity(s1, s2)
  val winkler = jaro + (0.1 * prefixLength * (1 - jaro))
  return min(winkler, 1.0)
}

private fun jaroSimilarity(s1: String, s2: String): Double {
  if (s1.isEmpty() && s2.isEmpty()) return 1.0
  else if (s1.isEmpty() || s2.isEmpty()) return 0.0
  else if (s1.length == 1 && s2.length == 1) return if (s1[0] == s2[0]) 1.0 else 0.0

  val searchRange: Int = max(s1.length, s2.length) / 2 - 1
  val s2Consumed = BooleanArray(s2.length)
  var matches = 0.0
  var transpositions = 0
  var s2MatchIndex = 0

  for ((i, c1) in s1.withIndex()) {
    val start = max(0, i - searchRange)
    val end = min(s2.lastIndex, i + searchRange)
    for (j in start..end) {
      val c2 = s2[j]
      if (c1 != c2 || s2Consumed[j]) continue
      s2Consumed[j] = true
      matches += 1
      if (j < s2MatchIndex) transpositions += 1
      s2MatchIndex = j
      break
    }
  }

  return when (matches) {
    0.0 -> 0.0
    else -> (matches / s1.length +
      matches / s2.length +
      (matches - transpositions) / matches) / 3.0
  }
}
