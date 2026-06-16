package io.alice.platform.api.engine.chat.command

import io.alice.platform.api.AliceObject
import io.alice.platform.api.Predicate
import io.alice.platform.api.engine.EngineException
import io.alice.platform.api.engine.chat.ChatEngine

interface CommandProvider<TEvent, TCEvent : CommandEvent<TEvent>, TEngine : ChatEngine<*, TEvent, TCEvent>> :
    AliceObject {
    val engine: TEngine
    val prefixManager: PrefixManager<TEvent>
    val names: Set<String>
    val commands: Set<Command<TEvent, TCEvent>>

    fun register(command: Command<TEvent, TCEvent>)
    fun unregister(name: String, aliased: Boolean = false)

    fun handle(event: TEvent)
}

interface PrefixManager<TEvent> {
    val default: String
    fun get(event: TEvent): String
}

interface CommandEvent<out TEvent> {
    val root: TEvent
    val message: String
    val command: String
    val argv: Array<String>
    val prefix: String

    val sender: Any
    val channel: Any
}

interface Command<TEvent, TCEvent : CommandEvent<TEvent>> {
    val name: String
    val alias: Array<String>
    val description: String?
    val access: Predicate<TCEvent>
    val group: Group<TEvent, TCEvent>

    @Throws(Exception::class)
    fun execute(event: TCEvent)

    class Group<TEvent, TCEvent : CommandEvent<TEvent>>(
        name: String,
        val displayName: String = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
        access: Predicate<TCEvent> = Predicate { true }
    ) : Predicate<TCEvent> by access {
        val name: String = name.mapIndexed { index, c ->
            when {
                index == 0 -> c.lowercase()
                c.isUpperCase() && index > 0 -> "_${c.lowercase()}"
                c == ' ' -> "_"
                else -> "$c"
            }
        }.joinToString("")
    }
}

abstract class AbstractCommand<TEvent, TCEvent : CommandEvent<TEvent>>(
    final override val name: String,
    final override val alias: Array<String> = arrayOf(),
    final override val description: String? = null,
    final override val access: Predicate<TCEvent> = Predicate { true },
    final override val group: Command.Group<TEvent, TCEvent> = Command.Group("unknown")
) : Command<TEvent, TCEvent>

open class CommandException : EngineException {
    constructor() : super()
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
