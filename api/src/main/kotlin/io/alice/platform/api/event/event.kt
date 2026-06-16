package io.alice.platform.api.event

import io.alice.platform.api.AliceObject
import kotlin.time.Instant
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

interface EventManager : AliceObject {
  fun registerAnnotatedEvent(event: Any)
  fun <E : Event> registerEvent(dispatcher: EventDispatcher<in E>)
  fun <E : Event> onEvent(type: Class<E>, dispatcher: EventDispatcher<E>)

  suspend fun <E : Event> dispatch(event: E)
}

interface Event : AliceObject {
  val source: Any
  @ExperimentalTime
  val timestamp: Instant
}

fun interface EventDispatcher<E : Event> {
  fun handle(event: E)
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
  val value: KClass<out Event>
)
