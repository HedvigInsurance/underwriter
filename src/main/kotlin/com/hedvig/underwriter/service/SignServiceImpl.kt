package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.Right
import arrow.core.flatMap
import arrow.core.getOrHandle
import com.hedvig.underwriter.model.AddressData
import com.hedvig.underwriter.model.Name
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.model.SignSessionRepository
import com.hedvig.underwriter.model.birthDate
import com.hedvig.underwriter.model.email
import com.hedvig.underwriter.model.firstName
import com.hedvig.underwriter.model.lastName
import com.hedvig.underwriter.model.ssn
import com.hedvig.underwriter.model.ssnMaybe
import com.hedvig.underwriter.service.exceptions.ErrorException
import com.hedvig.underwriter.service.exceptions.QuoteNotFoundException
import com.hedvig.underwriter.service.model.CompleteSignSessionData
import com.hedvig.underwriter.service.model.PersonPolicyHolder
import com.hedvig.underwriter.service.model.SignMethod
import com.hedvig.underwriter.service.model.StartSignErrors
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.underwriter.service.quotesSignDataStrategies.SignData
import com.hedvig.underwriter.service.quotesSignDataStrategies.SignStrategyService
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Nationality
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.util.logger
import com.hedvig.libs.logging.masking.toMaskedString
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationService
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.SignQuoteFromHopeRequest
import com.hedvig.underwriter.web.dtos.SignQuoteRequestDto
import com.hedvig.underwriter.web.dtos.SignQuotesRequestDto
import com.hedvig.underwriter.web.dtos.SignRequest
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import feign.FeignException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class SignServiceImpl(
    val quoteService: QuoteService,
    val bundleQuotesService: BundleQuotesService,
    // FIXME: SignService should only use quoteService in my opinion, but I'm not up for doing that refactor now.
    val quoteRepository: QuoteRepository,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val signSessionRepository: SignSessionRepository,
    val signStrategyService: SignStrategyService,
    val notificationService: NotificationService
) : SignService {

    override fun startSigningQuotes(
        quoteIds: List<UUID>,
        memberId: String,
        ipAddress: String?,
        successUrl: String?,
        failUrl: String?
    ): StartSignResponse {

        if (memberService.isMemberIdAlreadySignedMemberEntity(memberId.toLong()).memberAlreadySigned) {
            return StartSignErrors.memberIsAlreadySigned
        }

        val quotes = quoteService.getQuotes(quoteIds)
        quotes.forEach { quote ->
            if (quote.memberId == null) {
                logger.error("Member [id: $memberId] tried to sign quote without memberId [Quotes: $quotes]")
                return StartSignErrors.noMemberIdOnQuote
            }
            if (memberId != quote.memberId) {
                logger.error("Member [id: $memberId] tried to sign quote with mismatching memberId [Quotes: $quotes]")
                return StartSignErrors.variousMemberId
            }

            val quoteNotSignableErrorDto = assertQuoteIsNotSignedOrExpired(quote)
            if (quoteNotSignableErrorDto != null) {
                logger.error("Member [id: $memberId] tried to sign quote that was either signed or expired [Quotes: $quotes]")
                return StartSignErrors.fromErrorResponse(quoteNotSignableErrorDto)
            }
        }

        val personalInfoMatching = bundlePersonalInfoMatching(quotes)
        if (!personalInfoMatching) {
            logger.error("Member [id: $memberId] tried to sign bundle with mismatching personal info [Quotes: $quotes]")
            return StartSignErrors.personalInfoNotMatching
        }

        val startSignResponse = signStrategyService.startSign(
            quotes = quotes,
            signData = SignData(ipAddress, successUrl, failUrl)
        )
        if (startSignResponse !is StartSignResponse.FailedToStartSign) {
            memberService.finalizeOnboarding(quotes[0], quotes[0].email!!)
        }
        return startSignResponse
    }

    override fun completedSignSession(signSessionId: UUID, completeSignSessionData: CompleteSignSessionData) {
        val quoteIds = signSessionRepository.find(signSessionId)
        val quotes = quoteRepository.findQuotes(quoteIds)

        val createContractResponse = when (completeSignSessionData) {
            is CompleteSignSessionData.SwedishBankIdDataComplete ->
                productPricingService.createContractsFromQuotes(
                    quotes,
                    SignRequest(
                        referenceToken = completeSignSessionData.referenceToken,
                        signature = completeSignSessionData.signature,
                        oscpResponse = completeSignSessionData.oscpResponse
                    ),
                    token = null
                )
            is CompleteSignSessionData.NoMandate ->
                productPricingService.createContractsFromQuotesNoMandate(quotes)
        }
        quotes.forEach { quote ->
            val response = createContractResponse.first { quote.id == it.quoteId }
            redeemAndSignQuoteAndPostToCustomerio(quote, response.agreementId, true, response.contractId)
        }
    }

    private fun redeemAndSignQuoteAndPostToCustomerio(
        quote: Quote,
        agreementId: UUID,
        shouldCompleteSignInMemberService: Boolean,
        contractId: UUID
    ): Either<Nothing, SignedQuoteResponseDto> {
        val signedAt = Instant.now()
        return Right(
            quoteRepository.update(
                quote.copy(agreementId = agreementId, contractId = contractId)
            )
        )
            .map {
                redeemCampaigns(it)
            }
            .map {
                completeInMemberService(it, shouldCompleteSignInMemberService)
            }
            .map {
                quoteRepository.update(it.copy(state = QuoteState.SIGNED), signedAt)
            }
            .map {
                updateCustomerIO(it)
            }
            .map {
                checkNotNull(it.memberId) { "Quote must have a member id! Quote id: ${it.id}" }
                SignedQuoteResponseDto(contractId, it.memberId, signedAt, quote.market)
            }
    }

    private fun updateCustomerIO(quote: Quote): Quote {
        try {
            notificationService.postSignUpdate(quote)
        } catch (ex: Exception) {
            logger.error(
                "Something went wrong while posting a signing update to customerIO " +
                    "[SignQuote: $quote]"
            )
        }
        return quote
    }

    private fun completeInMemberService(
        quote: Quote,
        shouldCompleteSignInMemberService: Boolean
    ): Quote {
        checkNotNull(quote.memberId) { "Quote must have a member id! Quote id: ${quote.id}" }
        if (shouldCompleteSignInMemberService && quote.data is PersonPolicyHolder<*>) {
            memberService.signQuote(
                quote.memberId.toLong(),
                UnderwriterQuoteSignRequest(quote.data.ssn!!)
            )
        }
        return quote
    }

    private fun redeemCampaigns(
        quote: Quote
    ): Quote {
        checkNotNull(quote.memberId) { "Quote must have a member id! Quote id: ${quote.id}" }
        quote.attributedTo.campaignCode?.let { campaignCode ->
            try {
                productPricingService.redeemCampaign(
                    RedeemCampaignDto(
                        quote.memberId,
                        campaignCode,
                        LocalDate.now(quote.getTimeZoneId())
                    )
                )
            } catch (e: FeignException) {
                logger.error("Failed to redeem $campaignCode for partner ${quote.attributedTo} with response ${e.message}")
            }
        }
        return quote
    }

    override fun signQuoteFromRapio(
        quoteId: UUID,
        request: SignQuoteRequestDto
    ): Either<ErrorResponseDto, SignedQuoteResponseDto> {

        return try {
            val response = signQuotesFromRapio(
                SignQuotesRequestDto(
                    quoteIds = listOf(quoteId),
                    name = request.name,
                    ssn = request.ssn,
                    startDate = request.startDate,
                    insuranceCompany = request.insuranceCompany,
                    email = request.email,
                    price = null,
                    currency = null
                )
            )

            Either.right(response[0])
        } catch (e: ErrorException) {
            Either.left(ErrorResponseDto.from(e))
        }
    }

    override fun signQuotesFromRapio(
        request: SignQuotesRequestDto
    ): List<SignedQuoteResponseDto> {

        var quotes = quoteRepository.findQuotes(request.quoteIds)

        logger.info("Fetched quote from db: ${quotes.toMaskedString()}")

        if (quotes.isEmpty() || quotes.size != request.quoteIds.size) {
            throw ErrorException(ErrorCodes.NO_SUCH_QUOTE, "No or not all quotes found for ids: ${request.quoteIds})")
        }

        quotes = quotes
            .map { updateNameFromRequest(it, request.name) }
            .map { updateEmailFromRequest(it, request.email) }
            .map { updateSsnFromRequest(it, request.ssn) }
            .map { updateStartTimeFromRequest(it, request.startDate) }
            .map { updateCurrentInsurerFromRequest(it, request.insuranceCompany) }

        validateQuotesToSignFromRapio(quotes)
        validateBundlePrice(quotes, request.price, request.currency)

        val memberId = getCreateMember(quotes)

        quotes = quotes
            .map { it.copy(memberId = memberId) }
            .map { quoteRepository.update(it) }

        val response = quotes
            .map { signQuoteWithMemberId(it, true, SignRequest()) }
            .map { it.getOrHandle { throw RuntimeException("Failed to sign quote, unknown reason") } }
            .toList()

        logger.info("Quote in db after signing: ${quoteRepository.findQuotes(request.quoteIds).toMaskedString()}")

        return response
    }

    private fun validateQuotesToSignFromRapio(quotes: List<Quote>) {

        val memberIds = quotes.mapNotNull { it.memberId }.toSet()
        if (memberIds.size > 1) {
            throw ErrorException(ErrorCodes.INVALID_STATE, "Quotes must belong to same member: $memberIds")
        }
        if (memberIds.size == 1) {
            val memberId = memberIds.first()
            if (quotes.any { it.memberId != memberId }) {
                throw ErrorException(ErrorCodes.MEMBER_ID_IS_NOT_PROVIDED, "Member id not sat in all quotes ")
            }
        }

        val markets = quotes.map { it.market }.toSet()
        if (markets.size != 1) {
            throw ErrorException(ErrorCodes.INVALID_STATE, "Quotes must belong to same market: $markets")
        }

        quotes.map {
            with(it) {

                if (ssnMaybe == null || ssn.isEmpty()) {
                    throw ErrorException(ErrorCodes.INVALID_STATE, "Cannot sign quote $id, no ssn")
                }
                if (price == null) {
                    throw ErrorException(ErrorCodes.INVALID_STATE, "Cannot sign quote $id, it has no price")
                }
                if (agreementId != null) {
                    throw ErrorException(
                        ErrorCodes.INVALID_STATE,
                        "Cannot sign quote $id, there is a signed product id $agreementId already"
                    )
                }
                if (state == QuoteState.EXPIRED) {
                    throw ErrorException(ErrorCodes.MEMBER_QUOTE_HAS_EXPIRED, "Cannot sign quote $id, it has expired")
                }
                if (state == QuoteState.SIGNED) {
                    throw ErrorException(ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE, "Quote $id is already signed")
                }
                if (data !is PersonPolicyHolder<*>) {
                    throw ErrorException(ErrorCodes.INVALID_STATE, "Quote type is not supported: ${data::class.java}")
                }
                if(startDate == null && currentInsurer == null) {
                    throw ErrorException(ErrorCodes.INVALID_STATE, "currentInsurer is required when startDate is null")
                }
            }
        }
    }

    private fun validateBundlePrice(quotes: List<Quote>, price: BigDecimal?, currency: String?) {

        if (quotes.size < 2) {
            return
        }

        signStrategyService.validateBundling(quotes)
            ?.let { throw ErrorException(ErrorCodes.INVALID_BUNDLING, it.toString()) }

        if (price != null && currency != null) {
            val bundlePrice = bundleQuotesService.bundleQuotes(null, quotes.map { it.id }.toList()).cost.monthlyNet

            if (BigDecimal(bundlePrice.amount).compareTo(price) != 0 || bundlePrice.currency != currency) {
                throw ErrorException(
                    ErrorCodes.INVALID_BUNDLING,
                    "Supplied quote bundle price ($price $currency) does not match current $bundlePrice"
                )
            }
        }
    }

    private fun getCreateMember(quotes: List<Quote>): String {

        val ssn = quotes.first().ssn
        val email = quotes.first().email

        // Get existing memberId if any from quotes
        var memberId = quotes.mapNotNull { it.memberId }.firstOrNull()

        if (memberId != null) {
            logger.info("Existing member $memberId")
            return memberId
        }

        // Does this member already exist?
        val memberAlreadySigned = memberService.isSsnAlreadySignedMemberEntity(ssn).ssnAlreadySignedMember

        if (memberAlreadySigned) {
            throw ErrorException(ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE, "Member already have signed quote(s)")
        }

        memberId = memberService.createMember()
        logger.info("New member created: $memberId")

        memberService.updateMemberSsn(
            memberId.toLong(),
            UpdateSsnRequest(
                ssn = ssn,
                nationality = Nationality.fromQuote(quotes.first())
            )
        )

        val quoteWithAddress = quotes.filter { it.data is AddressData }.firstOrNull() ?: quotes.first()
        memberService.finalizeOnboarding(quoteWithAddress.copy(memberId = memberId), email!!)

        return memberId
    }

    override fun signQuoteFromHope(
        completeQuoteId: UUID,
        request: SignQuoteFromHopeRequest
    ): Either<ErrorResponseDto, SignedQuoteResponseDto> {
        val quote = quoteRepository.find(completeQuoteId)
            ?: throw QuoteNotFoundException("Quote $completeQuoteId not found when trying to sign")

        logger.info("Sign quote from hope. Quote from db: ${quote.toMaskedString()}")

        assertAgreementIdIsNull(quote)

        if (quote.memberId == null) {
            Either.Left(
                ErrorResponseDto(
                    ErrorCodes.MEMBER_ID_IS_NOT_PROVIDED,
                    "No member id connected to quote ${quote.id}"
                )
            )
        }

        val quoteNotSignableErrorDto = assertQuoteIsNotSignedOrExpired(quote)
        if (quoteNotSignableErrorDto != null) {
            return Either.left(quoteNotSignableErrorDto)
        }

        val memberAlreadySigned = checkIfMemberHasSignedInsurance(quote)

        if (!memberAlreadySigned) {
            return Either.Left(
                ErrorResponseDto(
                    ErrorCodes.MEMBER_DOES_NOT_HAVE_EXISTING_SIGNED_INSURANCE,
                    "Member does not have an existing signed insurance [QuoteId: ${quote.id}]"
                )
            )
        }

        val updatedQuote = quote.copy(startDate = request.activationDate, signFromHopeTriggeredBy = request.token)

        return signQuoteWithMemberId(
            updatedQuote,
            false,
            SignRequest()
        )
    }

    private fun checkIfMemberHasSignedInsurance(
        quote: Quote
    ): Boolean {
        return when (quote.data) {
            is PersonPolicyHolder<*> -> memberService.isSsnAlreadySignedMemberEntity(quote.data.ssn!!).ssnAlreadySignedMember
            else -> throw RuntimeException("Unsupported quote data class")
        }
    }

    override fun memberSigned(memberId: String, signedRequest: SignRequest) {
        quoteRepository.findLatestOneByMemberId(memberId)?.let { quote ->
            signQuoteWithMemberId(quote, false, signedRequest)
        } ?: throw IllegalStateException("Tried to perform member sign with no quote!")
    }

    override fun getSignMethodFromQuotes(quoteIds: List<UUID>): SignMethod {
        val quotes = quoteService.getQuotes(quoteIds)
        return signStrategyService.getSignMethod(quotes)
    }

    private fun signQuoteWithMemberId(
        quote: Quote,
        shouldCompleteSignInMemberService: Boolean,
        signedRequest: SignRequest
    ): Either<Nothing, SignedQuoteResponseDto> {
        return Right(quote)
            .map { checkNotNull(it.memberId) { "Quote must have a member id! Quote id: ${it.id}" }; it }
            .map { checkNotNull(it.price) { "Quote must have a price to sign! Quote id: ${it.id}" }; it }
            .map { createContractsForQuote(it, signedRequest) }
            .flatMap {
                redeemAndSignQuoteAndPostToCustomerio(
                    it,
                    it.agreementId!!,
                    shouldCompleteSignInMemberService,
                    it.contractId!!
                )
            }
    }

    private fun createContractsForQuote(quote: Quote, signedRequest: SignRequest): Quote {
        val result =
            productPricingService.createContractsFromQuotes(listOf(quote), signedRequest, quote.signFromHopeTriggeredBy)
                .first()

        return quoteRepository.update(
            quote.copy(agreementId = result.agreementId, contractId = result.contractId)
        )
    }
}

private fun bundlePersonalInfoMatching(quotes: List<Quote>): Boolean = quotes.windowed(2).all { (left, right) ->
    left.firstName == right.firstName &&
        left.lastName == right.lastName &&
        left.ssn == right.ssn &&
        left.email == right.email &&
        left.birthDate == right.birthDate
}

private fun assertAgreementIdIsNull(quote: Quote): Quote {
    if (quote.agreementId != null) {
        throw RuntimeException("There is a signed product id ${quote.agreementId} already")
    }
    return quote
}

private fun updateStartTimeFromRequest(
    quote: Quote,
    startDate: LocalDate?
): Quote {
    return when {
        startDate != null -> {
            quote.copy(
                startDate = startDate
            )
        }
        else -> quote.copy(startDate = null)
    }
}

private fun updateNameFromRequest(
    quote: Quote,
    name: Name?
): Quote {
    return if (name?.firstName != null && quote.data is PersonPolicyHolder<*>) {
        quote.copy(data = quote.data.updateName(firstName = name.firstName, lastName = name.lastName))
    } else {
        quote
    }
}

private fun updateSsnFromRequest(
    quote: Quote,
    ssn: String?
): Quote {

    if (ssn.isNullOrBlank() || quote.data !is PersonPolicyHolder<*>) {
        return quote
    }

    // Cannot override existing ssn with a different one
    if (quote.data.ssn != null && quote.data.ssn != ssn) {
        throw IllegalArgumentException("Invalid ssn, does not match existing ssn in quote")
    }

    return quote.copy(data = quote.data.updateSsn(ssn = ssn))
}

private fun updateCurrentInsurerFromRequest(quote: Quote, currentInsurer: String?): Quote {
    if (currentInsurer.isNullOrBlank()) {
        return quote
    }

    return quote.copy(currentInsurer = currentInsurer)
}

private fun updateEmailFromRequest(
    quote: Quote,
    email: String
): Quote {

    return if (quote.data is PersonPolicyHolder<*>) {
        quote.copy(data = quote.data.updateEmail(email = email))
    } else {
        quote
    }
}
