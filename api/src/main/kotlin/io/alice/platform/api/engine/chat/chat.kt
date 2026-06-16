package io.alice.platform.api.engine.chat

import io.alice.platform.api.Alice
import io.alice.platform.api.AliceObjectOperator
import io.alice.platform.api.Consumer
import io.alice.platform.api.engine.chat.command.Command
import io.alice.platform.api.engine.chat.command.CommandEvent
import io.alice.platform.api.engine.chat.command.CommandProvider

/**
 * The Chat Engine
 */
interface ChatEngine<TInstance, TEvent, TCEvent : CommandEvent<TEvent>> : AliceObjectOperator {
  val instance: TInstance
  val provider: CommandProvider<TEvent, TCEvent, ChatEngine<TInstance, TEvent, TCEvent>>

  fun registerCommand(command: Command<TEvent, TCEvent>)
  fun unregisterCommand(name: String, aliased: Boolean = false)

  interface Config {
    val token: String
  }
}