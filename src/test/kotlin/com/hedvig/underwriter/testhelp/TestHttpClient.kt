package com.hedvig.underwriter.testhelp

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class TestHttpClient(
    private val mapper: ObjectMapper,
    private val template: TestRestTemplate
) {

    fun get(uri: String): Response {
        return exchange(HttpMethod.GET, uri, null)
    }

    fun post(uri: String, body: Any? = null): Response {
        return exchange(HttpMethod.POST, uri, body)
    }

    fun put(uri: String, body: Any? = null): Response {
        return exchange(HttpMethod.PUT, uri, body)
    }

    private fun exchange(method: HttpMethod, uri: String, body: Any?): Response {
        val entity = template.exchange(uri, method, body?.let { HttpEntity(it) }, Map::class.java)
        return Response(mapper, entity)
    }

    class Response(
        private val mapper: ObjectMapper,
        private val entity: ResponseEntity<*>
    ) {

        fun assert2xx(): Response {
            assertThat(entity.statusCode.is2xxSuccessful).isTrue()
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
