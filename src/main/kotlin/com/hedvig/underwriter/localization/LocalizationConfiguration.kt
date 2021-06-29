package com.hedvig.underwriter.localization

import com.hedvig.libs.translations.RemoteJsonFileTranslationsClient
import com.hedvig.libs.translations.TranslationsClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.Locale

@Configuration
class LocalizationConfiguration {

    @Bean
    fun realService(
    ): TranslationsClient {
        return RemoteJsonFileTranslationsClient()
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!production")
    fun fakeService(): TranslationsClient = object : TranslationsClient {
        override fun getTranslation(key: String, locale: Locale) = key
    }
}
