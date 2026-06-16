package io.alice.platform.api.event

import io.alice.platform.api.AliceObject
import kotlin.time.Instant
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

/**
 * Central manager for event registration, subscription, and dispatching.
 *
 * The EventManager coordinates the event system across the entire Alice platform,
 * allowing components to react to platform events and user interactions.
 *
 * ## Event System Architecture
 *
 * The event system supports:
 * - **Event Registration**: Register event types and dispatchers
 * - **Annotated Handlers**: Automatic registration via @EventHandler annotation
 * - **Type-Safe Dispatching**: Dispatch events with compile-time type checking
 * - **Async Processing**: Events are dispatched asynchronously
 *
 * ## Usage Examples
 *
 * ### Direct Dispatcher Registration
 * ```kotlin
 * val manager = alice.events
 *
 * manager.registerEvent(EventDispatcher { event: MessageEvent ->
 *     println("Message received: ${event.message}")
 * })
 * ```
 *
 * ### Type-Based Registration
 * ```kotlin
 * manager.onEvent(MessageEvent::class.java) { event ->
 *     processMessage(event)
 * }
 * ```
 *
 * ### Annotated Event Handlers
 * ```kotlin
 * class MyEventHandler {
 *     @EventHandler(MessageEvent::class)
 *     fun onMessage(event: MessageEvent) {
 *         println("Got message: ${event.text}")
 *     }
 * }
 *
 * val handler = MyEventHandler()
 * manager.registerAnnotatedEvent(handler)
 * ```
 *
 * ### Event Dispatch
 * ```kotlin
 * val event = MessageEvent(
 *     source = "user-123",
 *     text = "Hello, bot!"
 * )
 * manager.dispatch(event)
 * ```
 *
 * ## Event Ordering
 *
 * Events are processed in the order they are dispatched.
 * Multiple handlers for the same event are executed in registration order.
 *
 * ## Error Handling
 *
 * If an event handler throws an exception:
 * - The exception is logged
 * - Other handlers for the same event continue to execute
 * - The exception does not propagate to the dispatcher
 *
 * @see Event for event interface
 * @see EventDispatcher for handling events
 * @see EventHandler for annotation-based registration
 */
interface EventManager : AliceObject {

    /**
     * Registers event handlers from an object annotated with @EventHandler methods.
     *
     * This method scans the object for methods annotated with @EventHandler
     * and automatically registers them as event dispatchers.
     *
     * Multiple handler methods can be defined in a single object,
     * each handling different event types.
     *
     * @param event the object containing @EventHandler annotated methods
     *
     * @sample
     * ```kotlin
     * class MyEventHandler {
     *     @EventHandler(MessageEvent::class)
     *     fun handleMessage(event: MessageEvent) {
     *         println(event.text)
     *     }
     *
     *     @EventHandler(PresenceEvent::class)
     *     fun handlePresence(event: PresenceEvent) {
     *         println(event.status)
     *     }
     * }
     *
     * manager.registerAnnotatedEvent(MyEventHandler())
     * ```
     */
    fun registerAnnotatedEvent(event: Any)

    /**
     * Registers a typed event dispatcher.
     *
     * The dispatcher will receive all events of its generic type (E) and subtypes.
     * This allows registration without specifying the event class explicitly.
     *
     * @param E the event type (covariant)
     * @param dispatcher the dispatcher to handle events
     *
     * @sample
     * ```kotlin
     * val dispatcher: EventDispatcher<MessageEvent> = EventDispatcher { event ->
     *     println("Received message: ${event.text}")
     * }
     * manager.registerEvent(dispatcher)
     * ```
     */
    fun <E : Event> registerEvent(dispatcher: EventDispatcher<in E>)

    /**
     * Registers an event dispatcher for a specific event type.
     *
     * The dispatcher will receive all events of the specified type and its subtypes.
     *
     * @param E the event type
     * @param type the class of the event type
     * @param dispatcher the dispatcher to handle events
     *
     * @sample
     * ```kotlin
     * manager.onEvent(MessageEvent::class.java) { event ->
     *     println("Message: ${event.text}")
     * }
     * ```
     */
    fun <E : Event> onEvent(type: Class<E>, dispatcher: EventDispatcher<E>)

    /**
     * Dispatches an event to all registered handlers asynchronously.
     *
     * The event is delivered to all registered dispatchers of compatible types.
     * Dispatching is non-blocking; the function completes after queuing the event,
     * not after all handlers have finished.
     *
     * @param E the event type
     * @param event the event to dispatch
     *
     * @sample
     * ```kotlin
     * val event = MessageEvent(
     *     source = "user-123",
     *     text = "Hello world"
     * )
     * manager.dispatch(event)
     * ```
     */
    suspend fun <E : Event> dispatch(event: E)
}

/**
 * Base interface for all events in the Alice platform.
 *
 * Events represent significant occurrences in the system:
 * - User messages
 * - Presence changes
 * - Connection status changes
 * - Custom platform events
 *
 * ## Event Properties
 *
 * All events have:
 * - **Source**: The origin of the event (user ID, channel, etc.)
 * - **Timestamp**: When the event occurred
 *
 * ## Creating Custom Events
 *
 * ```kotlin
 * class MyCustomEvent(
 *     override val source: Any,
 *     override val timestamp: Instant,
 *     val customData: String
 * ) : Event {
 *     override val alice = // ...
 *     override val logger = // ...
 * }
 * ```
 *
 * ## Event Hierarchy
 *
 * The Event interface is the base for all event types.
 * Platform-specific events (MessageEvent, PresenceEvent, etc.) extend this interface.
 *
 * @property source the source/origin of this event
 * @property timestamp the time when this event occurred
 * @see EventManager for event dispatching
 * @see EventDispatcher for event handling
 */
interface Event : AliceObject {
    /**
     * The source of this event.
     *
     * This can be a user ID, channel name, connection identifier,
     * or any identifier indicating where the event originated.
     */
    val source: Any

    /**
     * The time when this event occurred.
     *
     * Provides precise timing information for event ordering and logging.
     */
    @ExperimentalTime
    val timestamp: Instant
}

/**
 * Functional interface for handling events of a specific type.
 *
 * EventDispatchers are called when events matching their type are dispatched.
 * Multiple dispatchers can be registered for the same event type.
 *
 * ## Usage Examples
 *
 * ### Inline Dispatcher
 * ```kotlin
 * val dispatcher = EventDispatcher<MessageEvent> { event ->
 *     println("Message from ${event.source}: ${event.text}")
 * }
 * manager.registerEvent(dispatcher)
 * ```
 *
 * ### Object Method Reference
 * ```kotlin
 * class EventHandler {
 *     fun handleMessage(event: MessageEvent) {
 *         println("Got message: ${event.text}")
 *     }
 * }
 *
 * val handler = EventHandler()
 * manager.onEvent(MessageEvent::class.java) { event ->
 *     handler.handleMessage(event)
 * }
 * ```
 *
 * ## Error Handling
 *
 * If the dispatcher throws an exception:
 * - The exception is logged by the EventManager
 * - Other registered dispatchers continue to execute
 * - The exception does not propagate to the event source
 *
 * @param E the type of events this dispatcher handles
 * @see EventManager.registerEvent for registration
 * @see EventManager.onEvent for type-based registration
 */
fun interface EventDispatcher<E : Event> {
    /**
     * Handles the given event.
     *
     * This method is called by the EventManager when an event of the
     * appropriate type is dispatched.
     *
     * Implementations should handle exceptions internally to avoid
     * disrupting other event handlers.
     *
     * @param event the event to handle
     */
    fun handle(event: E)
}

/**
 * Annotation for marking event handler methods.
 *
 * Methods annotated with @EventHandler are automatically registered
 * as event handlers when the containing object is passed to
 * [EventManager.registerAnnotatedEvent].
 *
 * ## Usage
 *
 * ```kotlin
 * class MyComponent {
 *     @EventHandler(MessageEvent::class)
 *     fun onMessage(event: MessageEvent) {
 *         println("Message received")
 *     }
 *
 *     @EventHandler(UserJoinedEvent::class)
 *     fun onUserJoined(event: UserJoinedEvent) {
 *         println("User joined")
 *     }
 * }
 * ```
 *
 * ## Requirements
 *
 * Annotated methods must:
 * - Be non-private
 * - Accept exactly one parameter of type Event (or a subtype)
 * - Not throw checked exceptions
 *
 * @property value the event type class this handler processes
 * @target FUNCTION specifies this annotation applies to methods
 * @retention RUNTIME allows reflection-based discovery
 * @see EventManager.registerAnnotatedEvent for registration
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
    /**
     * The event type this handler processes.
     *
     * All events of this type and its subtypes will be delivered to this handler.
     */
    val value: KClass<out Event>
)
