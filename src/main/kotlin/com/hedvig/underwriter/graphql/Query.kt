package com.hedvig.underwriter.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.hedvig.graphql.commons.extensions.getAcceptLanguage
import com.hedvig.graphql.commons.extensions.getToken
import com.hedvig.localization.service.TextKeysLocaleResolver
import com.hedvig.underwriter.graphql.type.TypeMapper
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.service.QuoteService
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Query @Autowired constructor(
    private val quoteService: QuoteService,
    private val textKeysLocaleResolver: TextKeysLocaleResolver,
    private val typeMapper: TypeMapper
) : GraphQLQueryResolver {
    
    fun quote(id: UUID, env: DataFetchingEnvironment) = quoteService.getQuote(id)?.let { quote ->
        quote.toResult(env)
    } ?: throw IllegalStateException("No quote with id '$id' was found!")

    fun lastQuoteOfMember(env: DataFetchingEnvironment) =
        quoteService.getLatestQuoteForMemberId(env.getToken())?.toResult(env)
        ?: throw IllegalStateException("No quote found for memberId ${env.getToken()}!")


    private fun Quote.toResult(env: DataFetchingEnvironment) = typeMapper.mapToQuoteResult(
        this,
        quoteService.calculateInsuranceCost(this),
        textKeysLocaleResolver.resolveLocale(env.getAcceptLanguage())
    )
}
