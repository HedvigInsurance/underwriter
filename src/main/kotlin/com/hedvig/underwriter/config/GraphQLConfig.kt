package com.hedvig.underwriter.config

import com.coxautodev.graphql.tools.SchemaParserDictionary
import com.hedvig.underwriter.graphql.type.ExtraBuildingValue
import com.hedvig.underwriter.graphql.type.IncompleteQuoteDetails
import com.hedvig.underwriter.graphql.type.QuoteDetails
import com.hedvig.underwriter.graphql.type.QuoteResult
import com.hedvig.underwriter.graphql.type.UnderwritingLimitsHit
import com.hedvig.underwriter.graphql.type.depricated.CompleteQuoteDetails
import com.hedvig.underwriter.service.model.StartSignResponse
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GraphQLConfig {
    @Bean
    fun dataLoaderRegistry(loaderList: List<DataLoader<*, *>>): DataLoaderRegistry {
        val registry = DataLoaderRegistry()
        for (loader in loaderList) {
            registry.register(loader.javaClass.simpleName, loader)
        }
        return registry
    }

    @Bean
    fun schemaParserDictionary(): SchemaParserDictionary {
        return SchemaParserDictionary()
            .add(
                dictionary = listOf(
                    CompleteQuoteDetails.CompleteApartmentQuoteDetails::class.java,
                    CompleteQuoteDetails.CompleteHouseQuoteDetails::class.java,
                    CompleteQuoteDetails.UnknownQuoteDetails::class.java,
                    QuoteResult.CompleteQuote::class.java,
                    QuoteResult.IncompleteQuote::class.java,
                    UnderwritingLimitsHit::class.java,
                    QuoteDetails.SwedishApartmentQuoteDetails::class.java,
                    QuoteDetails.SwedishHouseQuoteDetails::class.java,
                    QuoteDetails.NorwegianHomeContentsDetails::class.java,
                    QuoteDetails.NorwegianTravelDetails::class.java,
                    QuoteDetails.DanishHomeContentsDetails::class.java,
                    QuoteDetails.DanishAccidentDetails::class.java,
                    QuoteDetails.DanishTravelDetails::class.java,
                    IncompleteQuoteDetails.IncompleteApartmentQuoteDetails::class.java,
                    IncompleteQuoteDetails.IncompleteHouseQuoteDetails::class.java,
                    StartSignResponse.SwedishBankIdSession::class.java,
                    StartSignResponse.NorwegianBankIdSession::class.java,
                    StartSignResponse.DanishBankIdSession::class.java,
                    StartSignResponse.SimpleSignSession::class.java,
                    StartSignResponse.AlreadyCompleted::class.java,
                    StartSignResponse.FailedToStartSign::class.java,
                    ExtraBuildingValue::class.java
                )
            )
    }
}
