package com.hedvig.underwriter.testhelp

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class TestHttpClient(
    private val mapper: ObjectMapper,
    private val template: TestRestTemplate
) {

    fun get(uri: String, headers: Map<String, String> = emptyMap()): Response {
        return exchange(HttpMethod.GET, uri, null, headers)
    }

    fun post(uri: String, body: Any? = null, headers: Map<String, String> = emptyMap()): Response {
        return exchange(HttpMethod.POST, uri, body, headers)
    }

    fun put(uri: String, body: Any? = null, headers: Map<String, String> = emptyMap()): Response {
        return exchange(HttpMethod.PUT, uri, body, headers)
    }

    private fun exchange(method: HttpMethod, uri: String, body: Any?, headers: Map<String, String>): Response {
        val httpEntity = body?.let { b ->
            HttpEntity(
                b,
                HttpHeaders().apply {
                    headers.forEach { (key, value) ->
                        set(key, value)
                    }
                }
            )
        }
        val entity = template.exchange(uri, method, httpEntity, Map::class.java)
        return Response(mapper, entity)
    }

    class Response(
        private val mapper: ObjectMapper,
        private val entity: ResponseEntity<*>
    ) {

        fun assert2xx(): Response {
            assertThat(entity.statusCode).isBetween(HttpStatus.OK, HttpStatus.IM_USED)
            return this
        }

        fun assertStatus(status: HttpStatus): Response {
            assertThat(entity.statusCode).isEqualTo(status)
            return this
        }

        inline fun <reified T> body(): T {
            return body(T::class.java)
        }

        @PublishedApi
        internal fun <T> body(type: Class<T>): T {
            return mapper.convertValue(entity.body, type)
        }
    }
}
