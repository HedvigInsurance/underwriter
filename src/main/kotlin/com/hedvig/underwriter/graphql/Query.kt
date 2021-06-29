package com.hedvig.underwriter.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.hedvig.graphql.commons.extensions.getAcceptLanguage
import com.hedvig.graphql.commons.extensions.getToken
import com.hedvig.libs.translations.LocaleResolver
import com.hedvig.underwriter.graphql.type.QuoteBundle
import com.hedvig.underwriter.graphql.type.QuoteBundleInputInput
import com.hedvig.underwriter.graphql.type.QuoteMapper
import com.hedvig.underwriter.graphql.type.QuoteResult
import com.hedvig.underwriter.graphql.type.SignMethod
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.service.BundleQuotesService
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.SignService
import com.hedvig.libs.logging.calls.LogCall
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class Query @Autowired constructor(
    private val quoteService: QuoteService,
    private val signService: SignService,
    private val bundleQuotesService: BundleQuotesService,
    private val quoteMapper: QuoteMapper
) : GraphQLQueryResolver {

    @LogCall
    fun quote(id: UUID, env: DataFetchingEnvironment): QuoteResult {

        return quoteService.getQuote(id)?.let { quote ->
            quote.toResult(env)
        } ?: throw QuoteNotFoundQueryException("No quote with id '$id' was found!")
    }

    @LogCall
    fun lastQuoteOfMember(env: DataFetchingEnvironment): QuoteResult {

        return quoteService.getLatestQuoteForMemberId(env.getToken())?.toResult(env)
            ?: throw QuoteNotFoundQueryException("No quote found for memberId: ${env.getToken()}")
    }

    @LogCall
    fun quoteBundle(input: QuoteBundleInputInput, env: DataFetchingEnvironment): QuoteBundle {

        if (input.ids.isEmpty()) {
            throw EmptyBundleQueryException()
        }

        val bundle = bundleQuotesService.bundleQuotes(
            env.getToken(),
            input.ids
        )

        return QuoteBundle(
            quotes = bundle.quotes.map {
                quoteMapper.mapToBundleQuote(
                    it,
                    LocaleResolver.resolve(env.getAcceptLanguage())
                )
            },
            bundleCost = bundle.cost
        )
    }

    @LogCall
    fun signMethodForQuotes(input: List<UUID>): SignMethod {
        return signService.getSignMethodFromQuotes(input).toGraphQL()
    }

    private fun Quote.toResult(env: DataFetchingEnvironment) = quoteMapper.mapToQuoteResult(
        this,
        quoteService.calculateInsuranceCost(this),
        LocaleResolver.resolve(env.getAcceptLanguage())
    )
}
