package com.hedvig.underwriter.testhelp.databuilder

import com.hedvig.underwriter.model.ONE_DAY
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.ProductType
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteState
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun quote(init: QB.() -> Unit): Quote {
    val qb = QB()
    qb.init()
    return qb.build()
}

class QB(
    var id: UUID = UUID.fromString("1c3463c4-0c71-11ea-8fd9-4865ee119be4"),
    var createdAt: Instant = Instant.now(),
    var price: BigDecimal? = BigDecimal.ZERO,
    var currency: String? = null,
    var productType: ProductType = ProductType.APARTMENT,
    var state: QuoteState = QuoteState.INCOMPLETE,
    var initiatedFrom: QuoteInitiatedFrom = QuoteInitiatedFrom.RAPIO,
    var attributedTo: Partner = Partner.HEDVIG,
    var data: DataBuilder<QuoteData> = ApartmentDataBuilder(),

    var currentInsurer: String? = null,

    var startDate: LocalDate? = null,

    var validity: Long = ONE_DAY * 30,
    var breachedUnderwritingGuidelines: List<String>? = null,
    var underwritingGuidelinesBypassedBy: String? = null,
    var memberId: String? = null,
    var originatingProductId: UUID? = null,
    var agreementId: UUID? = null,
    var contractId: UUID? = null
) {
    fun build() = Quote(
        id = id,
        createdAt = createdAt,
        updatedAt = null,
        price = price,
        currency = currency,
        productType = productType,
        state = state,
        initiatedFrom = initiatedFrom,
        attributedTo = attributedTo,
        data = data.build(),
        currentInsurer = currentInsurer,
        startDate = startDate,
        validity = validity,
        breachedUnderwritingGuidelines = breachedUnderwritingGuidelines,
        underwritingGuidelinesBypassedBy = underwritingGuidelinesBypassedBy,
        memberId = memberId,
        originatingProductId = originatingProductId,
        agreementId = agreementId,
        priceFrom = null,
        dataCollectionId = null,
        signFromHopeTriggeredBy = null,
        contractId = contractId
    )
}
