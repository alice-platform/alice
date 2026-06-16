package io.alice.platform.api.web

import io.alice.platform.api.Consumer
import io.alice.platform.api.Transformer
import io.alice.platform.api.objects.Provider
import java.io.InputStream
import java.net.InetAddress
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.ByteBuffer
import java.util.*

interface Request : Query {
    val uri: URI
    val method: Method

    interface Builder : Query.Builder {
        fun uri(url: String): Builder
        fun uri(uri: URI): Builder
        fun uri(url: URL): Builder
        fun uri(builder: Consumer<URIBuilder>): Builder
        fun method(method: Method): Builder
        override fun body(body: Any): Builder
        override fun header(key: String, vararg values: String): Builder
        override fun headers(vararg headers: Pair<String, String>): Builder
        override fun headers(headers: Map<String, Collection<String>>): Builder
    }
}

interface Response : Query {
    val request: Request
    val status: Status

    interface Builder : Query.Builder {
        fun status(status: Int): Builder
        fun status(status: Status): Builder
        override fun body(body: Any): Builder
        override fun header(key: String, vararg values: String): Builder
        override fun headers(vararg headers: Pair<String, String>): Builder
        override fun headers(headers: Map<String, Collection<String>>): Builder
    }
}

interface Query {
    val headers: Map<String, Collection<String>>
    val body: Body

    interface Builder {
        fun body(body: Any): Builder
        fun header(key: String, vararg values: String): Builder
        fun headers(vararg headers: Pair<String, String>): Builder
        fun headers(headers: Map<String, Collection<String>>): Builder
    }
}

interface Body {
    fun <T : Any> of(t: Class<T>): Provider<T>
    fun stream(): Provider<InputStream>
    fun bytes(): Provider<ByteArray>
    fun buffer(): Provider<ByteBuffer>
}

enum class Method {
    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE
}

enum class Status {
    ;

    companion object {
        @JvmStatic
        fun ofCode(code: Int): Status {
            TODO("Not implemented")
        }
    }
}

interface URIBuilder : Cloneable {
    fun fromUri(uri: URI): URIBuilder
    fun fromUrl(url: URL): URIBuilder

    @Throws(URISyntaxException::class, IllegalArgumentException::class)
    fun fromRaw(raw: String): URIBuilder

    fun scheme(scheme: String): URIBuilder
    fun userInfo(userInfo: String): URIBuilder
    fun host(host: InetAddress): URIBuilder
    fun host(host: String): URIBuilder
    fun port(port: Int): URIBuilder
    fun port(port: String): URIBuilder

    fun path(path: String): URIBuilder
    fun replacePath(path: String): URIBuilder
    fun pathSegment(pathSegment: String): URIBuilder
    fun query(query: String): URIBuilder
    fun replaceQuery(query: String): URIBuilder
    fun queryParam(name: String, vararg values: Any): URIBuilder
    fun queryParam(name: String, values: Collection<*>): URIBuilder
    fun queryParamIfPresent(name: String, value: Optional<*>): URIBuilder
    fun queryParamIfPresent(name: String, value: Provider<*>): URIBuilder
    fun queryParams(params: Map<String, Collection<String>>): URIBuilder
    fun replaceQueryParam(name: String, vararg values: Any): URIBuilder
    fun replaceQueryParam(name: String, values: Collection<*>): URIBuilder
    fun replaceQueryParams(params: Map<String, Collection<String>>): URIBuilder
    fun fragment(fragment: String): URIBuilder
}