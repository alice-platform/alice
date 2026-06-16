package io.alice.platform.api.dsl

import io.alice.platform.api.Alice
import io.alice.platform.api.objects.ScriptAction
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path

fun Alice.import(scriptPath: String) =
    objects.scripts.import(scriptPath)

fun Alice.import(scriptPath: Path) =
    objects.scripts.import(scriptPath)

fun Alice.import(scriptPath: File) =
    objects.scripts.import(scriptPath)

fun Alice.import(scriptPath: URI) =
    objects.scripts.import(scriptPath)

fun Alice.import(scriptPath: URL) =
    objects.scripts.import(scriptPath)

fun Alice.beforeInit(handler: Alice.() -> Unit) =
    objects.scripts.before(ScriptAction.INIT, handler)

fun Alice.afterInit(handler: Alice.() -> Unit) =
    objects.scripts.after(ScriptAction.INIT, handler)

fun Alice.beforeEval(handler: Alice.() -> Unit) =
    objects.scripts.before(ScriptAction.EVAL, handler)

fun Alice.afterEval(handler: Alice.() -> Unit) =
    objects.scripts.after(ScriptAction.EVAL, handler)

fun Alice.beforeStart(handler: Alice.() -> Unit) =
    objects.scripts.before(ScriptAction.START, handler)

fun Alice.afterStart(handler: Alice.() -> Unit) =
    objects.scripts.after(ScriptAction.START, handler)

fun Alice.beforeStop(handler: Alice.() -> Unit) =
    objects.scripts.before(ScriptAction.STOP, handler)

fun Alice.afterStop(handler: Alice.() -> Unit) =
    objects.scripts.after(ScriptAction.STOP, handler)

fun Alice.module(id: String) =
    modules.id(id)