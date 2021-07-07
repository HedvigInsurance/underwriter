package com.hedvig.underwriter.graphql

import arrow.core.Either
import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.hedvig.graphql.commons.extensions.getAcceptLanguage
import com.hedvig.graphql.commons.extensions.getEndUserIp
import com.hedvig.graphql.commons.extensions.getToken
import com.hedvig.graphql.commons.extensions.getTokenOrNull
import com.hedvig.graphql.commons.extensions.isAndroid
import com.hedvig.graphql.commons.extensions.isIOS
import com.hedvig.libs.translations.LocaleResolver
import com.hedvig.underwriter.graphql.type.CreateQuoteInput
import com.hedvig.underwriter.graphql.type.CreateQuoteResult
import com.hedvig.underwriter.graphql.type.EditQuoteInput
import com.hedvig.underwriter.graphql.type.QuoteMapper
import com.hedvig.underwriter.graphql.type.RemoveCurrentInsurerInput
import com.hedvig.underwriter.graphql.type.RemoveStartDateInput
import com.hedvig.underwriter.graphql.type.SignQuotesInput
import com.hedvig.underwriter.graphql.type.UnderwritingLimit
import com.hedvig.underwriter.graphql.type.UnderwritingLimitsHit
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.SignService
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.libs.logging.calls.LogCall
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class Mutation @Autowired constructor(
    private val quoteService: QuoteService,
    private val signService: SignService,
    private val quoteMapper: QuoteMapper
) : GraphQLMutationResolver {

    @LogCall
    fun createQuote(createQuoteInput: CreateQuoteInput, env: DataFetchingEnvironment): CreateQuoteResult {

        val input = when {
            createQuoteInput.apartment != null || createQuoteInput.house != null ||
                createQuoteInput.swedishApartment != null || createQuoteInput.swedishHouse != null -> {
                val ssn = if (createQuoteInput.ssn!!.length == 10) {
                    addCenturyToSwedishSSN(createQuoteInput.ssn)
                } else {
                    createQuoteInput.ssn
                }
                createQuoteInput.copy(ssn = ssn)
            }
            else -> createQuoteInput
        }

        val completeQuote = quoteService.createQuote(
            quoteRequest = input.toQuoteRequest(memberId = env.getTokenOrNull()),
            id = input.id,
            initiatedFrom = when {
                env.isAndroid() -> QuoteInitiatedFrom.ANDROID
                env.isIOS() -> QuoteInitiatedFrom.IOS
                else -> QuoteInitiatedFrom.WEBONBOARDING
            },
            underwritingGuidelinesBypassedBy = null,
            updateMemberService = true
        )

        return when (completeQuote) {
            is Either.Left -> getQuoteResultFromError(completeQuote.a)
            is Either.Right -> {
                val completeQuoteResponseDto = completeQuote.b

                val quote = quoteService.getQuote(completeQuoteResponseDto.id)
                    ?: throw RuntimeException("Quote must not be null!")

                quoteMapper.mapToCompleteQuoteResult(
                    quote,
                    quoteService.calculateInsuranceCost(quote),
                    LocaleResolver.resolve(env.getAcceptLanguage())
                )
            }
        }
    }

    @LogCall
    fun editQuote(input: EditQuoteInput, env: DataFetchingEnvironment): CreateQuoteResult {

        return responseForEditedQuote(
            quoteService.updateQuote(
                input.toQuoteRequest(memberId = env.getTokenOrNull()),
                input.id
            ),
            env
        )
    }

    @LogCall
    fun removeCurrentInsurer(input: RemoveCurrentInsurerInput, env: DataFetchingEnvironment): CreateQuoteResult {

        return responseForEditedQuote(
            quoteService.removeCurrentInsurerFromQuote(input.id),
            env
        )
    }

    @LogCall
    fun removeStartDate(input: RemoveStartDateInput, env: DataFetchingEnvironment): CreateQuoteResult {

        return responseForEditedQuote(
            quoteService.removeStartDateFromQuote(input.id),
            env
        )
    }

    @LogCall
    fun signQuotes(input: SignQuotesInput, env: DataFetchingEnvironment): StartSignResponse {
        return signService.startSigningQuotes(
            input.quoteIds,
            env.getToken(),
            env.getEndUserIp(),
            input.successUrl,
            input.failUrl
        )
    }

    @LogCall
    fun approveQuotes(quoteIds: List<UUID>, env: DataFetchingEnvironment): Boolean {
        signService.approveQuotes(quoteIds, env.getToken())
        return true
    }

    private fun responseForEditedQuote(
        errorOrQuote: Either<ErrorResponseDto, Quote>,
        env: DataFetchingEnvironment
    ): CreateQuoteResult =
        when (errorOrQuote) {
            is Either.Left -> getQuoteResultFromError(errorOrQuote.a)
            is Either.Right -> {
                val quote = errorOrQuote.b

                quoteMapper.mapToCompleteQuoteResult(
                    quote,
                    quoteService.calculateInsuranceCost(quote),
                    LocaleResolver.resolve(env.getAcceptLanguage())
                )
            }
        }

    private fun getQuoteResultFromError(errorResponse: ErrorResponseDto) = when (errorResponse.errorCode) {
        ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES -> {
            UnderwritingLimitsHit(
                errorResponse.breachedUnderwritingGuidelines?.map { breachedGuideline ->
                    UnderwritingLimit(
                        description = breachedGuideline.message,
                        code = breachedGuideline.code
                    )
                } ?: throw IllegalStateException("Breached underwriting guidelines with no list")
            )
        }
        ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE ->
            throw IllegalStateException("Member has existing insurance [Error Message: ${errorResponse.errorMessage}]")
        ErrorCodes.MEMBER_QUOTE_HAS_EXPIRED ->
            throw IllegalStateException("Quote has expired [Error Message: ${errorResponse.errorMessage}]")
        ErrorCodes.NO_SUCH_QUOTE ->
            throw IllegalStateException("No such quote [Error Message: ${errorResponse.errorMessage}]")
        ErrorCodes.INVALID_STATE ->
            throw IllegalStateException("Invalid state [Error Message: ${errorResponse.errorMessage}]")
        ErrorCodes.UNKNOWN_ERROR_CODE ->
            throw IllegalStateException("Unknown error code [Error Message: ${errorResponse.errorMessage}]")
        ErrorCodes.MEMBER_DOES_NOT_HAVE_EXISTING_SIGNED_INSURANCE ->
            throw IllegalStateException("Member has not existing signed insurance [Error Message: ${errorResponse.errorMessage}]")
        ErrorCodes.MEMBER_ID_IS_NOT_PROVIDED ->
            throw IllegalStateException("MemberId is not provided [Error Message: ${errorResponse.errorMessage}]")
        ErrorCodes.INVALID_BUNDLING ->
            throw IllegalStateException("Unsupported bundling [Error Message: ${errorResponse.errorMessage}]")
    }

    private fun addCenturyToSwedishSSN(ssn: String): String {
        val personalIdentityNumberYear = ssn.substring(0, 2).toInt()
        val breakPoint = LocalDate.now().minusYears(10).year % 100

        return if (personalIdentityNumberYear > breakPoint) {
            "19$ssn"
        } else {
            "20$ssn"
        }
    }
}
