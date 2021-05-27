package com.hedvig.underwriter.web

import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import io.mockk.every
import io.mockk.slot
import org.javamoney.moneta.Money
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.matchesPredicate
import assertk.assertions.startsWith
import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.productPricingObjects.dtos.AgreementQuote
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.FinalizeOnBoardingRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Flag
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import com.hedvig.underwriter.testhelp.IntegrationTest
import com.hedvig.underwriter.testhelp.QuoteClient
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random.Default.nextLong

class RapioIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var quoteClient: QuoteClient

    @Test
    fun `Create Norwegian travel quote and sign it successfully`() {

        val ssn = "11077941012"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val peQueryPriceRequest = slot<PriceQueryRequest.NorwegianTravel>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))

        // Create quote
        val quoteResponse = quoteClient.createNorwegianTravelQuote()

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("NOK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        // Sign quote
        val signResponse = quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Apan",
            lastName = "Apansson",
            ssn = ssn,
            email = "apan@apansson.se",
            startDate = today.toString()
        )

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.NORWAY)

        // Get quote
        val quote = quoteClient.getQuote(quoteResponse.id)!!

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("NOK")
        assertThat(quote.productType.name).isEqualTo("TRAVEL")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(NorwegianTravelData::class.java)
        val data = quote.data as NorwegianTravelData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1944-08-04")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.coInsured).isEqualTo(1)
        assertThat(data.isYouth).isEqualTo(false)
        assertThat(data.internalId).isNull()

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("NORWAY")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address).isNull()
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1944-08-04")
        }

        // Validate request to Price Enging
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1944-08-04")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.lineOfBusiness.name).isEqualTo("REGULAR")
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)
            val ppQuote = captured.quotes[0] as AgreementQuote.NorwegianTravelQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("NOK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.coInsured[0].ssn).isNull()
            assertThat(ppQuote.lineOfBusiness.name).isEqualTo("REGULAR")
        }
    }

    @Test
    fun `Create Norwegian travel quote and sign it without or invalid ssn should fail`() {

        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { productPricingClient.createContract(any(), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))

        val quoteResponse = quoteClient.createNorwegianTravelQuote("1912-12-12")

        assertThat(quoteResponse.price.toString(), "12")
        assertThat(quoteResponse.currency, "NOK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        val response1 = quoteClient.signQuoteRaw(
            quoteId = quoteResponse.id,
            firstName = "Apan",
            lastName = "Banansson",
            email = "apan@apansson.se"
        )

        assertThat(response1.statusCodeValue).isEqualTo(422)
        assertThat(response1.body!!.contains("no ssn"))

        val response2 = quoteClient.signQuoteRaw(
            quoteId = quoteResponse.id,
            ssn = "11077900000",
            firstName = "Apan",
            lastName = "Banansson",
            email = "apan@apansson.se"
        )

        assertThat(response2.statusCodeValue).isEqualTo(500)
        assertThat(response2.body!!.contains("Invalid Norwegian SSN"))
    }

    @Test
    fun `Create Norwegian home content quote and sign it successfully`() {

        val ssn = "11077941012"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val peQueryPriceRequest = slot<PriceQueryRequest.NorwegianHomeContent>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))

        // Create quote
        val quoteResponse = quoteClient.createNorwegianHomeContentQuote(birthdate = "1988-01-01", street = "ApStreet 999", livingSpace = 122)

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("NOK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        // Sign quote
        val signResponse = quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Apan",
            lastName = "Apansson",
            ssn = ssn,
            email = "apan@apansson.se",
            startDate = today.toString()
        )

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.NORWAY)

        // Get quote
        val quote = quoteClient.getQuote(quoteResponse.id)!!

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("NOK")
        assertThat(quote.productType.name).isEqualTo("HOME_CONTENT")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(NorwegianHomeContentsData::class.java)
        val data = quote.data as NorwegianHomeContentsData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1988-01-01")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.street).startsWith("ApStreet")
        assertThat(data.city).isEqualTo("ApCity")
        assertThat(data.zipCode).isEqualTo("12345")
        assertThat(data.livingSpace).isEqualTo(122)
        assertThat(data.coInsured).isEqualTo(1)
        assertThat(data.isYouth).isEqualTo(false)
        assertThat(data.type.name).isEqualTo("OWN")
        assertThat(data.internalId).isNull()

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("NORWAY")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address!!.street).startsWith("ApStreet")
            assertThat(captured.address!!.city).isEqualTo("ApCity")
            assertThat(captured.address!!.zipCode).isEqualTo("12345")
            assertThat(captured.address!!.apartmentNo).isEqualTo("")
            assertThat(captured.address!!.floor).isEqualTo(0)
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1988-01-01")
        }

        // Validate request to Price Enging
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1988-01-01")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.lineOfBusiness.name).isEqualTo("OWN")
            assertThat(captured.postalCode).isEqualTo("12345")
            assertThat(captured.squareMeters).isEqualTo(122)
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)

            val ppQuote = captured.quotes[0] as AgreementQuote.NorwegianHomeContentQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("NOK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.address.city).isEqualTo("ApCity")
            assertThat(ppQuote.address.postalCode).isEqualTo("12345")
            assertThat(ppQuote.address.country.name).isEqualTo("NO")
            assertThat(ppQuote.address.street).startsWith("ApStreet")
            assertThat(ppQuote.address.coLine).isEqualTo(null)
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.coInsured[0].ssn).isNull()
            assertThat(ppQuote.squareMeters).isEqualTo(122)
            assertThat(ppQuote.lineOfBusiness.name).isEqualTo("OWN")
        }
    }

    @Test
    fun `Create Swedish house quote and sign it successfully`() {

        val ssn = "199110112399"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val msCheckPersonDebtRequest = slot<String>()
        val msPersonStatusRequest = slot<String>()
        val peQueryPriceRequest = slot<PriceQueryRequest.SwedishHouse>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { memberServiceClient.checkPersonDebt(capture(msCheckPersonDebtRequest)) } returns ResponseEntity.status(200).body(null)
        every { memberServiceClient.personStatus(capture(msPersonStatusRequest)) } returns ResponseEntity.status(200).body(PersonStatusDto(Flag.GREEN))
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create quote
        val quoteResponse = quoteClient.createSwedishHouseQuote(
            ssn,
            street = "ApGatan 998",
            livingSpace = 100,
            householdSize = 2,
            ancillaryArea = 10,
            yearOfConstruction = 1980,
            numberOfBathrooms = 2,
            subleted = true
        )

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("SEK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        // Sign quote
        val signResponse = quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Apan",
            lastName = "Apansson",
            ssn = ssn,
            email = "apan@apansson.se",
            startDate = today.toString())

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.SWEDEN)

        // Get quote
        val quote = quoteClient.getQuote(quoteResponse.id)!!

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("SEK")
        assertThat(quote.productType.name).isEqualTo("HOUSE")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(SwedishHouseData::class.java)
        val data = quote.data as SwedishHouseData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1991-10-11")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.street).isEqualTo("ApGatan 998")
        assertThat(data.city).isEqualTo("ApCity")
        assertThat(data.zipCode).isEqualTo("12345")
        assertThat(data.livingSpace).isEqualTo(100)
        assertThat(data.householdSize).isEqualTo(2)
        assertThat(data.internalId).isNull()
        assertThat(data.ancillaryArea).isEqualTo(10)
        assertThat(data.extraBuildings!!.size).isEqualTo(1)
        assertThat(data.extraBuildings!![0].area).isEqualTo(11)
        assertThat(data.extraBuildings!![0].type.name).isEqualTo("CARPORT")
        assertThat(data.extraBuildings!![0].displayName).isNull()
        assertThat(data.extraBuildings!![0].hasWaterConnected).isEqualTo(true)
        assertThat(data.isSubleted).isEqualTo(true)
        assertThat(data.numberOfBathrooms).isEqualTo(2)
        assertThat(data.yearOfConstruction).isEqualTo(1980)

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("SWEDEN")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address!!.street).startsWith("ApGatan 998")
            assertThat(captured.address!!.city).isEqualTo("ApCity")
            assertThat(captured.address!!.zipCode).isEqualTo("12345")
            assertThat(captured.address!!.apartmentNo).isEqualTo("")
            assertThat(captured.address!!.floor).isEqualTo(0)
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1991-10-11")
        }

        // Validate request to Price Enging
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1991-10-11")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.postalCode).isEqualTo("12345")
            assertThat(captured.squareMeters).isEqualTo(100)
            assertThat(captured.ancillaryArea).isEqualTo(10)
            assertThat(captured.extraBuildings.size).isEqualTo(1)
            assertThat(captured.extraBuildings[0].area).isEqualTo(11)
            assertThat(captured.extraBuildings[0].hasWaterConnected).isEqualTo(true)
            assertThat(captured.extraBuildings[0].type.name).isEqualTo("CARPORT")
            assertThat(captured.isSubleted).isEqualTo(true)
            assertThat(captured.numberOfBathrooms).isEqualTo(2)
            assertThat(captured.yearOfConstruction.value).isEqualTo(1980)
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)

            val ppQuote = captured.quotes[0] as AgreementQuote.SwedishHouseQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("SEK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.address.city).isEqualTo("ApCity")
            assertThat(ppQuote.address.postalCode).isEqualTo("12345")
            assertThat(ppQuote.address.country.name).isEqualTo("SE")
            assertThat(ppQuote.address.street).isEqualTo("ApGatan 998")
            assertThat(ppQuote.address.coLine).isEqualTo(null)
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].ssn).isNull()
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.squareMeters).isEqualTo(100)
            assertThat(ppQuote.ancillaryArea).isEqualTo(10)
            assertThat(ppQuote.extraBuildings.size).isEqualTo(1)
            assertThat(ppQuote.extraBuildings[0].area).isEqualTo(11)
            assertThat(ppQuote.extraBuildings[0].displayName).isNull()
            assertThat(ppQuote.extraBuildings[0].hasWaterConnected).isEqualTo(true)
            assertThat(ppQuote.extraBuildings[0].type.name).isEqualTo("CARPORT")
            assertThat(ppQuote.isSubleted).isEqualTo(true)
            assertThat(ppQuote.numberOfBathrooms).isEqualTo(2)
            assertThat(ppQuote.yearOfConstruction).isEqualTo(1980)
        }
    }

    @Test
    fun `Create Swedish apartment quote and sign it successfully`() {

        val ssn = "199110112399"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val msCheckPersonDebtRequest = slot<String>()
        val msPersonStatusRequest = slot<String>()
        val peQueryPriceRequest = slot<PriceQueryRequest.SwedishApartment>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { memberServiceClient.checkPersonDebt(capture(msCheckPersonDebtRequest)) } returns ResponseEntity.status(200).body(null)
        every { memberServiceClient.personStatus(capture(msPersonStatusRequest)) } returns ResponseEntity.status(200).body(PersonStatusDto(Flag.GREEN))
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create quote
        val quoteResponse = quoteClient.createSwedishApartmentQuote(
            ssn,
            street = "ApGatan 997",
            zip = "1234",
            city = "ApCity",
            livingSpace = 122,
            householdSize = 2,
            subType = "BRF"
        )

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("SEK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        // Sign quote
        val signResponse = quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Apan",
            lastName = "Apansson",
            ssn = ssn,
            email = "apan@apansson.se",
            startDate = today.toString())

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.SWEDEN)

        // Get quote
        val quote = quoteClient.getQuote(quoteResponse.id)!!

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("SEK")
        assertThat(quote.productType.name).isEqualTo("APARTMENT")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(SwedishApartmentData::class.java)
        val data = quote.data as SwedishApartmentData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1991-10-11")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.street).isEqualTo("ApGatan 997")
        assertThat(data.city).isEqualTo("ApCity")
        assertThat(data.zipCode).isEqualTo("1234")
        assertThat(data.livingSpace).isEqualTo(122)
        assertThat(data.subType!!.name).isEqualTo("BRF")
        assertThat(data.householdSize).isEqualTo(2)
        assertThat(data.isStudent).isEqualTo(false)
        assertThat(data.internalId).isNull()

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("SWEDEN")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address!!.street).isEqualTo("ApGatan 997")
            assertThat(captured.address!!.city).isEqualTo("ApCity")
            assertThat(captured.address!!.zipCode).isEqualTo("1234")
            assertThat(captured.address!!.apartmentNo).isEqualTo("")
            assertThat(captured.address!!.floor).isEqualTo(0)
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1991-10-11")
        }

        // Validate request to Price Enging
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1991-10-11")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.lineOfBusiness.name).isEqualTo("BRF")
            assertThat(captured.postalCode).isEqualTo("1234")
            assertThat(captured.squareMeters).isEqualTo(122)
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)

            val ppQuote = captured.quotes[0] as AgreementQuote.SwedishApartmentQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("SEK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.address.city).isEqualTo("ApCity")
            assertThat(ppQuote.address.postalCode).isEqualTo("1234")
            assertThat(ppQuote.address.country.name).isEqualTo("SE")
            assertThat(ppQuote.address.street).isEqualTo("ApGatan 997")
            assertThat(ppQuote.address.coLine).isEqualTo(null)
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].ssn).isNull()
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.squareMeters).isEqualTo(122)
            assertThat(ppQuote.lineOfBusiness.name).isEqualTo("BRF")
        }
    }

    @Test
    fun `Create Norwegian travel and home content quote bundling`() {

        val ppCalculateBundleInsuranceCostRequest = slot<CalculateBundleInsuranceCostRequest>()

        val insuranceCostResponse = InsuranceCost(
            monthlyGross = MonetaryAmountV2.of(200.0, "NOK"),
            monthlyDiscount = MonetaryAmountV2.of(50.0, "NOK"),
            monthlyNet = MonetaryAmountV2.of(150.0, "NOK"),
            freeUntil = null
        )

        // Mock clients and capture the outgoing requests for later validation
        every { productPricingClient.calculateBundleInsuranceCost(capture(ppCalculateBundleInsuranceCostRequest)) } returns ResponseEntity.status(200).body(insuranceCostResponse)
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(100, "NOK"))

        // Create quotes
        val travelQuoteResponse = quoteClient.createNorwegianTravelQuote()
        val homeContentQuoteResponse = quoteClient.createNorwegianHomeContentQuote()

        // Get bundle cost
        val bundleResponse = quoteClient.createBundle(travelQuoteResponse.id, homeContentQuoteResponse.id)

        assertThat(bundleResponse.bundleCost.monthlyGross.amount.toString()).isEqualTo("200.00")
        assertThat(bundleResponse.bundleCost.monthlyGross.currency).isEqualTo("NOK")
        assertThat(bundleResponse.bundleCost.monthlyDiscount.amount.toString()).isEqualTo("50.00")
        assertThat(bundleResponse.bundleCost.monthlyDiscount.currency).isEqualTo("NOK")
        assertThat(bundleResponse.bundleCost.monthlyNet.amount.toString()).isEqualTo("150.00")
        assertThat(bundleResponse.bundleCost.monthlyNet.currency).isEqualTo("NOK")
    }

    @Test
    fun `Test invalid bundling combos`() {

        // Mock clients and capture the outgoing requests for later validation
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(100, "NOK"))
        every { memberServiceClient.checkPersonDebt(any()) } returns ResponseEntity.status(200).body(null)
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200).body(PersonStatusDto(Flag.GREEN))

        // Create quotes
        val norwayQuoteResponse1 = quoteClient.createNorwegianTravelQuote()
        val norwayQuoteResponse2 = quoteClient.createNorwegianTravelQuote()
        val swedishQuoteResponse1 = quoteClient.createSwedishApartmentQuote("199110112399")
        val swedishQuoteResponse2 = quoteClient.createSwedishHouseQuote("199110112399")

        val bundleResponse1 = quoteClient.createBundleRaw(norwayQuoteResponse1.id, swedishQuoteResponse1.id)

        assertThat(bundleResponse1.statusCode.value()).isEqualTo(500)
        assertThat(bundleResponse1.body).matchesPredicate { it!!.contains("Quotes belong to different markets") }

        val bundleResponse2 = quoteClient.createBundleRaw(norwayQuoteResponse1.id, norwayQuoteResponse2.id)

        assertThat(bundleResponse2.statusCode.value()).isEqualTo(500)
        assertThat(bundleResponse2.body).matchesPredicate { it!!.contains("Bundling not supported for quotes") }

        val bundleResponse3 = quoteClient.createBundleRaw(swedishQuoteResponse1.id, swedishQuoteResponse2.id)

        assertThat(bundleResponse3.statusCode.value()).isEqualTo(500)
        assertThat(bundleResponse3.body).matchesPredicate { it!!.contains("Bundling not supported for quotes") }

        val bundleResponse4 = quoteClient.createBundleRaw(swedishQuoteResponse1.id, swedishQuoteResponse1.id)

        assertThat(bundleResponse4.statusCode.value()).isEqualTo(500)
        assertThat(bundleResponse4.body).matchesPredicate { it!!.contains("Not all quotes found") }
    }

    @Test
    fun `Create Norwegian travel and home content quote bundle, get bundle price and sign it successfully`() {
        val ssn = "11077941012"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingRequest1 = slot<String>()
        val msFinalizeOnboardingRequest2 = slot<FinalizeOnBoardingRequest>()
        val peQueryPriceRequest1 = slot<PriceQueryRequest.NorwegianTravel>()
        val peQueryPriceRequest2 = slot<PriceQueryRequest.NorwegianHomeContent>()
        val ppCreateContractRequest = mutableListOf<CreateContractsRequest>()
        val ppCalculateBundleInsuranceCostRequest = slot<CalculateBundleInsuranceCostRequest>()

        val insuranceCostResponse = InsuranceCost(
            monthlyGross = MonetaryAmountV2.of(200.0, "NOK"),
            monthlyDiscount = MonetaryAmountV2.of(50.0, "NOK"),
            monthlyNet = MonetaryAmountV2.of(150.0, "NOK"),
            freeUntil = null
        )

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingRequest1), capture(msFinalizeOnboardingRequest2)) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { productPricingClient.calculateBundleInsuranceCost(capture(ppCalculateBundleInsuranceCostRequest)) } returns ResponseEntity.status(200).body(insuranceCostResponse)
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest1)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(100, "NOK"))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest2)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(200, "NOK"))

        // Create quotes
        val travelQuoteResponse = quoteClient.createNorwegianTravelQuote(birthdate = "1988-01-01")
        val homeContentQuoteResponse = quoteClient.createNorwegianHomeContentQuote(birthdate = "1988-01-01", street = "ApStreet 996", livingSpace = 122)

        // Get bundle cost
        val bundleResponse = quoteClient.createBundle(travelQuoteResponse.id, homeContentQuoteResponse.id)

        assertThat(bundleResponse.bundleCost.monthlyGross.amount.toString()).isEqualTo("200.00")
        assertThat(bundleResponse.bundleCost.monthlyGross.currency).isEqualTo("NOK")
        assertThat(bundleResponse.bundleCost.monthlyDiscount.amount.toString()).isEqualTo("50.00")
        assertThat(bundleResponse.bundleCost.monthlyDiscount.currency).isEqualTo("NOK")
        assertThat(bundleResponse.bundleCost.monthlyNet.amount.toString()).isEqualTo("150.00")
        assertThat(bundleResponse.bundleCost.monthlyNet.currency).isEqualTo("NOK")

        val signResponse = quoteClient.signQuoteBundle(
            firstName = "Apan",
            lastName = "Apansson",
            ssn = ssn,
            email = "apan@apansson.se",
            startDate = today,
            price = bundleResponse.bundleCost.monthlyNet.amount.toDouble(),
            currency = bundleResponse.bundleCost.monthlyNet.currency,
            quoteIds = *arrayOf(travelQuoteResponse.id, homeContentQuoteResponse.id)
        )

        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo("NORWAY")
        assertThat(signResponse.contracts.size).isEqualTo(2)
        assertThat(signResponse.contracts[0].id).isEqualTo(contractId)
        assertThat(signResponse.contracts[1].id).isEqualTo(contractId) // Should be different in irl, now we mock it to this for both contracts

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("NORWAY")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingRequest1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingRequest2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address!!.street).isEqualTo("ApStreet 996")
            assertThat(captured.address!!.city).isEqualTo("ApCity")
            assertThat(captured.address!!.zipCode).isEqualTo("12345")
            assertThat(captured.address!!.apartmentNo).isEqualTo("")
            assertThat(captured.address!!.floor).isEqualTo(0)
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1988-01-01")
        }

        // Validate travel request to Price Engine
        with(peQueryPriceRequest1) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(travelQuoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1988-01-01")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.lineOfBusiness.name).isEqualTo("REGULAR")
        }

        // Validate home content request to Price Engine
        with(peQueryPriceRequest2) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(homeContentQuoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1988-01-01")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.lineOfBusiness.name).isEqualTo("OWN")
            assertThat(captured.postalCode).isEqualTo("12345")
            assertThat(captured.squareMeters).isEqualTo(122)
        }

        // Validate request to Product Pricing Service
        assertThat(ppCreateContractRequest.size).isEqualTo(2)
        with(ppCreateContractRequest.first { it.quotes[0] is AgreementQuote.NorwegianHomeContentQuote }) {
            assertThat(memberId).isEqualTo(memberId)
            assertThat(mandate!!.firstName).isEqualTo("Apan")
            assertThat(mandate!!.lastName).isEqualTo("Apansson")
            assertThat(mandate!!.ssn).isEqualTo(ssn)
            assertThat(mandate!!.referenceToken).isEmpty()
            assertThat(mandate!!.signature).isEmpty()
            assertThat(mandate!!.oscpResponse).isEmpty()
            assertThat(signSource.name).isEqualTo("RAPIO")
            assertThat(quotes.size).isEqualTo(1)

            val ppQuote = quotes[0] as AgreementQuote.NorwegianHomeContentQuote
            assertThat(ppQuote.quoteId).isEqualTo(homeContentQuoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(homeContentQuoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("NOK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.address.street).isEqualTo("ApStreet 996")
            assertThat(ppQuote.address.city).isEqualTo("ApCity")
            assertThat(ppQuote.address.postalCode).isEqualTo("12345")
            assertThat(ppQuote.address.country.name).isEqualTo("NO")
            assertThat(ppQuote.address.coLine).isNull()
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].ssn).isNull()
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.squareMeters).isEqualTo(122)
            assertThat(ppQuote.lineOfBusiness.name).isEqualTo("OWN")
        }
        with(ppCreateContractRequest.first { it.quotes[0] is AgreementQuote.NorwegianTravelQuote }) {
            assertThat(memberId).isEqualTo(memberId)
            assertThat(mandate!!.firstName).isEqualTo("Apan")
            assertThat(mandate!!.lastName).isEqualTo("Apansson")
            assertThat(mandate!!.ssn).isEqualTo(ssn)
            assertThat(mandate!!.referenceToken).isEmpty()
            assertThat(mandate!!.signature).isEmpty()
            assertThat(mandate!!.oscpResponse).isEmpty()
            assertThat(signSource.name).isEqualTo("RAPIO")
            assertThat(quotes.size).isEqualTo(1)

            val ppQuote = quotes[0] as AgreementQuote.NorwegianTravelQuote
            assertThat(ppQuote.quoteId).isEqualTo(travelQuoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(travelQuoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("NOK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].ssn).isNull()
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.lineOfBusiness.name).isEqualTo("REGULAR")
        }
    }

    @Test
    fun `Test signing bundle failures`() {

        // Mock clients and capture the outgoing requests for later validation
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(100, "NOK"))
        every { memberServiceClient.checkPersonDebt(any()) } returns ResponseEntity.status(200).body(null)
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200).body(PersonStatusDto(Flag.GREEN))

        // Create quotes
        val norwayQuoteResponse1 = quoteClient.createNorwegianTravelQuote()
        val norwayQuoteResponse2 = quoteClient.createNorwegianTravelQuote()
        val swedishQuoteResponse1 = quoteClient.createSwedishApartmentQuote("199110112399")
        val swedishQuoteResponse2 = quoteClient.createSwedishHouseQuote("199110112399")

        // Cannot mix quotes from different markets
        val signResponse1 = quoteClient.signQuoteBundleRaw(
            firstName = "Apan",
            lastName = "Apansson",
            ssn = "199110112399",
            email = "apan@apansson.se",
            startDate = LocalDate.now(),
            price = 10.0,
            currency = "NOK",
            quoteIds = *arrayOf(norwayQuoteResponse1.id, swedishQuoteResponse1.id)
        )

        assertThat(signResponse1.statusCode.value()).isEqualTo(500)

        // Cannot bundle two of the same
        val signResponse2 = quoteClient.signQuoteBundleRaw(
            firstName = "Apan",
            lastName = "Apansson",
            ssn = "11077941012",
            email = "apan@apansson.se",
            startDate = LocalDate.now(),
            price = 10.0,
            currency = "NOK",
            quoteIds = *arrayOf(norwayQuoteResponse1.id, norwayQuoteResponse2.id)
        )

        assertThat(signResponse2.statusCode.value()).isEqualTo(422)
        assertThat(signResponse2.body).matchesPredicate { it!!.contains("Quotes can not be bundled") }

        val signResponse3 = quoteClient.signQuoteBundleRaw(
            firstName = "Apan",
            lastName = "Apansson",
            ssn = "199110112399",
            email = "apan@apansson.se",
            startDate = LocalDate.now(),
            price = 10.0,
            currency = "NOK",
            quoteIds = *arrayOf(swedishQuoteResponse1.id, swedishQuoteResponse2.id)
        )

        assertThat(signResponse3.statusCode.value()).isEqualTo(422)
        assertThat(signResponse3.body).matchesPredicate { it!!.contains("Quotes can not be bundled") }

        val signResponse4 = quoteClient.signQuoteBundleRaw(
            firstName = "Apan",
            lastName = "Apansson",
            ssn = "11077941012",
            email = "apan@apansson.se",
            startDate = LocalDate.now(),
            price = 10.0,
            currency = "NOK",
            quoteIds = *arrayOf(swedishQuoteResponse1.id, swedishQuoteResponse1.id)
        )

        assertThat(signResponse4.statusCode.value()).isEqualTo(422)
        assertThat(signResponse4.body).matchesPredicate { it!!.contains("not all quotes found") }

        val signResponse5 = quoteClient.signQuoteBundleRaw(
            firstName = "Apan",
            lastName = "Apansson",
            ssn = "199110112399",
            email = "apan@apansson.se",
            startDate = LocalDate.now(),
            price = 10.0,
            currency = "NOK",
            quoteIds = *arrayOf(swedishQuoteResponse1.id)
        )

        assertThat(signResponse5.statusCode.value()).isEqualTo(500)
        assertThat(signResponse5.body).matchesPredicate { it!!.contains("Not a bundle") }
    }

    @Test
    fun `Test signing bundle with wrong bundle price`() {

        val insuranceCostResponse = InsuranceCost(
            monthlyGross = MonetaryAmountV2.of(200.0, "NOK"),
            monthlyDiscount = MonetaryAmountV2.of(50.0, "NOK"),
            monthlyNet = MonetaryAmountV2.of(150.0, "NOK"),
            freeUntil = null
        )

        // Mock clients and capture the outgoing requests for later validation
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(100, "NOK"))
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200).body(PersonStatusDto(Flag.GREEN))
        every { productPricingClient.calculateBundleInsuranceCost(any()) } returns ResponseEntity.status(200).body(insuranceCostResponse)

        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(nextLong(Long.MAX_VALUE).toString()))
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { productPricingClient.createContract(any(), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))

        // Create quotes
        val norwayQuoteResponse1 = quoteClient.createNorwegianTravelQuote()
        val norwayQuoteResponse2 = quoteClient.createNorwegianHomeContentQuote()

        val signResponse1 = quoteClient.signQuoteBundleRaw(
            firstName = "Apa",
            lastName = "Apansson",
            ssn = "11077941012",
            email = "apan@apanson.se",
            startDate = LocalDate.now(),
            price = 10.0,
            currency = "NOK",
            quoteIds = *arrayOf(norwayQuoteResponse1.id, norwayQuoteResponse2.id)
        )

        assertThat(signResponse1.statusCode.value()).isEqualTo(422)
        assertThat(signResponse1.body).matchesPredicate { it!!.contains("bundle price") }

        val signResponse2 = quoteClient.signQuoteBundleRaw(
            firstName = "Apa",
            lastName = "Apansson",
            ssn = "11077941012",
            email = "apan@apanson.se",
            startDate = LocalDate.now(),
            price = null,
            currency = null,
            quoteIds = *arrayOf(norwayQuoteResponse1.id, norwayQuoteResponse2.id)
        )
        assertThat(signResponse2.statusCode.value()).isEqualTo(200)
    }

    @Test
    fun `Create Danish home content quote and sign it successfully`() {

        val ssn = "0411357627"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val peQueryPriceRequest = slot<PriceQueryRequest.DanishHomeContent>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "DKK"))

        // Create quote
        val quoteResponse = quoteClient.createDanishHomeContentQuote(
            birthdate = "1988-01-01",
            street = "ApStreet 995",
            apartment = "4",
            floor = "st",
            livingSpace = 122)

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("DKK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        // Sign quote
        val signResponse = quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Apan",
            lastName = "Apansson",
            ssn = ssn,
            email = "apan@apansson.se",
            startDate = today.toString()
        )

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.DENMARK)

        // Get quote
        val quote = quoteClient.getQuote(quoteResponse.id)!!

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("DKK")
        assertThat(quote.productType.name).isEqualTo("HOME_CONTENT")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(DanishHomeContentsData::class.java)

        val data = quote.data as DanishHomeContentsData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1988-01-01")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.street).startsWith("ApStreet")
        assertThat(data.city).isEqualTo("ApCity")
        assertThat(data.zipCode).isEqualTo("1234")
        assertThat(data.apartment).isEqualTo("4")
        assertThat(data.floor).isEqualTo("st")
        assertThat(data.livingSpace).isEqualTo(122)
        assertThat(data.coInsured).isEqualTo(1)
        assertThat(data.isStudent).isEqualTo(false)
        assertThat(data.type.name).isEqualTo("OWN")
        assertThat(data.internalId).isNull()

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("DENMARK")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address!!.street).startsWith("ApStreet")
            assertThat(captured.address!!.city).isEqualTo("ApCity")
            assertThat(captured.address!!.zipCode).isEqualTo("1234")
            assertThat(captured.address!!.apartmentNo).isEqualTo("4")
            assertThat(captured.address!!.floor).isEqualTo(0)
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1988-01-01")
        }

        // Validate request to Price Enging
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1988-01-01")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.housingType.name).isEqualTo("OWN")
            assertThat(captured.postalCode).isEqualTo("1234")
            assertThat(captured.squareMeters).isEqualTo(122)
            assertThat(captured.floor).isEqualTo("st")
            assertThat(captured.apartment).isEqualTo("4")
            assertThat(captured.street).startsWith("ApStreet")
            assertThat(captured.city).isEqualTo("ApCity")
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)

            val ppQuote = captured.quotes[0] as AgreementQuote.DanishHomeContentQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("DKK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.address.city).isEqualTo("ApCity")
            assertThat(ppQuote.address.postalCode).isEqualTo("1234")
            assertThat(ppQuote.address.country.name).isEqualTo("DK")
            assertThat(ppQuote.address.street).startsWith("ApStreet")
            assertThat(ppQuote.address.coLine).isEqualTo(null)
            assertThat(ppQuote.address.apartment).isEqualTo("4")
            assertThat(ppQuote.address.floor).isEqualTo("st")
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.coInsured[0].ssn).isNull()
            assertThat(ppQuote.squareMeters).isEqualTo(122)
            assertThat(ppQuote.lineOfBusiness.name).isEqualTo("OWN")
        }
    }

    @Test
    fun `Create Danish accident quote and sign it successfully`() {

        val ssn = "0411357627"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val peQueryPriceRequest = slot<PriceQueryRequest.DanishAccident>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "DKK"))

        // Create quote
        val quoteResponse = quoteClient.createDanishAccidentQuote(birthdate = "1988-01-01", street = "ApStreet 993", apartment = "4", floor = "st")

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("DKK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        // Sign quote
        val signResponse = quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Apan",
            lastName = "Apansson",
            ssn = ssn,
            email = "apan@apansson.se",
            startDate = today.toString()
        )

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.DENMARK)

        // Get quote
        val quote = quoteClient.getQuote(quoteResponse.id)!!

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("DKK")
        assertThat(quote.productType.name).isEqualTo("ACCIDENT")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(DanishAccidentData::class.java)

        val data = quote.data as DanishAccidentData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1988-01-01")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.street).startsWith("ApStreet")
        assertThat(data.city).isEqualTo("ApCity")
        assertThat(data.zipCode).isEqualTo("1234")
        assertThat(data.apartment).isEqualTo("4")
        assertThat(data.floor).isEqualTo("st")
        assertThat(data.coInsured).isEqualTo(1)
        assertThat(data.isStudent).isEqualTo(false)
        assertThat(data.internalId).isNull()

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("DENMARK")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address!!.street).startsWith("ApStreet")
            assertThat(captured.address!!.city).isEqualTo("ApCity")
            assertThat(captured.address!!.zipCode).isEqualTo("1234")
            assertThat(captured.address!!.apartmentNo).isEqualTo("4")
            assertThat(captured.address!!.floor).isEqualTo(0)
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1988-01-01")
        }

        // Validate request to Price Engine
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1988-01-01")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.postalCode).isEqualTo("1234")
            assertThat(captured.floor).isEqualTo("st")
            assertThat(captured.apartment).isEqualTo("4")
            assertThat(captured.street).startsWith("ApStreet")
            assertThat(captured.city).isEqualTo("ApCity")
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)

            val ppQuote = captured.quotes[0] as AgreementQuote.DanishAccidentQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("DKK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.address.city).isEqualTo("ApCity")
            assertThat(ppQuote.address.postalCode).isEqualTo("1234")
            assertThat(ppQuote.address.country.name).isEqualTo("DK")
            assertThat(ppQuote.address.street).startsWith("ApStreet")
            assertThat(ppQuote.address.coLine).isEqualTo(null)
            assertThat(ppQuote.address.apartment).isEqualTo("4")
            assertThat(ppQuote.address.floor).isEqualTo("st")
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.coInsured[0].ssn).isNull()
        }
    }

    @Test
    fun `Create Danish travel quote and sign it successfully`() {

        val ssn = "0411357627"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val peQueryPriceRequest = slot<PriceQueryRequest.DanishTravel>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "DKK"))

        // Create quote
        val quoteResponse = quoteClient.createDanishTravelQuote(
            birthdate = "1988-01-01",
            street = "ApStreet 992",
            apartment = "4",
            floor = "st"
        )

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("DKK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        // Sign quote
        val signResponse = quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Apan",
            lastName = "Apansson",
            ssn = ssn,
            email = "apan@apansson.se",
            startDate = today.toString()
        )

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.DENMARK)

        // Get quote
        val quote = quoteClient.getQuote(quoteResponse.id)!!

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("DKK")
        assertThat(quote.productType.name).isEqualTo("TRAVEL")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(DanishTravelData::class.java)

        val data = quote.data as DanishTravelData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1988-01-01")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.street).startsWith("ApStreet")
        assertThat(data.city).isEqualTo("ApCity")
        assertThat(data.zipCode).isEqualTo("1234")
        assertThat(data.apartment).isEqualTo("4")
        assertThat(data.floor).isEqualTo("st")
        assertThat(data.coInsured).isEqualTo(1)
        assertThat(data.isStudent).isEqualTo(false)
        assertThat(data.internalId).isNull()

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("DENMARK")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address!!.street).startsWith("ApStreet")
            assertThat(captured.address!!.city).isEqualTo("ApCity")
            assertThat(captured.address!!.zipCode).isEqualTo("1234")
            assertThat(captured.address!!.apartmentNo).isEqualTo("4")
            assertThat(captured.address!!.floor).isEqualTo(0)
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1988-01-01")
        }

        // Validate request to Price Engine
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1988-01-01")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.postalCode).isEqualTo("1234")
            assertThat(captured.floor).isEqualTo("st")
            assertThat(captured.apartment).isEqualTo("4")
            assertThat(captured.street).startsWith("ApStreet")
            assertThat(captured.city).isEqualTo("ApCity")
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)

            val ppQuote = captured.quotes[0] as AgreementQuote.DanishTravelQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("DKK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.address.city).isEqualTo("ApCity")
            assertThat(ppQuote.address.postalCode).isEqualTo("1234")
            assertThat(ppQuote.address.country.name).isEqualTo("DK")
            assertThat(ppQuote.address.street).startsWith("ApStreet")
            assertThat(ppQuote.address.coLine).isEqualTo(null)
            assertThat(ppQuote.address.apartment).isEqualTo("4")
            assertThat(ppQuote.address.floor).isEqualTo("st")
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.coInsured[0].ssn).isNull()
        }
    }
}
