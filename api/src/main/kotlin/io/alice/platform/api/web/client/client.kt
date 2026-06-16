package io.alice.platform.api.web.client

import io.alice.platform.api.Consumer
import io.alice.platform.api.objects.Provider
import io.alice.platform.api.web.Method
import io.alice.platform.api.web.Request
import io.alice.platform.api.web.Response
import java.net.URI

interface WebClient {
    fun handleRequest(request: Consumer<Request.Builder>): Provider<Response>

    fun handle(uri: URI, method: Method, body: Any? = null, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handleRequest {
            it.method(method)
            it.uri(uri)
            if (body != null) it.body(body)
        }

    fun get(uri: URI, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handle(uri, Method.GET, request = request)

    fun head(uri: URI, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handle(uri, Method.HEAD, request = request)

    fun post(uri: URI, body: Any? = null, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handle(uri, Method.POST, body, request)

    fun put(uri: URI, body: Any? = null, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handle(uri, Method.PUT, body, request)

    fun patch(uri: URI, body: Any? = null, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handle(uri, Method.PATCH, body, request)

    fun delete(uri: URI, body: Any? = null, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handle(uri, Method.DELETE, request = request)

    fun options(uri: URI, body: Any? = null, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handle(uri, Method.OPTIONS, body, request)

    fun trace(uri: URI, body: Any? = null, request: Consumer<Request.Builder> = Consumer {}): Provider<Response> =
        handle(uri, Method.TRACE, body, request)
}
