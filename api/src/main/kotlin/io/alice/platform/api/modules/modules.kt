package io.alice.platform.api.modules

import io.alice.platform.api.Alice
import io.alice.platform.api.AliceObject
import io.alice.platform.api.Version
import io.alice.platform.api.objects.NamedObjectCollection

interface ModuleProvider : NamedObjectCollection<Module>, AliceObject {
  fun id(id: String): ModuleSpec
  fun apply(module: Class<out Module>)
}

interface ModuleSpec : ModuleSpecConfigure {
  infix fun version(version: String): ModuleSpecConfigure
  infix fun version(version: Version): ModuleSpecConfigure
}

interface ModuleSpecConfigure {
  infix fun apply(apply: Boolean)
}

interface Module {
  fun apply(alice: Alice)
}
