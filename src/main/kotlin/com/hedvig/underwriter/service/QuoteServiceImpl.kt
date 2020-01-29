package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.Right
import arrow.core.orNull
import com.hedvig.underwriter.extensions.validTo
import com.hedvig.underwriter.model.ApartmentData
import com.hedvig.underwriter.model.ExtraBuilding
import com.hedvig.underwriter.model.HouseData
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.PersonPolicyHolder
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.service.dtos.HouseOrApartmentIncompleteQuoteDto
import com.hedvig.underwriter.service.exceptions.QuoteCompletionFailedException
import com.hedvig.underwriter.service.exceptions.QuoteNotFoundException
import com.hedvig.underwriter.serviceIntegration.customerio.CustomerIO
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateMemberNameAndSsnDto
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifyProductRequestDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import com.hedvig.underwriter.util.toStockholmLocalDate
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.IncompleteApartmentQuoteDataDto
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import feign.FeignException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class QuoteServiceImpl(
    val debtChecker: DebtChecker,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val quoteRepository: QuoteRepository,
    val customerIOClient: CustomerIO?,
    val env: Environment
) : QuoteService {

    val logger = getLogger(QuoteServiceImpl::class.java)!!

    override fun updateQuote(
        houseOrApartmentIncompleteQuoteDto: HouseOrApartmentIncompleteQuoteDto,
        id: UUID,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, Quote> {
        val quote = quoteRepository
            .find(id)
            ?: return Either.Left(
                ErrorResponseDto(ErrorCodes.NO_SUCH_QUOTE, "No such quote $id")
            )

        if (quote.state != QuoteState.INCOMPLETE && quote.state != QuoteState.QUOTED) {
            return Either.Left(
                ErrorResponseDto(
                    ErrorCodes.INVALID_STATE,
                    "quote must be incomplete or quoted to update but was really ${quote.state}"
                )
            )
        }

        return try {
            quoteRepository.modify(quote.id) { quoteToUpdate ->
                val updatedQuote = quoteToUpdate!!.update(houseOrApartmentIncompleteQuoteDto)
                if (updatedQuote.state == QuoteState.QUOTED) {
                    updatedQuote.complete(debtChecker, productPricingService, underwritingGuidelinesBypassedBy)
                        .bimap(
                            { errors ->
                                throw QuoteCompletionFailedException(
                                    "Unable to complete quote: $errors",
                                    errors
                                )
                            },
                            { quote -> quote }
                        )
                        .orNull()
                } else {
                    updatedQuote
                }
            }!!.let { updatedQuote -> Either.right(updatedQuote) }
        } catch (e: QuoteCompletionFailedException) {
            e.breachedUnderwritingGuidelines?.let { breachedUnderwritingGuidelines ->
                Either.left(
                    ErrorResponseDto(
                        ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES,
                        "quote cannot be calculated, underwriting guidelines are breached",
                        breachedUnderwritingGuidelines
                    )
                )
            } ?: Either.left(
                ErrorResponseDto(
                    ErrorCodes.UNKNOWN_ERROR_CODE,
                    "Unable to complete quote"
                )
            )
        }
    }

    override fun removeCurrentInsurerFromQuote(id: UUID): Either<ErrorResponseDto, Quote> {
        val updatedQuote = quoteRepository.modify(id) { quoteToUpdate ->
            quoteToUpdate!!.copy(currentInsurer = null)
        }

        return Either.right(updatedQuote!!)
    }

    override fun removeStartDateFromQuote(id: UUID): Either<ErrorResponseDto, Quote> {
        val updatedQuote = quoteRepository.modify(id) { quoteToUpdate ->
            quoteToUpdate!!.copy(startDate = null)
        }

        return Either.right(updatedQuote!!)
    }

    override fun createQuote(
        incompleteQuoteData: HouseOrApartmentIncompleteQuoteDto,
        id: UUID?,
        initiatedFrom: QuoteInitiatedFrom,
        shouldComplete: Boolean,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val now = Instant.now()

        val quote = Quote(
            id = id ?: UUID.randomUUID(),
            createdAt = now,
            productType = incompleteQuoteData.productType!!,
            initiatedFrom = initiatedFrom,
            attributedTo = incompleteQuoteData.quotingPartner ?: Partner.HEDVIG,
            data = when (val quoteData = incompleteQuoteData.incompleteQuoteData) {
                is IncompleteApartmentQuoteDataDto ->
                    ApartmentData(
                        id = UUID.randomUUID(),
                        ssn = incompleteQuoteData.ssn,
                        firstName = incompleteQuoteData.firstName,
                        lastName = incompleteQuoteData.lastName,
                        email = incompleteQuoteData.email,
                        subType = quoteData.subType,
                        street = quoteData.street,
                        zipCode = quoteData.zipCode,
                        city = quoteData.city,
                        householdSize = quoteData.householdSize,
                        livingSpace = quoteData.livingSpace
                    )
                is com.hedvig.underwriter.web.dtos.IncompleteHouseQuoteDataDto ->
                    HouseData(
                        id = UUID.randomUUID(),
                        ssn = incompleteQuoteData.ssn,
                        firstName = incompleteQuoteData.firstName,
                        lastName = incompleteQuoteData.lastName,
                        email = incompleteQuoteData.email,
                        street = quoteData.street,
                        zipCode = quoteData.zipCode,
                        city = quoteData.city,
                        householdSize = quoteData.householdSize,
                        livingSpace = quoteData.livingSpace,
                        numberOfBathrooms = quoteData.numberOfBathrooms,
                        isSubleted = quoteData.isSubleted,
                        extraBuildings = quoteData.extraBuildings?.map((ExtraBuilding)::from),
                        ancillaryArea = quoteData.ancillaryArea,
                        yearOfConstruction = quoteData.yearOfConstruction
                    )
                null -> throw IllegalArgumentException("Must provide either house or apartment data")
            },
            state = QuoteState.INCOMPLETE,
            memberId = incompleteQuoteData.memberId,
            breachedUnderwritingGuidelines = null,
            originatingProductId = incompleteQuoteData.originatingProductId,
            currentInsurer = incompleteQuoteData.currentInsurer,
            startDate = incompleteQuoteData.startDate?.toStockholmLocalDate(),
            dataCollectionId = incompleteQuoteData.dataCollectionId
        )

        return if (shouldComplete) {
            val potentiallySavedQuote = quote
                .complete(debtChecker, productPricingService, underwritingGuidelinesBypassedBy)
                .map { it: Quote -> this.quoteRepository.insert(it); it }

                transformCompleteQuoteReturn(potentiallySavedQuote, quote.id)
        } else {
            quoteRepository.insert(quote, now)
            Right(CompleteQuoteResponseDto(quote.id, BigDecimal.ZERO, quote.validTo))
        }
    }

    override fun getQuote(completeQuoteId: UUID): Quote? {
        return quoteRepository.find(completeQuoteId)
    }

    override fun getSingleQuoteForMemberId(memberId: String): QuoteDto? {
        val quote = quoteRepository.findOneByMemberId(memberId)
        return quote?.let((QuoteDto)::fromQuote)
    }

    override fun getLatestQuoteForMemberId(memberId: String): Quote? =
        quoteRepository.findLatestOneByMemberId(memberId)

    override fun getQuotesForMemberId(memberId: String): List<QuoteDto> =
        quoteRepository.findByMemberId(memberId)
            .map((QuoteDto)::fromQuote)

    override fun completeQuote(
        incompleteQuoteId: UUID,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val quote =
            quoteRepository.find(incompleteQuoteId)
                ?: return Either.left(ErrorResponseDto(ErrorCodes.UNKNOWN_ERROR_CODE, "No such quote"))

        if (quote.state != QuoteState.INCOMPLETE) {
            return Either.Left(
                ErrorResponseDto(
                    ErrorCodes.INVALID_STATE,
                    "quote must be completable but was really ${quote.state}"
                )
            )
        }

        val potentiallySavedQuote = quote.complete(debtChecker, productPricingService, underwritingGuidelinesBypassedBy)
            .map { it: Quote -> quoteRepository.update(it) }

        return transformCompleteQuoteReturn(potentiallySavedQuote, quote.id)
    }

    private fun transformCompleteQuoteReturn(
        potentiallySavedQuote: Either<List<String>, Quote>,
        quoteId: UUID
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        return potentiallySavedQuote.bimap(
            { breachedUnderwritingGuidelines ->
                logger.error(
                    "Underwriting guidelines breached for incomplete quote $quoteId: {}",
                    breachedUnderwritingGuidelines
                )
                ErrorResponseDto(
                    ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES,
                    "quote cannot be calculated, underwriting guidelines are breached",
                    breachedUnderwritingGuidelines
                )
            },
            { completeQuote ->
                CompleteQuoteResponseDto(
                    id = completeQuote.id,
                    price = completeQuote.price!!,
                    validTo = completeQuote.validTo
                )
            }
        )
    }

    override fun signQuote(
        completeQuoteId: UUID,
        body: SignQuoteRequest
    ): Either<ErrorResponseDto, SignedQuoteResponseDto> {
        val quote = getQuote(completeQuoteId)
            ?: throw QuoteNotFoundException("Quote $completeQuoteId not found when trying to sign")

        if (quote.signedProductId != null) {
            throw RuntimeException("There is a signed product id ${quote.signedProductId} already")
        }

        val updatedName = if (body.name != null && quote.data is PersonPolicyHolder<*>) {
            quote.copy(data = quote.data.updateName(firstName = body.name.firstName, lastName = body.name.lastName))
        } else {
            quote
        }

        val updatedStartTime = when {
            body.startDate != null -> {
                updatedName.copy(
                    startDate = body.startDate
                )
            }
            else -> updatedName.copy(startDate = null)
        }

        val quoteWithMember = if (quote.memberId == null) {
            val quoteNotSignableErrorDto = getQuoteStateNotSignableErrorOrNull(quote)
            if (quoteNotSignableErrorDto != null) {
                return Either.left(quoteNotSignableErrorDto)
            }

            val memberAlreadySigned = when (quote.data) {
                is PersonPolicyHolder<*> -> memberService.isSsnAlreadySignedMemberEntity(quote.data.ssn!!)
                else -> throw RuntimeException("Unsupported quote data class")
            }

            if (memberAlreadySigned.ssnAlreadySignedMember) {
                return Either.Left(
                    ErrorResponseDto(
                        ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
                        "quote is already signed"
                    )
                )
            }

            val memberId = memberService.createMember()

            memberService.updateMemberNameAndSsn(memberId.toLong(),
                UpdateMemberNameAndSsnDto(
                    body.name!!.firstName,
                    body.name.lastName,
                    quote.data.ssn!!

                )
            )

            quoteRepository.update(updatedStartTime.copy(memberId = memberId))
        } else {
            quote
        }

        return Right(
            signQuoteWithMemberId(
                quoteWithMember,
                false,
                SignRequest("", "", ""),
                body.email
            )
        )
    }

    override fun memberSigned(memberId: String, signedRequest: SignRequest) {
        quoteRepository.findLatestOneByMemberId(memberId)?.let { quote ->
            signQuoteWithMemberId(quote, true, signedRequest, null)
        } ?: throw IllegalStateException("Tried to perform member sign with no quote!")
    }

    private fun signQuoteWithMemberId(
        quote: Quote,
        signedInMemberService: Boolean,
        signedRequest: SignRequest,
        email: String?
    ): SignedQuoteResponseDto {
        checkNotNull(quote.memberId) { "Quote must have a member id! Quote id: ${quote.id}" }
        checkNotNull(quote.price) { "Quote must have a price to sign! Quote id: ${quote.id}" }

        val signedProductId = productPricingService.signedQuote(
            SignedQuoteRequest(
                price = Money.of(quote.price, "SEK"),
                quote = quote,
                referenceToken = signedRequest.referenceToken,
                signature = signedRequest.signature,
                oscpResponse = signedRequest.oscpResponse
            ),
            quote.memberId
        ).id

        val quoteWithProductId = quoteRepository.update(quote.copy(signedProductId = signedProductId))
        checkNotNull(quoteWithProductId.memberId) { "Quote must have a member id! Quote id: ${quote.id}" }

        if (quote.initiatedFrom == QuoteInitiatedFrom.RAPIO) {
            email?.let {
                memberService.finalizeOnboarding(quote, it)
            }
                ?: throw IllegalArgumentException("Must have an email when signing from rapio!")
        }

        quoteWithProductId.attributedTo.campaignCode?.let { campaignCode ->
            try {
                productPricingService.redeemCampaign(
                    RedeemCampaignDto(
                        quoteWithProductId.memberId,
                        campaignCode,
                        LocalDate.now(ZoneId.of("Europe/Stockholm"))
                    )
                )
            } catch (e: FeignException) {
                logger.error("Failed to redeem $campaignCode for partner ${quoteWithProductId.attributedTo} with response ${e.message}")
            }
        }

        if (!signedInMemberService && quoteWithProductId.data is PersonPolicyHolder<*>) {
            memberService.signQuote(
                quoteWithProductId.memberId.toLong(),
                UnderwriterQuoteSignRequest(quoteWithProductId.data.ssn!!)
            )
        }

        val signedAt = Instant.now()
        val signedQuote = quoteWithProductId.copy(state = QuoteState.SIGNED)

        quoteRepository.update(signedQuote, signedAt)

        if (quoteWithProductId.attributedTo != Partner.HEDVIG) {

            val activeProfiles = env.activeProfiles.intersect(listOf("staging", "production"))
            if (activeProfiles.isNotEmpty() && customerIOClient == null) {
                logger.error("customerIOClient is null even thou $activeProfiles is set")
            }

            customerIOClient?.postSignUpdate(quoteWithProductId)
        }

        return SignedQuoteResponseDto(signedProductId, signedAt)
    }

    override fun activateQuote(
        completeQuoteId: UUID,
        activationDate: LocalDate?,
        previousProductTerminationDate: LocalDate?
    ): Either<ErrorResponseDto, Quote> {
        val quote = getQuote(completeQuoteId)
            ?: throw QuoteNotFoundException("Quote $completeQuoteId not found when trying to sign")

        val quoteNotSignableErrorDto = getQuoteStateNotSignableErrorOrNull(quote)
        if (quoteNotSignableErrorDto != null) {
            Either.left(quoteNotSignableErrorDto)
        }

        val finalActivationDate = activationDate ?: quote.startDate
        val finalTerminationDate = previousProductTerminationDate ?: finalActivationDate

        if (finalActivationDate == null) {
            return Either.left(
                ErrorResponseDto(
                    errorCode = ErrorCodes.UNKNOWN_ERROR_CODE,
                    errorMessage = "No activation date given"
                )
            )
        }

        val result = productPricingService.createModifiedProductFromQuote(
            ModifyProductRequestDto.from(
                quote,
                activationDate = finalActivationDate,
                previousInsuranceTerminationDate = finalTerminationDate!!
            )
        )

        val updatedQuote = quoteRepository.update(
            quote.copy(
                signedProductId = result.id,
                state = QuoteState.SIGNED
            )
        )

        return Either.right(updatedQuote)
    }

    private fun getQuoteStateNotSignableErrorOrNull(quote: Quote): ErrorResponseDto? {
        if (quote.state == QuoteState.EXPIRED) {
            return ErrorResponseDto(
                ErrorCodes.MEMBER_QUOTE_HAS_EXPIRED,
                "cannot sign quote it has expired"
            )
        }

        if (quote.state == QuoteState.SIGNED) {
            return ErrorResponseDto(
                ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
                "quote is already signed"
            )
        }

        return null
    }
}
