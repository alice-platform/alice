package io.alice.platform.api.objects

import io.alice.platform.api.AliceObject
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path

/**
 * Manager for importing and executing external scripts.
 *
 * ScriptManager provides integration with external scripts, allowing dynamic
 * code execution at various platform lifecycle stages. This enables:
 * - Dynamic behavior configuration
 * - Plugin-like extensibility
 * - Runtime customization
 *
 * Similar to Gradle's buildSrc and init scripts, Alice supports scripts
 * that can hook into the platform lifecycle.
 *
 * ## Script Types
 *
 * The platform supports scripts at multiple lifecycle stages:
 * - **INIT** - Platform initialization (non-blocking)
 * - **EVAL** - Evaluation phase (non-blocking)
 * - **START** - Platform startup (blocking/IO)
 * - **STOP** - Platform shutdown (blocking/IO)
 *
 * ```kotlin
 * val scripts = alice.objects.scripts
 *
 * scripts.before(ScriptAction.INIT) {
 *     println("Initializing...")
 *     // alice is available as 'this'
 * }
 *
 * scripts.after(ScriptAction.START) {
 *     println("Started!")
 * }
 * ```
 *
 * ## Script Import
 *
 * Scripts can be loaded from multiple sources:
 * ```kotlin
 * scripts.import(File("init.kts"))
 * scripts.import(Paths.get("/opt/app/custom.kts"))
 * scripts.import("classpath:scripts/setup.kts")
 * scripts.import(URI("https://example.com/script.kts"))
 * scripts.import(URL("https://example.com/script.kts"))
 * ```
 *
 * ## Classpath Registration
 *
 * Register classpath entries for script dependencies:
 * ```kotlin
 * scripts.register(File("libs/mylib.jar"))
 * scripts.register("/opt/libs")
 * scripts.register("classpath:libs")
 * ```
 *
 * ## Script Context
 *
 * Scripts execute with the platform ([AliceObject]) as receiver:
 * ```kotlin
 * // In a script file:
 * logger.info("Current version: ${version}")
 * configuration.apply("custom.yaml")
 * engines { provider ->
 *     // ... configure engines
 * }
 * ```
 *
 * ## Lifecycle Hooks
 *
 * The [before] and [after] hooks let scripts participate in lifecycle:
 * ```kotlin
 * scripts.before(ScriptAction.INIT) {
 *     // O is bound to the AliceObject
 *     logger.info("Initializing platform")
 * }
 *
 * scripts.after(ScriptAction.START) {
 *     // Called after platform starts
 *     logger.info("Platform started successfully")
 * }
 * ```
 *
 * ## Use Cases
 *
 * Common uses for scripts:
 * - Environment-specific configuration
 * - Dynamic module loading
 * - Custom initialization logic
 * - Testing and debugging
 * - Gradual migration of configuration
 *
 * @see ScriptAction for lifecycle stages
 * @see AliceObject for script context
 */
interface ScriptManager : AliceObject {

    /**
     * Imports and loads a script from a file.
     *
     * The script file should contain Kotlin code that executes
     * within the Alice platform context.
     *
     * File format is determined by extension:
     * - `.kts` - Kotlin script
     * - `.groovy` - Groovy script (if available)
     *
     * @param script the script file to import
     * @throws IllegalArgumentException if file format is not supported
     * @throws java.io.IOException if file cannot be read
     *
     * @sample
     * ```kotlin
     * alice.objects { factory ->
     *     factory.scripts.import(File("scripts/init.kts"))
     * }
     * ```
     */
    fun import(script: File)

    /**
     * Imports and loads a script from a Path.
     *
     * The script file should contain Kotlin code that executes
     * within the Alice platform context.
     *
     * @param script the script path to import
     * @throws IllegalArgumentException if file format is not supported
     * @throws java.io.IOException if file cannot be read
     */
    fun import(script: Path)

    /**
     * Imports and loads a script from a path string.
     *
     * The path string can be:
     * - File path: "scripts/init.kts"
     * - Absolute: "/opt/alice/scripts/init.kts"
     * - URL: "https://example.com/script.kts"
     * - Classpath: "classpath:scripts/init.kts"
     *
     * @param script the script path as string
     * @throws IllegalArgumentException if format is not supported
     * @throws java.io.IOException if script cannot be loaded
     */
    fun import(script: String)

    /**
     * Imports and loads a script from a URI.
     *
     * Supported schemes:
     * - `file://` - Local file
     * - `http://` / `https://` - Remote script
     * - `classpath:` - Classpath resource
     * - `jar://` - Script from JAR file
     *
     * @param script the script URI
     * @throws IllegalArgumentException if scheme is not supported
     * @throws java.io.IOException if script cannot be loaded
     */
    fun import(script: URI)

    /**
     * Imports and loads a script from a URL.
     *
     * @param script the script URL to load
     * @throws IllegalArgumentException if protocol is not supported
     * @throws java.io.IOException if script cannot be loaded
     */
    fun import(script: URL)

    /**
     * Registers a classpath entry for script dependencies.
     *
     * Script dependencies (libraries, classes) should be added
     * to the classpath before script execution.
     *
     * @param cp the classpath entry (file or directory)
     *
     * @sample
     * ```kotlin
     * scripts.register(File("libs/mylib.jar"))
     * scripts.register(File("lib")) // Directory
     * ```
     */
    fun register(cp: File)

    /**
     * Registers a classpath entry from a Path.
     *
     * @param cp the classpath path
     */
    fun register(cp: Path)

    /**
     * Registers a classpath entry from a path string.
     *
     * Can be:
     * - File path: "libs/mylib.jar"
     * - Directory: "lib"
     * - Classpath: "classpath:libs"
     * - URL: "https://example.com/lib.jar"
     *
     * @param cp the classpath string
     */
    fun register(cp: String)

    /**
     * Registers a classpath entry from a URI.
     *
     * @param cp the classpath URI
     */
    fun register(cp: URI)

    /**
     * Registers a classpath entry from a URL.
     *
     * @param cp the classpath URL
     */
    fun register(cp: URL)

    /**
     * Registers a callback to execute before a lifecycle action.
     *
     * The handler is invoked before the specified action with the platform
     * ([AliceObject]) as the receiver (this context).
     *
     * Useful for setup, configuration, and initialization:
     * ```kotlin
     * scripts.before(ScriptAction.INIT) {
     *     logger.info("Initializing")
     *     configuration.apply("setup.yaml")
     * }
     * ```
     *
     * @param O the AliceObject type
     * @param action the lifecycle action to listen for
     * @param handler lambda with O as receiver
     *
     * @sample
     * ```kotlin
     * scripts.before(ScriptAction.START) {
     *     // 'this' is the Alice instance
     *     engines { provider ->
     *         provider.values().forEach { it.active = true }
     *     }
     * }
     * ```
     */
    fun <O : AliceObject> before(action: ScriptAction, handler: O.() -> Unit)

    /**
     * Registers a callback to execute after a lifecycle action.
     *
     * The handler is invoked after the specified action with the platform
     * ([AliceObject]) as the receiver (this context).
     *
     * Useful for cleanup, monitoring, and post-processing:
     * ```kotlin
     * scripts.after(ScriptAction.STOP) {
     *     logger.info("Shutdown complete")
     * }
     * ```
     *
     * @param O the AliceObject type
     * @param action the lifecycle action to listen for
     * @param handler lambda with O as receiver
     */
    fun <O : AliceObject> after(action: ScriptAction, handler: O.() -> Unit)
}

/**
 * Lifecycle stages where scripts can be executed.
 *
 * Scripts can hook into four main stages of platform execution:
 * 1. **INIT** - Configuration and initialization (non-blocking)
 * 2. **EVAL** - Evaluation phase (non-blocking)
 * 3. **START** - Platform startup and connection (blocking/IO)
 * 4. **STOP** - Platform shutdown and cleanup (blocking/IO)
 *
 * ## Stage Details
 *
 * ### INIT (Non-Blocking)
 * Platform is loading configuration and registering components.
 * - Fast setup operations
 * - Configuration loading
 * - Component registration
 * - Module initialization
 *
 * ### EVAL (Non-Blocking)
 * Evaluation and validation phase.
 * - Configuration validation
 * - Dependency checking
 * - Setup verification
 *
 * ### START (Blocking/IO)
 * Platform is connecting to services and becoming active.
 * - Long-running operations allowed
 * - Database connections
 * - External service initialization
 * - Engine startup
 *
 * ### STOP (Blocking/IO)
 * Platform is shutting down gracefully.
 * - Resource cleanup
 * - Connection closure
 * - Data persistence
 * - Logging and monitoring data
 *
 * @property isIO true if this action involves blocking IO operations
 * @see ScriptManager for script execution
 */
enum class ScriptAction(val isIO: Boolean) {
    /**
     * Initialization phase (non-blocking).
     *
     * Use this stage for:
     * - Loading configuration
     * - Registering modules
     * - Setting up components
     */
    INIT(false),

    /**
     * Evaluation phase (non-blocking).
     *
     * Use this stage for:
     * - Validating configuration
     * - Checking dependencies
     * - Verifying setup
     */
    EVAL(false),

    /**
     * Startup phase (blocking/IO allowed).
     *
     * Use this stage for:
     * - Connecting to services
     * - Starting engines
     * - Initializing long-running tasks
     */
    START(true),

    /**
     * Shutdown phase (blocking/IO allowed).
     *
     * Use this stage for:
     * - Closing connections
     * - Cleanup and finalization
     * - Saving state
     */
    STOP(true)
}
