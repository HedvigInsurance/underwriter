package com.hedvig.underwriter.localization

import com.hedvig.libs.translations.RemoteJsonFileTranslations
import com.hedvig.libs.translations.Translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Locale
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Configuration
class TranslationsConfiguration {

    @Bean
    @ConditionalOnProperty("hedvig.translations.fakes", havingValue = "true")
    fun echoTranslations(): Translations = object : Translations {
        override fun get(key: String, locale: Locale) = key
    }

    @Bean
    @ConditionalOnMissingBean
    fun liveTranslations(): Translations {
        return RemoteJsonFileTranslations()
    }
}
