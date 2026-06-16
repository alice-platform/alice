package io.alice.platform.api.config

import io.alice.platform.api.AliceObject
import io.alice.platform.api.objects.Provider
import java.io.File
import java.nio.file.Path
import java.util.*

/**
 * Configuration provider for managing bot environment and application settings.
 *
 * The ConfigurationProvider allows centralized access to configuration from multiple sources:
 * - System properties (JVM properties)
 * - Environment variables
 * - Local configuration files
 * - Default platform configuration
 *
 * ## Supported File Formats
 *
 * The provider supports the following configuration file formats:
 * - `*.yaml` - YAML format
 * - `*.yml` - YAML format
 * - `*.toml` - TOML format
 * - `*.json` - JSON format
 * - `*.conf` - Configuration format
 * - `*.properties` - Java properties format
 *
 * ## Configuration Hierarchy
 *
 * Configuration sources are merged with the following priority (highest to lowest):
 * 1. Programmatically applied configuration (via [apply] methods)
 * 2. Local configuration files
 * 3. System properties
 * 4. Environment variables
 * 5. Default platform configuration
 *
 * ## Usage Examples
 *
 * ### Loading from File
 * ```kotlin
 * val config: ConfigurationProvider = alice.configuration
 * config.apply("application.yaml")
 * config.apply("/etc/app/config.toml")
 * ```
 *
 * ### Accessing Properties
 * ```kotlin
 * // Get configuration node
 * val dbNode = config.getProperty("database")
 *     .ifPresent(Consumer { node ->
 *         val url = node.getChild("url")
 *     })
 *
 * // Get environment variable
 * val apiKey = config.getEnvironment("API_KEY")
 *     .getOrNull()
 *
 * // Get typed value
 * val timeout = config.get("server.timeout", Integer::class.java)
 *     .getOrElse(30000)
 * ```
 *
 * ### Type Conversion
 * ```kotlin
 * data class DatabaseConfig(
 *     val url: String,
 *     val port: Int,
 *     val poolSize: Int
 * )
 *
 * val dbConfig = config.get("database", DatabaseConfig::class.java)
 *     .getOrNull()
 * ```
 *
 * ## Configuration Nodes
 *
 * The [Node] type represents hierarchical configuration structure,
 * allowing access to nested properties:
 *
 * ```kotlin
 * val node: Provider<Node> = config.getProperty("app.server")
 * node.ifPresent(Consumer { n ->
 *     val host = n.get("host")
 *     val port = n.get("port")
 * })
 * ```
 *
 * @see Node for configuration structure
 * @see Provider for reactive configuration access
 */
interface ConfigurationProvider : AliceObject {

    /**
     * Retrieves a configuration node from system properties.
     *
     * System properties are typically set via JVM arguments:
     * ```
     * java -Dapp.name=MyBot -Dapp.version=1.0
     * ```
     *
     * @param path the property path (e.g., "app.name" or "app.server.port")
     * @return a provider containing the configuration node if found
     *
     * @sample
     * ```kotlin
     * val appName = config.getGlobalProperty("app.name")
     *     .getOrNull()
     * ```
     */
    fun getGlobalProperty(path: String): Provider<Node>

    /**
     * Retrieves an environment variable value.
     *
     * Environment variables are obtained from the system environment.
     *
     * @param env the environment variable name (e.g., "API_KEY", "DATABASE_URL")
     * @return a provider containing the environment variable value if found
     *
     * @sample
     * ```kotlin
     * val databaseUrl = config.getEnvironment("DATABASE_URL")
     *     .getOrElse("localhost:5432")
     * ```
     */
    fun getEnvironment(env: String): Provider<String>

    /**
     * Retrieves a configuration node from loaded configuration files.
     *
     * This accesses configuration from files applied via [apply] methods
     * and default platform configuration.
     *
     * @param path the property path in dot notation (e.g., "database.url")
     * @return a provider containing the configuration node if found
     *
     * @sample
     * ```kotlin
     * val dbUrl = config.getProperty("database.url")
     *     .ifPresent(Consumer { node ->
     *         val value = node.asString()
     *     })
     * ```
     */
    fun getProperty(path: String): Provider<Node>

    /**
     * Retrieves and converts configuration to a specific type.
     *
     * This operator searches all configuration sources (local properties,
     * system properties, environment variables) and attempts to convert
     * the result to the specified type.
     *
     * @param T the target type to convert to
     * @param path the property path
     * @param type the class of the target type
     * @return a provider containing the converted value if found
     *
     * @sample
     * ```kotlin
     * val timeout = config["server.timeout", Integer::class.java]
     *     .getOrElse(30000)
     *
     * val enabled = config["feature.experimental", Boolean::class.java]
     *     .getOrElse(false)
     * ```
     */
    operator fun <T : Any> get(path: String, type: Class<T>): Provider<T>

    /**
     * Applies configuration from a Java Properties object.
     *
     * Configuration from the Properties object takes precedence over
     * previously loaded configuration.
     *
     * @param config the Properties object containing configuration key-value pairs
     *
     * @sample
     * ```kotlin
     * val props = Properties()
     * props.load(FileInputStream("app.properties"))
     * config.apply(props)
     * ```
     */
    fun apply(config: Properties)

    /**
     * Applies configuration from a file.
     *
     * The file format is automatically detected from the file extension.
     * Configuration from the file takes precedence over previously loaded configuration.
     *
     * @param configPath path to the configuration file
     * @throws IllegalArgumentException if file format is not supported
     * @throws java.io.IOException if file cannot be read
     *
     * @sample
     * ```kotlin
     * config.apply(File("/etc/app/config.yaml"))
     * ```
     */
    fun apply(configPath: File)

    /**
     * Applies configuration from a file path.
     *
     * The file format is automatically detected from the file extension.
     * Configuration from the file takes precedence over previously loaded configuration.
     *
     * @param configPath path to the configuration file
     * @throws IllegalArgumentException if file format is not supported
     * @throws java.io.IOException if file cannot be read
     *
     * @sample
     * ```kotlin
     * config.apply(Paths.get("/etc/app/config.yaml"))
     * ```
     */
    fun apply(configPath: Path)

    /**
     * Applies configuration from a file path string.
     *
     * The file format is automatically detected from the file extension.
     * Configuration from the file takes precedence over previously loaded configuration.
     *
     * The path string can be relative or absolute:
     * - Relative: "config/app.yaml" (relative to working directory)
     * - Absolute: "/etc/app/config.yaml"
     * - URL-like: "file:///etc/app/config.yaml"
     *
     * @param configPath path to the configuration file as string
     * @throws IllegalArgumentException if file format is not supported
     * @throws java.io.IOException if file cannot be read
     *
     * @sample
     * ```kotlin
     * config.apply("config/application.yaml")
     * config.apply("${System.getProperty("user.home")}/.app/config.properties")
     * ```
     */
    fun apply(configPath: String)
}

/**
 * Represents a node in the hierarchical configuration structure.
 *
 * Configuration nodes provide access to nested properties and values,
 * allowing traversal and retrieval of configuration data in a type-safe manner.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val config: ConfigurationProvider = // ...
 * val serverNode = config.getProperty("server").get()
 *
 * // Access nested properties
 * val host = serverNode.getChild("host").getOrNull()
 * val port = serverNode.getChild("port").asInt()
 *
 * // Convert to specific types
 * val timeout = serverNode.asMap()["timeout"] as Int
 * ```
 *
 * @see ConfigurationProvider for accessing configuration nodes
 */
interface Node {
    // Node interface is intentionally left for implementation
    // Details will be documented in implementation classes
}
