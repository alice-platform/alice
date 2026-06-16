package io.alice.platform.api

import io.alice.platform.api.config.ConfigurationProvider
import io.alice.platform.api.engine.EngineProvider
import io.alice.platform.api.event.EventManager
import io.alice.platform.api.extensions.ExtensionsProvider
import io.alice.platform.api.modules.ModuleProvider
import io.alice.platform.api.objects.ObjectFactory
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Presents a root component of platform.
 *
 * Interpretation of code is to be a Gradle-like configurable platform.
 */
interface Alice : AliceObjectOperator {

    /**
     * Platform version
     */
    val version: Version

    /**
     * Configuration Provider
     *
     * Provides configuration from defined in [`apply(<path>)`][ConfigurationProvider.apply] a path of configuration, when existed.
     * Also, will load default configuration solutions provided of this platform.
     */
    val configuration: ConfigurationProvider

    /**
     * This is a source of this component. Without providing Chat Engine your platform will not work.
     *
     * It is not only apply required engine, it will apply [modules][ModuleProvider] and [extensions][io.alice.platform.api.extensions.ExtensionsProvider] for better configurable solution.
     */
    val engines: EngineProvider

    /**
     * This is a mostly supportive component. It helps provides some solutions to your environment.
     */
    val modules: ModuleProvider

    /**
     * Helps safety your code workflow.
     */
    val objects: ObjectFactory

    /**
     * Manage [events][io.alice.platform.api.event.Event] indication to handle actions
     */
    val events: EventManager

    /**
     * Extensions helps to better configure your environment
     */
    val extensions: ExtensionsProvider

    /**
     * Configuration Provider
     *
     * Provides configuration from defined in [`apply(<path>)`][ConfigurationProvider.apply] a path of configuration, when existed.
     * Also, will load default configuration solutions provided of this platform.
     */
    @AliceDsl
    fun configuration(configuration: Consumer<ConfigurationProvider>) {
        this.configuration.also { configuration.consume(it) }
    }

    /**
     * This is a source of this component. Without providing Chat Engine your platform will not work.
     *
     * It is not only apply required engine, it will apply [modules][ModuleProvider] and [extensions][ExtensionsProvider] for better configurable solution.
     */
    @AliceDsl
    fun engines(engines: Consumer<EngineProvider>) {
        this.engines.also { engines.consume(it) }
    }

    /**
     * This is a mostly supportive component. It helps provides some solutions to your environment.
     */
    @AliceDsl
    fun modules(modules: Consumer<ModuleProvider>) {
        this.modules.also { modules.consume(it) }
    }

    /**
     * Helps safety your code workflow.
     */
    @AliceDsl
    fun objects(objects: Consumer<ObjectFactory>) {
        this.objects.also { objects.consume(it) }
    }

    /**
     * Manage [events][io.alice.platform.api.event.Event] indication to handle actions
     */
    @AliceDsl
    fun events(events: Consumer<EventManager>) {
        this.events.also { events.consume(it) }
    }

    /**
     * Extensions helps to better configure your environment
     */
    @AliceDsl
    fun extensions(extensions: Consumer<ExtensionsProvider>) {
        this.extensions.also { extensions.consume(it) }
    }

    /**
     * Safety handle your interaction before some [state][AliceState] has been started
     * @param state the action state
     * @param alice root component
     */
    fun before(state: AliceState, alice: Consumer<Alice>)

    /**
     * Safety handle your interaction after some [state][AliceState] has been ended
     * @param state the action state
     * @param alice root component
     */
    fun after(state: AliceState, alice: Consumer<Alice>)
}

/**
 * Runtime State
 */
enum class AliceState {
    /**
     * State of Initialization
     */
    INIT,

    /**
     * State of running environment
     */
    START,

    /**
     * State of shutting down environment
     */
    STOP
}

/**
 * Lazy Late Initializer for avoid nulls
 */
class LateInit<T>(
    private val throwable: () -> Throwable
) : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        value ?: throw throwable()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}