package com.hedvig.underwriter.service

import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.libs.translations.TranslationsClient
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.service.quotesSignDataStrategies.SignStrategyService
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundledPriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.InsuranceType
import com.hedvig.underwriter.testhelp.databuilder.ApartmentDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianHomeContentDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianTravelDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.quote
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal
import java.util.UUID

@RunWith(MockitoJUnitRunner::class)
class BundleQuoteServiceImplTest {

    @MockK
    lateinit var quoteService: QuoteService

    @MockK
    lateinit var productPricingService: ProductPricingService

    @MockK
    lateinit var translationsClient: TranslationsClient

    private lateinit var cut: BundleQuotesServiceImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        cut = BundleQuotesServiceImpl(quoteService, productPricingService, SignStrategyService(mockk(), mockk(), mockk()))
    }

    @Test
    fun bundleNorwegianYouthQuotes() {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID())

        val quote1 = quote {
            id = ids[0]
            data = NorwegianHomeContentDataBuilder(isYouth = true)
            price = BigDecimal.TEN
            currency = "NOK"
        }
        val quote2 = quote {
            id = ids[1]
            data = NorwegianTravelDataBuilder(isYouth = true)
            price = BigDecimal.TEN
            currency = "NOK"
        }

        val requestCaptureMutableList = mutableListOf<CalculateBundleInsuranceCostRequest>()

        every {
            quoteService.getQuotes(ids)
        } returns listOf(quote1, quote2)

        every {
            productPricingService.calculateBundleInsuranceCostForMember(
                capture(requestCaptureMutableList),
                any()
            )
        } returns InsuranceCost(
            MonetaryAmountV2.of(BigDecimal(20), "NOK"),
            MonetaryAmountV2.of(BigDecimal(0), "NOK"),
            MonetaryAmountV2.of(BigDecimal(20), "NOK"),
            null
        )

        every {
            translationsClient.getTranslation(any(), any())
        } returns ""

        cut.bundleQuotes("1337", ids)

        assertThat(requestCaptureMutableList.first()).isEqualTo(
            CalculateBundleInsuranceCostRequest(
                listOf(
                    CalculateBundledPriceDto(
                        Money.of(BigDecimal.TEN, "NOK"),
                        InsuranceType.NORWEGIAN_YOUTH_HOME_CONTENTS
                    ),
                    CalculateBundledPriceDto(
                        Money.of(BigDecimal.TEN, "NOK"),
                        InsuranceType.NORWEGIAN_YOUTH_TRAVEL
                    )
                )
            )
        )
    }

    @Test
    fun bundleNorwegianRegularQuotes() {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID())

        val quote1 = quote {
            id = ids[0]
            data = NorwegianHomeContentDataBuilder()
            price = BigDecimal.TEN
            currency = "NOK"
        }
        val quote2 = quote {
            id = ids[1]
            data = NorwegianTravelDataBuilder()
            price = BigDecimal.TEN
            currency = "NOK"
        }

        val requestCaptureMutableList = mutableListOf<CalculateBundleInsuranceCostRequest>()

        every {
            quoteService.getQuotes(ids)
        } returns listOf(quote1, quote2)

        every {
            productPricingService.calculateBundleInsuranceCostForMember(
                capture(requestCaptureMutableList),
                any()
            )
        } returns InsuranceCost(
            MonetaryAmountV2.of(BigDecimal(20), "NOK"),
            MonetaryAmountV2.of(BigDecimal(0), "NOK"),
            MonetaryAmountV2.of(BigDecimal(20), "NOK"),
            null
        )

        every {
            translationsClient.getTranslation(any(), any())
        } returns ""

        cut.bundleQuotes("1337", ids)

        assertThat(requestCaptureMutableList.first()).isEqualTo(
            CalculateBundleInsuranceCostRequest(
                listOf(
                    CalculateBundledPriceDto(
                        Money.of(BigDecimal.TEN, "NOK"),
                        InsuranceType.NORWEGIAN_HOME_CONTENTS
                    ),
                    CalculateBundledPriceDto(
                        Money.of(BigDecimal.TEN, "NOK"),
                        InsuranceType.NORWEGIAN_TRAVEL
                    )
                )
            )
        )
    }

    @Test
    fun bundleSwedishStudentQuote() {
        val id = UUID.randomUUID()

        val quote = quote {
            this.id = id
            data = ApartmentDataBuilder(subType = ApartmentProductSubType.STUDENT_BRF)
            price = BigDecimal.TEN
            currency = "SEK"
        }

        val requestCaptureMutableList = mutableListOf<CalculateBundleInsuranceCostRequest>()

        every {
            quoteService.getQuotes(listOf(id))
        } returns listOf(quote)

        every {
            productPricingService.calculateBundleInsuranceCostForMember(
                capture(requestCaptureMutableList),
                any()
            )
        } returns InsuranceCost(
            MonetaryAmountV2.of(BigDecimal(10), "SEK"),
            MonetaryAmountV2.of(BigDecimal(0), "SEK"),
            MonetaryAmountV2.of(BigDecimal(10), "SEK"),
            null
        )

        every {
            translationsClient.getTranslation(any(), any())
        } returns ""

        cut.bundleQuotes("1337", listOf(id))

        assertThat(requestCaptureMutableList.first()).isEqualTo(
            CalculateBundleInsuranceCostRequest(
                listOf(
                    CalculateBundledPriceDto(
                        Money.of(BigDecimal.TEN, "SEK"),
                        InsuranceType.SWEDISH_STUDENT_BRF
                    )
                )
            )
        )
    }

    @Test
    fun bundleSwedishRegularQuote() {
        val id = UUID.randomUUID()

        val quote = quote {
            this.id = id
            data = ApartmentDataBuilder()
            price = BigDecimal.TEN
            currency = "SEK"
        }

        val requestCaptureMutableList = mutableListOf<CalculateBundleInsuranceCostRequest>()

        every {
            quoteService.getQuotes(listOf(id))
        } returns listOf(quote)

        every {
            productPricingService.calculateBundleInsuranceCostForMember(
                capture(requestCaptureMutableList),
                any()
            )
        } returns InsuranceCost(
            MonetaryAmountV2.of(BigDecimal(10), "SEK"),
            MonetaryAmountV2.of(BigDecimal(0), "SEK"),
            MonetaryAmountV2.of(BigDecimal(10), "SEK"),
            null
        )

        every {
            translationsClient.getTranslation(any(), any())
        } returns ""

        cut.bundleQuotes("1337", listOf(id))

        assertThat(requestCaptureMutableList.first()).isEqualTo(
            CalculateBundleInsuranceCostRequest(
                listOf(
                    CalculateBundledPriceDto(
                        Money.of(BigDecimal.TEN, "SEK"),
                        InsuranceType.SWEDISH_BRF
                    )
                )
            )
        )
    }
}
