package io.alice.platform.api.config

import io.alice.platform.api.AliceObject
import io.alice.platform.api.objects.Provider
import java.io.File
import java.nio.file.Path
import java.util.*

/**
 * Provider allows to configure your bot environment
 *
 * This provider allows use only specific supported configuration extensions:
 * - `*.yaml`
 * - `*.yml`
 * - `*.toml`
 * - `*.json`
 * - `*.conf`
 * - `*.properties`
 */
interface ConfigurationProvider : AliceObject {
  /**
   * Getting [Node] from [Global Properties][System.getProperties]
   *
   * @param path property path
   * @return valued [Node] of [Global Property][System.getProperties]
   */
  fun getGlobalProperty(path: String): Provider<Node>

  /**
   * Getting Environmental Variable
   *
   * @param env Environmental Variable Key
   * @return valued [String] of Environmental Variable
   */
  fun getEnvironment(env: String): Provider<String>

  /**
   * Getting [Node] from Local Properties
   *
   * @param path property path
   * @return valued [Node] of Local Property
   */
  fun getProperty(path: String): Provider<Node>

  /**
   * Getting from booth properties and convert them into specific [type][T]
   *
   * @param T Converted Type Class
   * @param path property path
   * @param type type of [T]
   * @return valued [type][T] of all properties
   */
  operator fun <T : Any> get(path: String, type: Class<T>): Provider<T>

  /**
   * Apply your own configuration
   *
   * @param config configuration
   */
  fun apply(config: Properties)

  /**
   * Apply your own configuration from file
   *
   * @param configPath configuration path
   */
  fun apply(configPath: File)

  /**
   * Apply your own configuration from file
   *
   * @param configPath configuration path
   */
  fun apply(configPath: Path)

  /**
   * Apply your own configuration from file
   *
   * @param configPath configuration path
   */
  fun apply(configPath: String)
}
