package com.hedvig.underwriter.localization

import com.hedvig.lokalise.client.LokaliseClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.Locale

@Configuration
class LocalizationConfiguration {

    @Bean
    @ConditionalOnProperty(
        value = ["lokalise.projectId", "lokalise.apiToken"]
    )
    fun realService(
        @Value("\${lokalise.projectId}") projectId: String,
        @Value("\${lokalise.apiToken}") apiToken: String
    ): LocalizationService {
        val client = LokaliseClient(projectId, apiToken)
        return object : LocalizationService {
            override fun getTranslation(key: String, locale: Locale): String? = client.getTranslation(key, locale)
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!production")
    fun fakeService(): LocalizationService = object : LocalizationService {
        override fun getTranslation(key: String, locale: Locale) = key
    }
}
