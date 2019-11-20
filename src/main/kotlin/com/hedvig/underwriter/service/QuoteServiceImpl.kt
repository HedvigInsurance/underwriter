package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.Right
import arrow.core.flatMap
import com.hedvig.underwriter.model.*
import com.hedvig.underwriter.service.exceptions.QuoteCompletionFailedException
import com.hedvig.underwriter.service.exceptions.QuoteNotFoundException
import com.hedvig.underwriter.serviceIntegration.customerio.CustomerIO
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ValidateCampaignDto
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.IncompleteQuoteDto
import com.hedvig.underwriter.web.dtos.IncompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class QuoteServiceImpl(
    val debtChecker: DebtChecker,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val quoteRepository: QuoteRepository,
    val customerIOClient: CustomerIO?
) : QuoteService {
    val logger = getLogger(QuoteServiceImpl::class.java)!!

    override fun updateQuote(incompleteQuoteDto: IncompleteQuoteDto, id: UUID): Either<ErrorResponseDto, Quote> {
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

        try {
            val result = quoteRepository.modify(quote.id) {
                var result: Either<List<String>, Quote> = Either.Right(quote.update(incompleteQuoteDto))
                if (quote.state == QuoteState.QUOTED) {
                    result = result.flatMap { quote -> quote.complete(debtChecker, productPricingService) }
                }
                when (result) {
                    is Either.Left -> throw QuoteCompletionFailedException("Unable to complete quote: " + result.a)
                    is Either.Right -> result.b
                }
            }!!

            return Either.Right(result)
        } catch (e: QuoteCompletionFailedException) {
            logger.error(e.message, e)
            return Either.Left(
                ErrorResponseDto(
                    ErrorCodes.UNKNOWN_ERROR_CODE,
                    "unable to update quote"
                )
            )
        }
    }

    override fun createApartmentQuote(incompleteQuoteDto: IncompleteQuoteDto): IncompleteQuoteResponseDto {
        val now = Instant.now()
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = now,
            productType = ProductType.APARTMENT,
            initiatedFrom = QuoteInitiatedFrom.RAPIO,
            attributedTo = incompleteQuoteDto.quotingPartner ?: Partner.HEDVIG,
            data = ApartmentData(UUID.randomUUID()),
            state = QuoteState.INCOMPLETE
        )

        quoteRepository.insert(quote.update(incompleteQuoteDto), now)

        return IncompleteQuoteResponseDto(quote.id, quote.productType, quote.initiatedFrom)
    }

    override fun getQuote(completeQuoteId: UUID): Quote? {
        return quoteRepository.find(completeQuoteId)
    }

    override fun getQuoteFromMemberId(memberId: String): QuoteDto? {
        val quote = quoteRepository.findByMemberId(memberId)
        return QuoteDto.fromQuoteDto(quote!!)
    }

    override fun completeQuote(incompleteQuoteId: UUID): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
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

        return quote.complete(debtChecker, productPricingService)
            .bimap(
                { breachedUnderwritingGuidelines ->
                    logger.error(
                        "Underwriting guidelines breached for incomplete quote $incompleteQuoteId: {}",
                        breachedUnderwritingGuidelines
                    )
                    ErrorResponseDto(
                        ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES,
                        "quote cannot be calculated, underwriting guidelines are breached"
                    )
                },
                { completeQuote ->
                    quoteRepository.update(completeQuote)
                    CompleteQuoteResponseDto(id = completeQuote.id, price = completeQuote.price!!)
                }
            )
    }

    override fun signQuote(
        completeQuoteId: UUID,
        body: SignQuoteRequest
    ): Either<ErrorResponseDto, SignedQuoteResponseDto> {
        try {
            val quote = getQuote(completeQuoteId)
                ?: throw QuoteNotFoundException("Quote $completeQuoteId not found when trying to sign")

            if (quote.state == QuoteState.EXPIRED) {
                return Either.Left(
                    ErrorResponseDto(
                        ErrorCodes.MEMBER_QUOTE_HAS_EXPIRED,
                        "cannot sign quote it has expired"
                    )
                )
            }

            if (quote.state == QuoteState.SIGNED) {
                return Either.left(
                    ErrorResponseDto(
                        ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
                        "quote is already signed"
                    )
                )
            }

            when (quote.data) {
                is ApartmentData -> {
                    val memberAlreadySigned = memberService.isSsnAlreadySignedMemberEntity(quote.data.ssn!!)
                    if (memberAlreadySigned.ssnAlreadySignedMember) {
                        return Either.Left(
                            ErrorResponseDto(
                                ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
                                "quote is already signed"
                            )
                        )
                    }
                }
                is HouseData -> {
                    val memberAlreadySigned = memberService.isSsnAlreadySignedMemberEntity(quote.data.ssn!!)
                    if (memberAlreadySigned.ssnAlreadySignedMember) {
                        return Either.Left(
                            ErrorResponseDto(
                                ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
                                "quote is already signed"
                            )
                        )
                    }
                }
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

            val memberId = quote.memberId ?: memberService.createMember()

            val quoteWithMember = quoteRepository.update(updatedStartTime.copy(memberId = memberId))

            if (quoteWithMember.data is PersonPolicyHolder<*>) {
                memberService.updateMemberSsn(memberId.toLong(), UpdateSsnRequest(ssn = quoteWithMember.data.ssn!!))
            }

            val signedQuoteId =
                productPricingService.createProduct(quoteWithMember.getRapioQuoteRequestDto(body.email), memberId).id

            quote.attributedTo.campaignCode?.let { campaignCode ->
                val response = productPricingService.redeemCampaign(
                    RedeemCampaignDto(
                        memberId,
                        campaignCode,
                        LocalDate.now()
                    )
                )
                if (response.statusCode.isError) {
                    logger.error("Failed to redeem $campaignCode for partner ${quote.attributedTo} with response $response")
                }
            }

            if (quoteWithMember.data is PersonPolicyHolder<*>) {
                memberService.signQuote(memberId.toLong(), UnderwriterQuoteSignRequest(quoteWithMember.data.ssn!!))
            }

            if (quoteWithMember.attributedTo != Partner.HEDVIG) {
                customerIOClient?.setPartnerCode(memberId, updatedStartTime.attributedTo)
            }

            val signedAt = Instant.now()
            val signedQuote = quoteWithMember.copy(state = QuoteState.SIGNED)

            quoteRepository.update(signedQuote, signedAt)

            return Right(SignedQuoteResponseDto(signedQuoteId, signedAt))
        } catch (exception: Exception) {
            throw RuntimeException("could not create a signed quote", exception)
        }
    }

    override fun redeemCode(completeQuoteId: UUID, code: String): Either<ErrorResponseDto, Campaign> {
        val quote = getQuote(completeQuoteId)
            ?: throw QuoteNotFoundException("Quote $completeQuoteId not found when trying to redeemCode")

        if (quote.state == QuoteState.EXPIRED) {
            return Either.Left(
                ErrorResponseDto(
                    ErrorCodes.MEMBER_QUOTE_HAS_EXPIRED,
                    "cannot redeemCode quote has expired"
                )
            )
        }

        if (quote.state == QuoteState.SIGNED) {
            return Either.left(
                ErrorResponseDto(
                    ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
                    "cannot redeemCode quote is already signed"
                )
            )
        }

        val result = this.productPricingService.validateRedeemableCampaign(ValidateCampaignDto(code))
        if(result is Either.Right){
            val quoteWithCampaign = quote.addCampaign(result.b)
            quoteRepository.update(quoteWithCampaign)
        }


        return result.bimap({ErrorResponseDto(ErrorCodes.UNKNOWN_ERROR_CODE, it)}, {it})
    }
}

