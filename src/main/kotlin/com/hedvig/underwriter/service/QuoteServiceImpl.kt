package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.*
import com.hedvig.underwriter.repository.CompleteQuoteRepository
import com.hedvig.underwriter.repository.IncompleteQuoteRepository
import com.hedvig.underwriter.serviceIntegration.productPricing.Dtos.QuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.Dtos.QuotePriceResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.web.Dtos.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.NullPointerException
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Service
class QuoteServiceImpl @Autowired constructor(
        val incompleteQuoteRepository: IncompleteQuoteRepository,
        val completeQuoteRepository: CompleteQuoteRepository,
        val productPricingService: ProductPricingService,
        val uwGuidelinesChecker: UwGuidelinesChecker
): QuoteService {
    override fun createIncompleteQuote(incompleteQuoteDto: IncompleteQuoteDto): IncompleteQuoteResponseDto {
        val incompleteQuote = incompleteQuoteRepository.save(IncompleteQuote.from(incompleteQuoteDto))
        return IncompleteQuoteResponseDto(incompleteQuote.id!!, incompleteQuote.productType, incompleteQuote.quoteInitiatedFrom)
    }

    //    TODO: refactor
    override fun updateIncompleteQuoteData(incompleteQuoteDto: IncompleteQuoteDto, quoteId: UUID) {

        val incompleteQuote = getIncompleteQuote(quoteId)

        if (incompleteQuoteDto.lineOfBusiness != null) incompleteQuote.lineOfBusiness = incompleteQuoteDto.lineOfBusiness
        if (incompleteQuoteDto.quoteInitiatedFrom != null) incompleteQuote.quoteInitiatedFrom = incompleteQuoteDto.quoteInitiatedFrom
        if (incompleteQuoteDto.birthDate != null) incompleteQuote.birthDate = incompleteQuoteDto.birthDate
        if (incompleteQuoteDto.livingSpace != null) incompleteQuote.livingSpace = incompleteQuoteDto.livingSpace
        if (incompleteQuoteDto.houseHoldSize != null) incompleteQuote.houseHoldSize = incompleteQuoteDto.houseHoldSize
        if (incompleteQuoteDto.isStudent != null) incompleteQuote.isStudent = incompleteQuoteDto.isStudent


        if (incompleteQuoteDto.incompleteQuoteDataDto != null && incompleteQuote.incompleteQuoteData is IncompleteQuoteData.House) {
            val incompleteHouseQuoteDataDto: IncompleteHouseQuoteDataDto? = incompleteQuoteDto.incompleteQuoteDataDto.incompleteHouseQuoteDataDto
            val incompleteHouseQuoteData: IncompleteQuoteData.House = incompleteQuote.incompleteQuoteData

            if (incompleteHouseQuoteDataDto?.zipcode != null) incompleteHouseQuoteData.zipcode = incompleteHouseQuoteDataDto.zipcode
            if (incompleteHouseQuoteDataDto?.city != null) incompleteHouseQuoteData.city = incompleteHouseQuoteDataDto.city
            if (incompleteHouseQuoteDataDto?.city != null) incompleteHouseQuoteData.personalNumber = incompleteHouseQuoteDataDto.personalNumber
            if (incompleteHouseQuoteDataDto?.street != null) incompleteHouseQuoteData.street = incompleteHouseQuoteDataDto.street
            if (incompleteHouseQuoteDataDto?.householdSize != null) incompleteHouseQuoteData.householdSize = incompleteHouseQuoteDataDto.householdSize
            if (incompleteHouseQuoteDataDto?.livingSpace != null) incompleteHouseQuoteData.livingSpace = incompleteHouseQuoteDataDto.livingSpace
        }

        if (incompleteQuoteDto.incompleteQuoteDataDto != null && incompleteQuote.incompleteQuoteData is IncompleteQuoteData.Home) {
            val incompleteHomeQuoteDataDto: IncompleteHomeQuoteDataDto? = incompleteQuoteDto.incompleteQuoteDataDto.incompleteHomeQuoteDataDto
            val incompleteHomeQuoteData: IncompleteQuoteData.Home = (incompleteQuote.incompleteQuoteData as IncompleteQuoteData.Home)

            if (incompleteHomeQuoteDataDto?.numberOfRooms != null) incompleteHomeQuoteData.numberOfRooms = incompleteHomeQuoteDataDto.numberOfRooms
            if (incompleteHomeQuoteDataDto?.address != null) incompleteHomeQuoteData.address = incompleteHomeQuoteDataDto.address
            if (incompleteHomeQuoteDataDto?.zipCode != null) incompleteHomeQuoteData.zipCode = incompleteHomeQuoteDataDto.zipCode
            if (incompleteHomeQuoteDataDto?.floor != null) incompleteHomeQuoteData.floor = incompleteHomeQuoteDataDto.floor
        }
        incompleteQuoteRepository.save(incompleteQuote)
    }

    override fun findIncompleteQuoteById(id: UUID): Optional<IncompleteQuote> {
        return incompleteQuoteRepository.findById(id)
    }

    override fun createCompleteQuote(incompleteQuoteId: UUID): QuotePriceResponseDto {

        val incompleteQuote = getIncompleteQuote(incompleteQuoteId)
        try {
            val nullableCompleteQuote = createQuoteWithInfoCompleteAwaitingPriceAndUnderwritingChecks(incompleteQuote)
                    ?:return QuotePriceResponseDto(BigDecimal(0))

            val completeQuote = nullableCompleteQuote

                val quoteMeetsUWGuidelines: Boolean
                if (completeQuote.completeQuoteData is CompleteQuoteData.Home) {
                    quoteMeetsUWGuidelines = uwGuidelinesChecker.meetsHomeUwGuidelines(completeQuote)
                } else {
                    quoteMeetsUWGuidelines = uwGuidelinesChecker.meetsHouseUwGuidelines(completeQuote)
                }

                if (quoteMeetsUWGuidelines) {
                    val quotePriceResponseDto = getQuotePriceDto(completeQuote)!!

                    completeQuote.price = quotePriceResponseDto.price
                    completeQuoteRepository.save(completeQuote)
                    return quotePriceResponseDto
                } else {
                    throw NullPointerException("Failed underwriting guideline")
                }
            } catch (exception: Exception) {
            throw NullPointerException("Cannot create quote, info missing $exception")
        }

        return QuotePriceResponseDto(BigDecimal(0))
    }

    private fun getIncompleteQuote(quoteId: UUID): IncompleteQuote {
        val optionalQuote: Optional<IncompleteQuote> = incompleteQuoteRepository.findById(quoteId)
        if (!optionalQuote.isPresent) throw NullPointerException("No Incomplete quote found with id $quoteId")
        return optionalQuote.get()
    }

    private fun getQuotePriceDto(completeQuote: CompleteQuote): QuotePriceResponseDto? {

        if (completeQuote.completeQuoteData is CompleteQuoteData.Home) {
            val quotePriceDto = QuotePriceDto(
                    birthDate = completeQuote.birthDate,
                    livingSpace = completeQuote.livingSpace,
                    houseHoldSize = completeQuote.houseHoldSize,
                    zipCode = (completeQuote.completeQuoteData as CompleteQuoteData.Home).zipCode,
                    floor = (completeQuote.completeQuoteData as CompleteQuoteData.Home).floor,
                    houseType = completeQuote.lineOfBusiness,
                    isStudent = completeQuote.isStudent
            )
            val quotePriceResponseDto: QuotePriceResponseDto? = productPricingService.getQuotePrice(quotePriceDto)
            return quotePriceResponseDto
        }
        return QuotePriceResponseDto(BigDecimal(0))
    }

    private fun createQuoteWithInfoCompleteAwaitingPriceAndUnderwritingChecks(incompleteQuote: IncompleteQuote): CompleteQuote? {

        if (incompleteQuote.incompleteQuoteData is IncompleteQuoteData.House) {

                val completeQuote = CompleteQuote(
                        incompleteQuote = incompleteQuote,
                        quoteState = incompleteQuote.quoteState,
                        quoteCreatedAt = Instant.now(),
                        productType = incompleteQuote.productType,
                        lineOfBusiness = incompleteQuote.lineOfBusiness!!,
                        price = null,
                        completeQuoteData = CompleteQuoteData.House(incompleteQuote.incompleteQuoteData.street!!,
                                incompleteQuote.incompleteQuoteData.zipcode!!,
                                incompleteQuote.incompleteQuoteData.city!!,
                                incompleteQuote.incompleteQuoteData.livingSpace!!,
                                incompleteQuote.incompleteQuoteData.personalNumber!!,
                                incompleteQuote.incompleteQuoteData.householdSize!!
                        ),
                        quoteInitiatedFrom = incompleteQuote.quoteInitiatedFrom!!,
                        birthDate = incompleteQuote.birthDate!!,
                        livingSpace = incompleteQuote.livingSpace!!,
                        houseHoldSize = incompleteQuote.houseHoldSize!!,
                        isStudent = incompleteQuote.isStudent!!
                )
                return completeQuote
                completeQuoteRepository.save(completeQuote)

            }

            else if (incompleteQuote.incompleteQuoteData is IncompleteQuoteData.Home) {
                    val completeQuote = CompleteQuote(
                            incompleteQuote = incompleteQuote,
                            quoteState = incompleteQuote.quoteState,
                            quoteCreatedAt = Instant.now(),
                            productType = incompleteQuote.productType,
                            lineOfBusiness = incompleteQuote.lineOfBusiness!!,
                            price = null,
                            completeQuoteData = CompleteQuoteData.Home(
                                    incompleteQuote.incompleteQuoteData.address!!,
                                    incompleteQuote.incompleteQuoteData.numberOfRooms!!,
                                    incompleteQuote.incompleteQuoteData.zipCode!!,
                                    incompleteQuote.incompleteQuoteData.floor!!
                            ),
                            quoteInitiatedFrom = incompleteQuote.quoteInitiatedFrom!!,
                            birthDate = incompleteQuote.birthDate!!,
                            livingSpace = incompleteQuote.livingSpace!!,
                            houseHoldSize = incompleteQuote.houseHoldSize!!,
                            isStudent = incompleteQuote.isStudent!!
                    )

                    completeQuoteRepository.save(completeQuote)
                    return completeQuote
            }
        return null
    }
}

