package io.alice.platform.api.engine.web

import io.alice.platform.api.Consumer
import io.alice.platform.api.engine.Engine
import io.alice.platform.api.web.Method
import io.alice.platform.api.web.Request
import io.alice.platform.api.web.Response

interface WebEngineServer : Engine, Router {
    fun get(path: String, handler: ServerQueryChain) = on(path) { get(handler) }
    fun post(path: String, handler: ServerQueryChain) = on(path) { post(handler) }
    fun put(path: String, handler: ServerQueryChain) = on(path) { put(handler) }
    fun patch(path: String, handler: ServerQueryChain) = on(path) { patch(handler) }
    fun delete(path: String, handler: ServerQueryChain) = on(path) { delete(handler) }
    fun head(path: String, handler: ServerQueryChain) = on(path) { head(handler) }
    fun options(path: String, handler: ServerQueryChain) = on(path) { options(handler) }
    fun trace(path: String, handler: ServerQueryChain) = on(path) { trace(handler) }
}

interface Router {
    fun on(path: String, handler: Route.() -> Unit)
}

interface Route : Router {
    fun chain(handler: ServerQueryChain)
    fun query(method: Method, handler: ServerQueryChain)

    fun get(handler: ServerQueryChain) = query(Method.GET, handler)
    fun post(handler: ServerQueryChain) = query(Method.POST, handler)
    fun put(handler: ServerQueryChain) = query(Method.PUT, handler)
    fun patch(handler: ServerQueryChain) = query(Method.PATCH, handler)
    fun delete(handler: ServerQueryChain) = query(Method.DELETE, handler)
    fun head(handler: ServerQueryChain) = query(Method.HEAD, handler)
    fun options(handler: ServerQueryChain) = query(Method.OPTIONS, handler)
    fun trace(handler: ServerQueryChain) = query(Method.TRACE, handler)
}

interface ServerQueryChain : Request, Consumer<Response.Builder>