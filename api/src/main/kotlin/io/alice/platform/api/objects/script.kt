package io.alice.platform.api.objects

import io.alice.platform.api.AliceObject
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path

interface ScriptManager : AliceObject {
    fun import(script: File)
    fun import(script: Path)
    fun import(script: String)
    fun import(script: URI)
    fun import(script: URL)

    fun register(cp: File)
    fun register(cp: Path)
    fun register(cp: String)
    fun register(cp: URI)
    fun register(cp: URL)

    fun <O : AliceObject> before(action: ScriptAction, handler: O.() -> Unit)
    fun <O : AliceObject> after(action: ScriptAction, handler: O.() -> Unit)
}

enum class ScriptAction(val isIO: Boolean) {
    INIT(false),
    EVAL(false),
    START(true),
    STOP(true)
}