package io.alice.platform.api.modules

import io.alice.platform.api.Alice
import io.alice.platform.api.AliceObject
import io.alice.platform.api.Version
import io.alice.platform.api.objects.NamedObjectCollection

/**
 * Provider for managing platform modules and extensions.
 *
 * ModuleProvider allows dynamic registration, configuration, and lifecycle management
 * of modules that extend the Alice platform's functionality.
 *
 * Modules are registered by unique identifiers and can specify version requirements.
 * The provider inherits from [NamedObjectCollection] allowing name-based module lookup.
 *
 * ## Module Lifecycle
 *
 * 1. **Registration** - Module is registered by ID
 * 2. **Configuration** - Version and apply settings are configured
 * 3. **Application** - Module.apply() is called with the Alice instance
 * 4. **Active** - Module provides its services to the platform
 *
 * ## Usage Examples
 *
 * ### Basic Module Registration
 * ```kotlin
 * alice.modules { provider ->
 *     provider.id("auth-module") version "1.0.0" apply true
 *     provider.id("database-module") version "2.1.0" apply true
 * }
 * ```
 *
 * ### Programmatic Module Class Registration
 * ```kotlin
 * provider.apply(DatabaseModule::class.java)
 * provider.apply(AuthenticationModule::class.java)
 * ```
 *
 * ### Retrieving Registered Modules
 * ```kotlin
 * val modules = provider.values() // Get all modules
 * val authModule = provider.named("auth-module").getOrNull()
 * ```
 *
 * ## Module Discovery
 *
 * As a [NamedObjectCollection], the ModuleProvider provides:
 * - `named(name)` - Get module by name
 * - `mapped()` - Get all modules as a map
 * - `keys()` - Get all module names
 * - `values()` - Get all module instances
 * - `entries()` - Get all name-module pairs
 *
 * ## Version Management
 *
 * Modules can specify version requirements:
 * ```kotlin
 * provider.id("db-module") version "1.2.3" apply true
 * provider.id("auth-module") version Version.of(2, 0, 0) apply true
 * ```
 *
 * @see Module for the Module interface
 * @see ModuleSpec for module specification
 * @see NamedObjectCollection for collection operations
 */
interface ModuleProvider : NamedObjectCollection<Module>, AliceObject {

    /**
     * Registers a module by unique identifier.
     *
     * Returns a [ModuleSpec] allowing further configuration of the module
     * such as version specification and application settings.
     *
     * @param id the unique identifier for the module
     * @return a module specification builder
     *
     * @sample
     * ```kotlin
     * provider.id("my-module") version "1.0.0" apply true
     * ```
     */
    fun id(id: String): ModuleSpec

    /**
     * Applies a module class directly.
     *
     * The module class is instantiated and applied immediately.
     * The module must have a no-argument constructor.
     *
     * @param module the module class to apply
     *
     * @sample
     * ```kotlin
     * provider.apply(DatabaseModule::class.java)
     * provider.apply(AuthenticationModule::class.java)
     * ```
     */
    fun apply(module: Class<out Module>)
}

/**
 * Specification builder for configuring modules.
 *
 * ModuleSpec provides a fluent API for configuring module properties
 * like version requirements before applying the module.
 *
 * ## Configuration Flow
 *
 * ```kotlin
 * provider.id("module-name")
 *     .version("1.0.0")
 *     .apply(true)  // or false to disable
 * ```
 *
 * @see ModuleProvider.id for starting module specification
 * @see Module for module implementation
 */
interface ModuleSpec : ModuleSpecConfigure {

    /**
     * Specifies the module version requirement as a string.
     *
     * Version string format: `major.minor.patch[-release]`
     * Examples: "1.0.0", "2.1.3-BETA", "1.2.0-RC1"
     *
     * @param version the version string
     * @return a configuration builder for further setup
     *
     * @sample
     * ```kotlin
     * provider.id("my-module") version "1.2.0"
     * provider.id("my-module") version "2.0.0-BETA"
     * ```
     */
    infix fun version(version: String): ModuleSpecConfigure

    /**
     * Specifies the module version requirement as a Version object.
     *
     * @param version the Version object
     * @return a configuration builder for further setup
     *
     * @sample
     * ```kotlin
     * val v1 = Version.of(1, 2, 3)
     * provider.id("my-module") version v1
     * ```
     */
    infix fun version(version: Version): ModuleSpecConfigure
}

/**
 * Configuration finalization interface for modules.
 *
 * Allows specifying whether the module should be applied (activated) or not.
 * This is the final step in module configuration.
 *
 * @see ModuleSpec for the specification builder
 */
interface ModuleSpecConfigure {

    /**
     * Specifies whether the module should be applied (activated).
     *
     * Set to `true` to activate the module, `false` to register but not activate.
     *
     * @param apply true to apply the module, false to skip activation
     *
     * @sample
     * ```kotlin
     * provider.id("module1") version "1.0" apply true  // Applied
     * provider.id("module2") version "2.0" apply false // Registered but not applied
     * ```
     */
    infix fun apply(apply: Boolean)
}

/**
 * Base interface for Alice platform modules.
 *
 * Modules provide additional functionality and services to the Alice platform.
 * They are applied during initialization to extend or configure the platform.
 *
 * ## Module Implementation
 *
 * ```kotlin
 * class MyModule : Module {
 *     override fun apply(alice: Alice) {
 *         // Register services
 *         alice.objects.register("my-service", MyService())
 *
 *         // Configure event handlers
 *         alice.events { manager ->
 *             manager.onEvent(MessageEvent::class.java) { event ->
 *                 // Handle events
 *             }
 *         }
 *
 *         // Setup extensions
 *         alice.extensions { ext ->
 *             ext.register("my-extension", MyExtension())
 *         }
 *     }
 * }
 * ```
 *
 * ## Module Lifecycle
 *
 * Modules are applied in registration order during the INIT state.
 * All modules are applied before the platform transitions to START state.
 *
 * ## Accessing Platform Services
 *
 * The Alice parameter provides access to all platform services:
 * - Configuration
 * - Engines
 * - Objects
 * - Events
 * - Extensions
 * - Other modules
 *
 * @see ModuleProvider for module registration
 * @see Alice for available platform services
 */
interface Module {

    /**
     * Applies this module to the Alice platform.
     *
     * This method is called once when the module is activated,
     * providing access to the root Alice platform instance.
     *
     * Use this to:
     * - Register services and components
     * - Configure event handlers
     * - Setup extensions
     * - Perform initialization
     *
     * @param alice the root platform instance
     *
     * @sample
     * ```kotlin
     * override fun apply(alice: Alice) {
     *     // Register a named service
     *     alice.objects { factory ->
     *         val service = MyService(alice.configuration)
     *         factory.register("my-service", service)
     *     }
     *
     *     // Register event handlers
     *     alice.events { manager ->
     *         manager.onEvent(MyEvent::class.java) { event ->
     *             handleEvent(event)
     *         }
     *     }
     *
     *     // Access configuration
     *     val config = alice.configuration
     *         .get("module.setting", String::class.java)
     *         .getOrNull()
     *
     *     alice.logger.info("Module initialized")
     * }
     * ```
     */
    fun apply(alice: Alice)
}
