package com.hedvig.underwriter.model

import com.hedvig.underwriter.service.DebtChecker
import com.hedvig.underwriter.service.UwGuidelinesChecker
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.Address
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HomeQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HouseQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RapioQuoteRequestDto
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@TypeDefs(
        TypeDef(name = "json", typeClass = JsonStringType::class),
        TypeDef(name = "jsonb", typeClass = JsonBinaryType::class))
class CompleteQuote (
        @field:Id
        @field:GeneratedValue(generator = "UUID")
        @field:GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
        var id: UUID? = null,

        @OneToOne
        val incompleteQuote: IncompleteQuote,

        @Enumerated(EnumType.STRING)
        var quoteState: QuoteState = QuoteState.QUOTED,
        val quoteCreatedAt: Instant,

        @Enumerated(EnumType.STRING)
        val productType: ProductType = ProductType.UNKNOWN,

        @Enumerated(EnumType.STRING)
        val lineOfBusiness: LineOfBusiness,
        var price: BigDecimal?,

        @field:Type(type = "jsonb")
        @field:Column(columnDefinition = "jsonb")
        val completeQuoteData: CompleteQuoteData,

        @Enumerated(EnumType.STRING)
        val quoteInitiatedFrom: QuoteInitiatedFrom,
        var firstName: String?,
        var lastName: String?,
        var currentInsurer: String? = "",
        val livingSpace: Int,
        val houseHoldSize: Int,
        val isStudent: Boolean,
        val ssn: String,
        var startDate: LocalDateTime?,

        @ElementCollection(targetClass=String::class)
        var reasonQuoteCannotBeCompleted: List<String> = listOf()
    ) {

    override fun hashCode(): Int {
        return 31
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other == null -> false
            IncompleteQuote::class.isInstance(other) && this.id == (other as IncompleteQuote).id -> true
            else -> false
        }
    }

    fun birthDateFromSsn(): LocalDate {
        val trimmedInput = ssn!!.trim().replace("-",  "").replace(" ", "")
        return LocalDate.parse(trimmedInput.substring(0, 4) + "-" + trimmedInput.substring(4, 6) + "-" + trimmedInput.substring(6, 8))
    }

    fun memberIsOver30(): Boolean {
        val dateToday = LocalDate.now()
        val date30YearsAgo = dateToday.minusYears(30)
        try {
            return this.birthDateFromSsn().isBefore(date30YearsAgo)
        } catch(exception: Exception) {
            throw RuntimeException("BirthDate cannot be calculated, ssn appears to be incorrect" + exception)
        }
    }

    fun memberIsOlderThan18(): Boolean {
        val dateToday = LocalDate.now()
        val date18YearsAgo = dateToday.minusYears(18)
        try {
            return this.birthDateFromSsn().isBefore(date18YearsAgo)
        } catch(exception: Exception) {
            throw RuntimeException("BirthDate cannot be calculated, ssn appears to be incorrect" + exception)
        }
    }


    fun ssnIsValid(): Boolean {
        val trimmedInput = ssn.trim().replace("-",  "").replace(" ", "")

        if (trimmedInput.length != 12) {
            reasonQuoteCannotBeCompleted += "ssn not valid"
            return false
        }

        try {
            LocalDate.parse(trimmedInput.substring(0, 4) + "-" + trimmedInput.substring(4, 6) + "-" + trimmedInput.substring(6, 8))
        } catch(exception: Exception) {
            reasonQuoteCannotBeCompleted += "ssn not valid"
            return false
        }
        return true
    }

    fun passedUnderwritingGuidelines(uwGuidelinesChecker: UwGuidelinesChecker): Boolean {
        return if (this.completeQuoteData is CompleteQuoteData.CompleteHomeData) {
            uwGuidelinesChecker.meetsHomeUwGuidelines(this)
        } else {
            uwGuidelinesChecker.meetsHouseUwGuidelines(this)
        }
    }

    fun passedDebtCheck(debtChecker: DebtChecker): Boolean {
        return debtChecker.passesDebtCheck(this)
    }

    fun setPriceRetrievedFromProductPricing(productPricingService: ProductPricingService) {
        when {
            this.completeQuoteData is CompleteQuoteData.CompleteHomeData -> {
                this.price = productPricingService.priceFromProductPricingForHomeQuote(homeQuotePriceDto(this)).price
            }
            this.completeQuoteData is CompleteQuoteData.CompleteHouseData ->  {
                this.price = productPricingService.priceFromProductPricingForHouseQuote(houseQuotePriceDto(this)).price
            }
            else -> throw RuntimeException("Cannot calculate price as incorrect product type - product type is ${this.productType}")
        }

    }

    fun getRapioQuoteRequestDto(email: String): RapioQuoteRequestDto {
        return when {
            this.completeQuoteData is CompleteQuoteData.CompleteHomeData -> {
                RapioQuoteRequestDto(
                        this.price!!,
                        this.firstName!!,
                        this.lastName!!,
                        this.birthDateFromSsn(),
                        this.isStudent,
                        Address(
                                this.completeQuoteData.street,
                                this.completeQuoteData.city,
                                this.completeQuoteData.zipCode,
                                0
                        ),
                        this.livingSpace.toFloat(),
                        this.lineOfBusiness,
                        this.currentInsurer,
                        this.houseHoldSize,
                        this.startDate,
                        this.ssn,
                        email,
                        ""
                )

            }
            else -> throw RuntimeException("Incomplete quote is of unknown type: ${this.completeQuoteData::class}")
        }
    }

    companion object {
        private fun homeQuotePriceDto(completeQuote: CompleteQuote): HomeQuotePriceDto {
            if (completeQuote.completeQuoteData is CompleteQuoteData.CompleteHomeData) {
                return HomeQuotePriceDto(
                        birthDate = completeQuote.birthDateFromSsn(),
                        livingSpace = completeQuote.livingSpace,
                        houseHoldSize = completeQuote.houseHoldSize,
                        zipCode = completeQuote.completeQuoteData.zipCode,
                        houseType = completeQuote.lineOfBusiness,
                        isStudent = completeQuote.isStudent
                )
            }
            throw RuntimeException("missing data cannot create home quote price dto")
        }

        private fun houseQuotePriceDto(completeQuote: CompleteQuote): HouseQuotePriceDto {
            if (completeQuote.completeQuoteData is CompleteQuoteData.CompleteHomeData) {
                //  TODO: complete
                return HouseQuotePriceDto(
                        birthDate = completeQuote.birthDateFromSsn()
                )
            }
            throw RuntimeException("missing data cannot create house quote price dto")
        }
    }
}






