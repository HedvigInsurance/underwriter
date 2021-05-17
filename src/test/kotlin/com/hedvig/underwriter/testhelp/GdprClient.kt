package com.hedvig.underwriter.testhelp

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class GdprClient {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    fun clean(dryRun: Boolean? = null, days: Int? = null): ResponseEntity<String> {
        val queryStrings = mutableListOf<String>()

        if (dryRun != null) {
            queryStrings.add("dry-run=$dryRun")
        }

        if (days != null) {
            queryStrings.add("days=$days")
        }

        val query = queryStrings.joinToString("&", "?")

        return restTemplate.postForEntity("/_/v1/gdpr/clean$query")
    }
}
